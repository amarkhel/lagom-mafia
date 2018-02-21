package com.amarkhel.mafia.service.impl

trait Extractor {
  def extractGames(countDays:Int = -1): List[Int]
}
