package tournament

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport

@JSExport
object WS {
  private var wsBaseUrl: String = ""
  private var client: Option[WSClient] = None

  def choose(player: String) = client.map(_.send(s"CHOOSE----$player"))
  def init = client.map(_.send("INIT"))
  def next = client.map(_.send("NEXT"))

  object WSClient {
    def connect(url: String): Option[WSClient] = {
      try {
        if (g.window.WebSocket.toString != "undefined") {
          Some(new WSClient(url))
        } else None
      } catch {
        case e: Throwable => {
          dom.window.alert("Невозможно подключиться к вебсокету "+e.toString)
          None
        }
      }
    }

    def receive(e: dom.MessageEvent):Unit = {
      val messages: String = e.data.toString
      EventHandler.parseEvents(messages)
    }
  }

  class WSClient(url: String) {
    private val socket = new dom.WebSocket(url)
    socket.onmessage = WSClient.receive
    socket.onopen = _ => {
      WS.init
      import scala.scalajs.js.timers._

      setInterval(1000) {
        client.map(_.send("TIME"))
      }
    }
    socket.onclose = _ => {
      dom.window.location.reload(true)
    }
    def send(msg: String) = socket.send(msg)
    def close():Unit = socket.close()
  }

  def start(url:String):Unit = {
    this.wsBaseUrl = url
    client = WSClient.connect(wsBaseUrl)
  }
}