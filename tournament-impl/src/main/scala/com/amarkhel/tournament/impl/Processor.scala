package com.amarkhel.tournament.impl

import akka.Done
import akka.stream.Materializer
import com.amarkhel.tournament.api.Tournament
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}

import scala.concurrent.{ExecutionContext, Future}

class Processor(readSide: CassandraReadSide, session: CassandraSession)(implicit mat:Materializer, ec: ExecutionContext)
  extends ReadSideProcessor[TournamentEvent] {

  private var insertStatement: PreparedStatement = _
  //private var deleteStatement: PreparedStatement = _
  //private var updateStatement: PreparedStatement = _

  def buildHandler = {
    readSide.builder[TournamentEvent]("tournamentOffset")
      .setGlobalPrepare(createTable)
      .setPrepare { _ => prepareStatements()}
      .setEventHandler[TournamentCreated](tournamentCreated)
      //.setEventHandler[TournamentDeleted](tournamentDeleted)
      //.setEventHandler[TournamentUpdated](tournamentUpdated)
      .build()
  }

  private def createTable() = {
    for {
      _ <- session.executeCreateTable("""
          CREATE TABLE IF NOT EXISTS tournaments (
            name text,
            countPlayers int,
            PRIMARY KEY (name)
          )
      """)
    } yield Done
  }

  private def prepareStatements() = {
    for {
      insert <- session.prepare("INSERT INTO tournaments(countPlayers, name) VALUES (?, ?)")
      //update <- session.prepare("UPDATE tournaments SET countPlayers=? where name = ?")
      //delete <- session.prepare("DELETE FROM tournaments where name = ?")
    } yield {
      insertStatement = insert
      //deleteStatement = delete
      //updateStatement = update
      Done
    }
  }

  private def tournamentCreated(event: EventStreamElement[TournamentCreated]) = list2future(bind(insertStatement, event.event.tournament))

  //private def tournamentDeleted(event: EventStreamElement[TournamentDeleted]) = list2future(bind2(deleteStatement, event.event.name))

  //private def tournamentUpdated(event: EventStreamElement[TournamentUpdated]) = list2future(bind(updateStatement, event.event.tournament))

  private def bind(statement:PreparedStatement, tournament:Tournament) = statement.bind(toI(tournament.countPlayers), tournament.name)

  private def toI(i:Int) = new java.lang.Integer(i)
  //private def bind2(statement:PreparedStatement, tournament:String) = statement.bind(tournament)

  private def list2future(statements:BoundStatement*) = Future.successful(statements.toList)

  def aggregateTags = TournamentEvent.Tag.allTags
}