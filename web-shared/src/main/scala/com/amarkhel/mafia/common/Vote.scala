package com.amarkhel.mafia.common

case class Vote(target:Gamer, destination:Gamer) {

  require(target != destination)

  def from(player:Gamer) = target == player

  def toMafiaFrom(player:Gamer) = from(player) && destination.isMafia

  def toMafia = destination.isMafia

  def toImportantGoodRole = destination.isImportantGoodRole

  def toRole(role:Role) = destination.isInRole(role)

  def toSomeRole = destination.isSomeRole

  def to(player:Gamer) = destination == player

  def fromRole(role:Role) = target.isInRole(role)

  def fromMafia = target.isInRole(Role.MAFIA)

  def toPlayerFromMafia(player:Gamer) = to(player) && target.isMafia

  def fromPlayerToGoodImportantRole(player:Gamer) = from(player) && destination.isImportantGoodRole

  def fromPlayerToSomeRole(player:Gamer) = from(player) && destination.isSomeRole

  def fromPlayerToRole(player:Gamer, role:Role) = from(player) && destination.isInRole(role)
}
