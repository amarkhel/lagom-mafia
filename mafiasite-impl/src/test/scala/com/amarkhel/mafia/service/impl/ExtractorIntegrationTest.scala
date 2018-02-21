package com.amarkhel.mafia.service.impl

import akka.stream.scaladsl.Sink
import com.amarkhel.mafia.common.Game
import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import com.softwaremill.macwire.wire
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import play.api.Configuration
import play.api.libs.ws.ahc.AhcWSComponents

class ExtractorIntegrationTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with MafiaComponentsAll with LocalServiceLocator with AhcWSComponents with TestTopicComponents
  }

  val service = server.serviceClient.implement[MafiaService]
  import server.materializer

  override def afterAll = server.stop()

  "The Extractor service" should {

    "emit scheduled events" in {
      for {
        events <- service.events.subscribe.atMostOnceSource
          .take(2)
          .runWith(Sink.seq)
      } yield {
        events.size shouldBe 2
        events.head shouldBe an[Game]
        events.head.day.year shouldBe (2011)
        events.head.day.month shouldBe (8)
        events.head.day.day shouldBe (22)
        events.tail.head shouldBe an[Game]
        events.tail.head.day.year shouldBe (2011)
        events.tail.head.day.month shouldBe (8)
        events.tail.head.day.day shouldBe (22)
      }
    }

    "should handle clear" in {
      for {
        _ <- service.clearAll.invoke()
        status <- service.status.invoke()
        events <- service.events.subscribe.atMostOnceSource.take(1)
          .runWith(Sink.seq)
      } yield {
        status shouldBe (ExtractorEntity.firstDay)
        events.size shouldBe 1
        events.head shouldBe an[Game]
        events.head.day.year shouldBe (2011)
        events.head.day.month shouldBe (8)
        events.head.day.day shouldBe (22)
      }
    }
  }
}
