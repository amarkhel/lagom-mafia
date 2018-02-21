package com.amarkhel.token.impl

import java.util.UUID

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.amarkhel.token.api.{MailToken, TokenService}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext

class TokenServiceImpl(registry: PersistentEntityRegistry, system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) extends TokenService {

  override def createToken(email:String, isSignUp: Boolean) = ServiceCall { _ =>
    val token = generateToken(email, isSignUp)
    refFor(token.id).ask(CreateToken(token))
  }

  private def generateToken(email:String, isSignUp: Boolean) = MailToken(UUID.randomUUID().toString, email, (new DateTime()).plusHours(24), isSignUp)

  override def consumeToken(token:String) = ServiceCall { _ =>
    refFor(token).ask(ConsumeToken(token))
  }

  override def expireToken(token:String) = ServiceCall { _ =>
    refFor(token).ask(ExpireToken(token))
  }

  override def getToken(token: String) = ServiceCall { _ =>
    refFor(token).ask(TokenExist(token))
  }

  private def refFor(token: String) = registry.refFor[TokenEntity](token)
}
