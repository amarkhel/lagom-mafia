package com.amarkhel.mafia.common

import org.slf4j.LoggerFactory

sealed trait Location{def name:String}

object Location extends Serializable {
  import prickle._
  implicit val pickler: PicklerPair[Location] = CompositePickler[Location].
    concreteType[KRESTY.type].concreteType[OZHA.type].concreteType[SUMRAK.type].concreteType[ALL.type]
  //private val log = LoggerFactory.getLogger(classOf[Location])
  case object KRESTY extends Location{val name="Улица Крещения"}
  case object OZHA extends Location{val name="Улица Ожидания"}
  case object SUMRAK extends Location{val name="Сумеречный переулок"}
  case object ALL extends Location{val name="Все улицы"}
  
  val values:List[Location] = List(KRESTY, OZHA, SUMRAK, ALL)
  
  def get(name:String):Location = {
    require(name != null)
    values.find(name contains _.name).getOrElse({
      //log.error(s"Location $name not found")
      KRESTY
    })
  }
}