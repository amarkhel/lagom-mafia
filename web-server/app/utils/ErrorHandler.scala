package utils

import play.api.http.DefaultHttpErrorHandler
import com.mohiva.play.silhouette.api.actions.{SecuredErrorHandler, UnsecuredErrorHandler}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.routing.Router

import scala.concurrent.Future
import javax.inject.{Inject, Provider, Singleton}

import controllers.routes
import play.api.mvc.{RequestHeader, Result}
import play.api._
import play.api.mvc.Results._

@Singleton
class ErrorHandler @Inject() (
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router],
    val messagesApi: MessagesApi
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with SecuredErrorHandler with UnsecuredErrorHandler with I18nSupport {

  // 401 - Unauthorized
  override def onNotAuthenticated(implicit request: RequestHeader): Future[Result] = Future.successful {
    Redirect(routes.Auth.signIn).withSession(request.session + ("ENTRY_URI" -> request.uri))
  }

  // 403 - Forbidden
  override def onNotAuthorized(implicit request: RequestHeader): Future[Result] = Future.successful {
    Forbidden(views.html.errors.accessDenied())
  }

  // 404 - page not found error
  override def onNotFound(request: RequestHeader, message: String): Future[Result] = Future.successful {
    NotFound(env.mode match {
      case Mode.Prod => views.html.errors.notFound(request)(request2Messages(request))
      case _ => views.html.defaultpages.devNotFound(request.method, request.uri, Some(router.get))
    })
  }

  // 500 - internal server error
  override def onProdServerError(request: RequestHeader, exception: UsefulException) = Future.successful {
    InternalServerError(views.html.errors.error(request, exception)(request2Messages(request)))
  }
}