package com.amarkhel.tournament.impl

import com.amarkhel.tournament.api.TournamentService
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

trait TournamentComponents extends LagomServerComponents with CassandraPersistenceComponents {
  implicit def mat = materializer
  implicit def executionContext: ExecutionContext
  val solutionService = wire[SolutionServiceImpl]
  override lazy val lagomServer = serverFor[TournamentService](wire[TournamentServiceImpl])
  override lazy val jsonSerializerRegistry = TournamentSerializerRegistry
  persistentEntityRegistry.register(wire[TournamentEntity])
  persistentEntityRegistry.register(wire[SolutionEntity])
  readSide.register(wire[Processor])
}

abstract class TournamentApplication(context: LagomApplicationContext) extends LagomApplication(context) with AhcWSComponents with TournamentComponents {
  implicit override def mat = materializer
  val service = serviceClient.implement[TournamentService]
  wire[Scheduler]
}

class TournamentApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new TournamentApplication(context) with ConductRApplicationComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new TournamentApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[TournamentService])
}
