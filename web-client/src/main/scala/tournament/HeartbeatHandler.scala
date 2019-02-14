package tournament

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
object HeartbeatHandler {
  @JSExport
  def main(e: js.Dynamic):Unit = {
    WSHB.start(e.toString)
  }
}
