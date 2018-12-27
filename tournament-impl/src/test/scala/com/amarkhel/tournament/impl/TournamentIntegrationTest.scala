package com.amarkhel.tournament.impl

import java.time.LocalDateTime

import com.amarkhel.mafia.common.Location
import com.amarkhel.tournament.api._
import com.lightbend.lagom.scaladsl.api.transport.{NotFound, TransportException}
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.duration._
import scala.concurrent.Await

class TournamentIntegrationTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with TournamentComponents with LocalServiceLocator with AhcWSComponents with TestTopicComponents
  }

  val service = server.serviceClient.implement[TournamentService]
  private val emptyTournament = Tournament("empty", 3, "", List(UserState("apa", List.empty)), List(GameDescription(1, Location.KRESTY.name, 9, 2, List("a", "b"), 1, None, None)), LocalDateTime.now, None, None, 1)
  private val unrealTournament = Tournament("Unreal tournament", 3, "apaapaapa", List.empty, List(GameDescription(1, Location.KRESTY.name, 9, 12, List("bax", "loh"), 1, None, None), GameDescription(2, Location.KRESTY.name, 7, 8, List("Inn-ah", "lost"), 1, None, None)), LocalDateTime.now, None, None, 1)
  override def afterAll = server.stop()

  "The Tournament service" should {

    "should handle create tournament" in {
      for {
        result <- service.createTournament.invoke(emptyTournament)
      } yield {
        result shouldBe (emptyTournament)
      }
    }
    "should handle create error tournament" in {
      service.createTournament.invoke(emptyTournament)
      val thrown = intercept[TransportException] {
        val f = service.createTournament.invoke(emptyTournament)
        Await.result(f, 10 seconds)
      }
      thrown.asInstanceOf[TransportException].errorCode.http shouldBe 500
      thrown.asInstanceOf[TransportException].getMessage should include (TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }

    "should handle update tournament" in {
      for {
        result <- service.updateTournament.invoke(emptyTournament.copy(countPlayers = 12))
      } yield {
        result shouldBe (emptyTournament.copy(countPlayers = 12))
      }
    }

    "should handle delete tournament" in {
      for {
        result <- service.deleteTournament(emptyTournament.name).invoke()
        res <- service.getTournament(emptyTournament.name).invoke()
      } yield {
        result shouldBe (true)
        res shouldBe (None)
      }
    }
    "should handle start tournament" in {
      for {
        result <- service.createTournament.invoke(emptyTournament)
        res <- service.startTournament(emptyTournament.name).invoke()
        res2 <- service.getTournament(emptyTournament.name).invoke()
      } yield {
        result shouldBe (emptyTournament)
        res shouldBe (true)
        res2.get shouldBe (emptyTournament.copy(start = res2.get.start))
      }
    }
    "should handle join tournament" in {
      for {
        result <- service.createTournament.invoke(emptyTournament.copy(name = "test"))
        res <- service.joinTournament("test", "andrey").invoke()
        res2 <- service.getTournament("test").invoke()
        _ <- service.deleteTournament("test").invoke()
      } yield {
        result shouldBe (emptyTournament.copy(name = "test"))
        res shouldBe (true)
        res2.get.havePlayer("andrey") should be (true)
      }
    }
    "should handle finish tournament" in {
      for {
        result <- service.createTournament.invoke(emptyTournament.copy(name = "finish"))
        res <- service.startTournament("finish").invoke()
        res3 <- service.finishTournament("finish").invoke()
        res2 <- service.getTournament("finish").invoke()
      } yield {
        result shouldBe (emptyTournament.copy(name = "finish"))
        res shouldBe (true)
        res3 shouldBe (true)
        res2.get shouldBe (emptyTournament.copy(name = "finish", start = res2.get.start, finish = res2.get.finish))
      }
    }
    "should handle get tournaments" in {
      for {
        result <- service.createTournament.invoke(emptyTournament.copy(name = "asd"))
        result <- service.createTournament.invoke(emptyTournament.copy(name = "asd2"))
        res <- service.getTournaments.invoke()
        _ <- service.deleteTournament("asd").invoke()
        _ <- service.deleteTournament("asd2").invoke()
      } yield {
        res.size shouldBe (4)
        res.head.name should be ("asd2")
        res.last.name should be ("asd")
      }
    }
    "should handle get tournaments for user" in {
      for {
        result <- service.createTournament.invoke(emptyTournament.copy(name = "a"))
        result <- service.createTournament.invoke(emptyTournament.copy(name = "a2"))
        result <- service.joinTournament("a", "and").invoke()
        res <- service.getTournaments.invoke()
        res2 <- service.getTournamentsForUser("and").invoke()
        res3 <- service.getTournamentsForUser("and2").invoke()
        _ <- service.deleteTournament("a").invoke()
        _ <- service.deleteTournament("a2").invoke()
      } yield {
        res.size shouldBe (4)
        res.head.name should be ("a2")
        res.last.name should be ("a")
        res2.size should be (1)
        res2.head.name should be ("a")
        res3 should be (Nil)
      }
    }
    "should handle whole interaction" in {
      for {
        create <- service.createTournament.invoke(unrealTournament)
        getAfterCreate <- service.getTournament(unrealTournament.name).invoke()
        join1 <- service.joinTournament(unrealTournament.name, "puf").invoke()
        getAfterJoin1 <- service.getTournament(unrealTournament.name).invoke()
        join2 <- service.joinTournament(unrealTournament.name, "never").invoke()
        getAfterJoin2 <- service.getTournament(unrealTournament.name).invoke()
        join3 <- service.joinTournament(unrealTournament.name, "unstop").invoke()
        getAfterJoin3 <- service.getTournament(unrealTournament.name).invoke()
        startGame <- service.startGame(unrealTournament.name, 1).invoke()
        getAfterStartGame <- service.getTournament(unrealTournament.name).invoke()
        pufg1h1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        getAfternextRound <- service.getTournament(unrealTournament.name).invoke()
        neverg1h1 <- service.nextRound(unrealTournament.name, "never").invoke()
        unstopg1h1 <- service.nextRound(unrealTournament.name, "unstop").invoke()
        pufg1ch1 <- service.chooseMafia(unrealTournament.name, "puf", "Eternity").invoke()
        getAfterChooseMafia <- service.getTournament(unrealTournament.name).invoke()
        neverg1h2 <- service.nextRound(unrealTournament.name, "never").invoke()
        getAfterNextRound2 <- service.getTournament(unrealTournament.name).invoke()
        unstopg1h2 <- service.nextRound(unrealTournament.name, "unstop").invoke()
        neverg1ch1 <- service.chooseMafia(unrealTournament.name, "never", "Inn-ah").invoke()
        getAfterChoose2 <- service.getTournament(unrealTournament.name).invoke()
        neverg1ch2 <- service.chooseMafia(unrealTournament.name, "never", "Eternity").invoke()
        getAfterChoose3 <- service.getTournament(unrealTournament.name).invoke()
        unstopg1ch1 <- service.chooseMafia(unrealTournament.name, "unstop", "Eternity").invoke()
        unstopg1h3 <- service.nextRound(unrealTournament.name, "unstop").invoke()
        unstopg1ch2 <- service.chooseMafia(unrealTournament.name, "unstop", "Inn-ah").invoke()
        pufg1ch2 <- service.chooseMafia(unrealTournament.name, "puf", "Gorod").invoke()
        getAfterGameCompleted <- service.getTournament(unrealTournament.name).invoke()
        getUserStatePuf <- service.getUserState("puf", unrealTournament.name).invoke()
        getUserStateNever <- service.getUserState("never", unrealTournament.name).invoke()
        getUserStateUnstop <- service.getUserState("unstop", unrealTournament.name).invoke()
        startGame2 <- service.startGame(unrealTournament.name, 2).invoke()
        getAfterStartGame2 <- service.getTournament(unrealTournament.name).invoke()
        pufn1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        pufn1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        pufn1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        pufn1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        pufn1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        pufn1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        pufn1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        pufn1 <- service.nextRound(unrealTournament.name, "puf").invoke()
        getAfterAlRoundsmaked <- service.getTournament(unrealTournament.name).invoke()
        never1 <- service.nextRound(unrealTournament.name, "never").invoke()
        never2 <- service.nextRound(unrealTournament.name, "never").invoke()
        never3 <- service.chooseMafia(unrealTournament.name, "never", "Inn-ah").invoke()
        never4 <- service.nextRound(unrealTournament.name, "never").invoke()
        never5 <- service.chooseMafia(unrealTournament.name, "never", "Eternity").invoke()
        _ <- service.nextRound(unrealTournament.name, "unstop").invoke()
        _ <- service.nextRound(unrealTournament.name, "unstop").invoke()
        _ <- service.nextRound(unrealTournament.name, "unstop").invoke()
        _ <- service.chooseMafia(unrealTournament.name, "unstop", "Inn-ah").invoke()
        unstop <- service.chooseMafia(unrealTournament.name, "unstop", "lost").invoke()
        getAfterSecondGameComplete <- service.getTournament(unrealTournament.name).invoke()
      } yield {
        getAfterCreate.get should be (unrealTournament)
        getAfterJoin1.get.allPlayersJoined should be (false)
        getAfterJoin1.get.havePlayer("puf") should be (true)
        getAfterJoin1.get.countJoinedPlayers should be (1)
        getAfterJoin1.get.started should be (false)
        getAfterJoin2.get.allPlayersJoined should be (false)
        getAfterJoin2.get.havePlayer("puf") should be (true)
        getAfterJoin2.get.havePlayer("never") should be (true)
        getAfterJoin2.get.countJoinedPlayers should be (2)
        getAfterJoin2.get.started should be (false)
        getAfterJoin3.get.allPlayersJoined should be (true)
        getAfterJoin3.get.havePlayer("puf") should be (true)
        getAfterJoin3.get.havePlayer("never") should be (true)
        getAfterJoin3.get.havePlayer("unstop") should be (true)
        getAfterJoin3.get.countJoinedPlayers should be (3)
        getAfterJoin3.get.started should be (true)
        join1 should be (true)
        join2 should be (true)
        join3 should be (true)
        startGame should be (true)
        getAfterStartGame.get.hasGameInProgress should be (true)
        getAfterStartGame.get.inProgressGameId should be (1)
        getAfterStartGame.get.players.map(_.getById(1)).size should be (3)
        getAfterStartGame.get.players.map(_.getById(1).get).head should be(Solution(1, Map.empty, 0, false))
        pufg1h1 should be (true)
        getAfternextRound.get.findPlayer("puf").get.getById(1).get should be (Solution(1, Map.empty, 1, false))
        getAfterNextRound2.get.findPlayer("never").get.getById(1).get should be (Solution(1, Map.empty, 2, false))
        pufg1ch1 should be (true)
        getAfterChooseMafia.get.findPlayer("puf").get.getById(1).get should be (Solution(1, Map("Eternity" -> 1), 1, false))
        getAfterChoose2.get.findPlayer("never").get.getById(1).get should be (Solution(1, Map("Inn-ah" -> 2), 2, false))
        getAfterChoose3.get.findPlayer("never").get.getById(1).get should be (Solution(1, Map("Inn-ah" -> 2, "Eternity" -> 2), 2, true))
        getAfterGameCompleted.get.findPlayer("never").get.getById(1).get should be (Solution(1, Map("Inn-ah" -> 2, "Eternity" -> 2), 2, true))
        getAfterGameCompleted.get.findPlayer("puf").get.getById(1).get should be (Solution(1, Map("Gorod" -> 1, "Eternity" -> 1), 1, true))
        getAfterGameCompleted.get.findPlayer("unstop").get.getById(1).get should be (Solution(1, Map("Inn-ah" -> 3, "Eternity" -> 2), 3, true))
        getAfterGameCompleted.get.hasGameInProgress should be (false)
        getAfterGameCompleted.get.games.filter(_.id == 1).head.finished.isDefined should be (true)
        getUserStatePuf.get.solutions should be (List(Solution(1, Map("Gorod" -> 1, "Eternity" -> 1), 1, true)))
        getAfterGameCompleted.get.stat(0) should be (("puf",1,0.0,0.0,0.0,0.0,List(),0,List(0.0)))
        getAfterGameCompleted.get.stat(1) should be (("never",1,0.0,0.0,0.0,0.0,List(),0,List(0.0)))
        getAfterGameCompleted.get.stat(2) should be (("unstop",1,0.0,0.0,0.0,0.0,List(),0,List(0.0)))
        startGame2 should be (true)
        getAfterStartGame2.get.hasGameInProgress should be (true)
        getAfterStartGame2.get.inProgressGameId should be (2)
        getAfterStartGame2.get.players.map(_.getById(2)).size should be (3)
        getAfterStartGame2.get.players.map(_.getById(2).get).head should be(Solution(2, Map.empty, 0, false))
        getAfterAlRoundsmaked.get.findPlayer("puf").get.getById(2).get should be (Solution(2, Map.empty, 8, true))
        getAfterSecondGameComplete.get.finished should be (true)
        getAfterSecondGameComplete.get.hasGameInProgress should be (false)
        getAfterSecondGameComplete.get.inProgress should be (false)
        getAfterSecondGameComplete.get.findPlayer("puf").get.getById(2).get should be (Solution(2,Map(),8,true))
        getAfterSecondGameComplete.get.findPlayer("never").get.getById(2).get should be (Solution(2,Map("Inn-ah" -> 2, "Eternity" -> 3),3,true))
        getAfterSecondGameComplete.get.findPlayer("unstop").get.getById(2).get should be (Solution(2, Map("Inn-ah" -> 3, "lost" -> 3), 3, true))
        getAfterSecondGameComplete.get.stat(0) should be (("puf",2,0.0,0.0,0.0,0.0,List(),0,List(0.0, 0.0)))
        getAfterSecondGameComplete.get.stat(1) should be (("never",2,37.50,37.50,0.0,18.75,List(("Inn-ah",2)),2,List(0.0, 37.50)))
        getAfterSecondGameComplete.get.stat(2) should be (("unstop",2,62.50,62.50,0.0,31.25,List(("Inn-ah",3), ("lost",3)),3,List(0.0, 62.50)))
      }
    }
  }
}