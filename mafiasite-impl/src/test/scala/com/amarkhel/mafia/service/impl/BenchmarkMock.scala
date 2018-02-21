package com.amarkhel.mafia.service.impl

import java.time.LocalDate

import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import com.softwaremill.macwire.wire
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import play.api.Configuration
import play.api.libs.ws.ahc.AhcWSComponents

class BenchmarkMock extends FunSuite with Matchers with BeforeAndAfterAll {

  var extract:Extractor = null
  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with MafiaComponentsAll with LocalServiceLocator with AhcWSComponents with TestTopicComponents{
      extract = this.extractor
    }
  }

  override def afterAll = server.stop()

  test("The mafia service"){
    java.lang.Thread.sleep(2000)
    val t0 = System.nanoTime()
    val content = extract.asInstanceOf[ExtractorFlow].handleDays(0, LocalDate.of(2011, 10, 1))
    val t1 = System.nanoTime()
    val sec = (t1 - t0).toDouble / 1000000000
    println("Elapsed time: " + sec + "s")
    println(s"time for 1 game is ${sec/content.size}")
    content.size shouldBe > (100)
  }
}
