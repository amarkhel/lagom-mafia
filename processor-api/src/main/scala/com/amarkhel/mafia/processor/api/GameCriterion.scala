package com.amarkhel.mafia.processor.api

import com.amarkhel.mafia.common.Game
import enumeratum._
import play.api.libs.json._

sealed abstract class GameCriterion(override val entryName:String, val tableName: String, val extractor:Game => Any, val columnType:String) extends EnumEntry

case object GameCriterion extends Enum[GameCriterion] {
  val values = findValues
  implicit val criterionReads: Reads[GameCriterion] = Reads {
    case JsString(s) => JsSuccess(GameCriterion.withName(s))
  }

  implicit val criterionWrites: Writes[GameCriterion] = Writes { loc =>
    JsString(loc.entryName)
  }
  case object LocationCriterion extends GameCriterion("Улица", "location", _.location.name, "text")
  case object ResultCriterion extends GameCriterion("Результат","result", _.result.descr, "text")
  case object TournamentResultCriterion extends GameCriterion("Результат учитывая омон","tournamentResult", _.tournamentResult.descr, "text")
  case object CountPlayersCriterion extends GameCriterion("Количество игроков","countPlayers", _.playersSize, "int")
  case object CountRoundsCriterion extends GameCriterion("Количество раундов","countRounds", _.countRounds, "int")
  case object PlayersCriterion extends GameCriterion("Игроки","players", _.toPlayersString, "text")
  case object YearCriterion extends GameCriterion("Год","year",_.day.year, "int")
  case object MonthCriterion extends GameCriterion("Месяц","month", _.day.month, "int")
  case object DayCriterion extends GameCriterion("День","day", _.day.day, "int")
  case object AverageMessagesPerRoundCriterion extends GameCriterion("Среднее количество сообщений за игру","averageMessages", _.averageMessagesPerRound, "double")
}
