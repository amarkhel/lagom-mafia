package com.amarkhel.mafia.service.impl
import com.amarkhel.mafia.common.Day
import org.jsoup.nodes.Document

import scala.concurrent.Future
import scalaz._

trait MafiaHubAPI {

  def loadDay(day: Day) : Future[\/[Throwable, Document]]

  def loadGame(id: Int) : Future[\/[Throwable, Document]]
}
