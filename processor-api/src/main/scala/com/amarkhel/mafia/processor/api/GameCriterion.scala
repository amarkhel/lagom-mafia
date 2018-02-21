package com.amarkhel.mafia.processor.api

import com.amarkhel.mafia.common.Game
import enumeratum._
import play.api.libs.json._

sealed abstract class GameCriterion(val tableName: String, val extractor:Game => Any, val columnType:String) extends EnumEntry

case object GameCriterion extends Enum[GameCriterion] {
  val values = findValues
  implicit val criterionReads: Reads[GameCriterion] = Reads {
    case JsString(s) => JsSuccess(GameCriterion.withName(s))
  }

  implicit val criterionWrites: Writes[GameCriterion] = Writes { loc =>
    JsString(loc.entryName)
  }
  case object LocationCriterion extends GameCriterion("location", _.location.name, "text")
  case object ResultCriterion extends GameCriterion("result", _.result.descr, "text")
  case object TournamentResultCriterion extends GameCriterion("tournamentResult", _.tournamentResult.descr, "text")
  case object CountPlayersCriterion extends GameCriterion("countPlayers", _.playersSize, "int")
  case object CountRoundsCriterion extends GameCriterion("countRounds", _.countRounds, "int")
  case object PlayersCriterion extends GameCriterion("players", _.toPlayersString, "text")
  case object YearCriterion extends GameCriterion("year",_.day.year, "int")
  case object MonthCriterion extends GameCriterion("month", _.day.month, "int")
  case object DayCriterion extends GameCriterion("day", _.day.day, "int")
  case object AverageMessagesPerRoundCriterion extends GameCriterion("averageMessages", _.averageMessagesPerRound, "double")
}
