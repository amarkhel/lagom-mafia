package com.amarkhel.user.impl

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.amarkhel.mafia.utils.TimeUtils.timeout
import com.amarkhel.user.api.{User, UserService}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
class UserServiceImpl(registry: PersistentEntityRegistry, system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) extends UserService {
  implicit val timeout = 600 seconds
  private val currentIdsQuery = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  val superadmins = List(User("amarkhel", "andreymarkhel@gmail.com", true, "$2a$10$FIqoqNS2aa76gsXHppy2WuiA6SJaWLYxYVNZSmNDqRd38v/sl3.m.", true), User("apaapaapa", "upijcy@gmail.com", true, "$2a$10$FIqoqNS2aa76gsXHppy2WuiA6SJaWLYxYVNZSmNDqRd38v/sl3.m.", true))
    superadmins.foreach( s => {
      val a: Future[Option[User]] = (for {
        check <- checkIfEmailExist(s.email).invoke() if(!check)
        f = createUser.invoke(s)
      } yield f).flatten
      a.onComplete(c => {
        println("User saved during init " + c)
      })
    })

  override def createUser = ServiceCall { user =>
    refFor(user.name).ask(CreateUser(user)).map { _ => Some(user)}
  }

  override def updateUser(name:String) = ServiceCall { u =>
    refFor(name).ask(UpdateUser(u)).map { _ => Some(u)}
  }

  override def deleteUser(name:String) = ServiceCall { _ =>
    refFor(name).ask(DeleteUser(name))
  }

  override def getUser(name: String) = ServiceCall { _ =>
    refFor(name).ask(GetUser)/*.map {
      case Some(user) => Some(user)
      case None => throw NotFound(s"User with id $name")
    }*/
  }

  override def getUserByEmail(email: String) = ServiceCall { _ =>
    getUsers.invoke().map(_.find(_.email == email).headOption)
  }

  override def getUsers = ServiceCall { _ =>
    // Note this should never make production....
    currentIdsQuery.currentPersistenceIds()
      .filter(_.startsWith("UserEntity|"))
      .mapAsync(4) { id =>
        val entityId = id.split("\\|", 2).last
        registry.refFor[UserEntity](entityId)
          .ask(GetUser)
          .map(_.map(identity))
      }.collect {
        case Some(user) => user
      }
      .runWith(Sink.seq)
  }

  private def refFor(name: String) = registry.refFor[UserEntity](name).withAskTimeout(timeout)

  override def checkIfEmailExist(email: String) = ServiceCall { _ =>
    getUsers.invoke().map(_.count(_.email == email) > 0)
  }
}
