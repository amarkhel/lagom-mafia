package tournament

import com.amarkhel.mafia.common.Role

sealed trait GamerStatus{def name:String; def title:String}

object GamerStatus extends Serializable {

  case object UNKNOWN extends GamerStatus{val name="alive"; val title="Неизвестная роль"}
  case object ALIVE extends GamerStatus{val name="alive"; val title=""}
  case object DEAD extends GamerStatus{val name="dead"; val title=" (убит)"}
  case object TIMEOUT extends GamerStatus{val name="timeout"; val title=" (вышел по тайм-ауту)"}
  case object PRISONED extends GamerStatus{val name="jailed"; val title=" (отправлен в тюрьму)"}

  val values:List[GamerStatus] = List(ALIVE, DEAD, TIMEOUT, PRISONED, UNKNOWN)
}

object Model {

  def pictureName(role: String) = role match {
    case f if(f == Role.MAFIA.role.head) => "maf"
    case f if(f == Role.MANIAC.role.head) => "man"
    case f if(f == Role.CITIZEN.role.head) => "chizh"
    case f if(f == Role.KOMISSAR.role.head) => "kom"
    case f if(f == Role.SERZHANT.role.head) => "serzh"
    case f if(f == Role.BOSS.role.head) => "boss"
    case f if(f == Role.DOCTOR.role.head) => "doc"
    case f if(f == Role.CHILD.role.head) => "vsb"
    case other => "unknown"
  }

  def timeTostr(time: Int) = {
    val min = time / 60
    val sec = time - min*60
    t(min) + ":" + t(sec)
  }

  def countMafia(countPl:Int) = countPl match {
    case it if 7 until 11 contains it  => 2
    case it if 12 until 20 contains it  => 3
    case it if 21 until 30 contains it  => 4
  }

  private def t(t:Int) = {
    if(t < 10) "0" + t else "" + t
  }
}