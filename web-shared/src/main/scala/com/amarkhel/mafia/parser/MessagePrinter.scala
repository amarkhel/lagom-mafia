package com.amarkhel.mafia.parser

import com.amarkhel.mafia.common._
import prickle._
object MessagePrinter {
  def print[T>:GameEvent](message:T) = {
    message match {
      case GameStarted(id, loc, start, players, _) =>
        s"""
          |<div class="col-xs-12">
          |                <div class="panel panel-default">
          |                    <div class="panel-body">
          |                        <h3 class="text-center">Лог партии № $id
          |                            <br>
          |                            <small>${loc.name}                               <br>завершена $start</small>
          |                        </h3>
          |
          |                    </div>
          |                </div>
          |            </div>
        """.stripMargin
      case other => other.toString
    }
  }

  def format(state:TournamentGameState) = {
    Pickle.intoString(state.copy(events = state.events.map(cutSmiles)))
  }

  private def cutSmiles(mess:GameEvent) = mess match {
    case MessageSent(text, time) => {
      val pattern = """<img src="https://st.mafiaonline.ru/images/smiles/(.+?).gif"[\s\S]*">""".r
      val converted = pattern.replaceAllIn(text, m => s"XXXXXX${m.group(1)}XXXXXX")
      MessageSent(converted, time)
    }
    case other:GameEvent => other
  }
}
