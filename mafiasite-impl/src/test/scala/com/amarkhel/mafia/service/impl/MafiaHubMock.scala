package com.amarkhel.mafia.service.impl

import java.io.File

import akka.actor.ActorSystem
import com.amarkhel.mafia.common._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/

class MafiaHubMock(implicit ec:ExecutionContext) extends MafiaHubAPI {

  def loadDay(day: Day) = {
    Future(\/.fromTryCatchNonFatal[Document](
      if (day.year == 2010) {
        Jsoup.parse(new File(s"C:\\Users\\amarkhel\\Downloads\\spark-course-master\\lagom\\lagom\\mafiasite-impl\\src\\test\\resources\\2010.html"), "utf-8")
      } else {
        Jsoup.parse(new File(s"C:\\Users\\amarkhel\\Downloads\\spark-course-master\\lagom\\lagom\\mafiasite-impl\\src\\test\\resources\\day_${day.day}.html"), "utf-8")
      }

    ))
  }

  def loadGame(id: Int) = {
    Future(\/.fromTryCatchNonFatal[Document](
      if(id == -1){
        Jsoup.parse(new File(s"C:\\Users\\amarkhel\\Downloads\\spark-course-master\\lagom\\lagom\\mafiasite-impl\\src\\test\\resources\\id1.html"), "utf-8")
      } else if(id == -2){
        Jsoup.parse(new File(s"C:\\Users\\amarkhel\\Downloads\\spark-course-master\\lagom\\lagom\\mafiasite-impl\\src\\test\\resources\\id2.html"), "utf-8")
      } else if(id == -3){
        Jsoup.parse(new File(s"C:\\Users\\amarkhel\\Downloads\\spark-course-master\\lagom\\lagom\\mafiasite-impl\\src\\test\\resources\\id3.html"), "utf-8")
      } else if(id == -4){
        Jsoup.parse(new File(s"C:\\Users\\amarkhel\\Downloads\\spark-course-master\\lagom\\lagom\\mafiasite-impl\\src\\test\\resources\\id4.html"), "utf-8")
      } else if(id == -5){
        Jsoup.parse(new File(s"C:\\Users\\amarkhel\\Downloads\\spark-course-master\\lagom\\lagom\\mafiasite-impl\\src\\test\\resources\\id5.html"), "utf-8")
      } else {
        Jsoup.parse(new File(s"C:\\Users\\amarkhel\\Downloads\\spark-course-master\\lagom\\lagom\\mafiasite-impl\\src\\test\\resources\\game.html"), "utf-8")
      }
    ))
  }
}
