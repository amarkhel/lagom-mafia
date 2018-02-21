package com.amarkhel.tournament.impl

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.amarkhel.tournament.api.TournamentService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.ExecutionContext

class TournamentServiceImpl(registry: PersistentEntityRegistry, system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) extends TournamentService {

  private val currentIdsQuery = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  private def refFor(name: String) = registry.refFor[TournamentEntity](name)

  override def createTournament = ServiceCall { tournament =>
    refFor(tournament.name).ask(CreateTournament(tournament))
  }

  override def getTournament(name: String) = ServiceCall { _ =>
    refFor(name).ask(GetTournament)
  }

  override def deleteTournament(name: String) = ServiceCall { _ =>
    refFor(name).ask(DeleteTournament)
  }

  override def finishTournament(name: String) = ServiceCall { _ =>
    refFor(name).ask(FinishTournament)
  }

  override def getTournaments = ServiceCall { _ =>
    loadTournaments.runWith(Sink.seq)
  }

  private def loadTournaments = {
    currentIdsQuery.currentPersistenceIds()
      .filter(_.startsWith("TournamentEntity|"))
      .mapAsync(4) { id =>
        val entityId = id.split("\\|", 2).last
        registry.refFor[TournamentEntity](entityId)
          .ask(GetTournament)
          .map(_.map(identity))
      }.collect {
      case Some(tournament) => tournament
    }
  }

  override def getTournamentsForUser(name: String) = ServiceCall { _ =>
    loadTournaments.filter(_.players.map(_.name).contains(name)).runWith(Sink.seq)
  }

  override def startTournament(name:String) = ServiceCall { _ =>
    refFor(name).ask(StartTournament)
  }

  override def startGame(name:String, user:String, id: Int) = ServiceCall { _ =>
    refFor(name).ask(StartGame(user, id))
  }

  override def nextRound(name:String, user:String) = ServiceCall { _ =>
    refFor(name).ask(NextRound(user))
  }

  override def expireGame(name:String, user:String) = ServiceCall { _ =>
    refFor(name).ask(ExpireGame(user))
  }

  override def chooseMafia(name:String, user:String, player: String) = ServiceCall { _ =>
    refFor(name).ask(Choose(user, player))
  }

  override def joinTournament(name: String, user: String) = ServiceCall { _ =>
    refFor(name).ask(JoinTournament(user))
  }
}
