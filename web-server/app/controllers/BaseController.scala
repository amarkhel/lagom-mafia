package controllers

import com.amarkhel.user.api.User
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._
import utils.silhouette.MyEnv

abstract class BaseController(val silhouette: Silhouette[MyEnv], messagesApi: MessagesApi, cc:ControllerComponents) extends AbstractController(cc) {

  val logger = Logger(getClass)
  implicit def securedRequest2User[A](implicit request: SecuredRequest[MyEnv, A]): User = request.identity
  implicit def userAwareRequest2UserOpt[A](implicit request: UserAwareRequest[MyEnv, A]): Option[User] = request.identity
  def env: Environment[MyEnv] = silhouette.env

  def SecuredAction = silhouette.SecuredAction
  def UnsecuredAction = silhouette.UnsecuredAction
  def UserAwareAction = silhouette.UserAwareAction
}
