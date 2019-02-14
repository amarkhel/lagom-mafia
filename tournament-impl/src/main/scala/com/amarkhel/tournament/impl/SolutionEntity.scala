package com.amarkhel.tournament.impl

import com.amarkhel.tournament.api.{GameDescription, Solution}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

class SolutionEntity extends PersistentEntity {
  override type Command = SolutionCommand
  override type Event = SolutionEvent
  override type State = String
  override def initialState = ""

  override def behavior: Behavior = {
    case _ => Actions().onCommand[PostSolution, String] {
      case (PostSolution(game, solution, player), ctx, _) => ctx.thenPersist(SolutionPosted(game, solution, player))(_ => ctx.reply(""))
    }.onEvent {
      case (SolutionPosted(_, _, _), _) => ""
    }
  }
}

trait SolutionEvent extends AggregateEvent[SolutionEvent] {
  override def aggregateTag: AggregateEventTagger[SolutionEvent] = SolutionEvent.Tag
}
case object SolutionEvent{
  val Tag = AggregateEventTag.sharded[SolutionEvent](5)
  implicit val formatTC: Format[SolutionPosted] = Json.format
}

case class SolutionPosted(game:GameDescription, solution:Solution, player:String) extends SolutionEvent

sealed trait SolutionCommand

object SolutionCommand {
  implicit val formatCT: Format[PostSolution] = Json.format
}

case class PostSolution(game:GameDescription, solution:Solution, player:String) extends SolutionCommand with ReplyType[String]

object SolutionSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[SolutionPosted],
    JsonSerializer[PostSolution]
  )
}
