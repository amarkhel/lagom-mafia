package com.amarkhel.mafia.processor.impl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Sink.{last, seq}
import akka.stream.scaladsl.Source
import com.amarkhel.mafia.common._
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, PersistentEntityRegistry}
import com.amarkhel.mafia.processor.api._
import com.datastax.driver.core.Row
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class GameProcessorImpl(registry: PersistentEntityRegistry, session:CassandraSession, system: ActorSystem)(implicit ec: ExecutionContext, mat:Materializer) extends GameProcessor {
  override def retrieve: ServiceCall[Int, GameSummary] = ServiceCall { id =>
    load(id)
  }

  private def load(id:Int): Future[GameSummary] = {
    val result: Try[Future[GameSummary]] = Try{
      val sel: Future[GameSummary] = session.select(s"SELECT id, year, month, day, countP, countR, result, tournamentResult, location, players FROM gameSummary WHERE id = $id")
      .map(row => {
        def int(name:String) = row.getInt(name)
        def players(str:String) = {
          val ps = str.split(",")
          ps.map(p => {
            val temp = p.split(";")
            val name = temp(0)
            val role = Role.get(temp(1))
            (name, role)
          })
        }
        GameSummary(int("id"), Location.get(row.getString("location")), Result.get(row.getString("result")), TournamentResult.byDescription(row.getString("tournamentResult")), int("countP"), int("countR"), players(row.getString("players")).toList, int("year"), int("month"), int("day"))
      }).runWith(last)
      sel
    }
    result match {
      case Failure(e) => throw new Exception("")
      case Success(f) => f
    }
  }

  override def search = ServiceCall { criterion =>
    val ids = Future.sequence(for {
      c <- criterion.list
    } yield fetchIds(c))
    val matched: Future[List[Int]] = ids.map(l =>{
      l.reduce((a,b) => a.intersect(b))
    })
    matched.map(m => {fetchGames(m).map(_.toList)}).flatten
  }

  private def mapVal(value: Any):String = {
    value match {
      case i:IntValue => i.value.toString
      case str:StringValue => s"'${str.value}'"
      case d:DoubleValue => d.value.toString
      case l:ListIntValue => s"(${l.value.map(a => mapVal(a)).mkString(",")})"
      case l:ListStrValue => s"(${l.value.map(a => mapVal(a)).mkString(",")})"
      case l:ListDoubleValue => s"(${l.value.map(a => mapVal(a)).mkString(",")})"
    }
  }

  private def fetchGames(list:List[Int]) = {
    val id = list.map(s => s"$s").mkString(",")
    val result = Try(session.select(s"SELECT id, year, month, day, countP, countR, result, tournamentResult, location, players FROM gameSummary WHERE id in ($id)")
      .map(row => {
        def int(name:String) = row.getInt(name)
        def players(str:String) = {
          val ps = str.split(",")
          ps.map(p => {
            val temp = p.split(";")
            val name = temp(0).replaceAll("name-", "")
            val role = Role.get(temp(1).replaceAll("role-List\\(", "").replaceAll("\\)", ""))
            (name, role)
          })
        }
        GameSummary(int("id"), Location.get(row.getString("location")), Result.get(row.getString("result")), TournamentResult.byDescription(row.getString("tournamentResult")), int("countP"), int("countR"), players(row.getString("players")).toList, int("year"), int("month"), int("day"))
      }).runWith(seq))  match {
      case Failure(e) => throw new Exception("")
      case Success(f) => f
    }
    result
  }

  private def fetchIds(s:SearchCriterion) = {
    val str = s"select id from ${s.crit.tableName}_criterion where ${s.crit.tableName} ${s.operation.entryName} ${mapVal(s.value)} ALLOW FILTERING"
    session.select(str).runWith(seq).map{
      row => row.map(a => a.getInt("id")).toList
    }
  }
}

