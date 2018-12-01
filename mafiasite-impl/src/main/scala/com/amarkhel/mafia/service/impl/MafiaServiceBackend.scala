package com.amarkhel.mafia.service.impl

import akka.actor.ActorSystem
import com.amarkhel.mafia.common.Game._
import com.amarkhel.mafia.common._
import com.amarkhel.mafia.dto.GameContent
import com.amarkhel.mafia.parser.Parser
import com.amarkhel.mafia.utils.TextUtils._
import com.amarkhel.mafia.utils.TimeUtils.{format, _}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._
import MafiaServiceBackend._
import com.amarkhel.mafia.common.{GameEvent => GE}
import com.typesafe.config.ConfigFactory

class MafiaServiceBackend(registry: PersistentEntityRegistry, system: ActorSystem, mafiaSite:MafiaHubAPI)(implicit ec: ExecutionContext){

  private val ignoredGames = ConfigFactory.load().getIntList("invalidGames")

  def clearAll = {
    extractorEntity.ask(ClearCommand)
    Future.successful(akka.Done)
  }

  def status = {
    extractorEntity.ask(GetStatusCommand).map(s => {
      log.info(s" ${s.year}/${s.month}/${s.day} is last handled day")
      s
    })
  }

  def loadDay(day: Day) = {
    dayEntity(day).ask(LoadDay).flatMap {
        case Some(x) => Future(x)
        case None => saveDay(day)
    }.map(convertDay).transform(handleSuccess, handleError(s"Day $day is not found: "))
  }

  def events: Topic[Game] = TopicProducer.taggedStreamWithOffset(GameEvent.Tag.allTags.to[collection.immutable.Seq]) { (tag, offset) =>
    registry.eventStream(tag, offset).filter(e => e.event.isInstanceOf[GameSaved])
      .mapAsync(1) { event =>
        event.event match {
          case GameSaved(game) => {
            log.debug(game.id + " is consumed by topic")
            Future.successful(game, offset)
          }
        }
      }
  }

  def errors: Topic[String] = TopicProducer.taggedStreamWithOffset(ExtractorEvent.Tag.allTags.to[collection.immutable.Seq]) { (tag, offset) =>
    registry.eventStream(tag, offset).filter(e => e.event.isInstanceOf[Error])
      .mapAsync(1) { event =>
        event.event match {
          case Error(err) => {
            log.error("Error in topic " + err)
            Future.successful(err, offset)
          }
        }
      }
  }

  def loadGame(id: Int, lastRound:Int): Future[Game] = {
    val result = if(ignoredGames.contains(id)) {
      notFound(new Exception("Game is invalid"), s"Game $id is invalid:")
    } else {
      gameEntity(id).ask(LoadGame).flatMap {
        case Some(game) => Future(dropToRound(game, lastRound))
        case None => fetchGame(id).map {
          case \/-(game) => dropToRound(game, lastRound)
          case -\/(error) => notFound(error, s"Game $id not found:")
        }
      }
    }
    result.transform(handleSuccess, handleError(s"Game $id not found:"))
  }

  private def dropToRound(game:Game, round:Int) = {
    def collect(r: Int, events: List[GE], collected: List[GE]): List[GE] = {
      events match {
        case RoundStarted(_, _) :: _ if r == 0 =>
          collected
        case RoundStarted(a, b) :: y if r > 0 =>
          collect(r - 1, y, RoundStarted(a, b) :: collected)
        case x :: y =>
          collect(r, y, x :: collected)
        case List() =>
          collected
      }
    }
    val filtered = if (round < 0) game.events else collect(round, game.events, List()).reverse
    game.copy(events = filtered)
  }

  private def handleSuccess[T] = (x:T) => x

  private def handleError(message:String) = (err:Throwable) => {
    val error = message + exceptionToStr(err)
    log.error(error)
    extractorEntity.ask(LoadError(error))
    throw NotFound(error)
  }

  private def saveDay(day: Day) = {
    fetchDay(day).map {
      case -\/(err) => notFound(err, s"day $day not found:")
      case \/-(content) =>
        if (!isToday(day)) dayEntity(day).ask(SaveDay(content))
        content
    }
  }

  private def fetchGame(id: Int) = {
    loadGameSource(id) map {
      _ flatMap { content =>
        val result = parseGame(content)
        result.map(saveGame(id, _))
        result
      }
    }
  }

  private def saveGame(id: Int, result: Game) = gameEntity(id).ask(SaveGame(result))

  private def parseGame(content: GameContent) = {
    if (content.chat.isEmpty) \/-(emptyGame(content))
    else Parser.parse(content)
  }

  private def fetchDay(day: Day) = mafiaSite.loadDay(day).map(_.flatMap(parseDay))

