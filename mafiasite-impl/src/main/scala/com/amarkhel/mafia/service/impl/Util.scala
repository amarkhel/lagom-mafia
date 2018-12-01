package com.amarkhel.mafia.service.impl

import com.amarkhel.mafia.utils.TimeUtils._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable}

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
}
