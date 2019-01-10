package com.amarkhel.mafia.common

case class TournamentGameState(tournament:String, player:String, events:List[GameEvent], currentRound:Int, chosen:Map[String, Int], error:String, finished:Boolean, timeToEnd:String=""){
  def finish = copy(currentRound = currentRound +1, finished = true)
  def next = copy(currentRound = currentRound +1)
  def choose(player:String) = copy(chosen = chosen + (player -> currentRound))
  def withEvents(events:List[GameEvent]) = {
    var count = 1
    val filtered = events.dropWhile({ e =>
      if(e.isInstanceOf[RoundStarted]){
        count = count + 1
      }
      count < currentRound
    }).takeWhile(!_.isInstanceOf[RoundStarted])
    copy(events = filtered)
  }
}
