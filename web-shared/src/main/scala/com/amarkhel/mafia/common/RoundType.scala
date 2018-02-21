package com.amarkhel.mafia.common

sealed trait RoundType{def description:String}

object RoundType extends Serializable {
  import prickle._
  implicit val pickler: PicklerPair[RoundType] = CompositePickler[RoundType].
    concreteType[BOSS.type].concreteType[CITIZEN.type].concreteType[MAFIA.type].concreteType[KOMISSAR.type].concreteType[INITIAL.type]
  case object BOSS extends RoundType{val description = "Ход босса"}
  case object CITIZEN extends RoundType{val description = "Ход честных"}
  case object MAFIA extends RoundType{val description = "Ход мафии"}
  case object KOMISSAR extends RoundType{val description = "Ход комиссара"}
  case object INITIAL extends RoundType{val description = "Начальный ход"}
}
