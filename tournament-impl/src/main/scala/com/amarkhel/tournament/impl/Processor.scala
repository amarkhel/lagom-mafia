package com.amarkhel.tournament.impl

import java.util.Date

import akka.Done
import akka.stream.Materializer
import com.amarkhel.tournament.api.{Choice, GameDescription, Solution, Util}
import com.datastax.driver.core._
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class Processor(readSide: CassandraReadSide, session: CassandraSession)(implicit mat:Materializer, ec: ExecutionContext)
  extends ReadSideProcessor[SolutionEvent] {

  private var insertStatement: PreparedStatement = _

  def buildHandler = {
    readSide.builder[SolutionEvent]("solutionOffset")
      .setGlobalPrepare(createTable)
      .setPrepare { _ => prepareStatements()}
      .setEventHandler[SolutionPosted](solutionPosted)
      .build()
  }

  private def createTable() = {
    for {
      /*_ <- session.executeWrite("""
          DROP TYPE IF EXISTS choice
      """)
      _ <- session.executeWrite("""
          CREATE TYPE choice (
            name text,
            round int,
            correct boolean,
            when int
          )
      """)*/
      _ <- session.executeCreateTable("""
          CREATE TABLE IF NOT EXISTS solutions (
            id int,
            name text,
            when timestamp,
            choices list<text>,
            points double,
            PRIMARY KEY (id, name, when)
          ) WITH CLUSTERING ORDER BY (name DESC, when DESC)
      """)
    } yield Done
  }

  private def prepareStatements() = {
/*    session.underlying().value.get.get.getCluster.getConfiguration.getCodecRegistry.register(ChoiceCodec)
    session.underlying().value.get.get.getCluster.getConfiguration.getCodecRegistry.register(ListCodec)*/
    for {
      insert <- session.prepare("INSERT INTO solutions(id, name, when, points, choices) VALUES (?, ?, ?, ?, ?)")
    } yield {
      insertStatement = insert
      Done
    }
  }

  private def solutionPosted(event: EventStreamElement[SolutionPosted]) = list2future(bind(insertStatement, event.event.game, event.event.solution, event.event.player))
  private def bind(statement:PreparedStatement, game:GameDescription, solution:Solution, player:String) = {
    val list = solution.mafia.map(s => {
      Choice(s._1, s._2._1, game.mafias.contains(s._1), s._2._2).asString
    }).toList.asJava
    statement.bind(toI(game.id), player, new Date(), new java.lang.Double(Util.calculatePoints(player, game, solution.mafia)._2), list)
  }

  private def toI(i:Int) = new java.lang.Integer(i)

  private def list2future(statement:BoundStatement) = Future.successful(List(statement))

  def aggregateTags = SolutionEvent.Tag.allTags
}

