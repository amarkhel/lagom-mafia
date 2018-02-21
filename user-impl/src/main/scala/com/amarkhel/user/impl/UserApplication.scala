package com.amarkhel.user.impl

import com.amarkhel.user.api.UserService
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeServiceLocatorComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

abstract class UserApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with CassandraPersistenceComponents {

  override lazy val lagomServer = serverFor[UserService](wire[UserServiceImpl])
  override lazy val jsonSerializerRegistry = UserSerializerRegistry

  persistentEntityRegistry.register(wire[UserEntity])
}

class UserApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new UserApplication(context) with LagomDevModeServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new UserApplication(context) with LagomDevModeServiceLocatorComponents

  override def describeService = Some(readDescriptor[UserService])
}
