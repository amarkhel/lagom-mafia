package com.amarkhel.mafia.service.impl

import java.time.LocalDate

import akka.stream.Materializer
import akka.stream.scaladsl.Sink._
import akka.stream.scaladsl.{Sink, Source}
import com.amarkhel.mafia.common.Game._
import com.amarkhel.mafia.common.Location
import com.amarkhel.mafia.utils.TextUtils._
import com.amarkhel.mafia.utils.TimeUtils._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory
import Util._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._

class ExtractorFlow(mafiaService:MafiaServiceBackend, session:CassandraSession, registry: PersistentEntityRegistry)(implicit materializer:Materializer, ec:ExecutionContext) extends Extractor{

  private val log = LoggerFactory.getLogger(classOf[ExtractorFlow])
  private val GAME_REGEX = """(?s)[\s\S]*<a href="/log/(.*)" target="_blank">[\s\S]*(?:<span class="zag_tab"><b>VIP-клуб</b>[\s\S]*<b>|<span class="zag_tab"><b>)(.*)(?:</b>[\s\S]*</span>|<\b><\span>)[\s\S]*align="absmiddle" alt="(.*)" title[\s\S]*""".r
  private val STOPPED = "Партия остановлена Авторитетом"
  private val PARALLELISM = 10

  def extractGames(countDays:Int = -1): List[Int]= {
    (for {
      date <- loadLastDate
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
    if(data._3 == STOPPED) {
      entity.ask(FinishGame(stoppedGame(data._1.toInt, data._2, date)))
      log.warn(s"Stooped game ${data._1.toInt} is saved")
      true
    } else data._2 == Location.SUMRAK.name
  }

  private def handleMissed(res: Future[(Int, Int, Int)], countDays:Int) = Try{
    val (year, month, day) = call(res)
    val date = LocalDate.of(year, month, day)
    val count = (countDays > -1)? countDays | diffInDaysFromNow(date)
    handleDays(count, date)
  }

  private def loadLastDate = {
    Try(session.select("SELECT year, month, day FROM extractedDays WHERE id = 1")
      .map(row => {
        def int(name:String) = row.getInt(name)
        (int("year"), int("month"), int("day"))
      }).runWith(last))
  }

  private def parseDay(day:Int, startDate:LocalDate) = {
    measure{
      val date = startDate.plusDays(day)
      log.info(s"Extracting games from ${date.getYear}\\${date.getMonthValue}\\${date.getDayOfMonth}")
      val list = Source(filterGames(date))
        .mapAsyncUnordered(PARALLELISM)( mafiaService.loadGame(_, -1))
        .mapAsyncUnordered(PARALLELISM)(game => entity.ask(FinishGame(game)))
        .runWith(Sink.fold(List.empty[Int])((a, b) => b :: a))
      val result = await(list)
      await(entity.ask(CompleteDay(date)))
      result
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
    log.error(s"not handled game ids are: ${wrong.mkString(",")}")
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