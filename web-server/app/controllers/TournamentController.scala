package controllers

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.amarkhel.mafia.service.api.MafiaService
import com.amarkhel.tournament.api.{GameDescription, Tournament, TournamentService}
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ControllerComponents
import utils.silhouette.{AdminAction, MyEnv}

import scala.concurrent.{ExecutionContext, Future}

case class TournamentForm(name: String, countPlayers:Int, expiration:Int, games:String, isNew:Boolean = true)

@Singleton
class TournamentController @Inject()(cc: ControllerComponents, silhouette: Silhouette[MyEnv], messagesApi:MessagesApi, tournamentService:TournamentService, mafiaService:MafiaService)
                              (implicit appActorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext)
  extends BaseController(silhouette, messagesApi, cc) with I18nSupport {

  def index = SecuredAction(AdminAction("")) { implicit request =>
    Ok(views.html.tournament.index(tournamentForm, request.identity))
  }

  def edit(name:String) = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentService.getTournament(name).invoke().flatMap {
      case Some(t) =>
        Future(Ok(views.html.tournament.index(
          tournamentForm.fill(
            TournamentForm(t.name, t.countPlayers, t.gameExpirationTime, t.games.map(_.id).mkString(","), false)
          ),
          request.identity)
        )
      )
      case None => Future(Ok(views.html.tournament.index(tournamentForm, request.identity)).flashing("Ошибка" -> "Турнир с таким именем не найден"))
    }
  }

  def delete(name:String) = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentService.deleteTournament(name).invoke().flatMap {
      case true => Future(Redirect(routes.TournamentController.list))
      case false => Future(Redirect(routes.TournamentController.list).flashing("Ошибка" -> "Турнир с таким именем не найден"))
    }
  }

  def list = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentService.getTournaments.invoke().flatMap(
      list => Future(Ok(views.html.tournament.list(list, Some(request.identity))))
    )
  }

  def create = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.tournament.index(formWithErrors, request.identity))),
      tournament => {
        editTournament(tournament, request.identity.name)
          .recover {
            case _: Exception => Redirect(routes.TournamentController.list).flashing("Ошибка" -> "Ашыпка((")
          }
      }
    )
  }

  private def editTournament(tournament:TournamentForm, name:String) = {
    val action = if(tournament.isNew) tournamentService.createTournament else tournamentService.updateTournament
    for {
      sources <- Future.sequence(
        tournament.games.split(",")
          .map(_.trim)
          .par
          .map(s => mafiaService.loadGame(s.toInt, 1).invoke)
          .seq
          .toList
      )
      games = sources.map(game => GameDescription(game.id, game.location.name, game.playersSize, game.countRounds, game.players.filter(_.isMafia).map(_.name), tournament.expiration))
      t = Tournament(tournament.name, tournament.countPlayers, name, List.empty, games, LocalDateTime.now, None, None)
      _ <- action.invoke(t)
    } yield Redirect(routes.TournamentController.list)
  }

  val tournamentForm = Form(mapping(
    "name" -> nonEmptyText,
    "countPlayers" -> number,
    "expiration" -> number,
    "games" -> nonEmptyText,
    "isNew" -> boolean
  )(TournamentForm.apply)(TournamentForm.unapply))
}