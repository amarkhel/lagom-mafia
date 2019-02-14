package com.amarkhel.mafia.service.impl

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.amarkhel.mafia.common._
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver.Reply
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpec}

class GameEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with Inside {

  private val game = Game(1, List(GameStarted(Location.OZHA, LocalDateTime.of(2014, 2, 2, 12, 22, 23).toString,
    List("Andrey", "Vika"), 0), GameCompleted("Вся мафия убита", 362)), OK, List(Gamer("Andrey", Role.CITIZEN),Gamer("Vika", Role.MAFIA)), 3)

  private val system = ActorSystem("GameEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(SerializerRegistry))

  override protected def afterAll() = TestKit.shutdownActorSystem(system)

  private def withTestDriver(block: PersistentEntityTestDriver[GameCommand, GameEvent, Option[Game]] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new GameEntity, "2668069")
    block(driver)
    if (driver.getAllIssues.nonEmpty) {
      driver.getAllIssues.foreach(println)
      fail("There were issues " + driver.getAllIssues.head)
    }
  }

  "The game entity" should {
    "load none for empty" in withTestDriver { driver =>
      val outcome = driver.run(LoadGame)
      outcome.events.size shouldBe (0)
      outcome.state should ===(None)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(None))
    }
    "save should work correctly" in withTestDriver { driver =>
      val outcome = driver.run(SaveGame(game))
      outcome.events.size shouldBe (1)
      outcome.events should contain only GameSaved(game)
      outcome.state should ===(Some(game))
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(game))
    }
    "save then load should work correctly" in withTestDriver { driver =>
      driver.run(SaveGame(game))
      val outcome = driver.run(LoadGame)
      outcome.events.size shouldBe (0)
      outcome.state should ===(Some(game))
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(Some(game)))
    }
  }
}
