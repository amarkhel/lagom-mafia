package tournament

import com.amarkhel.mafia.common._
import com.karasiq.bootstrap4.Bootstrap.default._
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.html.TableRow
import org.scalajs.dom.raw.MouseEvent
import scalatags.JsDom._
import scalatags.JsDom.all._
import tournament.GamerStatus._

import scala.scalajs.js.annotation.JSExport

@JSExport
object UI {
  def updateTime(time: String) = {
    timeToEnd.innerHTML = "" + time
    timeToEnd.render
  }

  private val playerRoleMap = collection.mutable.HashMap[String, (GamerStatus, String)]()

  def getColor(status: GamerStatus) = {
    status match {
      case ALIVE => "green"
      case TIMEOUT => "grey"
      case DEAD => "red"
      case PRISONED => "blue"
      case UNKNOWN => "black"
      case CHOSEN => "yellow"
    }
  }

  def updateFooter(state:TournamentGameState) = {
    countUnknown.innerHTML = "" + state.countPossibleChoices
    countUnknown.render
    chosenLabel.innerHTML = {
      if(state.chosen.size == 0) "Вы пока не выбрали никого"
      else "Вы считаете мафией " + "<span style=\"color:red;\">" + state.chosen.map(_._1).mkString(", ") + "</span>"
    }
    chosenLabel.render
    timeToEnd.innerHTML = "" + state.started
    timeToEnd.render
    currentRoundLabel.innerHTML = "" + state.currentRound
    currentRoundLabel.render
    for {
      player <- state.players
    } yield {
      val old = dom.document.getElementById(s"${player}_option")
      val newElem = if(state.chosen.contains(player)){
        span(id:=s"${player}_option", style:=s"padding-right:20px; color:${getColor(CHOSEN)}", title:=s"${player} - ${CHOSEN.title}")(raw(player))
      } else {
        if (!state.eliminatedPlayers.map(_.name).contains(player)) {
          label(id:=s"${player}_option", style:="padding-right:20px")(input(`type`:="checkbox", id:=s"${player}_option", value:=s"${player}", onclick:={ () => WS.choose(player)}), raw(player))
        } else span(id:=s"${player}_option", style:=s"padding-right:20px; color:${getColor(playerRoleMap.get(player).get._1)}", title:=s"${playerRoleMap.get(player).get._2} - ${playerRoleMap.get(player).get._1.title}")(raw(player))
      }
      if (old != null) {
        playerOptions.replaceChild(newElem.render, old.render)
      } else {
        playerOptions.appendChild(newElem.render)
      }
    }
    playerOptions.render
  }

  private val modal = dom.document.getElementById("modal")
  private val timeToEnd = dom.document.getElementById("timeToEnd")
  private val currentRoundLabel = dom.document.getElementById("currentRound")
  private val chosenLabel = dom.document.getElementById("chosen")
  private val playerOptions = dom.document.getElementById("playersOptions")
  private val countUnknown = dom.document.getElementById("countUnknownMafia")
  private val playersTable = dom.document.getElementById("playersTable")
  private val chatLog = dom.document.getElementById("chatLog")
  private val shortLog = dom.document.getElementById("shortLog")
  private val location = dom.document.getElementById("location")
  private val nextButton = dom.document.getElementById("nextRoundButton").addEventListener[MouseEvent]("click", e => WS.next)
  private def playerTag(name:String) = dom.document.getElementById(name)

  def showModal(title:String, msg:String, msg2:String="") = {
    Modal()
      .withTitle(title)
      .withBody(p(raw(msg)), p(raw(msg2)))
      .withButtons(Modal.button("OK", Modal.dismiss, onclick := Callback.onClick { _ ⇒
        dom.window.location.href = "http://" + dom.window.location.hostname + "/index"
      }))
      .withDialogStyle(ModalDialogSize.small)
      .show()
  }

  def finishRound(round: Int) = {
    addHr()
    addRoundNumber(round)
  }

  def replaceUserStatus(player:Gamer, status:GamerStatus) = {
    val old: Element = dom.document.getElementById(player.name)
    playersTable.replaceChild(renderPicture(player.name, player.role.role.head, status).render, old.render)
    playerRoleMap.put(player.name, (status, player.role.role.head))
  }

  def renderLocation(loc:String) = {
    location.appendChild(div()(raw(loc)).render)
  }

  def renderUnknownRole(name:String) = {
    playersTable.appendChild(renderPicture(name, "unknown", UNKNOWN).render)
  }

  def renderPicture(name:String, role:String, status:GamerStatus): TypedTag[TableRow] = {
    val picture = Model.pictureName(role)
    tr(id:=name, if(status != ALIVE && status != UNKNOWN) `class`:=status.name else ())(
      td(style:="width: 85px;")(
        img(src:=s"https://st.mafiaonline.ru/images/roles_new/${status.name}/64/${picture}_m.png",
          attr("srcset"):=s"https://st.mafiaonline.ru/images/roles_new/${status.name}/128/${picture}_m.png 2x",
          title:=s"${if(role != "unknown")role}${status.title}",
          `class`:="img-circle avatar")),
      td(`class`:="align-middle plus-size")(
        if(status != ALIVE && status != UNKNOWN){
          s(`class`:="text-muted")(raw(name))
        } else {
          raw(name)
        }
      )
    )
  }

  def systemMessage(text:String) = {
    message(text, true)
    shortChat(text, true)
  }

  def message(message:String, isSystem:Boolean = false) = {
    chatLog.appendChild(div(style:="font-size:20px;padding-bottom:5px;", if(isSystem) `class`:="system" else ())(raw(message)).render)
  }

  private def shortChat(message:String, isSystem:Boolean = false) = {
    shortLog.appendChild(div(style:="font-size:20px;padding-bottom:5px;", if(isSystem) `class`:="system" else ())(raw(message)).render)
  }

  private def addHr() = {
    chatLog.appendChild(hr().render)
    shortLog.appendChild(hr().render)
  }

  private def addRoundNumber(round:Int) = {
    chatLog.appendChild(span(`class`:="label label-success")(raw(s"Ход №&nbsp;$round.")).render)
    shortLog.appendChild(span(`class`:="label label-success")(raw(s"Ход №&nbsp;$round.")).render)
  }

  def addVote(time:String, target:String, destination:String) = {
    chatLog.appendChild(div(`class`:="move move-city")(raw(s"\t $time $target  xочет отправить в тюрьму $destination")).render)
    shortLog.appendChild(div(`class`:="move move-city")(raw(s"\t $time $target  xочет отправить в тюрьму $destination")).render)
  }
}
