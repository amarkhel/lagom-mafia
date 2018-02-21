package com.amarkhel.mafia.processor.api

import akka.{Done, NotUsed}
import com.amarkhel.mafia.common.Game
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait GameProcessor extends Service {
  //def replay: ServiceCall[Int, Game]

  def search: ServiceCall[SearchRequest, List[GameSummary]]
  def retrieve: ServiceCall[Int, GameSummary]

  //def events: Topic[Game]

  final override def descriptor = {
    import Service._

    named("processor").withCalls(
      //restCall(Method.GET, "/api/replay/:id", replay),
      restCall(Method.GET, "/api/retrieve/:id", retrieve _),
      restCall(Method.GET, "/api/search", search _)
    )/*.withTopics(
      topic("Games", events)
    )*/
  }
}
