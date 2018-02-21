package com.amarkhel.mafia.service.api

import akka.{Done, NotUsed}
import com.amarkhel.mafia.common.{Day, Game}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.amarkhel.mafia.utils.JsonFormats._
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer
import play.api.libs.json.{Format, Json}

trait MafiaService extends Service {
  def loadDay(day:Day): ServiceCall[NotUsed, List[String]]

  def loadGame(id:Int, lastRound:Int): ServiceCall[NotUsed, Game]

  implicit val pathParamSerializer: PathParamSerializer[Day] = PathParamSerializer.required("day")(s => {
    val date = s.split("&").map(_.toInt)
    Day(date(0), date(1), date(2))
  })(d => {d.year + "&" + d.month + "&" + d.day})

  def clearAll: ServiceCall[NotUsed, Done]

  def status: ServiceCall[NotUsed, Day]
  def events: Topic[Game]
  def errors: Topic[String]

  def descriptor = {
    import Service._
    named("mafiaSite").withCalls(
      restCall(Method.GET, "/api/load/:day", loadDay _),
      restCall(Method.GET, "/api/loadGame/:id/:round", loadGame _),
      restCall(Method.GET, "/api/clear", clearAll),
      restCall(Method.GET, "/api/status", status)
    ).withTopics(
      topic("Games", events),
      topic("Errors", errors)
    )
  }
}