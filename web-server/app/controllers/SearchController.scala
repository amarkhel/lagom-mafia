package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.amarkhel.mafia.processor.api._
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ControllerComponents
import utils.silhouette.MyEnv

import scala.concurrent.{ExecutionContext, Future}

case class SearchForm(criterias: List[SearchCriterion])

@Singleton
class SearchController @Inject()(cc: ControllerComponents, silhouette: Silhouette[MyEnv], searchService:GameProcessor, messagesApi:MessagesApi)
                              (implicit appActorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext)
  extends BaseController(silhouette, messagesApi, cc) with I18nSupport {

  def makeCriterion(tpe:String, op:String, value:String) = {
    val crit = GameCriterion.withName(tpe)
    val operation = Operation.withName(op)
    val v = StringValue(value)
    SearchCriterion(crit, operation, v)
  }

  def parseCriterion(crit:SearchCriterion) = {
    Some((crit.crit.toString, crit.operation.toString, crit.value.toString))
  }

  val searchForm = Form(mapping(
    "criterias" -> list(mapping(
      "type" -> text,
      "op" -> text,
      "value" -> text
    )(makeCriterion)(parseCriterion)
    ))(SearchForm.apply)(SearchForm.unapply))


  def search = UserAwareAction.async { implicit request =>
    Future(Ok(views.html.search.search(searchForm, request.identity.get)))
  }

  def searchGames = UserAwareAction.async { implicit request =>
    searchForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.search.search(formWithErrors, request.identity.get))),
      form => {

        Future.successful(BadRequest(views.html.search.search(searchForm.withError("location", "Партии с такими критериями не найдены"), request.identity.get)))
      }
    )
  }
}