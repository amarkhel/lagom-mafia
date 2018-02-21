package com.amarkhel.mafia.service.impl

import com.amarkhel.mafia.common.Day
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/

class MafiaHubImpl(implicit ec:ExecutionContext) extends MafiaHubAPI {

  private val TIMEOUT = 10000
  private def MAFIA_ROOT = "https://mafiaonline.ru/"

  def loadDay(day: Day) = Future(load(s"games/end_game.php?this_year=${day.year}&this_month=${day.month}&this_day=${day.day}"))

  def loadGame(id: Int) = Future(load(s"log/$id"))

  private def normalize(url: String) = if(url.startsWith(MAFIA_ROOT)) url else MAFIA_ROOT + url

  private def load(url: String) = {
    \/.fromTryCatchNonFatal[Document] {
      Jsoup.connect(normalize(url))
        .header("Accept-Encoding", "gzip, deflate")
        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0")
        .maxBodySize(0)
        .timeout(TIMEOUT)
        .validateTLSCertificates(false)
        .get()
    }
  }
}