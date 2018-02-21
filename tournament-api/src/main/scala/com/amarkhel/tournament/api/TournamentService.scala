package com.amarkhel.tournament.api

import java.time.LocalDateTime

import akka.NotUsed
import com.amarkhel.mafia.common.Location
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json._
import com.amarkhel.mafia.utils.JsonFormats._

trait TournamentService extends Service {
  def createTournament: ServiceCall[Tournament, Tournament]
  def getTournament(name: String): ServiceCall[NotUsed, Option[Tournament]]
  def deleteTournament(name: String): ServiceCall[NotUsed, Boolean]
  def finishTournament(name: String): ServiceCall[NotUsed, Boolean]
  def startTournament(name:String): ServiceCall[NotUsed, Boolean]

  def getTournaments: ServiceCall[NotUsed, Seq[Tournament]]
  def getTournamentsForUser(name:String): ServiceCall[NotUsed, Seq[Tournament]]
  def startGame(name:String, user:String, id:Int): ServiceCall[NotUsed, Boolean]
  def nextRound(name:String, user:String): ServiceCall[NotUsed, Boolean]
  def expireGame(name:String, user:String): ServiceCall[NotUsed, Boolean]
  def joinTournament(name:String, user:String): ServiceCall[NotUsed, Boolean]
  def chooseMafia(name:String, user:String, player:String): ServiceCall[NotUsed, Boolean]

  def descriptor = {
    import Service._
    named("tournament").withCalls(
      pathCall("/api/tournament", createTournament),
      pathCall("/api/delete/:name", deleteTournament _),
      pathCall("/api/finish/:name", finishTournament _),
      pathCall("/api/tournament/:name", getTournament _),
      pathCall("/api/tournaments", getTournaments),
      pathCall("/api/tournament/:name/:user/game/:id/start", startGame _),
      pathCall("/api/tournament/:name/:user/game/nextRound", nextRound _),
      pathCall("/api/tournament/:name/:user/game/expire", expireGame _),
      pathCall("/api/tournament/:name/join/:user", joinTournament _),
      pathCall("/api/tournament/:name/:user/game/chooseMafia/:player", chooseMafia _)
    )
  }
  import play.api.libs.json._

  implicit def optionReads[T: Format]: Reads[Option[T]] = Reads {
    case JsNull => JsSuccess(None)
    case other => other.validate[T].map(Some.apply)
  }

  implicit def optionWrites[T: Format]: Writes[Option[T]] = Writes {
    case None => JsNull
    case Some(t) => Json.toJson(t)
  }
}

case class Tournament(name: String, duration:Int, countPlayers:Int, creator:String, players: List[UserState], games: List[GameDescription], created:LocalDateTime, start: Option[LocalDateTime], finish:Option[LocalDateTime]){

  def countGames = games.size
  def countJoinedPlayers = players.size
  def isMatch = countPlayers == 2 && countGames == 1
  def winners = {
    players.map(x => x.name -> x.stat._1).sortBy(_._2)
  }
  def inProgress = {
    started && !finished
  }

  def expired = {
    if (finished) false
    else {
      if(started) {
        LocalDateTime.now().isAfter(start.get.plusDays(duration))
      }
      else false
    }
  }

  def findPlayer(user: String) = {
    players.find(_.name == user)
  }

  def started = start.isDefined
  def finished = finish.isDefined
}

object Tournament {
  implicit val format: Format[Tournament] = Json.format
}

case class UserState(name:String, games:List[GameResult]){
  def findStartedGame = {
    games.find(_.inProgress).headOption
  }

  def findGame(id:Int) = {
    games.find(_.id == id)
  }

  def stat = {
    val points = games.map(_.calculatePoints)
    val sum = points.sum
    val max = points.max
    val avg = sum / games.size
    val correct: Seq[(String, Int)] = games.map(_.correct).flatten
    val minCorrect = correct.map(_._2).min
    //val fastest
    (sum, max, avg, correct, minCorrect, points)
  }
}
object UserState {
  implicit val format: Format[UserState] = Json.format
}

case class GameDescription(id:Int, location:Location, countPlayers:Int, countRounds:Int, countMafia:Int, mafias:List[String])
object GameDescription {
  implicit val format: Format[GameDescription] = Json.format
}

case class GameResult(id:Int, currentRound:Int, game:GameDescription, solution:Solution, started:Option[LocalDateTime]) {
  def correct = {
    solution.mafia.filter( k => {
      game.mafias.contains(k._1)
    }).toList
  }

  def expired = {
    started.isDefined && LocalDateTime.now().isAfter(started.get.plusHours(1))
  }

  def calculatePoints = {
    val count = game.countMafia
    val rounds = game.countRounds
    val percentForOne = 100.toDouble / count
    val percentForRound = 100.toDouble / rounds
    val points = for {
      p <- solution.mafia if(game.mafias.contains(p._1))
    } yield (100 - percentForRound*p._2) * percentForOne
    points.sum
  }

  def choose(pl: String) = {
    if(solution.mafia.contains(pl)) throw new IllegalArgumentException()
    else {
      val players = solution.mafia + (pl -> currentRound)
      val solutionStatus = if(players.size == game.countMafia) SolutionStatus.FINISHED else SolutionStatus.INPROGRESS
      this.copy(solution = solution.copy(mafia = players, status = solutionStatus))
    }
  }

  def expire = {
    this.copy(solution = solution.copy(status = SolutionStatus.EXPIRED))
  }

  def nextRound = {
    if(game.countPlayers == currentRound){
      throw new IllegalArgumentException("Game already reached final round")
    } else {
      val solutionStatus = if(game.countPlayers == currentRound) SolutionStatus.FINISHED else SolutionStatus.INPROGRESS
      this.copy(currentRound = currentRound + 1, solution = solution.copy(status = solutionStatus))
    }
  }

  def inProgress: Boolean = started.isDefined && solution.status == SolutionStatus.INPROGRESS

  def notStarted = !started.isDefined
}

object GameResult {
  implicit val format: Format[GameResult] = Json.format
}

case class Solution(mafia:Map[String, Int], status:SolutionStatus)
object Solution {
  implicit val format: Format[Solution] = Json.format
}

sealed trait SolutionStatus{def name:String}

object SolutionStatus extends Serializable {
  case object AWAITING extends SolutionStatus{val name="Не начат"}
  case object INPROGRESS extends SolutionStatus{val name="В процессе"}
  case object FINISHED extends SolutionStatus{val name="Успешно завершен"}
  case object EXPIRED extends SolutionStatus{val name="Срок истек"}

  val values:List[SolutionStatus] = List(AWAITING, INPROGRESS, FINISHED, EXPIRED)

  def get(name:String):SolutionStatus = {
    require(name != null)
    values.find(name contains _.name).getOrElse({
      //log.error(s"Location $name not found")
      AWAITING
    })
  }
  implicit val statusReads: Reads[SolutionStatus] = Reads {
    case JsString(s) => JsSuccess(SolutionStatus.get(s))
  }

  implicit val statusWrites: Writes[SolutionStatus] = Writes { status =>
    JsString(status.name)
  }
}