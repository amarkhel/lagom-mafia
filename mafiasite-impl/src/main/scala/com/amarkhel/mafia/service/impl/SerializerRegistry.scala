package com.amarkhel.mafia.service.impl

import com.amarkhel.mafia.common.{Day, Game, Gamer}
import com.amarkhel.mafia.dto.GameContent
import com.amarkhel.mafia.utils.JsonFormats.singletonFormat
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}
import com.amarkhel.mafia.utils.JsonFormats._

object SerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[Gamer],
    JsonSerializer[Day],
    JsonSerializer[Game],
    JsonSerializer[DaySaved],
    JsonSerializer[SaveDay],
    JsonSerializer[LoadDay.type],
    JsonSerializer[GameSaved],
    JsonSerializer[SaveGame],
    JsonSerializer[LoadGame.type],
    JsonSerializer[DayCompleted],
    JsonSerializer[Error],
    JsonSerializer[ExtractorState],
    // Commands and replies
    JsonSerializer[FinishGame],
    JsonSerializer[CompleteDay],
    JsonSerializer[LoadError],
    JsonSerializer[GetStatusCommand.type],
    JsonSerializer[ClearCommand.type],
    // Events
    JsonSerializer[GameFinished],
    JsonSerializer[ClearEvent.type]
  )

  implicit val ce: Format[ClearEvent.type] = singletonFormat(ClearEvent)
  implicit val gf: Format[GameFinished] = Json.format
  implicit val dc: Format[DayCompleted] = Json.format
  implicit val ef: Format[Error] = Json.format
  implicit val format: Format[ExtractorState] = Json.format
  implicit val fgf: Format[FinishGame] = Json.format
  implicit val lef: Format[LoadError] = Json.format
  implicit val cdf: Format[CompleteDay] = Json.format
  implicit val cc: Format[ClearCommand.type] = singletonFormat(ClearCommand)
  implicit val gsc: Format[GetStatusCommand.type] = singletonFormat(GetStatusCommand)
  implicit val sg: Format[SaveGame] = Json.format
  implicit val lg: Format[LoadGame.type] = singletonFormat(LoadGame)
  implicit val gs: Format[GameSaved] = Json.format
  implicit val gcf: Format[GameContent] = Json.format
  implicit val sddf: Format[SaveDay] = Json.format
  implicit val ldf: Format[LoadDay.type] = singletonFormat(LoadDay)
  implicit val dssv: Format[DaySaved] = Json.format
}
