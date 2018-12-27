package com.amarkhel.mafia.common

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class Day(year:Int, month:Int, day:Int) {
  def isBeforeOrEqual(date: LocalDate): Boolean = {
    LocalDate.of(year, month, day).isBefore(date.plusDays(1))
  }
}

object Day {
  implicit val format: Format[Day] = Json.format
}
