package com.amarkhel.mafia.service.impl

import java.time.LocalDate

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.amarkhel.mafia.common.Day
import com.amarkhel.mafia.common.Game._
import com.amarkhel.mafia.service.impl.Util._
import com.amarkhel.mafia.utils.TextUtils._
import com.amarkhel.mafia.utils.TimeUtils._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import scalaz.Scalaz._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ExtractorFlow(mafiaService:MafiaServiceBackend, session:CassandraSession, registry: PersistentEntityRegistry)(implicit materializer:Materializer, ec:ExecutionContext) extends Extractor{

  private val log = LoggerFactory.getLogger(classOf[ExtractorFlow])
  private val GAME_REGEX = """(?s)[\s\S]*<a href="/log/(.*)" target="_blank">[\s\S]*(?:<span class="zag_tab"><b>VIP-клуб</b>[\s\S]*<b>|<span class="zag_tab"><b>)(.*)(?:</b>[\s\S]*</span>|<\b><\span>)[\s\S]*align="absmiddle" alt="(.*)" title[\s\S]*""".r
  private val STOPPED = "Партия остановлена Авторитетом"
  private val PARALLELISM = 2
  private val ignoredGames = ConfigFactory.load().getIntList("invalidGames")

  def extractGames(countDays:Int = -1): List[Int]= {
    (for {
      date <- Try{Util.loadLastDate(session)}
      handled <- handleMissed(date, countDays)
    } yield handled) match {
      case Failure(err) => {
        log.error(err.getMessage)
        entity.ask(LoadError(exceptionToStr(err)))
        List.empty
      }
      case Success(list) => {
        log.debug(s"Games ${list.mkString(",")} are loaded")
        list.toList
      }
    }
  }

  private def filterGame(data: (String, String, String), date:LocalDate) = {
    if(data._3 == STOPPED || ignoredGames.contains(data._1.toInt)) {
      entity.ask(FinishGame(stoppedGame(data._1.toInt, data._2, date)))
      log.warn(s"Stopped game ${data._1.toInt} is saved")
      true
    } else false //data._2 == Location.SUMRAK.name
  }

  private def handleMissed(f:Future[Day], countDays:Int) = Try {
    val last = call(f)
    val date = LocalDate.of(last.year, last.month, last.day)
    val count = (countDays > -1)? countDays | diffInDaysFromNow(date)
    handleDays(count, date)
  }

  private def parseDay(day:Int, startDate:LocalDate) = {
    measure{
      val date = startDate.plusDays(day)
      await(entity.ask(GetStatusCommand).map {
        day =>
          if (day.isBeforeOrEqual(date)) {
            log.info(s"Extracting games from ${date.getYear}\\${date.getMonthValue}\\${date.getDayOfMonth}")
            val list = Source(filterGames(date))
              .mapAsyncUnordered(PARALLELISM)( mafiaService.loadGame(_, -1))
              .mapAsyncUnordered(PARALLELISM)(game => entity.ask(FinishGame(game)))
              .runWith(Sink.fold(List.empty[Int])((a, b) => b :: a))
            val result = await(list)
            await(entity.ask(CompleteDay(date)))
            result
          } else {
            println(s"This day is already handled $day")
            List.empty[Int]
          }
      })
    }("Day was handled by: ")
  }

  private def filterGames(date:LocalDate) = {
    val games = loadDay(date)
    val (ok, wrong) = games.foldLeft(
      (List.empty[Int], List.empty[Int])){
      (a, b) => b match {
        case GAME_REGEX(d,w,e) => {
          if(!filterGame((d, w, e), date)) (d.toInt :: a._1, a._2)
          else (a._1, d.toInt :: a._2 )
        }
        case _ => log.error(s"not handled game template is: $b"); a
      }
    }
    log.error(s"not handled game ids are: ${wrong.mkString(",")}, count of handled games are ${ok.size}")
    ok
  }

  private[impl] def handleDays(days:Int, startDate:LocalDate) = {
    (for {
      i <- 0 to days
    } yield parseDay(i, startDate)).flatten
  }

  private def loadDay(date:LocalDate) = call(mafiaService.loadDay(date))

  private def entity = registry.refFor[ExtractorEntity]("1").withAskTimeout(timeout)

}