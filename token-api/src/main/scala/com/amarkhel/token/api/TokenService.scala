package com.amarkhel.token.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

trait TokenService extends Service {
  def createToken(email:String, isSignup:Boolean): ServiceCall[NotUsed, MailToken]
  def getToken(token: String): ServiceCall[NotUsed, Option[MailToken]]
  def expireToken(token: String): ServiceCall[MailToken, Done]
  def consumeToken(token: String): ServiceCall[MailToken, Done]

  import play.api.libs.json._

  implicit def optionReads[T: Format]: Reads[Option[T]] = Reads {
    case JsNull => JsSuccess(None)
    case other => other.validate[T].map(Some.apply)
  }

  implicit def optionWrites[T: Format]: Writes[Option[T]] = Writes {
    case None => JsNull
    case Some(t) => Json.toJson(t)
  }

  def descriptor = {
    import Service._
    named("token").withCalls(
      pathCall("/api/token?email&isSignup", createToken _),
      pathCall("/api/token/expire/:token", expireToken _),
      pathCall("/api/token/consume/:token", consumeToken _),
      pathCall("/api/token/:token", getToken _)
    )
  }
}

case class MailToken(id: String, email: String, expirationTime: DateTime, isSignUp:Boolean){
  def isExpired = expirationTime.isBeforeNow
}

object MailToken {
  implicit val format: Format[MailToken] = Json.format
}