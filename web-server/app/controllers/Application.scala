package controllers

import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import utils.silhouette._

@Singleton
class Application @Inject() (silhouette: Silhouette[MyEnv], messagesApi: MessagesApi, cc:ControllerComponents) extends BaseController(silhouette, messagesApi, cc) with I18nSupport {

  def index = silhouette.UserAwareAction { implicit request =>
    Ok(views.html.index())
  }
}