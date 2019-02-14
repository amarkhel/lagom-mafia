package com.amarkhel.mafia.service.impl

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.amarkhel.mafia.common._
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver.Reply
import org.scalatest._

class ExtractorEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with Inside {

  private val system = ActorSystem("ExtractorEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(SerializerRegistry))

  private val game = Game(1, List(GameStarted(Location.OZHA, LocalDateTime.of(2014, 2, 2, 12, 22, 23).toString,
    List("Andrey", "Vika"), 0), GameCompleted("Вся мафия убита", 362)), OK, List(Gamer("Andrey", Role.CITIZEN),Gamer("Vika", Role.MAFIA)), 3)

  private val game2 = Game(2, List(GameStarted(Location.OZHA, LocalDateTime.of(2012, 1, 1, 12, 22, 23).toString,
    List("Andrey", "Vika"), 0), GameCompleted("Вся мафия убита", 362)), OK, List(Gamer("Andrey", Role.CITIZEN),Gamer("Vika", Role.MAFIA)), 3)

  private val day = Day(2014, 2, 2)

  override protected def afterAll() = TestKit.shutdownActorSystem(system)

  private def withTestDriver(block: PersistentEntityTestDriver[ExtractorCommand, ExtractorEvent, ExtractorState] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new ExtractorEntity, "1")
    block(driver)
    if (driver.getAllIssues.nonEmpty) {
      driver.getAllIssues.foreach(println)
      fail("There were issues " + driver.getAllIssues.head)
    }
  }

  "The extractor entity" should {
    "should handle completion of day" in withTestDriver { driver =>
      val outcome = driver.run(CompleteDay(day))
      outcome.events should contain only DayCompleted(day)
      outcome.state.lastDay should === (day)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(akka.Done))
    }
    "should return initial state" in withTestDriver { driver =>
      val outcome = driver.run(GetStatusCommand)
      outcome.events.size should be(0)
      outcome.state.lastDay should ===(day)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(day))
    }
    "should not change state after game complete" in withTestDriver { driver =>
      driver.run(FinishGame(game))
      val outcome2 = driver.run(GetStatusCommand)
      outcome2.events.size should be (0)
      outcome2.state.lastDay should === (day)
      outcome2.sideEffects.size should be (1)
      outcome2.sideEffects.head should be (Reply(day))
    }
    "should return last modified state" in withTestDriver { driver =>
      driver.run(CompleteDay(day))
      driver.run(CompleteDay(Day(2015,2,2)))
      val outcome = driver.run(GetStatusCommand)
      outcome.events.size should be (0)
      outcome.state.lastDay should === (Day(2015,2,2))
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(Day(2015,2,2)))
    }
    "should clear state" in withTestDriver { driver =>
      val outcome = driver.run(ClearCommand)
      outcome.events.size should be(1)
      outcome.events should contain only ClearEvent
      outcome.state.lastDay should ===(day)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(akka.Done))
    }
    "should reset state" in withTestDriver { driver =>
      driver.run(CompleteDay(day))
      val outcome = driver.run(ClearCommand)
      outcome.events.size should be(1)
      outcome.events should contain only ClearEvent
      outcome.state.lastDay should ===(day)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(akka.Done))
    }
    "should not changes after error" in withTestDriver { driver =>
      val outcome = driver.run(GetStatusCommand)
      outcome.state.lastDay should ===(day)
      val outcome2 = driver.run(LoadError("Error"))
      outcome2.events.size should be(1)
      outcome2.events should contain only Error("Error")
      outcome2.events.head should === (Error("Error"))
      outcome2.state.lastDay should ===(day)
      outcome2.sideEffects.size should be(1)
      outcome2.sideEffects.head should be(Reply(akka.Done))
    }
  }
}
