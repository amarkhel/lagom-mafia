package com.amarkhel.mafia.common

import scalaz._
import Scalaz._
case class Message(target: Gamer, content: String, smiles: List[String], timeFromStart:Int) {
  require(content != null)

  def fromRole = target.role
  def hasSmiles = countSmiles > 0
  def text = content
  def author = target.name
  def from(player: Gamer) = target == player
  def hasSmileFrom(player: Gamer) = hasSmiles && from(player)
  def countSmiles = smiles.size
  def countSmilesFrom(player: Gamer) = from(player) ? countSmiles | 0
}
