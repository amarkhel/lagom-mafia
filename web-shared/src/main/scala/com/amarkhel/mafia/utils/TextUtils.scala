package com.amarkhel.mafia.utils

import java.io.{PrintWriter, StringWriter}

object TextUtils {

  def exceptionToStr(e:Throwable) = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    sw.toString
  }

  implicit class StringImprovements(val text: String) {
    def between(start: String, end: String) = {
      val index = text.indexOf(start) + 1
      text.substring(index, text.indexOf(end, index)).trim.replaceAll("&nbsp;", " ").replace("\u00A0", " ").trim
    }

    def erase(patterns: String*): String = erase(patterns.toList)

    def erase(patterns: List[String]): String = {
      patterns.foldLeft(text) { case (res, pattern) => res.replaceAll(pattern, "") }.trim
    }

    def findAll(pattern: String) = {
      val regex = pattern.r
      regex.findAllIn(text).toList
    }
  }
}
