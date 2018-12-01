package com.amarkhel.mafia.service.impl

import com.amarkhel.mafia.parser.Parser
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

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
    "should skip 2" in {
      Parser.needSkip("[ОМОНОВЕЦ] Договориться не смогли.") shouldBe (true)
    }
    "should parse sumrak " in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ]  <span class=\"move move-city\">Улитка нанёс удар по репутации жителя города Фемида −5</span>", "00:00", 0)
      groups.size shouldBe (1)
      groups.last._2.size shouldBe (3)
      groups.last._2.head shouldBe ("Улитка")
      groups.last._2(1) shouldBe ("Фемида")
      groups.last._2.last shouldBe ("−5")
    }
    "should parse sumrak nick with space" in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ]  <span class=\"move move-city\">п о б у к в а м нанёс удар по репутации жителя города arena55 loh −6</span>", "00:00", 0)
      groups.size shouldBe (1)
      groups.last._2.size shouldBe (3)
      groups.last._2.head shouldBe ("п о б у к в а м")
      groups.last._2(1) shouldBe ("arena55 loh")
      groups.last._2.last shouldBe ("−6")
    }
    "should parse sumrak prisoned" in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ] Бандитка  <b>lilac имеет очень низкую репутацию в городе. Она отправляется в тюрьму</b> .", "00:00", 0)
      groups.size shouldBe (1)
      groups.last._2.size shouldBe (1)
      groups.last._2.head shouldBe ("lilac")
    }
    "should parse sumrak prisoned 2" in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ] Честный(-ая)  <b>п о б у к в а м имеет очень низкую репутацию в городе. Он(-а) отправляется в тюрьму</b> .", "00:00", 0)
      groups.size shouldBe (1)
      groups.last._2.size shouldBe (1)
      groups.last._2.head shouldBe ("п о б у к в а м")
    }
    "should skip sumrak" in {
      Parser.needSkip("[ОМОНОВЕЦ] Команда честных получает 1 бонус к случайному стату; комиссар/cержант — 2 бонуса.") shouldBe (true)
    }
    "should skip sumrak 2" in {
      Parser.needSkip("[ОМОНОВЕЦ] Команда мафии получает 2 бонуса к случайному стату.") shouldBe (true)
    }
    "should skip sumrak 3" in {
      Parser.needSkip("[ОМОНОВЕЦ] Никто не получает бонусов: мёртвым они не нужны.") shouldBe (true)
    }
    "should parse maf earned" in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ] Выигрыш игрока Armenia составил 4.25 маф.", "00:00", 0)
      groups.size shouldBe (1)
      groups.last._2.size shouldBe (2)
      groups.last._2.head shouldBe ("Armenia")
      groups.last._2.last shouldBe ("4.25")
    }
    "should parse stopped" in {
      val groups = Parser.findGroups("<span class=\"text\"><b>Vera</b> остановил партию номер <b>3898893</b>.</span>", "00:00", 0)
      groups.size shouldBe (1)
      groups.last._2.size shouldBe (1)
      groups.last._2.head shouldBe ("Vera")
    }
    "should parse stopped 2" in {
      val groups = Parser.findGroups("<span class=\"text\">Авторитет<b>Vera</b> остановил партию номер <b>3898893</b>.</span>", "00:00", 0)
      groups.size shouldBe (1)
      groups.last._2.size shouldBe (1)
      groups.last._2.head shouldBe ("Vera")
    }
    "should parse with убит in nick" in {
      val groups = Parser.findGroups("[ОМОНОВЕЦ] Честный(-ая)  <b>Мне придётся убить тебя убит(-а)</b> .", "00:00", 0)
      groups.size shouldBe (1)
      groups.last._2.size shouldBe (1)
      groups.last._2.head shouldBe ("Мне придётся убить тебя")
    }

  }
}

