package com.amarkhel.tournament.impl

import akka.stream.Materializer
import akka.stream.scaladsl.Sink.seq
import com.amarkhel.tournament.api.{Choice, SolutionResult}
import com.google.common.reflect.TypeToken
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class SolutionServiceImpl(session: CassandraSession)(implicit mat:Materializer, ec: ExecutionContext) {

  def findById(id:Int) = {
    session.select("select id, name, when, choices, points from solutions where id=? ALLOW FILTERING", new java.lang.Integer(id))
      .map(row => {
        SolutionResult(row.getInt("id"), row.getString("name"), new java.sql.Timestamp(row.getTimestamp("when").getTime()).toLocalDateTime(), row.getList("choices", TypeToken.of(classOf[String]).wrap()).asScala.map(Choice.fromString).toList, row.getDouble("points"))
      })
      .runWith(seq)
  }
  def findByPlayer(name:String) = {
    session.select("select id, name, when, choices, points from solutions where name=? ALLOW FILTERING", name)
      .map(row => {
        SolutionResult(row.getInt("id"), row.getString("name"), new java.sql.Timestamp(row.getTimestamp("when").getTime()).toLocalDateTime(), row.getList("choices", TypeToken.of(classOf[String]).wrap()).asScala.map(Choice.fromString).toList, row.getDouble("points"))
      })
      .runWith(seq)
  }

  def findAll = {
    session.select("select id, name, when, choices, points from solutions")
      .map(row => {
        SolutionResult(row.getInt("id"), row.getString("name"), new java.sql.Timestamp(row.getTimestamp("when").getTime()).toLocalDateTime(), row.getList("choices", TypeToken.of(classOf[String]).wrap()).asScala.map(Choice.fromString).toList, row.getDouble("points"))
      })
      .runWith(seq)
  }
}
