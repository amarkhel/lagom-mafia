package com.amarkhel.mafia.service.impl

import com.amarkhel.mafia.common
import com.amarkhel.mafia.common.Gamer
import com.amarkhel.mafia.parser.Parser
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

import scala.util.matching.Regex

class ParserTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  "The Parser service" should {
    "parse omon string" in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ] Рассвирепевший ОМОНОВЕЦ, не разбираясь, кто прав, кто виноват, решил, что  <b>Картошка будет отправлен в тюрьму.</b>", "00:00", 0).get._2
      groups.size shouldBe (1)
      groups.head shouldBe ("Картошка")
    }
    "parse omon string2" in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ] Бандит  <b>Jaguar x type отправлен в тюрьму.</b>", "00:00", 0).get._2
      groups.size shouldBe (1)
      groups.head shouldBe ("Jaguar x type")
    }
    "should skip " in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ] Договориться не смогли. В тюрьму никто не отправляется.", "00:00", 0)
      groups shouldBe (None)
    }
  }
}

