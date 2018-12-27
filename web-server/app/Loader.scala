import akka.actor.ActorSystem
import com.amarkhel.mafia.processor.api.GameProcessor
import com.amarkhel.mafia.service.api.MafiaService
import com.amarkhel.token.api.TokenService
import com.amarkhel.tournament.api.TournamentService
import com.amarkhel.user.api.UserService
import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents
import controllers.{Application, AssetsComponents, Auth, GameController, MyAssets, TournamentController}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n.I18nComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.BodyParsers
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import utils.{ErrorHandler, Filters}

import scala.collection.immutable
import scala.concurrent.ExecutionContext

abstract class Web(context: Context) extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with I18nComponents
  with AssetsComponents
  with AhcWSComponents
  with CORSComponents
  with MailComponents
  with AuthComponents
  with LagomServiceClientComponents with LagomConfigComponent {

  override lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "web",
    Map(
      "web" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )

  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
  implicit lazy val appActorSystem: ActorSystem = actorSystem
  lazy val game = wire[GameController]
  lazy val tour = wire[TournamentController]
  lazy val ass = wire[MyAssets]
  lazy val auth = wire[Auth]
  lazy val app = wire[Application]
  lazy val userService = serviceClient.implement[UserService]
  lazy val tokenService = serviceClient.implement[TokenService]
  lazy val mafiaService = serviceClient.implement[MafiaService]
  lazy val tournamentService = serviceClient.implement[TournamentService]
  lazy val searchService = serviceClient.implement[GameProcessor]

  override lazy val httpFilters = wire[Filters].filters
  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }
  lazy val optionalSourceMapper:OptionalSourceMapper = wire[OptionalSourceMapper]
  lazy val bodyParser = wire[BodyParsers.Default]
  lazy val errorHandler:ErrorHandler = new ErrorHandler(environment, configuration, optionalSourceMapper, () => router, messagesApi)

}

class WebGatewayLoader extends ApplicationLoader {
  override def load(context: Context) = context.environment.mode match {
    case Mode.Dev =>
      (new Web(context) with LagomDevModeComponents).application
    case _ =>
      (new Web(context) with ConductRApplicationComponents).application
  }
}