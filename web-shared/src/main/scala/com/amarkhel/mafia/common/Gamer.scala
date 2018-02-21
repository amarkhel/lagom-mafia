package com.amarkhel.mafia.common

import com.amarkhel.mafia.common.Role._
import play.api.libs.json.Json
import com.amarkhel.mafia.utils.JsonFormats._

case class Gamer(name:String, role:Role){
  def isMafia = inRoles(List(BOSS, MAFIA))
  def isKomissar = inRoles(List(KOMISSAR, SERZHANT))
  def isCitizen = inRoles(List(CHILD, CITIZEN))
  def isChild = inRoles(List(CHILD))
  def isJustCitizen = isInRole(CITIZEN)
  def isPositive = !isNegative
  def isNegative = isMafia || isManiac
  def isSomeRole = !isInRole(CITIZEN)
  def canBeKilledByMafia = !isMafia && !isChild
  def isDoctor = isInRole(DOCTOR)
  def isManiac = isInRole(MANIAC)
  def isBoss = isInRole(BOSS)
  def isMainKomissar = isInRole(KOMISSAR)
  def isSerzhant = isInRole(SERZHANT)
  def isImportantGoodRole = inRoles(List(KOMISSAR, SERZHANT, DOCTOR))
  private def inRoles(roles: List[Role]) = roles contains role
  private[common] def isInRole(role: Role) = inRoles(List(role))
}

object Gamer {
  implicit val format = Json.format[Gamer]
}
