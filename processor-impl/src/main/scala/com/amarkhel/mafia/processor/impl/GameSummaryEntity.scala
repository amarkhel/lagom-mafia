package com.amarkhel.mafia.processor.impl

import akka.Done
import com.amarkhel.mafia.common.Game
import com.amarkhel.mafia.processor.api.GameSummary
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import org.slf4j.LoggerFactory

class GameSummaryEntity extends PersistentEntity {

  private final val log = LoggerFactory.getLogger(getClass)

  override type Command = GameSummaryCommand
  override type Event = GameSummaryEvent
  override type State = Option[Boolean]
  override def initialState = None

  override def behavior: Behavior = {
    Actions().onCommand[SaveGameSummary, Done] {
      case (SaveGameSummary(game, source), ctx, _) =>
        ctx.thenPersist(GameSummarySaved(game, source))(_ => {
          log.debug(s"${game.id} is saved")
          ctx.reply(Done)
        })
    }.onEvent {
      case (GameSummarySaved(_, _), _) => Some(true)
    }
  }
}

sealed trait GameSummaryEvent extends AggregateEvent[GameSummaryEvent] {
  override def aggregateTag: AggregateEventTagger[GameSummaryEvent] = GameSummaryEvent.Tag
}
case object GameSummaryEvent{
  val Tag = AggregateEventTag.sharded[GameSummaryEvent](20)
}
case class GameSummarySaved(game:GameSummary, source:Game) extends GameSummaryEvent

sealed trait GameSummaryCommand
case class SaveGameSummary(game:GameSummary, source:Game) extends GameSummaryCommand with ReplyType[Done]
