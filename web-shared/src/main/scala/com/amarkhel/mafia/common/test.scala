package com.amarkhel.mafia.common


object test extends App {

  private val PLAYER_REGEX = """[\s\S]*(?:<font class="chat_text">|<span class="chat_text">)(.*?)(?:</span>|</font>)""".r
  val str ="04:17  <font class=\"chat_text\">[Клинский] мир Так жесток и несправедлив, </font>".replaceAll("\u2028", "")
  val str2 ="""<td class="align-middle plus-size">second</td></tr>"""

  for ((_, index) <- str.zipWithIndex)
    yield println(str.charAt(index) + " " + Character.codePointAt(str, index))
  def find(str:String) = {
    PLAYER_REGEX.findFirstMatchIn(str) match {
      case Some(data) => data.group(1).trim
      case None => "Not found"
    }
  }
  println(find(str))
  println(find(str2))
}
