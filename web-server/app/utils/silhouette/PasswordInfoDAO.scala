package utils.silhouette

import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.api.LoginInfo

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import Implicits._
import com.amarkhel.user.api.UserService

class PasswordInfoDAO @Inject() (userService:UserService) extends DelegableAuthInfoDAO[PasswordInfo] {

  def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    update(loginInfo, authInfo)

  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    userService.getUser(loginInfo).invoke().map {
      case Some(user) if user.emailConfirmed => Some(user.password)
      case _ => None
    }

  def remove(loginInfo: LoginInfo): Future[Unit] = userService.deleteUser(loginInfo).invoke().map(_ => ())

  def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }

  def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    userService.getUser(loginInfo).invoke().map {
      case Some(user) => {
        userService.updateUser(user.name).invoke(user.copy(password = authInfo))
        authInfo
      }
      case _ => throw new Exception("PasswordInfoDAO - update : the user must exists to update its password")
    }

}