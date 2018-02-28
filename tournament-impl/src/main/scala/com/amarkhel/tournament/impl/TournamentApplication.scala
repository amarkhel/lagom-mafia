package com.amarkhel.tournament.impl

import com.amarkhel.tournament.api.TournamentService
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents

abstract class TournamentApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with CassandraPersistenceComponents {

  override lazy val lagomServer = serverFor[TournamentService](wire[TournamentServiceImpl])
  override lazy val jsonSerializerRegistry = TournamentSerializerRegistry
  val service = serviceClient.implement[TournamentService]
  persistentEntityRegistry.register(wire[TournamentEntity])
  wire[Scheduler]
}

class TournamentApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new TournamentApplication(context) with ConductRApplicationComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new TournamentApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[TournamentService])
}
