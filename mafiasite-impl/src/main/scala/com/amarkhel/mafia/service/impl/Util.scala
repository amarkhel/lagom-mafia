package com.amarkhel.mafia.service.impl

import java.time.LocalDateTime

import akka.actor.Status.Success
import akka.stream.Materializer
import akka.stream.scaladsl.Sink.last
import com.amarkhel.mafia.common.Day
import com.amarkhel.mafia.utils.TimeUtils._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}
import scala.util.Failure

object Util {
  private val log = LoggerFactory.getLogger(classOf[MafiaServiceBackend])

  def awaitAndLog[T](op:Awaitable[T])(message:String) = {
    val start = System.nanoTime()
    val result = Await.result(op, timeout)
    val time = (System.nanoTime() - start).toDouble / 1000000 + " msec"
    if(!message.isEmpty){
      log.info(s"$message $time")
    }
    result
  }

  def await[T](op:Awaitable[T]) = awaitAndLog[T](op)("")

  def measure[T](op: => T)(message:String="") = {
    val start = System.nanoTime()
    val result = op
    val time = (System.nanoTime() - start).toDouble / 1000000 + " msec"
    log.warn(s"$message $time")
    result
  }

  def call[T](op: ServiceCall[akka.NotUsed, T])(implicit timeout:Duration) = Await.result(op.invoke(), timeout)
  def call[T](awaitable: Awaitable[T])(implicit timeout:Duration) = Await.result(awaitable, timeout)

  def loadLastDate(session:CassandraSession)(implicit materializer:Materializer, executionContext: ExecutionContext) = {
    session.select("SELECT year, month, day FROM extractedDays WHERE id = 1")
      .map(row => {
        def int(name:String) = row.getInt(name)
        Day(int("year"), int("month"), int("day"))
      })
      .runWith(last).fallbackTo(Future(firstDay))
  }

  val config = ConfigFactory.load()
  lazy val firstDay = {
    if (config.hasPath("initial")) {
      Day(config.getInt("initial.year"), config.getInt("initial.month"), config.getInt("initial.day"))
    } else {
      val now = LocalDateTime.now()
      Day(now.getYear, now.getMonthValue, now.getDayOfMonth)
    }
  }
}
