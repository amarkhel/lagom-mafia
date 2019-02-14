package controllers

import akka.actor._
import com.amarkhel.tournament.api.TournamentService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object HeartBeatActor {
  def props(out: ActorRef, tournamentService:TournamentService, user:String)(implicit ec:ExecutionContext) = Props(new HeartBeatActor(out, tournamentService, user))
}

class HeartBeatActor(out: ActorRef, tournamentService:TournamentService, user:String)(implicit ec:ExecutionContext) extends Actor {

  override def postStop() = println(s"Heart beat Actor closed for $user and player $user")

  override def preStart(): Unit = {
    super.preStart()
  }

  def receive = {
    case msg: String if (msg == "HEARTBEAT") => {
      val tournaments = Await.result(tournamentService.getTournamentsForUser(user).invoke(), Duration.Inf)
      val needRedirect = !tournaments.isEmpty && tournaments.head.gameInProgress.isDefined && !tournaments.head.isSolutionCompleted(tournaments.head.gameInProgress.get.id, user)
      out ! needRedirect.toString
    }
    case _ => println("Unrecognized message")

  }
}

