//
// Copyright (C) 2016 Lightbend Inc. <https://www.lightbend.com>
//

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.27")

addSbtPlugin("com.lightbend.conductr" % "sbt-conductr" % "2.7.2")

addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.4.0-RC1") 

// Resolvers
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.6")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.21")