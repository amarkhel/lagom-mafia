package com.amarkhel.mafia.service.impl

import akka.Done
import com.amarkhel.mafia.common.Day
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Future}

class SchedulerProcessor(readSide: CassandraReadSide, session: CassandraSession)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[ExtractorEvent] {

  private var insertStatement: PreparedStatement = _
  private var deleteStatement: PreparedStatement = _
  private var updateStatement: PreparedStatement = _

  def buildHandler = {
    readSide.builder[ExtractorEvent]("extractorSchedulerOffset")
      .setGlobalPrepare(createTable)
      .setPrepare { _ => prepareStatements()}
      .setEventHandler[ClearEvent.type](clearAll)
      .setEventHandler[DayCompleted](dayCompleted)
      .build()
  }

  private def createTable() = {
    for {
      _ <- session.executeCreateTable("""
          CREATE TABLE IF NOT EXISTS extractedDays (
            id int,
            year int,
            month int,
            day int,
            PRIMARY KEY (id)
          )
      """)
    } yield Done
  }

  private def prepareStatements() = {
    for {
      insert <- session.prepare("INSERT INTO extractedDays(id, year, month, day) VALUES (1, ?, ?, ?)")
      update <- session.prepare("UPDATE extractedDays SET year=?, month=?, day=? where id = 1")
      delete <- session.prepare("DELETE FROM extractedDays where id = 1")
    } yield {
      insertStatement = insert
      deleteStatement = delete
      updateStatement = update
      session.executeWrite(bind(insertStatement, ExtractorEntity.firstDay))
      Done
    }
  }

  private def clearAll(clear: EventStreamElement[_]) = {
    list2future(
      deleteStatement.bind(),
      bind(insertStatement, ExtractorEntity.firstDay)
    )
  }

  private def dayCompleted(event: EventStreamElement[DayCompleted]) = list2future(bind(updateStatement, event.event.day))

  private def bind(statement:PreparedStatement, day:Day) = statement.bind(toI(day.year), toI(day.month), toI(day.day))

  private def toI(i:Int) = new java.lang.Integer(i)

  private def list2future(statements:BoundStatement*) = Future.successful(statements.toList)

  def aggregateTags = ExtractorEvent.Tag.allTags
}
