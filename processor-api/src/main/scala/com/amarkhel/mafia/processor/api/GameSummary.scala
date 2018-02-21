package com.amarkhel.mafia.processor.api

import com.amarkhel.mafia.common.{Location, Result, Role, TournamentResult}
import play.api.libs.json.{Format, Json}
import com.amarkhel.mafia.utils.JsonFormats._

case class GameSummary(id:Int, location:Location, result:Result, tournamentResult:TournamentResult, countPlayers:Int, countRounds:Int, players:List[(String, Role)], year:Int, month:Int, day:Int)
object GameSummary {
  implicit val format: Format[GameSummary] = Json.format
}