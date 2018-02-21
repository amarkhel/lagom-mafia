package com.amarkhel.mafia.processor.impl

import akka.stream.Materializer
import com.amarkhel.mafia.processor.api.GameProcessor
import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeServiceLocatorComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServerComponents}
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

trait ProcessorComponents extends LagomServerComponents with CassandraPersistenceComponents {
  implicit val mat = materializer
  override lazy val lagomServer = serverFor[GameProcessor](wire[GameProcessorImpl])
  override lazy val jsonSerializerRegistry = SerializerRegistry
  implicit def executionContext: ExecutionContext
  persistentEntityRegistry.register(wire[GameSummaryEntity])
  readSide.register(wire[SaveReadSideProcessor])
}

abstract class ProcessorApplication(context: LagomApplicationContext)
  extends LagomApplication(context) with AhcWSComponents with ProcessorComponents with LagomKafkaComponents{
  val service = serviceClient.implement[GameProcessor]
  lazy val mafiaService = serviceClient.implement[MafiaService]
  wire[GameSubscriber]
}

class ProcessorApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new ProcessorApplication(context) with LagomDevModeServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new ProcessorApplication(context) with LagomDevModeServiceLocatorComponents

  override def describeService = Some(readDescriptor[GameProcessor])
}
