package com.amarkhel.mafia.common

import java.time.{LocalDate, LocalDateTime, LocalTime}

import com.amarkhel.mafia.dto.GameContent
import com.amarkhel.mafia.parser.Parser
import play.api.libs.json.Json
import com.amarkhel.mafia.utils.JsonFormats._
import prickle.{CompositePickler, PicklerPair}

import scala.collection.mutable

sealed trait GameEvent{def time:Int}
object GameEvent {
  import prickle._
  def cutSmiles(mess:GameEvent) = mess match {
    case MessageSent(text, time) => {
      val pattern = """<img src="https://st.mafiaonline.ru/images/smiles/(.+?).gif"[\s\S]*">""".r
      val converted = pattern.replaceAllIn(text, m => s"XXXXXX${m.group(1)}XXXXXX")
      MessageSent(converted, time)
    }
    case other:GameEvent => other
  }

  implicit val pickler: PicklerPair[GameEvent] = CompositePickler[GameEvent].concreteType[GameResultRendered].
    concreteType[GameStarted].concreteType[GameCompleted].concreteType[Voted].concreteType[Prisoned].concreteType[Killed]
    .concreteType[Timeouted].concreteType[RoundEnded].concreteType[RoundStarted].concreteType[OmonHappened].concreteType[RecoveredByDoctor]
    .concreteType[MessageSent].concreteType[PrivateMessageSent].concreteType[MafiaNotKilled].concreteType[GameStopped]
    .concreteType[EarnedMaf].concreteType[SumrakVoted]
}
case class GameStarted(loc:Location, start:String, players:List[String], time:Int) extends GameEvent{
  def date = LocalDateTime.parse(start)
}
case class GameCompleted(message:String, time:Int) extends GameEvent
case class GameResultRendered(players:List[Gamer], time:Int) extends GameEvent
case class RoundStarted(kind:RoundType, time:Int) extends GameEvent
case class RoundEnded(time:Int) extends GameEvent
case class MessageSent(message:String, time:Int) extends GameEvent
case class Voted(target:String, destination:String, time:Int) extends GameEvent
case class SumrakVoted(target:String, destination:String, time:Int, damage:Int) extends GameEvent
case class Killed(player:Gamer, time:Int) extends GameEvent
case class RecoveredByDoctor(time:Int) extends GameEvent
case class Prisoned(player:Gamer, time:Int) extends GameEvent
case class Timeouted(player:Gamer, time:Int) extends GameEvent
case class OmonHappened(time:Int) extends GameEvent
case class PrivateMessageSent(from:String, to:String, time:Int) extends GameEvent
case class GameStopped(avtor:String, time:Int) extends GameEvent
case class MafiaNotKilled(time:Int) extends GameEvent
case class EarnedMaf(player:String, amount:Double, time:Int) extends GameEvent

sealed trait FinishStatus

object FinishStatus{
  implicit val pickler: PicklerPair[FinishStatus] = CompositePickler[FinishStatus].
    concreteType[OK.type].concreteType[STOPPED.type].concreteType[EMPTY_LOG.type]
}


case object OK extends FinishStatus
case object STOPPED extends FinishStatus
case object EMPTY_LOG extends FinishStatus

case class Game(id: Int, events:List[GameEvent], status:FinishStatus, players:List[Gamer], countRounds:Int){
  def averageMessagesPerRound: Double = {
    val r = events.count(_.isInstanceOf[RoundStarted])
    val m = events.count(_.isInstanceOf[MessageSent])
    m.toDouble / r
  }

  def day = Day(firstEvent.date.getYear, firstEvent.date.getMonthValue, firstEvent.date.getDayOfMonth)

  lazy val roundSize = events.count(_.isInstanceOf[RoundStarted])
  lazy val result = lastEvent.asInstanceOf[GameCompleted].message match {
    case "Вся мафия убита" => Result.GOROD_WIN
    case "Ничья" => Result.DRAW
    case "Мафия победила" => Result.MAFIA_WIN
  }
  lazy val tournamentResult = {
    events.contains(OmonHappened) match {
      case true => {
         val eventsBeforeOmon = events.takeWhile(_.isInstanceOf[OmonHappened])
         val countDead = eventsBeforeOmon.count(_.isInstanceOf[Killed]) + eventsBeforeOmon.count(_.isInstanceOf[Prisoned]) + eventsBeforeOmon.count(_.isInstanceOf[Timeouted])
         val aliveMafia = (playersSize - countDead) / 2
        aliveMafia match {
          case 1 => TournamentResult.OMON_1
          case 2 => TournamentResult.OMON_2
          case 3 => TournamentResult.OMON_3
          case 4 => TournamentResult.OMON_4
        }
      }
      case false => {
        lastEvent.asInstanceOf[GameCompleted].message match {
          case "Вся мафия убита" => TournamentResult.GOROD_WIN
          case "Мафия победила" => TournamentResult.MAFIA_WIN
          case "Ничья" => TournamentResult.DRAW
        }
      }
    }
  }
  private lazy val firstEvent = events.head.asInstanceOf[GameStarted]
  private lazy val lastEvent = events.filter(_.isInstanceOf[GameCompleted]).last.asInstanceOf[GameCompleted]
  def playersSize = firstEvent.players.size

  def location = firstEvent.loc

  def start = firstEvent.start

  def finish = firstEvent.date.plusSeconds(lastEvent.time)

  def toPlayersString: String = {
    players.map(p => s"name-${p.name};role-${p.role.role.head}").mkString(",")
  }

  //def countRounds = events.filter(_.isInstanceOf[RoundStarted]).size
}

object Game {
  implicit val format = Json.format[Game]

  def emptyGame(content: GameContent) = Game(content.id, Parser.defaultEvents(content, content.finish), EMPTY_LOG, List.empty, 0)

  def stoppedGame(id:Int, location:String, date:LocalDate) = {
    val dateTime = LocalDateTime.of(date, LocalTime.now)
    Game(id, Parser.defaultEvents(GameContent(id, Location.get(location), List.empty, dateTime, List.empty), dateTime), STOPPED, List.empty, 0)
  }
}