package tournament

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport

@JSExport
object WS {

  private var wsBaseUrl: String = ""

  private var client: Option[WSClient] = None

  def choose(player: String) = {
    client.map(_.send(s"CHOOSE:$player"))
  }

  def next = {
    client.map(_.send("NEXT"))
  }

  object WSClient {
    def connect(url: String): Option[WSClient] = {
      try {
        if (g.window.WebSocket.toString != "undefined") {
          Some(new WSClient(url))
        } else None
      } catch {
        case e: Throwable => {
          dom.window.alert("Unable to connect because "+e.toString)
          None
        }
      }
    }

    def receive(e: dom.MessageEvent):Unit = {
      val messages: String = e.data.toString
      //dom.console.log(messages)
      /*if(data.error.toString != "undefined"){
        dom.window.alert(data.error.toString)
      }else{*/
        EventHandler.parseEvents(messages)
      //}
    }
  }

  class WSClient(url: String) {
    private val socket = new dom.WebSocket(url)
    socket.onmessage = {
      WSClient.receive(_)
    }
    def send(msg: String): Unit = {
      socket.send(msg)
    }
    socket.onopen = e => {
      WS.next
    }
    def close():Unit = socket.close()
  }

  def start(url:String):Unit = {
    this.wsBaseUrl = url
    client = WSClient.connect(wsBaseUrl)
  }
}