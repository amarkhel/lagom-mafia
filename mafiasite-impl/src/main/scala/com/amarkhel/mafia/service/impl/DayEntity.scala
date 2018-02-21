package com.amarkhel.mafia.service.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

class DayEntity extends PersistentEntity {
  override type Command = DayCommand
  override type Event = DayEvent
  override type State = Option[String]
  override def initialState = None

  override def behavior: Behavior = {
      Actions().onReadOnlyCommand[LoadDay.type, Option[String]] {
        case (LoadDay, ctx, state) => ctx.reply(state)
      }.onCommand[SaveDay, Done] {
        case (SaveDay(content), ctx, _) =>
        ctx.thenPersist(DaySaved(content))(_ => ctx.reply(Done))
      }.onEvent {
        case (DaySaved(content), _) => Option(content)
      }
  }
}

sealed trait DayEvent
case class DaySaved(content:String) extends DayEvent

sealed trait DayCommand
case class SaveDay(content:String) extends DayCommand with ReplyType[Done]
case object LoadDay extends DayCommand with ReplyType[Option[String]]