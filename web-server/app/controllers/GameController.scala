package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.amarkhel.mafia.service.api.MafiaService
import com.amarkhel.tournament.api.TournamentService
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject._
import play.api.Mode.Prod
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{WebSocket, _}
import play.api.{Environment => PlayEnv}
import utils.silhouette.{AdminAction, MyEnv}

import scala.concurrent.{ExecutionContext, Future}

case class LoadGameForm(id: Int)

@Singleton
class GameController @Inject()(cc: ControllerComponents, silhouette: Silhouette[MyEnv], mafiaService:MafiaService, tournamentService:TournamentService, messagesApi:MessagesApi, playEnv:PlayEnv)
                              (implicit appActorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext)
  extends BaseController(silhouette, messagesApi, cc) with I18nSupport {

  private type WSMessage = String

  def index = UserAwareAction.async { implicit request =>
    if(request.identity.get.isAdmin){
      Future(Ok(views.html.game.index(gameForm, request.identity.get)))
    } else {
      val tournaments = tournamentService.getTournamentsForUser(request.identity.get.name).invoke()
      tournaments.flatMap{
        case Nil => Future(Redirect(routes.TournamentController.joinPage))
        case list => {
          val outcome = list.head.gameInProgress match {
              case None => Redirect(routes.TournamentController.joinPage)
              case Some(g) => Ok(views.html.game.game(request.identity.get, playEnv.mode == Prod)).withSession(("gameId", g.id.toString), ("tournament", list.head.name))
            }
          Future(outcome)
        }
      }
    }
  }

  def game = SecuredAction(AdminAction("")).async { implicit request =>
    gameForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.game.index(formWithErrors, request.identity))),
      game => {
        val gameFuture = mafiaService.loadGame(game.id, 1).invoke()
        for {
          g <- gameFuture
        } yield Ok(views.html.game.game(request.identity, playEnv.mode == Prod)).withSession(("gameId", game.id.toString))
      }
    )
  }

  val gameForm = Form(mapping(
    "id" -> number
  )(LoadGameForm.apply)(LoadGameForm.unapply))

  def chat(username:String): WebSocket = {
    WebSocket.acceptOrResult[String, String] {
      case rh => {
        Future(Right(ActorFlow.actorRef { out =>
          UserActor.props(out, rh.session.get("gameId").getOrElse(""), mafiaService, tournamentService, rh.session.get("tournament").getOrElse(""), username)
        }))
        }.recover {
          case e: Exception =>
            val msg = "Произошла ошибка, попробуйте перезагрузить страницу."
            logger.error(msg, e)
            Left(InternalServerError(msg))
        }
    }
  }
}