  private def parseDay(doc: Document) = {
    checkSelector[String](TABLE_SELECTOR, NO_TABLES_FOUND) {
      table => \/-(table.head.toString)
    }(doc)
  }

  private def convertDay(content: String) = {
    val doc = Jsoup.parse(content)
    val rows = doc.select("tr")
    rows.map(_.toString).toList
  }

  private def loadGameSource(id: Int) = {
    log.debug("loading game " + id)
    mafiaSite.loadGame(id) map {
      _ flatMap (implicit doc => extractContent(id))
    }
  }

  private def extractContent(id: Int)(implicit doc: Document) = {
    checkSelector[GameContent](GAME_SUMMARY_SELECTOR, HEADER_CSS_CHANGED) {
      contentArea => {
        FINISH_REGEX.findFirstMatchIn(contentArea.text) match {
          case Some(data) => for {
            gamers <- extractGamers
            messages <- handleChat
            finish = format(data.group(3))
            location = Location.get(data.group(2).trim)
          } yield GameContent(id, location, gamers, finish, messages)
          case None => fail[GameContent](HEADER_FORMAT_WRONG)
        }
      }
    }
  }

  private def extractGamers(implicit doc: Document) = {
    checkSelector[List[Gamer]](PLAYERS_SELECTOR, PLAYERS_EXTRACT_ERROR) {
      gamersTable => {
        val rows = gamersTable.first.select("tr")
        val gamers = rows.toList.map(grabGamer)
        gamers.sequenceU
      }
    }
  }

  private def handleChat(implicit doc: Document) = {
    checkSelector[List[String]](CHAT_SELECTOR, NO_MESSAGES_ERROR) {
      chat => {
        \/-(chat
          .head
          .childNodes
          .mkString(" ")
          .replaceAll("<br>", "&br&")
          .replaceAll("&nbsp;", " ")
          .erase("<hr>")
          .split("&br&")
          .map(_.trim)
          .filter(!_.isEmpty)
          .toList)
      }
    }
  }

  private def grabGamer(elem: Element) = {
    PLAYER_REGEX.findFirstMatchIn(elem.toString) match {
      case Some(data) => {
        val role = Role.get(data.group(1).trim)
        val name = data.group(2).trim
        \/-(Gamer(name, role))
      }
      case None => fail(PLAYER_FORMAT_WRONG)
    }
  }

  private def checkSelector[T](selector: String, message: String)(block: Elements => \/[Throwable, T])(implicit doc: Document) = {
    val elem = doc.select(selector)
    if (elem.isEmpty) fail[T](message)
    else block(elem)
  }

  private def gameEntity(id: Int) = registry.refFor[GameEntity](id.toString).withAskTimeout(timeout)

  private def dayEntity(day: Day) = registry.refFor[DayEntity](s"${day.year}-${day.month}-${day.day}").withAskTimeout(timeout)

  private def extractorEntity = registry.refFor[ExtractorEntity]("1").withAskTimeout(timeout)

  private def fail[T](text: String): \/[Throwable, T] = -\/(throw new IllegalStateException(text))

  private def notFound(err: Throwable, message:String ="") = {
    val error = message + exceptionToStr(err)
    extractorEntity.ask(LoadError(error))
    throw NotFound(error)
  }
}

object MafiaServiceBackend {
  private val log = LoggerFactory.getLogger(classOf[MafiaServiceBackend])
  private val FINISH_REGEX = """Лог партии № (\d{7}) (.*) завершена (.*)""".r
  private val PLAYER_REGEX =
    """[\s\S]*<img src="https://st.mafiaonline.ru/images/roles_new/[\s\S]*srcset="https://st.mafiaonline.ru/images/roles_new/[\s\S]* alt="(.*)" title=[\s\S]*class="img-circle avatar"> </td>[\s\S]*(?:<td class="align-middle plus-size"> <s class="text-muted">|<td class="align-middle plus-size">)(.*?)(?:</s> </td>|</td>)[\s\S]*""".r
  private val TABLE_SELECTOR = "table:nth-child(5) > tbody"
  val NO_TABLES_FOUND = "No table with game results is on the page, something definitely wrong"
  val HEADER_CSS_CHANGED = "Game output header CSS style is changed, please review"
  val HEADER_FORMAT_WRONG = "Game output header format is changed, please review"
  private val GAME_SUMMARY_SELECTOR = "h3.text-center"
  private val PLAYERS_SELECTOR = "table.table-condensed"
  val PLAYERS_EXTRACT_ERROR = "Can't extract players. Something changed in HTML for completed games, please check."
  val NO_MESSAGES_ERROR = "There is no messages in chat, probably css selector fot chat messages is changed"
  private val CHAT_SELECTOR = "#log_chat"
  val PLAYER_FORMAT_WRONG = "Couldn't match player, please check HTML"
}