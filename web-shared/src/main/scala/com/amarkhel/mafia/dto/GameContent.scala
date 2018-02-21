package com.amarkhel.mafia.dto

import java.time.LocalDateTime

import com.amarkhel.mafia.common.{Gamer, Location}

case class GameContent (id:Int, location:Location, players:List[Gamer], finish:LocalDateTime, chat:List[String])
