package com.amarkhel.mafia.service.impl

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Scheduler(system: ActorSystem, extractor:Extractor)(implicit ec: ExecutionContext, mat:Materializer) {
  private val delay = system.settings.config.getDuration("extractorSchedulerDelay", TimeUnit.MILLISECONDS).milliseconds

  private val log = LoggerFactory.getLogger(classOf[ExtractorEntity])

  private def timer():Unit = system.scheduler.scheduleOnce(delay) {
    log.warn("Start extractor")
    log.info("cluster ip is " + System.getenv("CLUSTER_IP"))
    extractor.extractGames()
    log.warn("Stop extractor")
    timer()
  }
  timer()
}