import sbt.Project.projectToRef
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.less.Import.LessKeys
import sbt.Keys.licenses

lazy val root = (project in file("."))
  .settings(name := "mafia")
  .aggregate(mafiaSiteApi, mafiaSiteImpl, processorApi, processorImpl, userApi, userImpl,
    tokenApi, tokenImpl, tournamentApi, tournamentImpl, webServer)
  .settings(commonSettings: _*)

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

lazy val clients = Seq(webClient)

lazy val webServer = (project in file("web-server"))
  .settings(commonSettings: _*)
  .settings(lagomServicePort := 11000)
  .settings(
    routesImport += "config.Routes._",
    includeFilter in (Assets, LessKeys.less) := "*.less",
    LessKeys.compress := true,
    BundleKeys.endpoints := Map("webserver" -> Endpoint("http", 11000, Set.empty[URI])),
    scalaJSProjects := clients,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(scalaJSProd, gzip),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      filters,
      "com.typesafe.akka" %% "akka-cluster" % "2.5.18",
      "com.typesafe.akka" %% "akka-remote" % "2.5.18",
      "com.typesafe.conductr" %% "play26-conductr-bundle-lib" % "2.1.1",
      "com.vmunier" %% "scalajs-scripts" % "1.1.1",
      "org.webjars" %% "webjars-play" % "2.6.3",
      "org.webjars" % "bootstrap" % "4.1.3",
      "org.webjars" % "jquery" % "3.2.1",
      "org.webjars" % "font-awesome" % "4.4.0",
      "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3",
      // https://mvnrepository.com/artifact/net.logstash.logback/logstash-logback-encoder
      "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
      lagomScaladslServer,
      "com.mohiva" %% "play-silhouette" % "5.0.3",
      "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.3",
      "com.mohiva" %% "play-silhouette-persistence" % "5.0.3",
      "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.3",
      "com.mohiva" %% "play-silhouette-testkit" % "5.0.3" % "test",
      "net.codingwell" %% "scala-guice" % "4.1.1",
      "com.iheart" %% "ficus" % "1.4.3",
      "com.typesafe.play" %% "play-mailer" % "6.0.1"
      )
  )
  .enablePlugins(PlayScala && LagomPlay && SbtWeb && LagomScala && JavaAppPackaging)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(common, mafiaSiteApi, processorApi, userApi, tokenApi, tournamentApi, webSharedJvm)

lazy val webClient = (project in file("web-client")).settings(commonSettings: _*).settings(
  scalaJSUseMainModuleInitializer := false,
  scalaJSMainModuleInitializer := None,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.4",
    "com.lihaoyi" %%% "scalatags" % "0.6.7",
    "com.lihaoyi" %%% "scalarx" % "0.3.2",
    "be.doeraene" %%% "scalajs-jquery" % "0.9.2",
    "org.scalaz" %%% "scalaz-core" % "7.2.16",
    "com.lihaoyi" %%% "utest" % "0.6.3" % "test"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(webSharedJs)

val webSharedJvmSettings = List(
  libraryDependencies ++= Seq(
    "com.github.benhutchison" %% "prickle" % "1.1.13",
    "com.lihaoyi" %%% "utest" % "0.6.3" % "test"
  )
)

val webShared = (crossProject.crossType(CrossType.Pure) in file("web-shared")).
  settings(
    testFrameworks += new TestFramework("utest.runner.Framework"),
    libraryDependencies ++= Seq(
      "com.github.benhutchison" %% "prickle" % "1.1.13",
      playJsonDerivedCodecs,
      lagomScaladslApi,
      "org.scalaz" %% "scalaz-core" % "7.2.16",
      lagomScaladslServer % Optional
    )
  ).
  jvmSettings(webSharedJvmSettings: _*).
  jsSettings(
    libraryDependencies ++= Seq(
      "com.github.benhutchison" %%% "prickle" % "1.1.13",
      "com.lihaoyi" %%% "utest" % "0.6.3" % "test",
      playJsonDerivedCodecs,
      lagomScaladslApi,
      "org.scalaz" %% "scalaz-core" % "7.2.16",
      "org.slf4j" % "slf4j-simple" % "1.7.25",
      "org.slf4j" % "slf4j-api" % "1.7.25"
    )
  ).jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val webSharedJvm = webShared.jvm
lazy val webSharedJs = webShared.js

// loads the jvm project at sbt startup
//onLoad in Global := (Command.process("project webServer", _: State)) compose (onLoad in Global).value

lazy val common = (project in file("com.amarkhel.mafia.common"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.jsoup" % "jsoup" % "1.8.3",
      "com.beachape" %% "enumeratum" % "1.5.12"
    )
  ).dependsOn(webSharedJvm)

lazy val processorApi = (project in file("processor-api"))
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val mafiaSiteApi = (project in file("mafiasite-api"))
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val processorImpl = (project in file("processor-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala, JavaAppPackaging)
  .dependsOn(processorApi, mafiaSiteApi)

lazy val mafiaSiteImpl = (project in file("mafiasite-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala, JavaAppPackaging)
  .dependsOn(mafiaSiteApi)

lazy val tournamentApi = (project in file("tournament-api"))
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val tournamentImpl = (project in file("tournament-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala, JavaAppPackaging)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.2"
    )
  )
  .dependsOn(tournamentApi)

lazy val userApi = (project in file("user-api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.mohiva" %% "play-silhouette" % "5.0.3"
    )
  )
  .dependsOn(common)

lazy val userImpl = (project in file("user-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala, JavaAppPackaging)
  .dependsOn(userApi)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-server" % "2.6.20",
      "com.typesafe.play" %% "play-openid" % "2.6.20",
      "com.typesafe.play" %% "play-logback" % "2.6.20",
      "com.typesafe.play" %% "filters-helpers" % "2.6.20"
    )
  )

lazy val tokenApi = (project in file("token-api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json-joda" % "2.6.0"
    )
  )
  .dependsOn(common)

lazy val tokenImpl = (project in file("token-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala, JavaAppPackaging)
  .dependsOn(tokenApi)

val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
val guava = "com.google.guava" % "guava" % "18.0"

def commonSettings: Seq[Setting[_]] = Seq(
  resolvers ++= Seq(
    "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
  "Atlassian Releases" at "https://maven.atlassian.com/public/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  //Resolver.url("amarkhel", url("https://dl.bintray.com/amarkhel/maven"))(Resolver.ivyStylePatterns),
  Resolver.jcenterRepo
  ),
  javaOptions in Universal += "-Duser.timezone=Europe/Moscow",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  version := "1.1",
  envVars := Map("CLUSTER_IP" -> "127.0.0.1"),
  libraryDependencies ++= Seq(
    macwire,
    scalaTest,
    playJsonDerivedCodecs,
    guava,
    lagomScaladslApi,
    lagomScaladslPersistenceCassandra,
    lagomScaladslTestKit,
    lagomScaladslKafkaBroker,
    "com.softwaremill.quicklens" %% "quicklens" % "1.4.11"
  )
)

lagomCassandraCleanOnStart in ThisBuild := true
lagomKafkaCleanOnStart in ThisBuild := true
lagomCassandraEnabled in ThisBuild := true
//lagomUnmanagedServices in ThisBuild := Map("cas_native" -> "http://localhost:9042")
lagomKafkaEnabled in ThisBuild := true
lagomKafkaAddress in ThisBuild := "localhost:9092"
