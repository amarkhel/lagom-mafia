package controllers

import akka.actor._
import com.amarkhel.mafia.common.{Game, GameEvent, RoundStarted, TournamentGameState}
import com.amarkhel.mafia.parser.MessagePrinter
import com.amarkhel.mafia.service.api.MafiaService
import com.amarkhel.tournament.api.TournamentService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object UserActor {
  def props(out: ActorRef, gameId:String, mafiaService:MafiaService, tournamentService:TournamentService, tournamentName:String, user:String)(implicit ec:ExecutionContext) = Props(new UserActor(out, gameId.toInt, mafiaService, tournamentService, tournamentName, user))
}

class UserActor(out: ActorRef, gameId:Int, mafiaService:MafiaService, tournamentService:TournamentService, tournamentName:String, user:String)(implicit ec:ExecutionContext) extends Actor {

  var events:List[GameEvent] = _
  var countRounds:Int = _
  var currentRound = 0
  var chosen:Map[String, Int] = Map.empty
  var mafia:List[String] = List.empty
  var state = TournamentGameState(tournamentName, user, List.empty, 0, Map.empty, "", false)

  override def postStop() = println(s"Actor closed for $gameId and player $user")
  override def preStart(): Unit = {
    super.preStart()
    val gameFuture = mafiaService.loadGame(gameId, -1).invoke()
    val game = Await.result(gameFuture, Duration.Inf)
    countRounds = game.countRounds
    events = game.events
    mafia = game.players.filter(_.isMafia).map(_.name)
    val t = Await.result(tournamentService.getTournament(tournamentName).invoke(), Duration.Inf).get
    currentRound = t.findPlayer(user).get.getById(t.gameInProgress.get.id).get.currentRound
    chosen = t.findPlayer(user).get.getById(t.gameInProgress.get.id).get.mafia
  }

  def receive = {
    case msg:String if(msg == "NEXT") => {
      currentRound = currentRound + 1
      Await.result(tournamentService.nextRound(tournamentName, user).invoke(), Duration.Inf)
      state = if(countRounds == currentRound) state.finish else state.next
      out ! MessagePrinter.format(state.withEvents(events))
    }

    case msg:String if(msg.contains("CHOOSE")) => {
      val m = msg.split("----")
      val chosen = m.last
      Await.result(tournamentService.chooseMafia(tournamentName, user, chosen).invoke(), Duration.Inf)
      out ! MessagePrinter.format(state.choose(chosen).withEvents(events))
    }
    case _ => println("Unrecognized message")
  }
}
