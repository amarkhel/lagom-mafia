package com.amarkhel.mafia.processor.impl

import com.amarkhel.mafia.common.{Day, Game, Gamer}
import com.amarkhel.mafia.dto.GameContent
import com.amarkhel.mafia.utils.JsonFormats.singletonFormat
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

object SerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[Gamer],
    JsonSerializer[Day],
    JsonSerializer[Game]
  )
}
