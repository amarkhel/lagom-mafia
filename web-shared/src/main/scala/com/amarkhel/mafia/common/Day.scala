package com.amarkhel.mafia.common

import play.api.libs.json.{Format, Json}

case class Day(year:Int, month:Int, day:Int)

object Day {
  implicit val format: Format[Day] = Json.format
}
