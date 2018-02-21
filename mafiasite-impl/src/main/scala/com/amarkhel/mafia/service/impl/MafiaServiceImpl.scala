package com.amarkhel.mafia.service.impl

import akka.NotUsed
import akka.actor.ActorSystem
import com.amarkhel.mafia.common._
import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.{ExecutionContext, Future}

class MafiaServiceImpl(registry: PersistentEntityRegistry, system: ActorSystem, backend:MafiaServiceBackend)(implicit ec: ExecutionContext) extends MafiaService {

  override def clearAll = ServiceCall { _ => {
    backend.clearAll
  }}

  override def status = ServiceCall { _ => {
    backend.status
  }}

  override def loadDay(day: Day) = ServiceCall { _ => {
    backend.loadDay(day)
  }}

  override def events: Topic[Game] = backend.events

  override def errors: Topic[String] = backend.errors

  override def loadGame(id: Int, round:Int = -1) = ServiceCall { _ => {
    backend.loadGame(id, round)
  }}
}