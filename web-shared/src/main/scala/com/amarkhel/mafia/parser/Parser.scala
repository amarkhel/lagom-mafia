package com.amarkhel.mafia.parser

import java.time.LocalDateTime

import com.amarkhel.mafia.common._
import com.amarkhel.mafia.dto.GameContent
import com.amarkhel.mafia.utils.TimeUtils._

import scalaz.Scalaz._
import scalaz._

object Parser {
  private val timeRegex = """(\d{2}:\d{2})(.+?)""".r

  def defaultEvents(gameSource:GameContent, time:LocalDateTime) = {
    List(
      GameStarted(gameSource.id, gameSource.location, time.toString, gameSource.players.map(_.name), 0),
      RoundStarted(RoundType.INITIAL, 0)
    )
  }

  def parse[T>:GameEvent](gameSource:GameContent): \/[Throwable, Game] = {
    val initialMoment = findStartTime(gameSource)
    val (state, parsed) = run(gameSource, initialMoment)
    val start = gameSource.finish.minusSeconds(state)
    val result = parsed.reduce(_ +++ _)
    result match {
      case Success(list) => asGame(gameSource, start, list).right
      case Failure(str) => new Exception(s"Error while parsing ${gameSource.id}" + str.toList.mkString("|")).left
    }
  }

  private def asGame[T >: GameEvent](gameSource: GameContent, start: LocalDateTime, list: List[GameEvent]) = {
    val events = defaultEvents(gameSource, start) ++ list
    if (events.contains(GameStopped)) {
      Game.stoppedGame(gameSource.id, gameSource.location.name, gameSource.finish.toLocalDate)
    } else {
      Game(gameSource.id, events.take(events.indexWhere(_.isInstanceOf[GameCompleted]) + 1) ++ List(GameResultRendered(gameSource.players, 0)), OK, gameSource.players, events.filter(_.isInstanceOf[RoundStarted]).size)
    }
  }

  private def run[T >: GameEvent](gameSource: GameContent, start: String) = {
    gameSource.chat.mapAccumLeft(0, (a: Int, b: String) => parseEvents(a, b, start, gameSource.players))
  }

  private def findStartTime[T >: GameEvent](gameSource: GameContent) = {
    (
      for {
        first <- gameSource.chat.headOption
        found <- timeRegex.findFirstMatchIn(first)
      } yield found.group(1)
    ).getOrElse("")
  }

  private def timeFromStart(str: String, started:String, lastTime:Int) = {
    (for {
      matched <- timeRegex.findFirstMatchIn(str)
      time = matched.group(1)
      (min, sec) = timeDiff(started, time, lastTime)
    } yield min*60 + sec).orElse(lastTime.some)
  }

  def findGroups(str:String, started:String, lastTime:Int) = {
    val text = str.startsWith("[ОМОНОВЕЦ]") ? str | str.substring(5)
    for {
      action <- actions.find(_._1.findFirstMatchIn(text).isDefined)
      time <- timeFromStart(str, started, lastTime)
      groups <- action._1.findFirstMatchIn(text)
      size = groups.groupCount
      list = (1 to size).map(groups.group(_).trim).toList
    } yield (action, list, time)
  }

  private def handle[T>:GameEvent](lastTime:Int, str: String, started:String, players:List[Gamer]) : (Int, ValidationNel[String, List[T]]) = {
    try {
      val emitted = for {
        (command, groups, time) <- findGroups(str, started, lastTime)
        events = command._2(players, groups, time)
      } yield events
      emitted match {
        case None => {
          (lastTime, s"Could not find handler for $str".failureNel)
        }
        case Some(list) => (list.head.time, list.success)
      }
    } catch {
      case e:Throwable => (lastTime, s"Could not find handler for $str".failureNel)
    }
  }

  private def parseEvents[T>:GameEvent](state:Int, str:String, start:String, players:List[Gamer]) :(Int, ValidationNel[String, List[T]]) = {
    if(needSkip(str)) (state, List.empty[T].success)
    else handle(state, str.replaceAll("\u2028", ""), start, players)
  }

