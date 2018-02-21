package utils.silhouette

import javax.inject.Inject

import Implicits._
import com.amarkhel.user.api.{User, UserService}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService

import scala.concurrent.{ExecutionContext, Future}

class UserFacade @Inject() (userService:UserService)(implicit executionContext:ExecutionContext) extends IdentityService[User] {
  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userService.getUser(loginInfo).invoke()
  def retrieveByEmail(email:String): Future[Option[User]] = userService.getUserByEmail(email).invoke()
  def save(user: User): Future[Option[User]] = userService.createUser.invoke(user)
  def update(user: User): Future[Option[User]] = userService.updateUser(user.name).invoke(user)
  def checkUnique(user:User) = {
    val nameExist = userService.getUser(user.name).invoke()
    val emailExist = userService.getUserByEmail(user.email).invoke()
    for {
      nameCheck <- nameExist
      emailCheck <- emailExist
    } yield errorMessage(nameCheck, emailCheck)
  }

  private def errorMessage(nameUnique:Option[User], emailUnique:Option[User]): Option[String] = {
    (nameUnique, emailUnique) match {
      case (Some(_), Some(_)) => Option("auth.useremail.notunique")
      case (Some(_), None) => Option("auth.user.notunique")
      case (None, Some(_)) => Option("auth.email.notunique")
      case (None, None) => None
    }
  }
}