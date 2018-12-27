package com.amarkhel.tournament.impl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.amarkhel.tournament.api.{Tournament, TournamentService, UserState}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.{ExecutionContext, Future}

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

  override def startTournament(name: String) = ServiceCall { _ =>
    refFor(name).ask(StartTournament)
  }

  override def updateTournament = ServiceCall { tournament =>
    refFor(tournament.name).ask(UpdateTournament(tournament))
  }

  override def finishTournament(name: String) = ServiceCall { _ =>
    refFor(name).ask(FinishTournament)
  }

  override def getTournaments = ServiceCall { _ =>
    loadTournaments.runWith(Sink.seq)
  }

  private def loadTournaments = {
    println(currentIdsQuery.currentPersistenceIds())
    currentIdsQuery.currentPersistenceIds()
      //.filter(_.startsWith("TournamentEntity|"))
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
    loadTournaments.filter(_.havePlayer(name)).runWith(Sink.seq)
  }

  override def startGame(name:String, id: Int) = ServiceCall { _ =>
    refFor(name).ask(StartGame(id))
  }

  override def nextRound(name:String, user:String) = ServiceCall { _ =>
    for {
      result <- refFor(name).ask(NextRound(user))
      tournament <- if (result) refFor(name).ask(GetTournament) else Future.successful(Some(Tournament("empty")))
      id = for {
        t <- tournament if t.shouldFinishCurrentGame
        game <- t.gameInProgress
      } yield game.id
      _ <- if(id.isDefined) refFor(name).ask(FinishGame(id.get)) else Future.successful(false)
    } yield result
  }

  override def finishGame(name:String, id:Int) = ServiceCall { _ =>
    for {
      result <- refFor(name).ask(FinishGame(id))
      tournament <- if (result) refFor(name).ask(GetTournament) else Future.successful(Some(Tournament("empty")))
      _ <- if (tournament.get.allGamesCompleted) refFor(name).ask(FinishTournament) else Future.successful(false)
    } yield result
  }

  override def chooseMafia(name:String, user:String, player: String) = ServiceCall { _ =>
    for {
      result <- refFor(name).ask(Choose(user, player))
      tournament <- if (result) refFor(name).ask(GetTournament) else Future.successful(Some(Tournament("empty")))
      id = for {
        t <- tournament if t.shouldFinishCurrentGame
        game <- t.gameInProgress
      } yield game.id
      finished <- if (id.isDefined) refFor(name).ask(FinishGame(id.get)) else Future.successful(false)
      tournament2 <- if (finished) refFor(name).ask(GetTournament) else Future.successful(Some(Tournament("empty")))
      _ <- if (tournament2.get.allGamesCompleted) refFor(name).ask(FinishTournament) else Future.successful(false)
    } yield result
  }

  override def joinTournament(name: String, user: String) = ServiceCall { _ =>
    for {
      result <- refFor(name).ask(JoinTournament(user))
      t <- if (result) refFor(name).ask(GetTournament) else Future.successful(Some(Tournament("empty")))
      _ <- if (t.get.allPlayersJoined) refFor(name).ask(StartTournament) else Future.successful(false)
    } yield result
  }

  override def getUserState(name: String, tournament:String): ServiceCall[NotUsed, Option[UserState]] = { _ =>
    loadTournaments.filter(t => t.name == tournament && t.havePlayer(name)).runWith(Sink.seq).map(_.head.findPlayer(name))
  }
}
