package com.amarkhel.tournament.impl

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.amarkhel.tournament.api.TournamentService
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Scheduler(system: ActorSystem, registry: PersistentEntityRegistry, tournamentService:TournamentService)(implicit ec: ExecutionContext, mat:Materializer) {
  private val delay = system.settings.config.getDuration("tournamentSchedulerDelay", TimeUnit.MILLISECONDS).milliseconds

  private val log = LoggerFactory.getLogger(classOf[TournamentEntity])

  private def timer():Unit = system.scheduler.scheduleOnce(delay) {
    log.warn("Start tournament scheduler")
    tournamentService.getTournaments.invoke().map{
      tournaments => {
        for {
          t <- tournaments if t.inProgress && t.gameInProgress.get.expired
        } yield tournamentService.finishGame(t.name, t.gameInProgress.get.id)
      }
    }
    log.warn("Stop tournament scheduler")
    timer()
  }
  timer()
}
