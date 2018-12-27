package com.amarkhel.mafia.service.impl

import java.time.LocalDateTime

import akka.Done
import com.amarkhel.mafia.common.{Day, Game}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

class ExtractorEntity extends PersistentEntity {

  private val log = LoggerFactory.getLogger(classOf[ExtractorEntity])
  override type State = ExtractorState
  override type Command = ExtractorCommand
  override type Event = ExtractorEvent

  override def behavior: Behavior = {
        Actions().onCommand[FinishGame , Int] {
          case (FinishGame(game), ctx, _) =>
            ctx.thenPersist(GameFinished(game))(_ => ctx.reply(game.id))
        }.onCommand[CompleteDay , Done] {
          case (CompleteDay(day), ctx, _) =>
            ctx.thenPersist(DayCompleted(day))(_ => {
              log.debug(s"$day saved ${LocalDateTime.now}")
              ctx.reply(Done)
            })
        }.onReadOnlyCommand[GetStatusCommand.type , Day] {
          case (GetStatusCommand, ctx, state) =>
            ctx.reply(state.lastDay)
        }.onCommand[LoadError , Done] {
          case (LoadError(err), ctx, _) =>
            ctx.thenPersist(Error(err))(_ => ctx.reply(Done))
        }.onCommand[ClearCommand.type, Done] {
          case (ClearCommand, ctx, _) =>
            ctx.thenPersist(ClearEvent)(_ => ctx.reply(Done))
        }.onEvent {
          case (GameFinished(g), state) => {log.debug(g.id + " is saved to event log "); state}
          case (DayCompleted(day), state) => {
            log.debug(s"$day completed ${LocalDateTime.now}")
            state.copy(lastDay = day)
          }
          case (ClearEvent, state) => state.copy(lastDay = initialState.lastDay)
          case (Error(_), state) => state
        }
  }

  override def initialState = ExtractorState(Util.firstDay)
}

case class ExtractorState(lastDay:Day)

trait ExtractorCommand
case class FinishGame(game: Game) extends ExtractorCommand with ReplyType[Int]
case class LoadError(error:String) extends ExtractorCommand with ReplyType[Done]
case class CompleteDay(day: Day) extends ExtractorCommand with ReplyType[Done]
case object ClearCommand extends ExtractorCommand with ReplyType[Done]
case object GetStatusCommand extends ExtractorCommand with ReplyType[Day]

trait ExtractorEvent extends AggregateEvent[ExtractorEvent] {
  override def aggregateTag: AggregateEventTagger[ExtractorEvent] = ExtractorEvent.Tag
}
case object ExtractorEvent{
  val Tag = AggregateEventTag.sharded[ExtractorEvent](5)
}
case class GameFinished(game: Game) extends ExtractorEvent
case class DayCompleted(day: Day) extends ExtractorEvent
case class Error(error:String) extends ExtractorEvent
case object ClearEvent extends ExtractorEvent