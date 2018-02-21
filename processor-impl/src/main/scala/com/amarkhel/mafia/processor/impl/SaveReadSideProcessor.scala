package com.amarkhel.mafia.processor.impl

import java.lang

import akka.Done
import com.amarkhel.mafia.common.{Game, Role}
import com.amarkhel.mafia.processor.api.{GameCriterion, GameSummary}
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class SaveReadSideProcessor(readSide: CassandraReadSide, session: CassandraSession)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[GameSummaryEvent] {

  private var mainInsertStatement: PreparedStatement = _
  private var addonInsertStatement: Map[GameCriterion, PreparedStatement] = _

  def buildHandler = {
    readSide.builder[GameSummaryEvent]("gameSummaryOffset")
      .setGlobalPrepare(createTable)
      .setPrepare { _ => prepareStatements()}
      .setEventHandler[GameSummarySaved](gameSaved)
      .build()
  }

  private def createTable() = {
    for {
      _ <- session.executeCreateTable("""
          CREATE TABLE IF NOT EXISTS gameSummary (
            id int,
            year int,
            month int,
            day int,
            countP int,
            countR int,
            result text,
            tournamentResult text,
            location text,
            players text,
            PRIMARY KEY (id)
          )
      """)
    } yield ()
    for {
      criterion <- GameCriterion.values
      _ = session.executeCreateTable(s"""
          CREATE TABLE IF NOT EXISTS ${criterion.tableName}_criterion (
            id int,
            ${criterion.tableName} ${criterion.columnType},
            PRIMARY KEY (id)
          )
      """)
    } yield ()
    Future(Done)
  }

  private def prepareStatements() = {
    for {
      insert <- session.prepare("INSERT INTO gameSummary(id, year, month, day, countP, countR, result, tournamentResult, location, players) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    } yield {
      mainInsertStatement = insert
    }
    addonInsertStatement = (for {
      criterion <- GameCriterion.values
      statement = Await.result(session.prepare(s"INSERT INTO ${criterion.tableName}_criterion(id, ${criterion.tableName}) VALUES (?, ?)"),Duration.Inf)
    } yield criterion -> statement).toMap
    Future(Done)
  }

  private def gameSaved(event: EventStreamElement[GameSummarySaved]) = {
    val main = bind(mainInsertStatement, event.event.game)
    val addons = for {
      criterion <- GameCriterion.values
      statement = addonInsertStatement(criterion)
    } yield statement.bind(toI(event.event.game.id), bindCriterion(criterion, event.event.source))
    Future.successful(addons.toList :+ main)
  }

  private def bindCriterion(crit:GameCriterion, game:Game) = {
    if(crit.columnType == "text") crit.extractor(game).toString
    else if(crit.columnType == "int") toI(crit.extractor(game).toString.toInt)
    else toD(crit.extractor(game).toString.toDouble)
  }

  def toPlayersString(players: List[(String, Role)]): String = {
    players.map(p => s"name-${p._1};role-${p._2.role}").mkString(",")
  }

  private def bind(statement:PreparedStatement, game:GameSummary) = {
    statement.bind(toI(game.id), toI(game.year), toI(game.month), toI(game.day), toI(game.countPlayers), toI(game.countRounds), game.result.descr, game.tournamentResult.descr, game.location.name, toPlayersString(game.players))
  }

  private def toI(i:Int) = new java.lang.Integer(i)
  private def toD(d:Double) = new lang.Double(d)

  def aggregateTags = GameSummaryEvent.Tag.allTags
}