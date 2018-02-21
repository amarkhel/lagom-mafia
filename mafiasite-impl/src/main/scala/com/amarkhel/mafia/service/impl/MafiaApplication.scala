package com.amarkhel.mafia.service.impl

import akka.actor.{TypedActor, TypedProps}
import akka.stream.{ActorMaterializer, Materializer}
import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

trait MafiaComponentsCommon extends LagomServerComponents with CassandraPersistenceComponents {
  val hub:MafiaHubAPI = wire[MafiaHubImpl]
  override lazy val lagomServer = serverFor[MafiaService](wire[MafiaServiceImpl])
  override lazy val jsonSerializerRegistry = SerializerRegistry
  implicit def executionContext: ExecutionContext
  persistentEntityRegistry.register(wire[DayEntity])
  persistentEntityRegistry.register(wire[GameEntity])
  persistentEntityRegistry.register(wire[ExtractorEntity])
  readSide.register(wire[SchedulerProcessor])
  val backend = wire[MafiaServiceBackend]
}

trait MafiaComponentsAll extends MafiaComponentsCommon {
  implicit val mat = materializer
  val extractor = wire[ExtractorFlow]
  wire[Scheduler]

}

abstract class MafiaApplication(context: LagomApplicationContext)
  extends LagomApplication(context) with AhcWSComponents with MafiaComponentsAll with LagomKafkaComponents{
  val service = serviceClient.implement[MafiaService]
}

class MafiaApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new MafiaApplication(context) with LagomDevModeComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new MafiaApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[MafiaService])
}
