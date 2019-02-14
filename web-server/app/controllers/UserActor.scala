package controllers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import akka.actor._
import com.amarkhel.mafia.common._
import com.amarkhel.mafia.parser.MessagePrinter
import com.amarkhel.mafia.service.api.MafiaService
import com.amarkhel.tournament.api.{GameDescription, Solution, TournamentService, Util}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import GameMessage._

object UserActor {
  def props(out: ActorRef, gameId:String, mafiaService:MafiaService, tournamentService:TournamentService, tournamentName:String, user:String, trackCompletion:Boolean = true)(implicit ec:ExecutionContext) = Props(new UserActor(out, gameId.toInt, mafiaService, tournamentService, tournamentName, user, trackCompletion))
}

class UserActor(out: ActorRef, gameId:Int, mafiaService:MafiaService, tournamentService:TournamentService, tournamentName:String, user:String, trackCompletion:Boolean = true)(implicit ec:ExecutionContext) extends Actor {

  var started:String = _
  var gameDescription:GameDescription = _
  var events:List[GameEvent] = _
  var countRounds:Int = _
  var mafia:List[String] = List.empty
  var state:TournamentGameState = _

  private def getEvents(round:Int) = {
    var count = 0
    events.takeWhile({ e =>
      if(e.isInstanceOf[RoundStarted]){
        count = count + 1
      }
      count <= round
    }).map(GameEvent.cutSmiles)
  }

  private def eliminatedPlayers(events:List[GameEvent]) = {
    events.map(
      _ match {
        case Timeouted(player, _) => Some(player)
        case Killed(player, _) => Some(player)
        case Prisoned(player, _) => Some(player)
        case _ => None
      }
    ).filter(!_.isEmpty).map(_.get)
  }

  private def filterEvents = {
    var count = 0
    val filtered = events.dropWhile({ e =>
      if(e.isInstanceOf[RoundStarted]){
        count = count + 1
      }
      count < state.currentRound
    })
    filtered.head +: filtered.tail.takeWhile(!_.isInstanceOf[RoundStarted]).map(GameEvent.cutSmiles)
  }

  override def postStop() = println(s"Actor closed for $gameId and player $user")
  override def preStart(): Unit = {
    super.preStart()
    val gameFuture = mafiaService.loadGame(gameId, -1).invoke()
    val game = Await.result(gameFuture, Duration.Inf)
    countRounds = game.countRounds
    events = game.events
    mafia = game.players.filter(_.isMafia).map(_.name)
    gameDescription = GameDescription(game.id, game.location.name, game.playersSize, game.countRounds, game.players.filter(_.isMafia).map(_.name), 1, None, None)
    if(!tournamentName.isEmpty){
      val t = Await.result(tournamentService.getTournament(tournamentName).invoke(), Duration.Inf).get
      val currentRound = t.findPlayer(user).get.getById(t.gameInProgress.get.id).get.currentRound
      val chosen = t.findPlayer(user).get.getById(t.gameInProgress.get.id).get.mafia
      started = t.gameInProgress.get.started.get.format(DateTimeFormatter.ISO_DATE_TIME)
      state = TournamentGameState(tournamentName, user, game.players.map(_.name), currentRound, chosen, timeToEnd, eliminatedPlayers(getEvents(currentRound)), game.players.count(_.isMafia))
    } else {
      started = LocalDateTime.now.format(DateTimeFormatter.ISO_DATE_TIME)
      state = TournamentGameState(tournamentName, user,  game.players.map(_.name), 0, Map.empty, timeToEnd, List.empty, game.players.count(_.isMafia))
    }
  }

  def timeToEnd: String = {
    val start = LocalDateTime.parse(started, DateTimeFormatter.ISO_DATE_TIME)
    val now = LocalDateTime.now()
    import java.time.temporal.ChronoUnit
    val seconds = 3600 - ChronoUnit.SECONDS.between(start, now)
    val minutes = seconds / 60
    val sec = seconds - minutes * 60
    val m = if(minutes < 10) "0" + minutes else minutes
    val s = if(sec < 10) "0" + sec else sec
    if(minutes <0 || seconds < 0) "error" else s"$m:$s"
  }

  def receive = {
    case msg:String if(msg == "NEXT") => {
      state = state.next
      if(!tournamentName.isEmpty){
        Await.result(tournamentService.nextRound(tournamentName, user).invoke(), Duration.Inf)
      }
      if(state.currentRound >= countRounds) {
        val correct = Util.correctGuesses((user, gameDescription, state.chosen))
        val points = Util.calculatePoints((user, gameDescription, state.chosen))
        if(trackCompletion){
          Await.result(tournamentService.saveSolution(user).invoke((gameDescription, Solution(gameDescription.id, state.chosen, state.currentRound, true))), Duration.Inf)
        }
        out ! MessagePrinter.format(FINISHED(points._2, correct.size, state, mafia.mkString(",")))
      }
      else {
        val e = filterEvents
        val elim = state.eliminatedPlayers ++: eliminatedPlayers(e)
        state = state.copy(eliminatedPlayers = elim)
        out ! MessagePrinter.format(Events(e, state))
      }
    }
    case msg:String if(msg == "INIT") => {
      if(countRounds == state.currentRound) {
        val correct = Util.correctGuesses((user, gameDescription, state.chosen))
        val points = Util.calculatePoints((user, gameDescription, state.chosen))
        out ! MessagePrinter.format(FINISHED(points._2, correct.size, state, mafia.mkString(",")))
      }
      else out ! MessagePrinter.format(Events(getEvents(state.currentRound), state))
    }
    case msg:String if(msg == "TIME") => {
      val t = timeToEnd
      if (t == "error") {
        val correct = Util.correctGuesses((user, gameDescription, state.chosen))
        val points = Util.calculatePoints((user, gameDescription, state.chosen))
        if(trackCompletion){
          Await.result(tournamentService.saveSolution(user).invoke((gameDescription, Solution(gameDescription.id, state.chosen, state.currentRound, true))), Duration.Inf)
        }
        out ! MessagePrinter.format(FINISHED(points._2, correct.size, state, mafia.mkString(",")))
      } else {
        out ! MessagePrinter.format(TIME(state.copy(started = timeToEnd)))
      }
    }
    case msg:String if(msg.contains("CHOOSE")) => {
      val m = msg.split("----")
      val chosen = m.last
      val start = LocalDateTime.parse(started, DateTimeFormatter.ISO_DATE_TIME)
      state = state.copy(chosen = state.chosen + (chosen -> (state.currentRound, ChronoUnit.SECONDS.between(start, LocalDateTime.now).toInt)))
      if(!tournamentName.isEmpty){
        Await.result(tournamentService.chooseMafia(tournamentName, user, chosen).invoke(), Duration.Inf)
      }
      if(state.countPossibleChoices == 0) {
        val correct = Util.correctGuesses((user, gameDescription, state.chosen))
        val points = Util.calculatePoints((user, gameDescription, state.chosen))
        if(trackCompletion) {
          Await.result(tournamentService.saveSolution(user).invoke((gameDescription, Solution(gameDescription.id, state.chosen, state.currentRound, true))), Duration.Inf)
        }
        out ! MessagePrinter.format(FINISHED(points._2, correct.size, state, mafia.mkString(",")))
      }
      else out ! MessagePrinter.format(CONFIRM(state))
    }
    case _ => println("Unrecognized message")
  }

}
