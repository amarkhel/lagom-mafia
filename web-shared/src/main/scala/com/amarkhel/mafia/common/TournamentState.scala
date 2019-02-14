package com.amarkhel.mafia.common

import java.time.{LocalDateTime, format}
import java.time.format.DateTimeFormatter

sealed trait GameMessage
object GameMessage extends Serializable {
  import prickle._
  implicit val pickler: PicklerPair[GameMessage] = CompositePickler[GameMessage].concreteType[FINISHED].concreteType[TIME].concreteType[Error].concreteType[CONFIRM].concreteType[Events]

  case class Events(events:List[GameEvent], state:TournamentGameState) extends GameMessage
  case class FINISHED(points:Double, correct:Int, state:TournamentGameState, mafias:String) extends GameMessage
  case class CONFIRM(state:TournamentGameState) extends GameMessage
  case class TIME(state:TournamentGameState) extends GameMessage
  case class Error(msg:String, state:TournamentGameState) extends GameMessage
}
case class TournamentGameState(tournament:String, player:String, players:List[String],currentRound:Int, chosen:Map[String, (Int, Int)], started:String, eliminatedPlayers:List[Gamer] = List.empty, countMafia:Int) extends Serializable {
  def next = copy(currentRound = currentRound +1)
  def choose(player:String, time:Int) = copy(chosen = chosen + (player -> (currentRound, time)))
  def countPossibleChoices = {
    countMafia - chosen.size - eliminatedPlayers.filter(_.isMafia).map(_.name).filter(x => !chosen.contains(x)).size
  }
}




