package com.amarkhel.tournament.impl

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.amarkhel.mafia.common.Location
import com.amarkhel.tournament.api.{GameDescription, Solution, Tournament, TournamentService}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Scheduler(system: ActorSystem, registry: PersistentEntityRegistry, tournamentService:TournamentService)(implicit ec: ExecutionContext, mat:Materializer) {
  private val delay = system.settings.config.getDuration("tournamentSchedulerDelay", TimeUnit.MILLISECONDS).milliseconds

  private val log = LoggerFactory.getLogger(classOf[TournamentEntity])
  private val unrealTournament = Tournament("Тестовый турнир", 3, "apaapaapa", List.empty, List(GameDescription(3886797, Location.KRESTY.name, 9, 11, List("Мамин Сибиряк", "Denimus"), 1, None, None), GameDescription(4043331, Location.KRESTY.name, 17, 22, List("arena55", "Armenia", "Apelliere"), 1, None, None)), LocalDateTime.now, None, None, 1)

  private val notFnishedTournament = Tournament("Текущий турнир", 2, "apaapaapa", List.empty, List(GameDescription(3886797, Location.KRESTY.name, 9, 11, List("Мамин Сибиряк", "Denimus"), 1, None, None), GameDescription(4043331, Location.KRESTY.name, 17, 22, List("arena55", "Armenia", "Apelliere"), 1, None, None)), LocalDateTime.now, None, None, 1)
  private val notStartedTournament = Tournament("Созданный турнир", 2, "apaapaapa", List.empty, List(GameDescription(3886797, Location.KRESTY.name, 9, 11, List("Мамин Сибиряк", "Denimus"), 1, None, None), GameDescription(4043331, Location.KRESTY.name, 17, 22, List("arena55", "Armenia", "Apelliere"), 1, None, None)), LocalDateTime.now, None, None, 1)

  private var started = false

  private def timer():Unit = system.scheduler.scheduleOnce(delay) {
    log.warn("Start tournament scheduler")
    tournamentService.getTournaments.invoke().map{
      tournaments => {
        for {
          t <- tournaments if t.inProgress && t.gameInProgress.get.expired
        } yield tournamentService.finishGame(t.name, t.gameInProgress.get.id).invoke()
      }
    }
    log.warn("Stop tournament scheduler")
    /*if(!started){
      implicit val timeout = 600 seconds

      for {
        _ <- tournamentService.createTournament.invoke(unrealTournament)
        _ <- tournamentService.joinTournament(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.joinTournament(unrealTournament.name, "never").invoke()
        _ <- tournamentService.joinTournament(unrealTournament.name, "unstop").invoke()
        _ <- tournamentService.startTournament(unrealTournament.name).invoke()
        _ <- tournamentService.startGame(unrealTournament.name, 3886797).invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "never").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "unstop").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "puf", "Сирень").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "never").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "unstop").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "never", "Закон Сансары").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "never", "Сирень").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "unstop", "Сирень").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "unstop").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "unstop", "Закон Сансары").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "puf", "tempo").invoke()
        _ <- tournamentService.startGame(unrealTournament.name, 4043331).invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "puf").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "never").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "never").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "never", "Armenia").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "never").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "never", "Purpur").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "never", "bijo666").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "unstop").invoke()
        _ <- tournamentService.nextRound(unrealTournament.name, "unstop").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "unstop", "Armenia").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "unstop", "bijo666").invoke()
        _ <- tournamentService.chooseMafia(unrealTournament.name, "unstop", "arena55").invoke()
      } yield ()

      for {
        _ <- tournamentService.createTournament.invoke(notFnishedTournament)
        _ <- tournamentService.joinTournament(notFnishedTournament.name, "test").invoke()
        _ <- tournamentService.joinTournament(notFnishedTournament.name, "test2").invoke()
        _ <- tournamentService.startTournament(notFnishedTournament.name).invoke()
        _ <- tournamentService.startGame(notFnishedTournament.name, 3886797).invoke()
        _ <- tournamentService.nextRound(notFnishedTournament.name, "test").invoke()
        _ <- tournamentService.nextRound(notFnishedTournament.name, "test").invoke()
        _ <- tournamentService.nextRound(notFnishedTournament.name, "test2").invoke()
        _ <- tournamentService.chooseMafia(notFnishedTournament.name, "test", "Сирень").invoke()
        _ <- tournamentService.createTournament.invoke(notStartedTournament)
      } yield ()

      for {
        _ <- tournamentService.saveSolution("test").invoke(unrealTournament.games.head, Solution(3886797, Map("Denimus" -> (2, 129), "Lari" -> (5, 370)), 5, true))
        _ <- tournamentService.saveSolution("test3").invoke(unrealTournament.games.head, Solution(3886797, Map("tempo" -> (4, 322), "Lari" -> (5, 370)), 5, true))
        _ <- tournamentService.saveSolution("test2").invoke(unrealTournament.games.head, Solution(3886797, Map("tempo" -> (4, 322), "Сирень" -> (5, 370)), 5, true))
        a <- tournamentService.getSolutionsForId(3886797).invoke()
        b <- tournamentService.getSolutionsForPlayer("test").invoke()
        d <- tournamentService.getAllSolutions.invoke()
        c = 1
      } yield ()

      started = true
    }*/
    timer()
  }
  timer()
}
