package controllers

import akka.actor._
import com.amarkhel.mafia.common.RoundStarted
import com.amarkhel.mafia.parser.MessagePrinter
import com.amarkhel.mafia.service.api.MafiaService

import scala.concurrent.ExecutionContext

object UserActor {
  def props(out: ActorRef, gameId:String, mafiaService:MafiaService)(implicit ec:ExecutionContext) = Props(new UserActor(out, gameId.toInt, mafiaService))
}

class UserActor(out: ActorRef, gameId:Int, mafiaService:MafiaService)(implicit ec:ExecutionContext) extends Actor {
  var curRound = 0
  def receive = {
    case "NEXT" =>
      curRound = curRound + 1
      val gameFuture = mafiaService.loadGame(gameId, curRound).invoke()
      gameFuture.onComplete(game => {
        val events = game.get.events
        if (curRound > game.get.countRounds) {
          out ! "GAME_ENDED"
        } else {
          if (curRound == 1) {
            out ! MessagePrinter.format(events)
          } else {
            var count = 0
            val filtered = events.dropWhile({ e =>
              if(e.isInstanceOf[RoundStarted]){
                count = count + 1
              }
              count < curRound
            })
            out ! MessagePrinter.format(filtered)
          }
        }
      })
  }
}
