package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.amarkhel.mafia.common.Location
import com.amarkhel.mafia.processor.api.GameCriterion.{CountPlayersCriterion, CountRoundsCriterion, LocationCriterion}
import com.amarkhel.mafia.processor.api._
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

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

case class LoadGameForm(id: Int)
case class RandomForm(location: String, countPlayers:Int)

@Singleton
class GameController @Inject()(cc: ControllerComponents, silhouette: Silhouette[MyEnv], mafiaService:MafiaService, tournamentService:TournamentService, searchService:GameProcessor, messagesApi:MessagesApi, playEnv:PlayEnv)
                              (implicit appActorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext)
  extends BaseController(silhouette, messagesApi, cc) with I18nSupport {

  private type WSMessage = String

  val randomForm = Form(mapping(
    "location" -> text,
    "countPlayers" -> number(min = 7, max = 21)
  )(RandomForm.apply)(RandomForm.unapply))

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
              case Some(g) => {
                if(!list.head.isSolutionCompleted(list.head.gameInProgress.get.id, request.identity.get.name)){
                  Ok(views.html.game.game(request.identity.get, playEnv.mode == Prod)).withSession(("gameId", g.id.toString), ("tournament", list.head.name))
                } else {
                  Redirect(routes.TournamentController.joinPage)
                }
              }
            }
          Future(outcome)
        }
      }
    }
  }

  def random = UserAwareAction.async { implicit request =>
    Future(Ok(views.html.tournament.random(randomForm, request.identity)))
  }

  def randomGame = UserAwareAction.async { implicit request =>
    randomForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.tournament.random(formWithErrors, request.identity))),
      form => {
        val req = SearchRequest(List(SearchCriterion(LocationCriterion, Operation.EQ, StringValue(form.location)), SearchCriterion(CountPlayersCriterion, Operation.EQ, IntValue(form.countPlayers)), SearchCriterion(CountRoundsCriterion, Operation.GE, IntValue(countRounds(form.location, form.countPlayers)))))
        val list = Await.result(searchService.search.invoke(req), Duration.Inf)
        list match {
          case Nil => Future.successful(BadRequest(views.html.tournament.random(randomForm.withError("location", "Партии с такими критериями не найдены"), request.identity)))
          case list => {
            val index = scala.util.Random.nextInt(list.size - 1)
            val game = list(index)
            Future(Ok(views.html.game.game(request.identity.get, playEnv.mode == Prod)).withSession(("gameId", game.id.toString)))
          }
        }
      }
    )
  }

  private def countRounds(location:String, countPlayers:Int) = {
    location match {
      case Location.SUMRAK.name => {
        countPlayers match {
          case i if (i == 7) => 7
          case i if (i == 8 || i == 9) => 9
          case i if (i > 9 && i < 13) => 12
          case _ => 14
        }
      }
      case Location.OZHA.name => {
        countPlayers match {
          case i if (i == 7) => 7
          case i if (i == 8 || i == 9) => 9
          case i if (i > 9 && i < 13) => 12
          case _ => 14
        }
      }
      case Location.KRESTY.name => {
        countPlayers match {
          case i if (i == 7) => 7
          case i if (i == 8 || i == 9) => 9
          case i if (i > 9 && i < 13) => 12
          case _ => 14
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
        } yield Ok(views.html.game.game(request.identity, playEnv.mode == Prod)).withSession(("gameId", game.id.toString), ("needTrack" -> "false"))
      }
    )
  }

  val gameForm = Form(mapping(
    "id" -> number
  )(LoadGameForm.apply)(LoadGameForm.unapply))

  def heartbeat(username:String): WebSocket = {
    WebSocket.acceptOrResult[String, String] {
      case rh => {
        Future(Right(ActorFlow.actorRef { out =>
          HeartBeatActor.props(out, tournamentService,  username)
        }))
      }.recover {
        case e: Exception =>
          val msg = "Произошла ошибка, попробуйте перезагрузить страницу."
          logger.error(msg, e)
          Left(InternalServerError(msg))
      }
    }
  }

  def chat(username:String): WebSocket = {
    WebSocket.acceptOrResult[String, String] {
      case rh => {
        Future(Right(ActorFlow.actorRef { out =>
          UserActor.props(out, rh.session.get("gameId").getOrElse(""), mafiaService, tournamentService, rh.session.get("tournament").getOrElse(""), username, rh.session.get("needTrack").getOrElse("true").toBoolean)
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