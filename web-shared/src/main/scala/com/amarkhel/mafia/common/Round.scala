package com.amarkhel.mafia.common

import java.time.LocalDateTime

case class Round(alive: List[Gamer], tpe: RoundType, order: Int, start: LocalDateTime, finish: LocalDateTime,
                 votes:List[Vote], messages:List[Message])
