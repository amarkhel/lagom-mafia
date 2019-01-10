package tournament

import com.amarkhel.mafia.common._
import prickle._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalaz.syntax.std.all._

@JSExport
object EventHandler {
  import GamerStatus._
  type State = (Int, List[String], RoundType, Int, Boolean)
  private var state:State = (0, List.empty[String], RoundType.INITIAL, 0, false)

  private def sys(message:String)(implicit time:String) = {
    UI.systemMessage(time + message)
  }

  private def handle(state:State, event: GameEvent): (State, Unit) = {
    implicit val time:String = Model.timeTostr(event.time)
    event match {
      case GameStarted(_, loc, _, players, _) => {
        UI.setPlayers(players)
        UI.renderLocation(loc.name)
        UI.updateFooter(state._1, state._2, List.empty, 1, "")
        sys(" Минуточку, распределяем роли.")
        sys(" Игра началась! В игре участвуют: " + players.map("<b>" + _ + "</b>").mkString(","))
        for {
          player <- players
        } yield UI.renderUnknownRole(player)
        (state, ())
      }
      case Killed(player, _) => {
        sys(" Внимание! Сейчас будет следующий ход.")
        sys(" Считаем трупы! Результаты ночных беспорядков.")
        sys(s" ${player.role.role.head} ${player.name} убит")
        removePlayer(state, player, DEAD)
      }
      case OmonHappened(_) => {
        sys(" Внимание! Сейчас будет следующий ход.")
        sys(" Считаем трупы! Результат хода честных людей.")
        sys("Договориться не смогли. Рассвирепевший ОМОНОВЕЦ решает, кто отправится в тюрьму...")
        (state.copy(state._1, state._2, state._3, state._4, true), ())
      }
      case Prisoned(player, _) => {
        if(!state._5){
          sys(" Внимание! Сейчас будет следующий ход.")
          sys(" Считаем трупы! Результат хода честных людей.")
        }
        sys(s" ${player.role.role.head} ${player.name} отправлен в тюрьму")
        removePlayer(state, player, PRISONED)
      }
      case Timeouted(player, _) => {
        sys(s" ${player.role.role.head} ${player.name} вышел из партии по тайм-ауту")
        removePlayer(state, player, TIMEOUT)
      }
      case RoundStarted(RoundType.INITIAL, _) => {
        sys(" Дадим мафии время договориться. Приват включен!")
        (state.copy(state._1, state._2, RoundType.CITIZEN), ())
      }
      case RoundStarted(RoundType.KOMISSAR, _) => {
        UI.finishRound(state._4 + 1)
        sys(" Ход комиссара.")
        (state.copy(state._1, state._2, RoundType.KOMISSAR, state._4 + 1), ())
      }
      case RoundStarted(RoundType.MAFIA, _) => {
        UI.finishRound(state._4 + 1)
        sys(" Ход мафии.")
        (state.copy(state._1, state._2, RoundType.MAFIA, state._4 + 1), ())
      }
      case RoundStarted(RoundType.BOSS, _) => {
        UI.finishRound(state._4 + 1)
        sys(" Ход босса.")
        (state.copy(state._1, state._2, RoundType.BOSS, state._4 + 1), ())
      }
      case RoundStarted(RoundType.CITIZEN, _) => {
        if(state._3 == RoundType.CITIZEN){
          sys(" Договориться не смогли. В тюрьму никто не отправляется.")
        }
        UI.finishRound(state._4 + 1)
        if(state._3 == RoundType.CITIZEN){
          sys(" Честные продолжают поиск.")
        } else {
          if(state._4 == 0) {
            sys(" Наступил день. Честные люди ищут мафию. Приват выключен!")
          } else {
            sys(" Наступил день. Честные люди ищут мафию.")
          }
        }
        (state.copy(state._1, state._2, RoundType.CITIZEN, state._4 + 1), ())
      }
      case RoundEnded(_) => {
        (state, ())
      }
      case GameCompleted(message, _) => {
        sys(" Игра окончена. " + message)
        (state, ())
      }
      case Voted(target, destination, _) => {
        if(state._3 == RoundType.CITIZEN){
          UI.addVote(time, target, destination)
        }
        (state, ())
      }
      case MessageSent(message, _) => {
        UI.message(time + " " + message)
        (state, ())
      }
      case PrivateMessageSent(from, to, _) => {
        sys(from + " что-то шепнул " + to)
        (state, ())
      }
      case MafiaNotKilled(_) => {
        sys(" Мафия никого не убила.")
        (state, ())
      }
      case GameStopped(avtor, _) => {
        sys(" Партия остановлена пидором под ником " + avtor)
        (state, ())
      }
      case RecoveredByDoctor(_) => {
        sys(" Мафия хотела убить жителя города, но врач успел вовремя и спас игрока от смертельных ран.")
        (state, ())
      }
      case GameResultRendered(players, _) => {
        for {
          player <- state._2
          p = players.find(_.name == player).get
        } yield UI.replaceUserStatus(p, ALIVE)
        (state, ())
      }
      case _ => (state, ())
    }
  }

  private def removePlayer(state: State, player: Gamer, status:GamerStatus) = {
    val mafiaKilled = player.isBoss || player.isMafia
    UI.replaceUserStatus(player, status)
    (state.copy(if (mafiaKilled) state._1 - 1 else state._1, state._2.filter(_ != player.name)), ())
  }

  @JSExport
  def main(e: js.Dynamic):Unit = {
    WS.start(e.toString)
  }

  def parseEvents(e: String) = {
    val s = Unpickle[TournamentGameState].fromString(e).get
    val events = s.events.map(cutSmiles)
    if (state._3 == RoundType.INITIAL) {
      val firstEvent = events.head.asInstanceOf[GameStarted]
      val mafias = Model.countMafia(firstEvent.players.size)
      val playerNames = firstEvent.players
      state = (mafias, playerNames, RoundType.INITIAL, 0, false)
    }
    state = events.mapAccumLeft(state, (s: State, event: GameEvent) => handle(s, event))._1
    UI.updateFooter(state._1, state._2, s.chosen.map(_._1).toList, s.currentRound, s.timeToEnd)
  }

  private def cutSmiles(mess:GameEvent) = mess match {
    case MessageSent(text, time) => {
      val pattern = """XXXXXX(.+?)XXXXXX""".r
      val converted:String = pattern.replaceAllIn(text, m => {
        """<img src="https://st.mafiaonline.ru/images/smiles/""" + m.group(1) + """.gif" alt="{""" + m.group(1) + """}" >"""
      })
      MessageSent(converted, time)
    }
    case other:GameEvent => other
  }
}