/*
object ChoiceCodec extends TypeCodec[Choice](DataType.custom("tournament.choice"), TypeToken.of(classOf[Choice]).wrap()) {

  override def serialize(value: Choice, protocolVersion: ProtocolVersion): ByteBuffer =
    ByteBuffer.allocate(12 + value.name.getBytes.size)
      .put(value.name.getBytes, 0, value.name.getBytes.size)
      .putInt(value.name.getBytes.size, value.round)
      .putInt(value.name.getBytes.size + 4, value.when)
      .putInt(value.name.getBytes.size + 8, if(value.correct) 1 else 0)

  override def deserialize(bytes: ByteBuffer, protocolVersion: ProtocolVersion): Choice = {
    val count = bytes.remaining
    val length = count - 12
    val arr = new Array[Byte](length)
    bytes.get(arr)
    val round = bytes.getInt(bytes.position)
    val when = bytes.getInt(bytes.position)
    val correct = if(bytes.getInt(bytes.position) == 1) true else false
    val name = arr.map(_.toChar).mkString
    Choice(name, round, correct, when)
  }

  override def format(value: Choice): String = value.toString

  override def parse(value: String): Choice = {
    try {
      val arr = value.replaceAll("Choice(", "").replaceAll(")", "").split(",")
      Choice(arr(0), arr(1).toInt, arr(2).toBoolean, arr(3).toInt)
    }
    catch {
      case e: NumberFormatException =>
        throw new InvalidTypeException( s"""Cannot parse 32-bits integer value from "$value"""", e)
    }
  }
  override def accepts(value: AnyRef): Boolean = value match {
    case Choice(_, _, _, _) => true
    case _ => false
  }
  def acceptsValue(value: Any): Boolean = value match {
    case Choice(_, _, _, _) => true
    case _ => false
  }
  override def accepts(value: DataType): Boolean = value match {
    case _ => true
  }
}
import java.nio.ByteBuffer

import com.datastax.driver.core.CodecUtils.{readSize, readValue}
import com.datastax.driver.core._
import com.datastax.driver.core.exceptions.InvalidTypeException

object ListCodec extends TypeCodec[java.util.List[Choice]](
  DataType.list(ChoiceCodec.getCqlType),
  TypeTokens.listOf(ChoiceCodec.getJavaType).wrap())
{

  override def serialize(value: java.util.List[Choice], protocolVersion: ProtocolVersion): ByteBuffer = {
    if (value == null) return null
    val bbs: List[ByteBuffer] = (for (elt <- value.asScala) yield {
      if (elt == null) throw new NullPointerException("List elements cannot be null")
      ChoiceCodec.serialize(elt, protocolVersion)
    }).toList
    CodecUtils.pack(bbs.toArray, value.size, protocolVersion)
  }

  override def deserialize(bytes: ByteBuffer, protocolVersion: ProtocolVersion): java.util.List[Choice] = {
    if (bytes == null || bytes.remaining == 0) return new java.util.ArrayList[Choice]()
    val input: ByteBuffer = bytes.duplicate
    val size: Int = readSize(input, protocolVersion)
    (for (_ <- 1 to size) yield ChoiceCodec.deserialize(readValue(input, protocolVersion), protocolVersion)).toList.asJava
  }

  override def format(value: java.util.List[Choice]): String = {
    if (value == null) "NULL" else '[' + value.asScala.map(e => ChoiceCodec.format(e)).mkString(",") + ']'
  }

  override def parse(value: String): java.util.List[Choice] = {
    if (value == null || value.isEmpty || value.equalsIgnoreCase("NULL")) return new java.util.ArrayList[Choice]()
    var idx: Int = ParseUtils.skipSpaces(value, 0)
    if (value.charAt(idx) != '[') throw new InvalidTypeException( s"""Cannot parse list value from "$value", at character $idx expecting '[' but got '${value.charAt(idx)}'""")
    idx = ParseUtils.skipSpaces(value, idx + 1)
    val seq = List.newBuilder[Choice]
    if (value.charAt(idx) == ']') return seq.result.asJava
    while (idx < value.length) {
      val n = ParseUtils.skipCQLValue(value, idx)
      seq += ChoiceCodec.parse(value.substring(idx, n))
      idx = n
      idx = ParseUtils.skipSpaces(value, idx)
      if (value.charAt(idx) == ']') return seq.result.asJava
      if (value.charAt(idx) != ',') throw new InvalidTypeException( s"""Cannot parse list value from "$value", at character $idx expecting ',' but got '${value.charAt(idx)}'""")
      idx = ParseUtils.skipSpaces(value, idx + 1)
    }
    throw new InvalidTypeException( s"""Malformed list value "$value", missing closing ']'""")
  }

  override def accepts(value: AnyRef): Boolean = value match {
    case seq: Wrappers.SeqWrapper[_] => if (seq.isEmpty) true else ChoiceCodec.acceptsValue(seq.get(0))
    case _ => false
  }
  override def accepts(value: DataType): Boolean = value match {
    case _ => true
  }
}*/
