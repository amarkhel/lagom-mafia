package tournament

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.annotation.JSExport

@JSExport
object WSHB {
  private var wsBaseUrl: String = ""
  private var client: Option[WSClientHB] = None

  object WSClientHB {
    def connect(url: String): Option[WSClientHB] = {
      try {
        if (g.window.WebSocket.toString != "undefined") {
          Some(new WSClientHB(url))
        } else None
      } catch {
        case e: Throwable => {
          dom.window.alert("Невозможно подключиться к вебсокету "+e.toString)
          None
        }
      }
    }

    def receive(e: dom.MessageEvent):Unit = {
      val message: String = e.data.toString
      if(message == "true") dom.window.location.href = "http://" + dom.window.location.hostname + "/index"
    }
  }

  class WSClientHB(url: String) {
    private val socket = new dom.WebSocket(url)
    socket.onmessage = WSClientHB.receive
    socket.onopen = _ => {
      WS.init
      import scala.scalajs.js.timers._

      setInterval(1000) {
        client.map(_.send("HEARTBEAT"))
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
    client = WSClientHB.connect(wsBaseUrl)
  }
}
