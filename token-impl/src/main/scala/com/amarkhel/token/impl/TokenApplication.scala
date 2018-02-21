package com.amarkhel.token.impl

import com.amarkhel.token.api.TokenService
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

abstract class TokenApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with CassandraPersistenceComponents {

  override lazy val lagomServer = serverFor[TokenService](wire[TokenServiceImpl])
  override lazy val jsonSerializerRegistry = TokenSerializerRegistry

  persistentEntityRegistry.register(wire[TokenEntity])
}

class TokenApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new TokenApplication(context) with LagomDevModeComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new TokenApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[TokenService])
}
