package com.amarkhel.mafia.service.impl

import java.time.LocalDate

import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import com.softwaremill.macwire.wire
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import play.api.Configuration
import play.api.libs.ws.ahc.AhcWSComponents
import scala.concurrent.duration._
import scala.concurrent.Await

class BenchmarkReal extends FunSuite with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with MafiaComponentsAll with LocalServiceLocator with AhcWSComponents with TestTopicComponents
  }

  val cs = server.application.cassandraSession
  val pr = server.application.persistentEntityRegistry
  val service = server.serviceClient.implement[MafiaService]
  import server.materializer
  implicit val ec = server.executionContext
  val as = server.actorSystem
  val hub = wire[MafiaHubImpl]
  val backend = new MafiaServiceBackend(pr, as, hub)
  val extractor = wire[ExtractorFlow]

  override def afterAll = server.stop()

  test("The mafia service"){
    Await.result(backend.status, 10 seconds)
    val t0 = System.nanoTime()
    val content = extractor.extractGames(30)
    val t1 = System.nanoTime()
    val sec = (t1 - t0).toDouble / 1000000000
    println("Elapsed time: " + sec + "s")
    println(s"time for 1 game is ${sec/content.size}")
    content.size shouldBe  (12989)
  }
}