  private def needSkip(str:String) = skipPatterns.foldLeft(false)((a, b) => b.findFirstMatchIn(str).isDefined || a)

  private def skipPatterns = List(
    """\[ОМОНОВЕЦ\] [\s\S]*Минуточку, распределяем роли.[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Дадим [\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Игра началась![\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Внимание! Сейчас будет следующий ход.[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Считаем трупы![\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Договориться не смогли. (?:Результатов[\s\S]*|В тюрьму никто[\s\S]*)""",
    """\[ОМОНОВЕЦ\] [\s\S]*<b>Сержант получает повышение.[\s\S]*Мои поздравления, господин комиссар.[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Маньяк проспал свой ход[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Жертвой маньяка никто не стал[\s\S]*""",
    """[\s\S]*<b> убит[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Комиссар закончил своё расследование.[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Мафия не может поднять руку на дитя босса.[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*Босс не тронет своё дитя.[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*<b> (?:убит|убита|убит\(-а\)|убит\(а\))[\s\S]*</b>[\s\S]*""",
    """\[ОМОНОВЕЦ\] [\s\S]*<b> (?:отправлен в тюрьму|отправлена в тюрьму|отправлен\(-а\) в тюрьму|отправлен\(а\) в тюрьму)[\s\S]*</b>[\s\S]*""",
    """[\s\S]*<span class=\"label label-success\">[\s\S]*""",
    """\[ОМОНОВЕЦ\][\s\S]*оспользовался правом выйти из партии[\s\S]*"""
  ).map(_.r)

