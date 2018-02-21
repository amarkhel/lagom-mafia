package com.amarkhel.mafia.utils

import com.amarkhel.mafia.common._
import julienrf.json.derived
import play.api.libs.json._

object JsonFormats {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = Reads {
    case JsString(s) =>
      try {
        JsSuccess(enum.withName(s).asInstanceOf[E#Value])
      } catch {
        case _: NoSuchElementException =>
          JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not contain '$s'")
      }
    case _ => JsError("String value expected")
  }
  def enumWrites[E <: Enumeration]: Writes[E#Value] = Writes(v => JsString(v.toString))
  def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

  def singletonReads[O](singleton: O): Reads[O] = Reads{_ => JsSuccess(singleton)}

  def singletonWrites[O]: Writes[O] = Writes { singleton =>
    Json.obj("value" -> singleton.getClass.getSimpleName)
  }
  def singletonFormat[O](singleton: O): Format[O] = {
    Format(singletonReads(singleton), singletonWrites)
  }

  implicit val locationReads: Reads[Location] = Reads {
    case JsString(s) => JsSuccess(Location.get(s))
  }

  implicit val locationWrites: Writes[Location] = Writes { loc =>
    JsString(loc.name)
  }

  implicit val resultReads: Reads[Result] = Reads {
    case JsString(s) => JsSuccess(Result.get(s))
  }

  implicit val resultWrites: Writes[Result] = Writes { res =>
    JsString(res.descr)
  }

  implicit val roleReads: Reads[Role] = Reads {
    case JsString(s) => JsSuccess(Role.get(s))
  }

  implicit val roleWrites: Writes[Role] = Writes { res =>
    JsString(res.role.head)
  }

  implicit val dayReads: Reads[Day] = Json.reads[Day]

  implicit val dayWrites: Writes[Day] = Json.writes[Day]

  implicit val trReads: Reads[TournamentResult] = Reads {
    case JsString(s) => JsSuccess(TournamentResult.byDescription(s))
  }

  implicit val trWrites: Writes[TournamentResult] = Writes { tr =>
    JsString(tr.descr)
  }

  implicit val gamerReads: Reads[Gamer] = Json.reads[Gamer]

  implicit val gamerWrites: Writes[Gamer] = Json.writes[Gamer]

  implicit val gameEvents: Format[GameEvent] = derived.flat.oformat((__ \ "type").format[String])

  implicit val finishTypes: Format[FinishStatus] = derived.flat.oformat((__ \ "type").format[String])

  implicit val format: Format[RoundType] = derived.flat.oformat((__ \ "type").format[String])

  implicit val voteReads: Reads[Vote] = Json.reads[Vote]

  implicit val voteWrites: Writes[Vote] = Json.writes[Vote]

  implicit val messageReads: Reads[Message] = Json.reads[Message]

  implicit val messageWrites: Writes[Message] = Json.writes[Message]

  implicit val roundReads: Reads[Round] = Json.reads[Round]

  implicit val roundWrites: Writes[Round] = Json.writes[Round]

  implicit val gameReads: Reads[Game] = Json.reads[Game]

  implicit val gameWrites: Writes[Game] = Json.writes[Game]

}
