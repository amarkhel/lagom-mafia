package tournament

import com.amarkhel.mafia.common._
import prickle._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalaz.syntax.std.all._
import GameMessage._
import org.scalajs.dom

@JSExport
object EventHandler {
  import GamerStatus._
  private var round:RoundType = RoundType.INITIAL
  private var omonHappened = false

  private def sys(message:String)(implicit time:String) = {
    UI.systemMessage(time + message)
  }

  private def handle(state:TournamentGameState, event: GameEvent): Unit = {
    implicit val time:String = Model.timeTostr(event.time)
    event match {
      case GameStarted(loc, _, _, _) => {
        UI.renderLocation(loc.name)
        val c = state.countPossibleChoices
        val pl = state.eliminatedPlayers.map(_.name)
        UI.updateFooter(state)
        sys(" Минуточку, распределяем роли.")
        sys(" Игра началась! В игре участвуют: " + state.players.map("<b>" + _ + "</b>").mkString(","))
        for {
          player <- state.players
        } yield UI.renderUnknownRole(player)
        (state, ())
      }
      case Killed(player, _) => {
        sys(" Внимание! Сейчас будет следующий ход.")
        sys(" Считаем трупы! Результаты ночных беспорядков.")
        sys(s" ${player.role.role.head} ${player.name} убит")
        removePlayer(player, DEAD)
      }
      case OmonHappened(_) => {
        sys(" Внимание! Сейчас будет следующий ход.")
        sys(" Считаем трупы! Результат хода честных людей.")
        sys("Договориться не смогли. Рассвирепевший ОМОНОВЕЦ решает, кто отправится в тюрьму...")
        omonHappened = true
      }
      case Prisoned(player, _) => {
        if(!omonHappened){
          sys(" Внимание! Сейчас будет следующий ход.")
          sys(" Считаем трупы! Результат хода честных людей.")
        }
        sys(s" ${player.role.role.head} ${player.name} отправлен в тюрьму")
        removePlayer(player, PRISONED)
      }
      case Timeouted(player, _) => {
        sys(s" ${player.role.role.head} ${player.name} вышел из партии по тайм-ауту")
        removePlayer(player, TIMEOUT)
      }
      case RoundStarted(RoundType.INITIAL, _) => {
        sys(" Дадим мафии время договориться. Приват включен!")
        round = RoundType.CITIZEN
      }
      case RoundStarted(RoundType.KOMISSAR, _) => {
        UI.finishRound(state.currentRound)
        sys(" Ход комиссара.")
        round = RoundType.KOMISSAR
      }
      case RoundStarted(RoundType.MAFIA, _) => {
        UI.finishRound(state.currentRound)
        sys(" Ход мафии.")
        round = RoundType.MAFIA
      }
      case RoundStarted(RoundType.BOSS, _) => {
        UI.finishRound(state.currentRound)
        sys(" Ход босса.")
        round = RoundType.BOSS
      }
      case RoundStarted(RoundType.CITIZEN, _) => {
        if(round == RoundType.CITIZEN){
          sys(" Договориться не смогли. В тюрьму никто не отправляется.")
        }
        UI.finishRound(state.currentRound)
        if(round == RoundType.CITIZEN){
          sys(" Честные продолжают поиск.")
        } else {
          if(state.currentRound == 0) {
            sys(" Наступил день. Честные люди ищут мафию. Приват выключен!")
          } else {
            sys(" Наступил день. Честные люди ищут мафию.")
          }
        }
        round = RoundType.CITIZEN
      }
      case RoundEnded(_) => ()
      case GameCompleted(message, _) => {
        sys(" Игра окончена. " + message)
      }
      case Voted(target, destination, _) => {
        if(round == RoundType.CITIZEN){
          UI.addVote(time, target, destination)
        }
      }
      case MessageSent(message, _) => {
        UI.message(time + " " + message)
      }
      case PrivateMessageSent(from, to, _) => {
        sys(from + " что-то шепнул " + to)
      }
      case MafiaNotKilled(_) => {
        sys(" Мафия никого не убила.")
      }
      case GameStopped(avtor, _) => {
        sys(" Партия остановлена пидором под ником " + avtor)
      }
      case RecoveredByDoctor(_) => {
        sys(" Мафия хотела убить жителя города, но врач успел вовремя и спас игрока от смертельных ран.")
      }
      case GameResultRendered(players, _) => {
        for {
          player <- state.eliminatedPlayers
          p = players.find(_.name == player).get
        } yield UI.replaceUserStatus(p, ALIVE)
      }
      case _ => ()
    }
  }

  private def removePlayer(player: Gamer, status:GamerStatus) = {
    UI.replaceUserStatus(player, status)
  }

  @JSExport
  def main(e: js.Dynamic):Unit = {
    WS.start(e.toString)
  }

  def trimD(d:Double) = BigDecimal(d).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  def parseEvents(e: String) = {
    val s = Unpickle[GameMessage].fromString(e).get
    s match {
      case Events(events, state) => {
        events.map(GameEvent.cutSmiles).foreach(e => handle(state, e))
        UI.updateFooter(state)
      }
      case FINISHED(points, correct, _, mafias) => {
        UI.showModal("Игра закончена", s"Вы правильно угадали <b>$correct</b> мафиози. Набрано очков - <b>${trimD(points)}</b>",
          s"Мафией в этой партии были <span style='color:red;'>$mafias</span>")
      }
      case CONFIRM(state) => {
        UI.updateFooter(state)
      }
      case TIME(state) => {
        UI.updateTime(state.started)
      }
      case Error(msg, _) => {
        UI.showModal("Ошибка", s"$msg")
      }
    }

  }
}