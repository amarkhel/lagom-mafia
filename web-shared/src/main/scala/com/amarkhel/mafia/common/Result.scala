package com.amarkhel.mafia.common

sealed trait Result{def descr:String}
object Result extends Serializable {
  case object GOROD_WIN extends Result{val descr ="Победа города"}
  case object MAFIA_WIN extends Result{val descr ="Победа мафии"}
  case object DRAW extends Result{val descr ="Ничья"}
  private val values:List[Result] = List(GOROD_WIN, MAFIA_WIN, DRAW)
  def get(name:String) = {
    require(name != null)
    values.find(_.descr == name).getOrElse({
      throw new IllegalArgumentException(s"Result $name not found")
    })
  }
	
}