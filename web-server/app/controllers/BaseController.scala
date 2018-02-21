package controllers

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{Date, UUID}

import com.amarkhel.user.api.{User, UserService}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseController(cc: ControllerComponents, messagesApi: MessagesApi, userService: UserService)(implicit ec: ExecutionContext) extends AbstractController(cc) {
/*  protected def withUser[T](block: Option[String] => T)(implicit rh: RequestHeader): T = {
    block(rh.session.get("user").map(identity))
  }

  protected def requireUser(block: String => Future[Result])(implicit rh: RequestHeader): Future[Result] = withUser {
    case Some(user) => block(user)
    case None => Future.successful(Redirect(routes.Main.index))
  }
  */
}
