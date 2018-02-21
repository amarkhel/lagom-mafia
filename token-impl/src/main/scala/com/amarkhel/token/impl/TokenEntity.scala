package com.amarkhel.token.impl

import akka.Done
import com.amarkhel.token.api.MailToken
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

class TokenEntity extends PersistentEntity {
  override type Command = TokenCommand
  override type Event = TokenEvent
  override type State = (MailToken, String)
  val CREATED="CREATED"
  val EXPIRED="EXPIRED"
  val CONSUMED="CONSUMED"
  val NOT_EXIST ="NOT_EXIST"
  override def initialState: (MailToken, String) = (null, NOT_EXIST)

  override def behavior: Behavior = {
    case (_, NOT_EXIST)  =>
      Actions().onReadOnlyCommand[ConsumeToken, Done] {
        case (ConsumeToken(_), ctx, _) => ctx.invalidCommand("Token not exists")
      }.onReadOnlyCommand[TokenExist, Option[MailToken]] {
        case (TokenExist(_), ctx, _) => ctx.reply(None)
      }.onReadOnlyCommand[ExpireToken, Done] {
        case (ExpireToken(_), ctx, _) => ctx.invalidCommand("Token not exists")
      }.onCommand[CreateToken, MailToken] {
        case (CreateToken(token), ctx, _) =>
          ctx.thenPersist(TokenCreated(token))(_ => ctx.reply(token))
      }.onEvent {
        case (TokenCreated(token), _) => (token, CREATED)
      }
    case (_, CREATED)  =>
      Actions().onCommand[ConsumeToken, Done] {
        case (ConsumeToken(token), ctx, _) => ctx.thenPersist(TokenConsumed(token))(_ => ctx.reply(Done))
      }.onReadOnlyCommand[TokenExist, Option[MailToken]] {
        case (TokenExist(_), ctx, state) => ctx.reply(Some(state._1))
      }.onCommand[ExpireToken, Done] {
        case (ExpireToken(token), ctx, _) => ctx.thenPersist(TokenExpired(token))(_ => ctx.reply(Done))
      }.onReadOnlyCommand[CreateToken, MailToken] {
        case (CreateToken(_), ctx, _) => ctx.invalidCommand("Token already created")
      }.onEvent {
        case (TokenConsumed(_), state) => (state._1, CONSUMED)
        case (TokenExpired(_), state) => (state._1, EXPIRED)
      }
    case (_, CONSUMED)  =>
      Actions().onReadOnlyCommand[ConsumeToken, Done] {
        case (ConsumeToken(_), ctx, _) => ctx.invalidCommand("Token already consumed")
      }.onReadOnlyCommand[TokenExist, Option[MailToken]] {
        case (TokenExist(_), ctx, _) => ctx.reply(None)
      }.onReadOnlyCommand[ExpireToken, Done] {
        case (ExpireToken(_), ctx, _) => ctx.invalidCommand("Token already consumed")
      }.onReadOnlyCommand[CreateToken, MailToken] {
        case (CreateToken(_), ctx, _) => ctx.invalidCommand("Token already consumed")
      }
    case (_, EXPIRED)  =>
      Actions().onReadOnlyCommand[ConsumeToken, Done] {
        case (ConsumeToken(_), ctx, _) => ctx.invalidCommand("Token already expired")
      }.onReadOnlyCommand[TokenExist, Option[MailToken]] {
        case (TokenExist(_), ctx, _) => ctx.reply(None)
      }.onReadOnlyCommand[ExpireToken, Done] {
        case (ExpireToken(_), ctx, _) => ctx.invalidCommand("Token already expired")
      }.onReadOnlyCommand[CreateToken, MailToken] {
        case (CreateToken(_), ctx, _) => ctx.invalidCommand("Token already expired")
      }
  }
}

sealed trait TokenEvent

case class TokenCreated(token:MailToken) extends TokenEvent
case class TokenConsumed(token:String) extends TokenEvent
case class TokenExpired(token:String) extends TokenEvent

object TokenCreated {
  implicit val format: Format[TokenCreated] = Json.format
}

object TokenConsumed {
  implicit val format: Format[TokenConsumed] = Json.format
}

object TokenExpired {
  implicit val format: Format[TokenExpired] = Json.format
}

sealed trait TokenCommand

case class CreateToken(token:MailToken) extends TokenCommand with ReplyType[MailToken]
case class ConsumeToken(token:String) extends TokenCommand with ReplyType[Done]
case class ExpireToken(token:String) extends TokenCommand with ReplyType[Done]
case class TokenExist(token:String) extends TokenCommand with ReplyType[Option[MailToken]]

object CreateToken {
  implicit val format: Format[CreateToken] = Json.format
}

object ConsumeToken {
  implicit val format: Format[ConsumeToken] = Json.format
}

object ExpireToken {
  implicit val format: Format[ExpireToken] = Json.format
}

object TokenExist {
  implicit val format: Format[TokenExist] = Json.format
}

object TokenSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[MailToken],
    JsonSerializer[TokenCreated],
    JsonSerializer[TokenConsumed],
    JsonSerializer[TokenExpired],
    JsonSerializer[CreateToken],
    JsonSerializer[ExpireToken],
    JsonSerializer[ConsumeToken],
    JsonSerializer[TokenExist]
  )
}