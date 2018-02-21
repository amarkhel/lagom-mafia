package com.amarkhel.user.api

import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}
import com.amarkhel.mafia.utils.JsonFormats._
import com.mohiva.play.silhouette.api.Identity

trait UserService extends Service {
  def createUser: ServiceCall[User, Option[User]]
  def getUser(name: String): ServiceCall[NotUsed, Option[User]]
  def getUserByEmail(email: String): ServiceCall[NotUsed, Option[User]]
  def updateUser(name: String): ServiceCall[User, Option[User]]
  def deleteUser(name: String): ServiceCall[NotUsed, Boolean]
  def checkIfEmailExist(email: String): ServiceCall[NotUsed, Boolean]

  // Remove once we have a proper user service
  def getUsers: ServiceCall[NotUsed, Seq[User]]

  def descriptor = {
    import Service._
    named("user").withCalls(
      pathCall("/api/user", createUser),
      pathCall("/api/update/:name", updateUser _),
      pathCall("/api/delete/:name", deleteUser _),
      pathCall("/api/unique/:email", checkIfEmailExist _),
      pathCall("/api/user/:name", getUser _),
      pathCall("/api/user/email/:email", getUserByEmail _),
      pathCall("/api/users", getUsers)
    )
  }
  import play.api.libs.json._

  implicit def optionReads[T: Format]: Reads[Option[T]] = Reads {
    case JsNull => JsSuccess(None)
    case other => other.validate[T].map(Some.apply)
  }

  implicit def optionWrites[T: Format]: Writes[Option[T]] = Writes {
    case None => JsNull
    case Some(t) => Json.toJson(t)
  }
}

case class User(name: String, email: String, emailConfirmed: Boolean,  password: String, isAdmin: Boolean) extends Identity

object User {
  implicit val format: Format[User] = Json.format
}