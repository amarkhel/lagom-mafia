package controllers

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.amarkhel.mafia.service.api.MafiaService
import com.amarkhel.tournament.api._
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ControllerComponents
import utils.silhouette.{AdminAction, MyEnv}

import scala.concurrent.{ExecutionContext, Future}

case class TournamentForm(name: String, countPlayers:Int, expiration:Int, games:String, isNew:String = "true")
case class Tournaments(created:Seq[Tournament], started:Seq[Tournament], finished:Seq[Tournament])
case class StatForm(id: Int)
case class StatFormGamer(player: String)
case class PlayerStat(name:String, count:Int, average:Double, correct:Int, total:Int, percent:Double)

@Singleton
class TournamentController @Inject()(cc: ControllerComponents, silhouette: Silhouette[MyEnv], messagesApi:MessagesApi, tournamentService:TournamentService, mafiaService:MafiaService)
                              (implicit appActorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext)
  extends BaseController(silhouette, messagesApi, cc) with I18nSupport {

  def index = SecuredAction(AdminAction("")) { implicit request =>
    Ok(views.html.tournament.index(tournamentForm, request.identity))
  }

  def bestPlayer =  UserAwareAction.async { implicit request =>
    tournamentService.getAllSolutions.invoke().flatMap {
      case Nil => Future(Ok(views.html.tournament.bestPlayer(Nil, request.identity)))
      case list:Seq[SolutionResult] => {
        val gr = list.groupBy(_.name).map {
          pl => {
            val count = pl._2.size
            val avg = pl._2.map(_.points).sum / pl._2.size
            val correct = pl._2.flatMap(_.choices.filter(_.correct)).size
            val total = pl._2.flatMap(_.choices).size
            val percent = correct.toDouble / total * 100
            PlayerStat(pl._1, count, avg, correct, total, percent)
          }
        }.toSeq.sortBy(_.average).reverse.zipWithIndex
        Future(Ok(views.html.tournament.bestPlayer(gr, request.identity)))
      }
    }
  }

  def statForId = UserAwareAction.async { implicit request =>
    Future(Ok(views.html.tournament.gameStat(statForm, Seq.empty, request.identity)))
  }

  def statFor = UserAwareAction.async { implicit request =>
    Future(Ok(views.html.tournament.gameStatForUser(statFormGamer, Seq.empty, request.identity)))
  }

  def statForGame = UserAwareAction.async { implicit request =>
    statForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.tournament.gameStat(formWithErrors, Seq.empty, request.identity))),
      form => {
        val future = tournamentService.getSolutionsForId(form.id).invoke()
        for {
          list <- future
        } yield Ok(views.html.tournament.gameStat(statForm, list.sortBy(_.points).reverse, request.identity))
      }
    )
  }

  def statForPlayer = UserAwareAction.async { implicit request =>
    statFormGamer.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.tournament.gameStatForUser(formWithErrors, Seq.empty, request.identity))),
      form => {
        val future = tournamentService.getSolutionsForPlayer(form.player).invoke()
        for {
          list <- future
        } yield Ok(views.html.tournament.gameStatForUser(statFormGamer, list.sortBy(_.points).reverse, request.identity))
      }
    )
  }

  val statForm = Form(mapping(
    "id" -> number
  )(StatForm.apply)(StatForm.unapply))

  val statFormGamer = Form(mapping(
    "name" -> text
  )(StatFormGamer.apply)(StatFormGamer.unapply))

  def allSolutions =  UserAwareAction.async { implicit request =>
    tournamentService.getAllSolutions.invoke().flatMap {
      case Nil => Future(Ok(views.html.tournament.solutions(Nil, request.identity)))
      case list:Seq[SolutionResult] => Future(Ok(views.html.tournament.solutions(list, request.identity)))
    }
  }

  def edit(name:String) = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentService.getTournament(name).invoke().flatMap {
      case Some(t) =>
        Future(Ok(views.html.tournament.index(
          tournamentForm.fill(
            TournamentForm(t.name, t.countPlayers, t.gameExpirationTime, t.games.map(_.id).mkString(","), "false")
          ),
          request.identity)
        )
      )
      case None => Future(Ok(views.html.tournament.index(tournamentForm, request.identity)).flashing("Ошибка" -> "Турнир с таким именем не найден"))
    }
  }

  def join(name:String) = UserAwareAction.async { implicit request =>
    tournamentService.joinTournament(name, request.identity.get.name).invoke().flatMap {
      case true => Future(Redirect(routes.TournamentController.joinPage))
      case false => Future(Redirect(routes.TournamentController.joinPage).flashing("Ошибка" -> "Невозможно присоединиться к турниру"))
    }
  }

  def removeUser(name:String, player:String) = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentService.removeUser(name, player).invoke().flatMap {
      case true => Future(Redirect(routes.TournamentController.list("created")))
      case false => Future(Redirect(routes.TournamentController.list("created")).flashing("Ошибка" -> "Невозможно удалить игрока"))
    }
  }

  def result(name:String, id:Int) = UserAwareAction.async { implicit request =>
    tournamentService.getTournament(name).invoke().flatMap {
      case Some(t) => Future(Ok(views.html.tournament.result(t, t.games.filter(_.id == id).head)))
    }
  }

  def currentState(name:String) = UserAwareAction.async { implicit request =>
    tournamentService.getTournament(name).invoke().flatMap {
      case Some(t) => Future(Ok(views.html.tournament.currentGame(Some(t), request.identity)))
      case None => Future(Ok(views.html.tournament.currentGame(None, request.identity)).flashing("Ошибка" -> "Турнир с таким именем не найден"))
    }
  }

  def tournamentState(name:String) = UserAwareAction.async { implicit request =>
    tournamentService.getTournament(name).invoke().flatMap {
      case Some(t) => Future(Ok(views.html.tournament.currentStandings(Some(t), request.identity)))
      case None => Future(Ok(views.html.tournament.currentStandings(None, request.identity)).flashing("Ошибка" -> "Турнир с таким именем не найден"))
    }
  }

  def startGame(name:String, id:Int) = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentService.startGame(name, id).invoke().flatMap {
      case true => Future(Redirect(routes.TournamentController.list("started")))
      case false => Future(Redirect(routes.TournamentController.list("started")).flashing("Ошибка" -> "Невозможно стартовать игру"))
    }
  }

  def startTournament(name:String) = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentService.startTournament(name).invoke().flatMap {
      case true => Future(Redirect(routes.TournamentController.list("started")))
      case false => Future(Redirect(routes.TournamentController.list("started")).flashing("Ошибка" -> "Невозможно стартовать турнир"))
    }
  }

  def delete(name:String) = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentService.deleteTournament(name).invoke().flatMap {
      case true => Future(Redirect(routes.TournamentController.list("created")))
      case false => Future(Redirect(routes.TournamentController.list("created")).flashing("Ошибка" -> "Турнир с таким именем не найден"))
    }
  }

  def list(activeTab:String) = UserAwareAction.async { implicit request =>
    tournamentService.getTournaments.invoke().flatMap(
      list => {
        val t = new Tournaments(list.filter(!_.started), list.filter(t => t.started && !t.finished), list.filter(_.finished))
        Future(Ok(views.html.tournament.list(t, Some(request.identity.get), activeTab)))
      }
    )
  }

  def joinPage = UserAwareAction.async { implicit request =>
    tournamentService.getTournaments.invoke().flatMap(
      list => {
        Future(Ok(views.html.tournament.join(list.filter(!_.started), Some(request.identity.get))))
      }
    )
  }

  def create = SecuredAction(AdminAction("")).async { implicit request =>
    tournamentForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.tournament.index(formWithErrors, request.identity))),
      tournament => {
        editTournament(tournament, request.identity.name)
          .recover {
            case _: Exception => Redirect(routes.TournamentController.list("created")).flashing("Ошибка" -> "Ашыпка((")
          }
      }
    )
  }

  private def editTournament(tournament:TournamentForm, name:String) = {
    val action = if(tournament.isNew == "true") tournamentService.createTournament else tournamentService.updateTournament
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
    } yield Redirect(routes.TournamentController.list("created"))
  }

  val tournamentForm = Form(mapping(
    "name" -> nonEmptyText,
    "countPlayers" -> number,
    "expiration" -> number,
    "games" -> nonEmptyText,
    "isNew" -> default(text, "true")
  )(TournamentForm.apply)(TournamentForm.unapply))
}