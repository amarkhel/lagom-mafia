package controllers

//import models.ChatRoom
import play.api.data.Forms._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, WebSocket}
import java.net.URI
import javax.inject._

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}
import com.amarkhel.mafia.parser.MessagePrinter
import com.amarkhel.mafia.service.api.MafiaService
import com.amarkhel.user.api.User
import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.Credentials
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import utils.silhouette.{AdminAction, MyEnv}

import scala.concurrent.{ExecutionContext, Future}

case class LoadGameForm(id: Int)

@Singleton
class ChatController @Inject()(cc: ControllerComponents, silhouette: Silhouette[MyEnv], mafiaService:MafiaService)
                              (implicit appActorSystem: ActorSystem, mat: Materializer)
  extends AbstractController(cc) with I18nSupport with RequestMarkerContext {

  def env: Environment[MyEnv] = silhouette.env

  def SecuredAction = silhouette.SecuredAction
  def UnsecuredAction = silhouette.UnsecuredAction
  def UserAwareAction = silhouette.UserAwareAction

  implicit def securedRequest2User[A](implicit request: SecuredRequest[MyEnv, A]): User = request.identity
  implicit def userAwareRequest2UserOpt[A](implicit request: UserAwareRequest[MyEnv, A]): Option[User] = request.identity

/*  case class InMessage(text:String)
  case class OutMessage(text:String, user:String)
  import play.api.libs.json._*/

/*  implicit val inEventFormat = Json.format[InMessage]
  implicit val outEventFormat = Json.format[OutMessage]
  import play.api.mvc.WebSocket.MessageFlowTransformer

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InMessage, OutMessage]*/
  private type WSMessage = String

  private val logger = Logger(getClass)

  private implicit val logging = Logging(appActorSystem.eventStream, logger.underlyingLogger.getName)


  def index = SecuredAction(AdminAction("")) { implicit request =>
    Ok(views.html.game.index(gameForm, request.identity))
  }

  /**
    * Saves the new password and renew the cookie
    */
  def game = SecuredAction.async { implicit request =>
    gameForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.game.index(formWithErrors, request.identity))),
      game => {
        val gameFuture = mafiaService.loadGame(game.id, 1).invoke()
        for {
          g <- gameFuture
        } yield Ok(views.html.game.game(request.identity)).withSession(("gameId", game.id.toString))
      }
    )
  }

  val gameForm = Form(mapping(
    "id" -> number
  )(LoadGameForm.apply)(LoadGameForm.unapply))

  def chat(username:String): WebSocket = {
    WebSocket.acceptOrResult[String, String] {
      case rh if sameOriginCheck(rh) => {
        Future(Right(ActorFlow.actorRef { out =>
          UserActor.props(out, rh.session.get("gameId").getOrElse(""), mafiaService)
        }))
        }.recover {
          case e: Exception =>
            val msg = "Cannot create websocket"
            logger.error(msg, e)
            val result = InternalServerError(msg)
            Left(result)
        }
      case rejected =>
        logger.error(s"Request ${rejected} failed same origin check")
        Future.successful {
          Left(Forbidden("forbidden"))
        }
    }
  }

  /**
    * Checks that the WebSocket comes from the same origin.  This is necessary to protect
    * against Cross-Site WebSocket Hijacking as WebSocket does not implement Same Origin Policy.
    *
    * See https://tools.ietf.org/html/rfc6455#section-1.3 and
    * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
    */
  private def sameOriginCheck(implicit rh: RequestHeader): Boolean = {
    true
    //rh.session.get("user").map(!_.isEmpty).getOrElse(false)
    // The Origin header is the domain the request originates from.
    // https://tools.ietf.org/html/rfc6454#section-7
    /*logger.debug("Checking the ORIGIN ")

    rh.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        logger.debug(s"originCheck: originValue = $originValue")
        true

      case Some(badOrigin) =>
        logger.error(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false

      case None =>
        logger.error("originCheck: rejecting request because no Origin header found")
        false
    }*/
  }

  /**
    * Returns true if the value of the Origin header contains an acceptable value.
    */
  private def originMatches(origin: String): Boolean = {
    try {
      val url = new URI(origin)
      url.getHost == "localhost" &&
        (url.getPort match { case 9000 | 19001 => true; case _ => false })
    } catch {
      case e: Exception => false
    }
  }
}
