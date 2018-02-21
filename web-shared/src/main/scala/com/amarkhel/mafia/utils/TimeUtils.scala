package com.amarkhel.mafia.utils

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.DAYS
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration._

import com.amarkhel.mafia.common.Day

object TimeUtils {
  implicit val timeout = 600 seconds
  val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
  val COLON:String = ":"

  implicit def dateToDay(date:LocalDate) = Day(date.getYear, date.getMonthValue, date.getDayOfMonth)

  def diffInDaysFromNow(date:LocalDate) = Math.abs(DAYS.between(date, LocalDate.now)).toInt

  def isToday(d: Day) = {
    val now = LocalDate.now
    now.getYear == d.year && now.getMonthValue == d.month && now.getDayOfMonth == d.day
  }

  def extractTimePattern(text:String) = text.charAt(2) match {
    case ':' => text.substring(0, 5)
    case _ => throw new IllegalArgumentException(text + " is not contains time pattern in it")
  }

  def extractTimePattern(text:String, startTime:String, lastTime:Int) = text.charAt(2) match {
    case ':' => {
      val time = text.substring(0, 5)
      val (min, sec) = timeDiff(startTime, time, lastTime)
      min*60 + sec
    }
    case _ => -1
  }

  def timeDiff(end:String, start:String, lastTime:Int) = {
    val old = end.split(COLON)
    val cur = start.split(COLON)
    val m1 = old(0).toInt
    var m2 = cur(0).toInt
    val s1 = old(1).toInt
    val s2 = cur(1).toInt
    if(m1 > m2 || (m1 == m2 && s1 > s2)){
      m2 +=60
    }
    val diff = m2*60 + s2 - m1*60 -s1
    val diffsec = diff + lastTime/3600 * 3600
    diffsec/60 -> diffsec%60
  }

  /*def timeDiff(time:Int) = {
    (time/60 -> time%60)
  }*/

  def addTime(diff:(Int, Int), previous:LocalDateTime) = {
    val (min, sec) = diff
    previous.plusMinutes(min).plusSeconds(sec)
  }

  def format(time:LocalDateTime) = {
    require(time != null)
    val formatter:DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
    time.format(formatter) + " по московскому времени"
  }

  def format(time:String) = {
    require(time != null)
    val formatter:DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
    LocalDateTime.parse(time, formatter)
  }

  def formatDuration(start: LocalDateTime, finish: LocalDateTime) = {
    val toTime = toMinAndSec(start, finish)
    s"${toTime._1} минут ${toTime._2} секунд"
  }

  private def toMinAndSec(start: LocalDateTime, finish: LocalDateTime) = {
    require(start != null && finish != null && start.isBefore(finish))
    val minutes = start.until(finish, ChronoUnit.MINUTES)
    val seconds = start.until(finish, ChronoUnit.SECONDS)%60
    (minutes, seconds)
  }
}
