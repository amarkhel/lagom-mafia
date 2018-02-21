package com.amarkhel.user.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}
import com.amarkhel.mafia.utils.JsonFormats._
import com.amarkhel.user.api.User
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

class UserEntity extends PersistentEntity {
  override type Command = UserCommand
  override type Event = UserEvent
  override type State = Option[User]
  override def initialState: Option[User] = None

  override def behavior: Behavior = {
    case Some(_) =>
      Actions().onReadOnlyCommand[GetUser.type, Option[User]] {
        case (GetUser, ctx, state) => ctx.reply(state)
      }.onReadOnlyCommand[CreateUser, Option[User]] {
        case (CreateUser(_), ctx, _) => ctx.invalidCommand("User already exists")
      }.onCommand[DeleteUser, Boolean] {
        case (DeleteUser(name), ctx, _) =>
          ctx.thenPersist(UserDeleted(name))(_ => ctx.reply(true))
      }.onCommand[UpdateUser, Option[User]] {
        case (UpdateUser(user), ctx, _) =>
          ctx.thenPersist(UserUpdated(user))(_ => ctx.reply(Some(user)))
      }.onEvent {
        case (UserDeleted(_), _) => None
      }.onEvent {
        case (UserUpdated(user), _) => Some(user)
      }
    case None =>
      Actions().onReadOnlyCommand[GetUser.type, Option[User]] {
        case (GetUser, ctx, state) => ctx.reply(state)
      }.onReadOnlyCommand[UpdateUser, Option[User]] {
        case (UpdateUser(_), ctx, _) => ctx.invalidCommand("User not exists")
      }.onReadOnlyCommand[DeleteUser, Boolean] {
        case (DeleteUser(_), ctx, _) => ctx.invalidCommand("User not exists")
      }.onCommand[CreateUser, Option[User]] {
        case (CreateUser(user), ctx, _) =>
          ctx.thenPersist(UserCreated(user))(_ => ctx.reply(Some(user)))
      }.onEvent {
        case (UserCreated(user), _) => Some(user)
      }
  }
}

sealed trait UserEvent

case class UserCreated(user:User) extends UserEvent
case class UserUpdated(user:User) extends UserEvent
case class UserDeleted(name:String) extends UserEvent

object UserCreated {
  implicit val format: Format[UserCreated] = Json.format
}

object UserUpdated {
  implicit val format: Format[UserUpdated] = Json.format
}

object UserDeleted {
  implicit val format: Format[UserDeleted] = Json.format
}

sealed trait UserCommand

case class CreateUser(user:User) extends UserCommand with ReplyType[Option[User]]
case class UpdateUser(user:User) extends UserCommand with ReplyType[Option[User]]
case class DeleteUser(name:String) extends UserCommand with ReplyType[Boolean]
case object GetUser extends UserCommand with ReplyType[Option[User]] {
  implicit val format: Format[GetUser.type] = singletonFormat(GetUser)
}

object CreateUser {
  implicit val format: Format[CreateUser] = Json.format
}

object UpdateUser {
  implicit val format: Format[UpdateUser] = Json.format
}

object DeleteUser {
  implicit val format: Format[DeleteUser] = Json.format
}

object UserSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[User],
    JsonSerializer[UserCreated],
    JsonSerializer[UserUpdated],
    JsonSerializer[UserDeleted],
    JsonSerializer[CreateUser],
    JsonSerializer[GetUser.type],
    JsonSerializer[UpdateUser],
    JsonSerializer[DeleteUser]
  )
}