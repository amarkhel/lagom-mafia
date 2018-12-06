package com.amarkhel.mafia.processor.impl

import akka.Done
import akka.stream.scaladsl.Flow
import com.amarkhel.mafia.common.{Game, OK}
import com.amarkhel.mafia.processor.api.GameSummary
import com.amarkhel.mafia.service.api.MafiaService
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.Future

class GameSubscriber(persistentEntityRegistry: PersistentEntityRegistry, mafiaService: MafiaService) {

  mafiaService.events.subscribe.atLeastOnce(Flow[Game].mapAsync(1) {
    case g: Game => {
      if (g.status == OK) {
        println(s"Handling game ${g.id}")
        try {
          val summary = GameSummary(g.id, g.location, g.result, g.tournamentResult, g.playersSize, g.roundSize, g.players.map(p => (p.name, p.role)), g.finish.getYear, g.finish.getMonthValue, g.finish.getDayOfMonth)
          entityRef(g.id).ask(SaveGameSummary(summary, g))
        } catch {
          case e:Exception => {
            println(s"${g.id} error")
            if(g.id == 3929442) Future.successful(Done) else throw e
          }
        }

      }
      else Future.successful(Done)
    }
    case _ => Future.successful(Done)
  })

  private def entityRef(id: Int) = persistentEntityRegistry.refFor[GameSummaryEntity](id.toString)

}
