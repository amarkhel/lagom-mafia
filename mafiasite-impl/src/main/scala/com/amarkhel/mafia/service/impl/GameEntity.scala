package com.amarkhel.mafia.service.impl

import com.amarkhel.mafia.common.Game
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import org.slf4j.LoggerFactory

class GameEntity extends PersistentEntity {

  private final val log = LoggerFactory.getLogger(classOf[GameEntity])

  override type Command = GameCommand
  override type Event = GameEvent
  override type State = Option[Game]
  override def initialState = None

  override def behavior: Behavior = {
    Actions().onReadOnlyCommand[LoadGame.type, State] {
      case (LoadGame, ctx, state) => ctx.reply(state)
    }.onCommand[SaveGame, Game] {
      case (SaveGame(game), ctx, _) =>
        ctx.thenPersist(GameSaved(game))(_ => {
          log.debug(s"${game.id} is handled")
          ctx.reply(game)
        })
    }.onEvent {
      case (GameSaved(game), _) => Some(game)
    }
  }
}

sealed trait GameEvent extends AggregateEvent[GameEvent] {
  override def aggregateTag: AggregateEventTagger[GameEvent] = GameEvent.Tag
}
case object GameEvent{
  val Tag = AggregateEventTag.sharded[GameEvent](20)
}
case class GameSaved(game:Game) extends GameEvent

sealed trait GameCommand
case class SaveGame(game:Game) extends GameCommand with ReplyType[Game]
case object LoadGame extends GameCommand with ReplyType[Option[Game]]