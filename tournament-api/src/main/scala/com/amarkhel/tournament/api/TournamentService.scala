package com.amarkhel.tournament.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json._
import scalaz.std.list._
import scalaz.std.option._
import scalaz.syntax.traverse._
import com.softwaremill.quicklens._
import Util._
import com.amarkhel.mafia.common.Location

trait TournamentService extends Service {
  def createTournament: ServiceCall[Tournament, Tournament]
  def getTournament(name: String): ServiceCall[NotUsed, Option[Tournament]]
  def deleteTournament(name: String): ServiceCall[NotUsed, Boolean]
  def updateTournament: ServiceCall[Tournament, Tournament]
  def finishTournament(name: String): ServiceCall[NotUsed, Boolean]
  def startTournament(name:String): ServiceCall[NotUsed, Boolean]
  def getUserState(name:String, tournament:String): ServiceCall[NotUsed, Option[UserState]]

  def getTournaments: ServiceCall[NotUsed, Seq[Tournament]]
  def getTournamentsForUser(name:String): ServiceCall[NotUsed, Seq[Tournament]]
  def startGame(name:String, id:Int): ServiceCall[NotUsed, Boolean]
  def nextRound(name:String, user:String): ServiceCall[NotUsed, Boolean]
  def finishGame(name:String, id:Int): ServiceCall[NotUsed, Boolean]
  def joinTournament(name:String, user:String): ServiceCall[NotUsed, Boolean]
  def removeUser(name:String, user:String): ServiceCall[NotUsed, Boolean]
  def chooseMafia(name:String, user:String, player:String): ServiceCall[NotUsed, Boolean]
  def getAllSolutions: ServiceCall[NotUsed, Seq[SolutionResult]]
  def getSolutionsForPlayer(player:String): ServiceCall[NotUsed, Seq[SolutionResult]]
  def getSolutionsForId(id:Int): ServiceCall[NotUsed, Seq[SolutionResult]]
  def saveSolution(player:String): ServiceCall[(GameDescription, Solution), String]

