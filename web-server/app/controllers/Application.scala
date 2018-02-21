package controllers

import javax.inject.{Inject, Singleton}

import com.amarkhel.user.api.User
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import utils.silhouette._

@Singleton
class Application @Inject() (val silhouette: Silhouette[MyEnv], messagesApi: MessagesApi, cc:ControllerComponents) extends AbstractController(cc) with I18nSupport {

  implicit def securedRequest2User[A](implicit request: SecuredRequest[MyEnv, A]): User = request.identity
  implicit def userAwareRequest2UserOpt[A](implicit request: UserAwareRequest[MyEnv, A]): Option[User] = request.identity

  def index = silhouette.UserAwareAction { implicit request =>
    Ok(views.html.index())
  }
}