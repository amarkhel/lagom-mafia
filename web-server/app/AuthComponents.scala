import akka.actor.ActorSystem
import com.amarkhel.user.api.UserService
import com.mohiva.play.silhouette.api.actions._
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder, Signer}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.{Clock, PasswordHasherRegistry}
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.crypto.{JcaCrypter, JcaCrypterSettings, JcaSigner, JcaSignerSettings}
import com.mohiva.play.silhouette.impl.authenticators.{CookieAuthenticator, CookieAuthenticatorService, CookieAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.softwaremill.macwire.wire
import controllers.Auth
import play.api.Configuration
import play.api.mvc.{BodyParsers, DefaultCookieHeaderEncoding}
import utils.silhouette.{MyEnv, PasswordInfoDAO, UserFacade}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import utils.ErrorHandler

import scala.concurrent.ExecutionContext

trait AuthComponents {

  def configuration: Configuration
  def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext
  lazy val eventBus = EventBus()
  def userService:UserService
  lazy val userFacade = wire[UserFacade]
  private lazy val env: Environment[MyEnv] = Environment[MyEnv](
    userFacade, authenticatorService, List(), eventBus
  )

  lazy val fingerprintGenerator = new DefaultFingerprintGenerator(false)
  lazy val idGenerator = new SecureRandomIDGenerator()

  lazy val crypter: Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
    new JcaCrypter(config)
  }

  lazy val authenticatorService: AuthenticatorService[CookieAuthenticator] = {
    val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
    val encoder = new CrypterAuthenticatorEncoder(crypter)
    val cookieHeaderEncoder = new DefaultCookieHeaderEncoding()
    val signer:Signer = new JcaSigner(JcaSignerSettings("secret", "secret"))
    new CookieAuthenticatorService(config, None, signer, cookieHeaderEncoder, encoder, fingerprintGenerator, idGenerator, clock)
  }

  lazy val providePasswordHasherRegistry: PasswordHasherRegistry = PasswordHasherRegistry(passwordHasher)
  def bodyParser:BodyParsers.Default
  def errorHandler:ErrorHandler
  lazy val defaultSecuredHandler = wire[DefaultSecuredRequestHandler]
  lazy val defaultUnsecuredHandler = wire[DefaultUnsecuredRequestHandler]
  lazy val defaultUserAwareHandler = new DefaultUserAwareRequestHandler
  lazy val securedAction: SecuredAction = wire[DefaultSecuredAction]
  lazy val unsecuredAction: UnsecuredAction = wire[DefaultUnsecuredAction]
  lazy val userAwareAction:UserAwareAction = wire[DefaultUserAwareAction]
  lazy val passwordDao:PasswordInfoDAO = wire[PasswordInfoDAO]
  lazy val authInfoRepository:AuthInfoRepository = new DelegableAuthInfoRepository(passwordDao)
  lazy val passwordHasher = new BCryptPasswordHasher()

  lazy val credentialsProvider = new CredentialsProvider(authInfoRepository, providePasswordHasherRegistry)
  lazy val clock = Clock()
  lazy val silhouette: Silhouette[MyEnv] = wire[SilhouetteProvider[MyEnv]]
  def auth:Auth
}
