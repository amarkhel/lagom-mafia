package com.amarkhel.mafia.parser

import com.amarkhel.mafia.common._
import prickle._
object MessagePrinter {
  def print[T>:GameEvent](message:T) = {
    message match {
      case GameStarted(loc, start, _, _) =>
        s"""
          |<div class="col-xs-12">
          |                <div class="panel panel-default">
          |                    <div class="panel-body">
          |                        <h3 class="text-center">Лог партии №
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

  def format(message:GameMessage) = Pickle.intoString(message)
}
