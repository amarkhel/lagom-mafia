package com.amarkhel.mafia.service.impl

import akka.stream.scaladsl.Sink
import com.amarkhel.mafia.common.Day
import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import com.softwaremill.macwire.wire
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import play.api.libs.ws.ahc.AhcWSComponents
import scala.concurrent.duration._

import scala.concurrent.Await

class MafiaIntegrationMockTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll{

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with MafiaComponentsCommon with LocalServiceLocator with AhcWSComponents with TestTopicComponents{
      override val hub = wire[MafiaHubMock]
      override val backend = wire[MafiaServiceBackend]
    }
  }

  val service = server.serviceClient.implement[MafiaService]

  override def afterAll = server.stop()

  "The mafia service" should {

    "should throw error if table absent on day page" in {
      val thrown = intercept[NotFound] {
        val f = service.loadDay(Day(2010,1, 1)).invoke()
        val r = Await.result(f, 10 seconds)
      }
      import server.materializer
      val events = Await.result(service.errors.subscribe.atMostOnceSource
        .take(1)
        .runWith(Sink.seq), 100 seconds)
      events.size shouldBe (1)
      thrown.asInstanceOf[NotFound].errorCode.http shouldBe 404
      thrown.asInstanceOf[NotFound].getMessage should include (MafiaServiceBackend.NO_TABLES_FOUND)
    }
    "should throw error if game header selector wrong" in {
      val thrown = intercept[NotFound] {
        val f = service.loadGame(-1, -1).invoke()
        val r = Await.result(f, 10 seconds)
      }
      import server.materializer
      val events = Await.result(service.errors.subscribe.atMostOnceSource
        .take(1)
        .runWith(Sink.seq), 100 seconds)
      events.size shouldBe (1)
      thrown.asInstanceOf[NotFound].errorCode.http shouldBe 404
      thrown.asInstanceOf[NotFound].getMessage should include (MafiaServiceBackend.HEADER_CSS_CHANGED)
    }
    "should throw error if game header changed" in {
      val thrown = intercept[NotFound] {
        val f = service.loadGame(-2, -1).invoke()
        val r = Await.result(f, 10 seconds)
      }
      import server.materializer
      val events = Await.result(service.errors.subscribe.atMostOnceSource
        .take(1)
        .runWith(Sink.seq), 100 seconds)
      events.size shouldBe (1)
      thrown.asInstanceOf[NotFound].errorCode.http shouldBe 404
      thrown.asInstanceOf[NotFound].getMessage should include (MafiaServiceBackend.HEADER_FORMAT_WRONG)
    }
    "should throw error if chat selector changed" in {
      val thrown = intercept[NotFound] {
        val f = service.loadGame(-3, -1).invoke()
        val r = Await.result(f, 10 seconds)
      }
      import server.materializer
      val events = Await.result(service.errors.subscribe.atMostOnceSource
        .take(1)
        .runWith(Sink.seq), 100 seconds)
      events.size shouldBe (1)
      thrown.asInstanceOf[NotFound].errorCode.http shouldBe 404
      thrown.asInstanceOf[NotFound].getMessage should include (MafiaServiceBackend.NO_MESSAGES_ERROR)
    }
    "should throw error if players selector changed" in {
      val thrown = intercept[NotFound] {
        val f = service.loadGame(-4, -1).invoke()
        val r = Await.result(f, 10 seconds)
      }
      import server.materializer
      val events = Await.result(service.errors.subscribe.atMostOnceSource
        .take(1)
        .runWith(Sink.seq), 100 seconds)
      events.size shouldBe (1)
      thrown.asInstanceOf[NotFound].errorCode.http shouldBe 404
      thrown.asInstanceOf[NotFound].getMessage should include (MafiaServiceBackend.PLAYERS_EXTRACT_ERROR)
    }
    "should throw error if single player selector changed" in {
      val thrown = intercept[NotFound] {
        val f = service.loadGame(-5, -1).invoke()
        val r = Await.result(f, 10 seconds)
      }
      import server.materializer
      val events = Await.result(service.errors.subscribe.atMostOnceSource
        .take(1)
        .runWith(Sink.seq), 100 seconds)
      events.size shouldBe (1)
      thrown.asInstanceOf[NotFound].errorCode.http shouldBe 404
      thrown.asInstanceOf[NotFound].getMessage should include (MafiaServiceBackend.PLAYER_FORMAT_WRONG)
    }
  }
}
