package tournament

import com.amarkhel.mafia.common._
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.html.TableRow

import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom._
import scalatags.JsDom.all._
import GamerStatus._
import org.scalajs.dom.raw.{MouseEvent, Node}

import scala.collection.mutable.ListBuffer

@JSExport
object UI {

  private val playerRoleMap = collection.mutable.HashMap[String, (GamerStatus, String)]()
  private var allPlayers = List.empty[String]
  private var selectedPlayers = ListBuffer[String]()

  def setPlayers(p:List[String]) = allPlayers = p

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

  def updateFooter(mafias: Int, players: List[String], chosen:List[String], currentRound:Int, time:String) = {
    val count = mafias - selectedPlayers.size
    countUnknown.innerHTML = "" + count
    countUnknown.render
    chosenLabel.innerHTML = "" + chosen.mkString(", ")
    chosenLabel.render
    timeToEnd.innerHTML = "" + time
    timeToEnd.render
    currentRoundLabel.innerHTML = "" + currentRound
    currentRoundLabel.render
    for {
      player <- allPlayers
    } yield {
      val old = dom.document.getElementById(s"${player}_option")
      val newElem = if(players.contains(player)){
        label(id:=s"${player}_option", style:="padding-right:20px")(input(`type`:="checkbox", id:=s"${player}_option", value:=s"${player}", onclick:={ () => WS.choose(player)}), raw(player))
      } else {
        if (chosen.contains(player)) {
          span(id:=s"${player}_option", style:=s"padding-right:20px; color:${getColor(CHOSEN)}", title:=s"${playerRoleMap.get(player).get._2} - ${CHOSEN.title}")(raw(player))
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