  private def actions[T>:GameEvent] = {
    Map(
      """\[ОМОНОВЕЦ\][\s\S]*(?:Игрок | <b>)(.+?) (?:вышел|вышла|вышел\(-ла\)) из партии по таймауту.[\s\S]*""".r ->
        event((pl, l, t) => {
          Timeouted(pl.find(_.name == l.head.replaceAll("<b>", "")).get, t)
        }),
      """\[ОМОНОВЕЦ\][\s\S]*(?:<b style="text-decoration: underline;">|<span class="move move-city">)(.+?)xочет отправить в тюрьму (.+)(?:</b>|</span>)""".r ->
        voteEvent,
      """\[ОМОНОВЕЦ\][\s\S]*Мафия [\s\S]*убить жителя города, но врач успел вовремя и спас игрока от смертельных ран.""".r ->
        event((_, _, t) => RecoveredByDoctor(t)),
      """\[ОМОНОВЕЦ\][\s\S]*Ход босса[\s\S]*""".r ->
        roundStarted(RoundType.BOSS),
      """\[ОМОНОВЕЦ\][\s\S]*Ход комиссара[\s\S]*""".r ->
        roundStarted(RoundType.KOMISSAR),
      """\[ОМОНОВЕЦ\][\s\S]*Ход мафии[\s\S]*""".r ->
        roundStarted(RoundType.MAFIA),
      """\[ОМОНОВЕЦ\][\s\S]*Наступил день[\s\S]*""".r ->
        roundStarted(RoundType.CITIZEN),
      """\[ОМОНОВЕЦ\][\s\S]*Честные продолжают поиск.""".r ->
        roundStarted(RoundType.CITIZEN),
      """\[ОМОНОВЕЦ\][\s\S]*Так как честные не смогли договориться, дадим им ещё попытку.""".r ->
        roundStarted(RoundType.CITIZEN),
      """\[ОМОНОВЕЦ\][\s\S]*(?:<b style="text-decoration: underline; color: lightgreen;">|<span class="move move-positive">)Врач (.+?)пытается вылечить (.+).(?:</b>|</span>)""".r ->
        voteEvent,
      """\[ОМОНОВЕЦ\][\s\S]*Игра окончена.(.+).""".r ->
        event((_, l, t) => GameCompleted(l.head.trim, t)),
      """\[ОМОНОВЕЦ\][\s\S]*<b>(.+)</b>[\s\S]*что-то шепнул[\s\S]*<b>(.*?) </b>[\s\S]*""".r ->
        event((_, l, t) => PrivateMessageSent(l.head.trim, l.last.trim, t)),
      """\[ОМОНОВЕЦ\][\s\S]*(?:<b style="text-decoration: underline; color: red;">|<span class="move move-maf">)Мафиози (.+?)(?:стреляет в |пытается убить )(.+).(?:</b>|</span>)""".r ->
        voteEvent,
      """\[ОМОНОВЕЦ\][\s\S]*(?:<b style="text-decoration: underline; color: #d5c32d;">|<span class="move move-positive">)Комиссар (.+?)проверил жителя (.+).(?:</b>|</span>)""".r ->
        voteEvent,
      """\[ОМОНОВЕЦ\][\s\S]*(?:<b style="text-decoration: underline; color: cyan;">|<span class="move move-man">)Маньяк (.+?)убивает (.+).(?:</b>|</span>)""".r ->
        voteEvent,
      """\[ОМОНОВЕЦ\][\s\S]*(?:<b style="text-decoration: underline; color: #990000;">|<span class="move move-maf">)Босс (.+?)морозит (.+).(?:</b>|</span>)""".r ->
        voteEvent,
      """\[ОМОНОВЕЦ\][\s\S]*<b>(.+?)(?:отправлен в тюрьму|отправлена в тюрьму|отправлен\(-а\) в тюрьму|отправлен\(а\) в тюрьму)[\s\S]*</b>[\s\S]*""".r ->
        event((pl, l, t) => {
          Prisoned(pl.find(_.name == l.head.trim).get, t)
        }),
      """\[ОМОНОВЕЦ\][\s\S]*Договориться не смогли. Рассвирепевший ОМОНОВЕЦ решает, кто отправится в тюрьму[\s\S]*""".r->
        event((_, _, t) => OmonHappened(t)),
      """\[ОМОНОВЕЦ\][\s\S]*Рассвирепевший ОМОНОВЕЦ, не разбираясь, кто прав, кто виноват, решил, что  <b>(.+?)(?:будет отправлен в тюрьму|будет отправлена в тюрьму|будет отправлен\(-а\) в тюрьму|будет отправлен\(а\) в тюрьму)[\s\S]*</b>[\s\S]*""".r->
        event((_, _, t) => OmonHappened(t)),
      """\[ОМОНОВЕЦ\][\s\S]*<b>(.+?)(?:убит|убита|убит\(-а\)|убит\(а\))[\s\S]*</b>[\s\S]*""".r ->
        event((pl, l, t) => {
          Killed(pl.find(_.name == l.head.trim).get, t)
        }),
      """[\s\S]*Авторитет<b>(.+?)</b>остановил партию номер[\s\S]*""".r ->
        event((_, l, t) => GameStopped(l.head, t)),
      """\[ОМОНОВЕЦ\][\s\S]*<b>Мафия никого не убила.</b>[\s\S]*""".r ->
        event((_, l, t) => MafiaNotKilled(t)),
      """[\s\S]*(?:<font class="chat_text">|<span class="chat_text">)(.*?)(?:</span>|</font>)""".r ->
        event((_, l, t) => MessageSent(l.head, t))
    )
  }

  private def event[T>:GameEvent](a: (List[Gamer], List[String], Int) =>T) : (List[Gamer], List[String], Int) => List[T] = (players, list, time) => List(a(players, list, time))

  private def voteEvent[T>:GameEvent] : (List[Gamer], List[String], Int) => List[T] = (_, list, time) => List(Voted(list.head, list.last, time))

  private def roundStarted[T>:GameEvent](tpe:RoundType) : (List[Gamer], List[String], Int) => List[T] = (_, _, time) => List(RoundEnded(time), RoundStarted(tpe, time))
}