package com.amarkhel.mafia.common

import com.amarkhel.mafia.utils.TextUtils._
sealed trait Role {
  def role: List[String]
}
object Role extends Serializable {
  import prickle._
  implicit val pickler: PicklerPair[Role] = CompositePickler[Role].
    concreteType[BOSS.type].concreteType[CITIZEN.type].concreteType[MAFIA.type].concreteType[KOMISSAR.type].concreteType[SERZHANT.type]
    .concreteType[CHILD.type].concreteType[MANIAC.type].concreteType[DOCTOR.type]
  case object CITIZEN extends Role { val role = List("Честный житель") }
  case object MAFIA extends Role { val role = List("Мафия") }
  case object BOSS extends Role { val role = List("Босс") }
  case object KOMISSAR extends Role { val role = List("Комиссар") }
  case object SERZHANT extends Role { val role = List("Сержант") }
  case object CHILD extends Role { val role = List("Внебрачный сын босса", "Внебрачная дочь босса", "Внебрачный ребёнок босса", "Внебрачное дитя босса") }
  case object MANIAC extends Role { val role = List("Маньяк") }
  case object DOCTOR extends Role { val role = List("Врач") }
  val values: List[Role] = List(Role.CITIZEN, Role.MAFIA, Role.BOSS, Role.KOMISSAR, Role.SERZHANT, Role.CHILD, Role.MANIAC, Role.DOCTOR)
  def get(role: String): Role = {
    require(role != null)
    val filtered = role.erase("\\(в тюрьме\\)", "\\(убит\\)", "\\(вышел по тайм-ауту\\)").trim
    values.find(_.role contains filtered).getOrElse(throw new IllegalArgumentException(s"Role $filtered not found"))
  }

}
