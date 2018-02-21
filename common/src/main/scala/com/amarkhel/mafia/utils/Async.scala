package com.amarkhel.mafia.utils

import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Awaitable}

object Async {
  def call[T](op: ServiceCall[akka.NotUsed, T])(implicit timeout:Duration) = Await.result(op.invoke(), timeout)
  def call[T](awaitable: Awaitable[T])(implicit timeout:Duration) = Await.result(awaitable, timeout)
}