  def descriptor = {
    import Service._
    named("tournament").withCalls(
      pathCall("/api/saveSolution/:player", saveSolution _),
      pathCall("/api/getAllSolutions", getAllSolutions _),
      pathCall("/api/getSolutionsPlayer/:player", getSolutionsForPlayer _),
      pathCall("/api/getSolutions/:id", getSolutionsForId _),
      pathCall("/api/tournament", createTournament),
      pathCall("/api/update", updateTournament),
      pathCall("/api/delete/:name", deleteTournament _),
      pathCall("/api/start/:name", startTournament _),
      pathCall("/api/finish/:name", finishTournament _),
      pathCall("/api/tournament/:name", getTournament _),
      pathCall("/api/tournaments/user/:name", getTournamentsForUser _),
      pathCall("/api/tournament/:tournament/state/:name", getUserState _),
      pathCall("/api/tournaments", getTournaments),
      pathCall("/api/tournament/:name/game/:id/start", startGame _),
      pathCall("/api/tournament/:name/:user/nextRound", nextRound _),
      pathCall("/api/tournament/:name/game/:id/finish", finishGame _),
      pathCall("/api/tournament/:name/join/:user", joinTournament _),
      pathCall("/api/tournament/:name/removeUser/:user", removeUser _),
      pathCall("/api/tournament/:name/:user/chooseMafia/:player", chooseMafia _)
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
case class Choice(name:String, round:Int, correct:Boolean, when:Int) {
  def asString = s"$name---$round---$correct---$when"
}
object Choice {
  def fromString(str:String) = {
    val arr = str.split("---")
    val name = arr.head
    val round = arr(1).toInt
    val correct = arr(2).toBoolean
    val when = arr(3).toInt
    Choice(name, round, correct, when)
  }
  implicit val format: Format[Choice] = Json.format
}

case class SolutionResult(id:Int, name:String, when:LocalDateTime, choices:List[Choice], points:Double)
object SolutionResult {
  implicit val format: Format[SolutionResult] = Json.format
}

object Util {

  def locations = Location.values.map(l => (l.name -> l.name))
  def formatSeconds(seconds: Long): String = {
    val minutes = seconds / 60
    val sec = seconds - minutes * 60
    val m = if(minutes < 10) "0" + minutes else minutes
    val s = if(sec < 10) "0" + sec else sec
    s"$m:$s"
  }

  def trimD(d:Double) = BigDecimal(d).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  def formatD(time:LocalDateTime) = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

  def idCondition(id:Int):{def id: Int} => Boolean = _.id == id
  def nameCondition(name:String):UserState => Boolean = _.name == name
  val timeModification: Any => Option[LocalDateTime] = _ => Some(LocalDateTime.now)
  def correctGuesses(pair:(String, GameDescription,Map[String, (Int, Int)])) = {
    val (_, game, solution) = pair
    solution.filter(s => game.mafias.intersect(solution.keys.toList).contains(s._1))
  }

  def calculatePoints(pair:(String, GameDescription,Map[String, (Int, Int)])) = {
    val (name, game, solution) = pair
    val count = game.countMafia
    val rounds = game.countRounds
    val percentForOne = 100.toDouble / count
    val percentForRound = 100.toDouble / rounds
    val points = for {
      p <- solution if game.mafias.contains(p._1)
    } yield (100 - percentForRound * p._2._1) * percentForOne / 100
    name -> (if(points.isEmpty) 0.0 else points.sum)
  }
}

case class Tournament(name: String, countPlayers:Int = 0, creator:String = "", players: List[UserState] = List.empty, games: List[GameDescription] = List.empty, created:LocalDateTime = LocalDateTime.now, start: Option[LocalDateTime] = None, finish:Option[LocalDateTime] = None, gameExpirationTime:Int = 1){
  def isSolutionCompleted(id: Int, name: String): Boolean = solutionCompleted(players.filter(_.name == name).head.getById(id).get)

  def wasStarted(id: Int): Boolean = games.exists(g => g.id == id && g.started.isDefined)
  def gameExist(id: Int) = games.exists(_.id == id)

  def countFinishedGames = games.count(!_.finished.isEmpty)

  def joined(player:String) = players.exists(_.name == player)
  private def haveSolution(user:String, op: Solution => Boolean) = {
    (for {
      player <- findPlayer(user)
      solution <- player.getById(inProgressGameId)
    } yield op(solution)).getOrElse(false)
  }

  def alreadyVoted(user: String, pl: String): Boolean = haveSolution(user, _.mafia.contains(pl))
  def isGameFinishedForUser(user:String) = haveSolution(user, _.isFinished || shouldFinishCurrentGame)

  def startGame(gameId: Int): Tournament = {
    this
      .modify(_.games.eachWhere(idCondition(gameId)).started).using(timeModification)
      .modify(_.players.each.solutions).using(Solution(gameId, Map.empty, 0, isFinished = false) :: _)
  }

  def finishTournament: Tournament = copy(finish = Some(LocalDateTime.now))

  def finishGame(id: Int): Tournament = this.modify(_.games.eachWhere(idCondition(id)).finished).using(timeModification)

  def nextRound(pl: String, id: Int): Tournament = {
    this.modify(_.players.eachWhere(nameCondition(pl)).solutions.eachWhere(idCondition(id))).using(s => {
      s.copy(currentRound = s.currentRound + 1, isFinished = s.currentRound + 1 == gameInProgress.get.countRounds)
    })
  }

  def chooseMafia(pl: String, maf: String, id: Int, time:Int): Tournament = {
    this.modify(_.players.eachWhere(nameCondition(pl)).solutions.eachWhere(idCondition(id))).using(s => {
      s.copy(mafia = s.mafia + (maf -> (s.currentRound, time)), isFinished = s.mafia.size + 1 == gameInProgress.get.countMafia)
    })
  }

  def startTournament: Tournament = this.modify(_.start).using(timeModification)
  def join(user: String): Tournament = copy(players = UserState(user, List.empty) :: players)
  def remove(user: String): Tournament = copy(players = players.filter(_.name != user))
  def allPlayersJoined: Boolean = countJoinedPlayers == countPlayers
  def allGamesCompleted = games.forall(_.isFinished) && started

  private def joinGamesWithSolutions = {
    for {
      game <- games
      player <- players
      solution <- player.solutions
      if game.id == solution.id
    } yield (player.name, game, solution.mafia)
  }

  def stat = {
    val players = joinGamesWithSolutions.map(calculatePoints).groupBy(_._1).map(a => a._1 -> a._2.map(_._2))
    (for {
      (player, points) <- players
      sum = points.sum
      max = points.max
      min = points.min
      avg = sum / points.size
      correct = joinGamesWithSolutions.filter(_._1 == player).flatMap(correctGuesses)
      minCorrect = if(correct.isEmpty) 0 else correct.map(_._2._1).min
    } yield (player, points.size, sum, max, min, avg, correct, minCorrect, points)).toList
  }

  def havePlayer(user: String): Boolean = findPlayer(user).isDefined
  def hasGameInProgress = gameInProgress.isDefined
  def gameInProgress : Option[GameDescription] = games.find(_.inProgress)
  def inProgressGameId = gameInProgress.map(_.id).get
  def countGames = games.size
  def countJoinedPlayers = players.size
  def playersString = players.map(_.name).mkString(",")
  def winners = stat.map(x => x._1 -> x._3).sortBy(_._2)
  def inProgress = started && !finished
  def findPlayer(user: String) = players.find(_.name == user)
  def started = start.isDefined
  def finished = finish.isDefined

  def allVotesCollected(s: Solution): Boolean = s.mafia.size == gameInProgress.get.countMafia
  def lastRoundReached(s:Solution) = s.currentRound == gameInProgress.get.countRounds

  def currentGameState = {
    for {
      game <- gameInProgress
      state = players.map(pl => (pl.name -> pl.getById(game.id).get))
    } yield state
  }

  def solutionCompleted: Solution => Boolean = {
    s => s.isFinished || allVotesCollected(s) || lastRoundReached(s)
  }

  def shouldFinishCurrentGame = {
    (for {
      game <- gameInProgress
      solutions <- players.map(_.getById(game.id)).sequence
    } yield solutions.forall(solutionCompleted)).getOrElse(false)
  }
}

object Tournament {
  implicit val format: Format[Tournament] = Json.format
}

case class UserState(name:String, solutions:List[Solution]){
  def nextRound(id:Int) = this.modify(_.solutions.eachWhere(idCondition(id)).currentRound).using(_ + 1)
  def choose(player:String, id:Int, time:Int) = {
    this.modify(_.solutions.eachWhere(idCondition(id))).using(s => s.copy(mafia = s.mafia + (player -> (s.currentRound, time))))
  }
  def getById(id:Int) = solutions.find(_.id == id)
}

object UserState {
  implicit val format: Format[UserState] = Json.format
}

case class GameDescription(id:Int, location:String, countPlayers:Int, countRounds:Int, mafias:List[String], expirationTime:Int, started:Option[LocalDateTime] = None, finished:Option[LocalDateTime] = None){
  def countMafia:Int = mafias.size
  def expired = started.isDefined && LocalDateTime.now().isAfter(started.get.plusHours(expirationTime))
  def inProgress: Boolean = started.isDefined && finished.isEmpty
  def isFinished: Boolean = finished.isDefined
  def timeToEnd: String = {
    val now = LocalDateTime.now()
    import java.time.temporal.ChronoUnit
    val seconds = 3600 - ChronoUnit.SECONDS.between(started.get, now)
    Util.formatSeconds(seconds)
  }
}

object GameDescription {
  implicit val format: Format[GameDescription] = Json.format
}

case class Solution(id:Int, mafia:Map[String, (Int,Int)], currentRound:Int, isFinished:Boolean)
object Solution {
  implicit val format: Format[Solution] = Json.format
}