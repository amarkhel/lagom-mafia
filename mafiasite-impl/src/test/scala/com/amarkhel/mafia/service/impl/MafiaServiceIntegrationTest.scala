package com.amarkhel.mafia.service.impl

import java.time.LocalDateTime

import akka.stream.scaladsl.Sink
import com.amarkhel.mafia.common.{Day, Role, RoundStarted}
import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._


class MafiaServiceIntegrationTest extends WordSpec with Matchers with BeforeAndAfterAll{

  val l = List(3893387, 3892483, 2678534, 2461895, 3787059, 3812706, 3820533, 3746301, 3766199, 3770955, 2275974, 2295008, 2299165, 2720229, 2815241, 3694168, 3787711, 3789678, 3792003, 2727416, 3787059, 3795078, 3799219, 3799221, 3799140, 3801123, 3800986, 3801621, 3801308, 3801437, 3801470, 3804943, 3804957, 3804325, 3804409, 3804741, 2284461)
  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with MafiaComponentsCommon with LocalServiceLocator with AhcWSComponents with TestTopicComponents
  }
  import scala.concurrent.ExecutionContext.Implicits.global
  val service = server.serviceClient.implement[MafiaService]

  override def afterAll = server.stop()

  "The mafia service" should {

    "should load games for given day" in {
      for {
        content <- service.loadDay(Day(2015, 1, 1)).invoke()
      } yield {
        content.size shouldBe (271)
        content.head.substring(140, 150) shouldBe ("01/01/2015")
      }
    }
    //should skip 2166203
    "should load game for given id" in {
      for {
        game <- service.loadGame(2668067, -1).invoke()
      } yield {
        game.id should be(2668067)
        game.playersSize should be(7)
        game.players(0).name should be("onanetvoya")
        game.players(0).role should be(Role.MAFIA)
        game.players(1).name should be("White Star")
        game.players(1).role should be(Role.CITIZEN)
        game.players(2).name should be("don Andrii")
        game.players(2).role should be(Role.CITIZEN)
        game.players(3).name should be("kombain Valera")
        game.players(3).role should be(Role.CITIZEN)
        game.players(4).name should be("Поцелуй ангела")
        game.players(4).role should be(Role.MAFIA)
        game.players(5).name should be("Dark Dance")
        game.players(5).role should be(Role.CITIZEN)
        game.players(6).name should be("mifo")
        game.players(6).role should be(Role.KOMISSAR)
        game.events.size should be(86)
        game.finish should be(LocalDateTime.of(2012, 10, 26, 23, 56, 49))
        game.start should be(LocalDateTime.of(2012, 10, 26, 23, 51, 14).toString)
        val games = for {
          e <- l
          res = service.loadGame(e, -1).invoke()
        } yield Await.result(res, 10 seconds)
        games.size should be(l.size)
      }
    }
    "should load game filtered by round" in {
      for {
        game <- service.loadGame(2668067, 1).invoke()
      } yield {
        game.id should be(2668067)
        game.playersSize should be(7)
        game.players(0).name should be("onanetvoya")
        game.players(0).role should be(Role.MAFIA)
        game.players(1).name should be("White Star")
        game.players(1).role should be(Role.CITIZEN)
        game.players(2).name should be("don Andrii")
        game.players(2).role should be(Role.CITIZEN)
        game.players(3).name should be("kombain Valera")
        game.players(3).role should be(Role.CITIZEN)
        game.players(4).name should be("Поцелуй ангела")
        game.players(4).role should be(Role.MAFIA)
        game.players(5).name should be("Dark Dance")
        game.players(5).role should be(Role.CITIZEN)
        game.players(6).name should be("mifo")
        game.players(6).role should be(Role.KOMISSAR)
        game.events.size should be(26)
        game.events.filter(_.isInstanceOf[RoundStarted]).size should be(1)
        game.start should be(LocalDateTime.of(2012, 10, 26, 23, 51, 14).toString)
        val games = for {
          e <- l
          res = service.loadGame(e, -1).invoke()
        } yield Await.result(res, 10 seconds)
        games.size should be(l.size)
      }
    }
    "should load game filtered by round 0" in {
      for {
        game <- service.loadGame(2668067, 0).invoke()
      } yield {
        game.id should be(2668067)
        game.playersSize should be(7)
        game.players(0).name should be("onanetvoya")
        game.players(0).role should be(Role.MAFIA)
        game.players(1).name should be("White Star")
        game.players(1).role should be(Role.CITIZEN)
        game.players(2).name should be("don Andrii")
        game.players(2).role should be(Role.CITIZEN)
        game.players(3).name should be("kombain Valera")
        game.players(3).role should be(Role.CITIZEN)
        game.players(4).name should be("Поцелуй ангела")
        game.players(4).role should be(Role.MAFIA)
        game.players(5).name should be("Dark Dance")
        game.players(5).role should be(Role.CITIZEN)
        game.players(6).name should be("mifo")
        game.players(6).role should be(Role.KOMISSAR)
        game.events.size should be(1)
        game.events.filter(_.isInstanceOf[RoundStarted]).size should be(0)
        game.start should be(LocalDateTime.of(2012, 10, 26, 23, 51, 14).toString)
        val games = for {
          e <- l
          res = service.loadGame(e, -1).invoke()
        } yield Await.result(res, 10 seconds)
        games.size should be(l.size)
      }
    }
    "should load none if game not exist id " in {
      val thrown = intercept[NotFound] {
        val f = service.loadGame(2668066, -1).invoke()
        val r = Await.result(f, 10 seconds)
      }
      thrown.asInstanceOf[NotFound].errorCode.http shouldBe 404
      thrown.asInstanceOf[NotFound].getMessage should include ("Game 2668066 not found")
    }
    /*"should save day " in {
      val status = getStatus
      statusShouldBeInitial(status)
      val now = LocalDateTime.now.minusDays(1)
      Await.result(service.loadDay(Day(now.getYear, now.getMonthValue, now.getDayOfMonth)).invoke(), 10 seconds)
      val statusAfter = getStatus
      statusAfter.year shouldBe (now.getYear)
      statusAfter.month shouldBe (now.getMonthValue)
      statusAfter.day shouldBe (now.getDayOfMonth)
    }*/
    "should not save today " in {
      val status = getStatus
      statusShouldBeInitial(status)
      val today = LocalDateTime.now
      val t = new Day(today.getYear, today.getMonthValue, today.getDayOfMonth)
      val d = for {
        day <- Await.result(service.loadDay(t).invoke(), 10 seconds)
      } yield day
      d.size shouldBe > (1)
      d.head.substring(140, 150) shouldBe (s"${cropLeadingZero(t.day)}/${cropLeadingZero(t.month)}/${t.year}")
      val statusAfter = getStatus
      statusShouldBeInitial(statusAfter)
    }
    "should not save day in future " in {
      val status = getStatus
      statusShouldBeInitial(status)

      val thrown = intercept[NotFound] {
        val f = service.loadDay(Day(2026,1, 1)).invoke()
        val r = Await.result(f, 10 seconds)
      }
      import server.materializer
      val events = Await.result(service.errors.subscribe.atMostOnceSource
        .take(1)
        .runWith(Sink.seq), 100 seconds)
      events.size shouldBe (1)
      thrown.asInstanceOf[NotFound].errorCode.http shouldBe 404
      thrown.asInstanceOf[NotFound].getMessage should include ("Day(2026,1,1) not found")
      val statusAfter = getStatus
      statusShouldBeInitial(statusAfter)
    }
  }

  private def cropLeadingZero(value:Int) = {
    if(value < 10) s"0$value" else value.toString
  }

  private def getStatus = {
    Await.result(server.application.persistentEntityRegistry.refFor[ExtractorEntity]("1").ask(GetStatusCommand), 5 seconds)
  }

  private def statusShouldBeInitial(status: GetStatusCommand.ReplyType) = {
    status.year shouldBe (ExtractorEntity.firstDay.year)
    status.month shouldBe (ExtractorEntity.firstDay.month)
    status.day shouldBe (ExtractorEntity.firstDay.day)
  }
}
