package com.amarkhel.mafia.service.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver.Reply
import org.scalatest._

class DayEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with Inside {

  private val system = ActorSystem("DayEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(SerializerRegistry))

  override protected def afterAll() = TestKit.shutdownActorSystem(system)

  private def withTestDriver(block: PersistentEntityTestDriver[DayCommand, DayEvent, Option[String]] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new DayEntity, "2011-1-1")
    block(driver)
    if (driver.getAllIssues.nonEmpty) {
      driver.getAllIssues.foreach(println)
      fail("There were issues " + driver.getAllIssues.head)
    }
  }

  "The day entity" should {
    "load none for empty" in withTestDriver { driver =>
      val outcome = driver.run(LoadDay)
      outcome.events.size shouldBe (0)
      outcome.state should ===(None)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(None))
    }
    "save should work correctly" in withTestDriver { driver =>
      val outcome = driver.run(SaveDay("content"))
      outcome.events.size shouldBe (1)
      outcome.events should contain only DaySaved("content")
      outcome.state should ===(Some("content"))
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(akka.Done))
    }
    "save then load should work correctly" in withTestDriver { driver =>
      driver.run(SaveDay("content"))
      val outcome = driver.run(LoadDay)
      outcome.events.size shouldBe (0)
      outcome.state should ===(Some("content"))
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be(Reply(Some("content")))
    }
  }
}
