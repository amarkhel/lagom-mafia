package com.amarkhel.mafia.processor.api

case class SearchRequest(list:List[SearchCriterion])

case class SearchCriterion(crit:GameCriterion, operation:Operation, value:Value)

import enumeratum._
import play.api.libs.json._

object SearchRequest {
  implicit val format: Format[SearchRequest] = Json.format
}
object SearchCriterion {
  implicit val format: Format[SearchCriterion] = Json.format
}

sealed abstract class Operation(override val entryName:String) extends EnumEntry

object Operation extends Enum[Operation] {

  implicit val criterionReads: Reads[Operation] = Reads {
    case JsString(s) => JsSuccess(Operation.withName(s))
  }

  implicit val criterionWrites: Writes[Operation] = Writes { loc =>
    JsString(loc.entryName)
  }

  val values = findValues

  case object EQ        extends Operation("=")
  case object CONTAINS  extends Operation("contains")
  case object LT        extends Operation("<")
  case object GT        extends Operation(">")
  case object LE        extends Operation("<=")
  case object GE        extends Operation(">=")
  case object IN        extends Operation("in")
  case object BETWEEN   extends Operation("between")
}

sealed abstract class Value(entryName:Any) extends EnumEntry

case class IntValue(value:Int) extends Value(value)
case class StringValue(value:String) extends Value(value)
case class DoubleValue(value:Double) extends Value(value)
case class ListIntValue(value:List[Int]) extends Value(value)
case class ListStrValue(value:List[String]) extends Value(value)
case class ListDoubleValue(value:List[Double]) extends Value(value)

object Value {

  implicit val str = Json.format[StringValue]
  implicit val int = Json.format[IntValue]
  implicit val double = Json.format[DoubleValue]
  implicit val liststr = Json.format[ListStrValue]
  implicit val listint = Json.format[ListIntValue]
  implicit val listdouble = Json.format[ListDoubleValue]
  implicit val format: Format[Value] = Json.format

  implicit object FormatValue extends Format[Value] {
    def writes(o: Value) = o match {
      case i@IntValue(_)     => Json.obj("IntValue"     -> Json.toJson(i)(int))
      case s@StringValue(_) => Json.obj("StrValue" -> Json.toJson(s)(str))
      case d@DoubleValue(_) => Json.obj("DoubleValue" -> Json.toJson(d)(double))
      case li@ListIntValue(_) => Json.obj("ListIntValue" -> Json.toJson(li)(listint))
      case ls@ListStrValue(_) => Json.obj("ListStrValue" -> Json.toJson(ls)(liststr))
      case ld@ListDoubleValue(_) => Json.obj("ListDoubleValue" -> Json.toJson(ld)(listdouble))
    }

    def reads(j: JsValue) = (
      Json.fromJson((j \ "IntValue").get)(int) orElse
        Json.fromJson((j \ "StrValue").get)(str) orElse
        Json.fromJson((j \ "DoubleValue").get)(double) orElse
        Json.fromJson((j \ "ListIntValue").get)(listint) orElse
        Json.fromJson((j \ "ListStrValue").get)(liststr) orElse
        Json.fromJson((j \ "ListDoubleValue").get)(listdouble)
      )
  }
}
