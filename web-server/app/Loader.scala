import akka.actor.ActorSystem
import com.amarkhel.mafia.processor.api.GameProcessor
import com.amarkhel.mafia.service.api.MafiaService
import com.amarkhel.token.api.TokenService
import com.amarkhel.user.api.UserService
import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeServiceLocatorComponents
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder, Signer}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.crypto.{JcaCrypter, JcaCrypterSettings, JcaSigner, JcaSignerSettings}
import com.softwaremill.macwire._
import controllers.{Application, Assets, AssetsComponents, Auth, ChatController, MyAssets}
//import filters.ContentSecurityPolicyFilter
import com.mohiva.play.silhouette.api.actions._
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.authenticators.{CookieAuthenticator, CookieAuthenticatorService, CookieAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n.I18nComponents
import play.api.libs.mailer.{SMTPConfiguration, SMTPMailer}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.{BodyParsers, DefaultCookieHeaderEncoding}
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import utils.silhouette._
import utils.{ErrorHandler, Filters, MailService, Mailer}

import scala.collection.immutable
import scala.concurrent.ExecutionContext

abstract class Web(context: Context) extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with I18nComponents
  with AhcWSComponents
  with AssetsComponents
  with CORSComponents
  with LagomServiceClientComponents with LagomConfigComponent{

  //lazy val csFilter: ContentSecurityPolicyFilter = new ContentSecurityPolicyFilter()

  // gzipFilter is defined in GzipFilterComponents
  lazy val f:Filters = wire[Filters]
  override lazy val httpFilters = /*Seq(csFilter) ++*/ f.filters

  override lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "web",
    Map(
      "web" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )

  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }

  lazy val fingerprintGenerator = new DefaultFingerprintGenerator(false)
  lazy val idGenerator = new SecureRandomIDGenerator()

  lazy val crypter: Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
    new JcaCrypter(config)
  }

  lazy val smtpConfig = SMTPConfiguration("smtp.gmail.com", 587, false, true, true, Some("upijcy@gmail.com"), Some("3chili94"), true, None, None, false)

  lazy val authenticatorService: AuthenticatorService[CookieAuthenticator] = {
    val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
    val encoder = new CrypterAuthenticatorEncoder(crypter)
    val cookieHeaderEncoder = new DefaultCookieHeaderEncoding()
    val signer:Signer = new JcaSigner(new JcaSignerSettings("secret", "secret"))
    new CookieAuthenticatorService(config, None, signer, cookieHeaderEncoder, encoder, fingerprintGenerator, idGenerator, clock)
  }

  lazy val providePasswordHasherRegistry: PasswordHasherRegistry = new PasswordHasherRegistry(passwordHasher)

  lazy val eventBus = EventBus()
  lazy val userService = serviceClient.implement[UserService]
  lazy val tokenService = serviceClient.implement[TokenService]
  lazy val userFacade = wire[UserFacade]
  private lazy val env: Environment[MyEnv] = Environment[MyEnv](
    userFacade, authenticatorService, List(), eventBus
  )

/*  lazy val securedErrorHandler: SecuredErrorHandler = wire[DefaultSecuredErrorHandler]
  lazy val unSecuredErrorHandler: UnsecuredErrorHandler = wire[DefaultUnsecuredErrorHandler]*/

  lazy val optionalSourceMapper:OptionalSourceMapper = wire[OptionalSourceMapper]

  lazy val bodyParser = wire[BodyParsers.Default]
  lazy val errorHandler:ErrorHandler = new ErrorHandler(environment, configuration, optionalSourceMapper, () => router, messagesApi)
  lazy val defaultSecuredHandler = wire[DefaultSecuredRequestHandler]
  lazy val defaultUnsecuredHandler = wire[DefaultUnsecuredRequestHandler]
  lazy val defaultUserAwareHandler = new DefaultUserAwareRequestHandler
  lazy val securedAction: SecuredAction = wire[DefaultSecuredAction]
  lazy val unsecuredAction: UnsecuredAction = wire[DefaultUnsecuredAction]
  lazy val userAwareAction:UserAwareAction = wire[DefaultUserAwareAction]
  lazy val passwordDao:PasswordInfoDAO = wire[PasswordInfoDAO]
  lazy val authInfoRepository:AuthInfoRepository = new DelegableAuthInfoRepository(passwordDao)
  lazy val passwordHasher = new BCryptPasswordHasher()
  lazy val mailClient = wire[SMTPMailer]
  lazy val mailerService = wire[MailService]
  lazy val mailer = wire[Mailer]

  lazy val credentialsProvider = new CredentialsProvider(authInfoRepository, providePasswordHasherRegistry)
  lazy val clock = Clock()
  lazy val silhouette: Silhouette[MyEnv] = wire[SilhouetteProvider[MyEnv]]

  lazy val mafiaService = serviceClient.implement[MafiaService]
  lazy val searchService = serviceClient.implement[GameProcessor]

  implicit lazy val appActorSystem: ActorSystem = actorSystem
  lazy val chat = wire[ChatController]
  lazy val auth = wire[Auth]
  lazy val ass = wire[MyAssets]
  lazy val app = wire[Application]
  override lazy val assets = wire[Assets]
}

class WebGatewayLoader extends ApplicationLoader {
  override def load(context: Context) = context.environment.mode match {
    case Mode.Dev =>
      (new Web(context) with LagomDevModeServiceLocatorComponents).application
    case _ =>
      (new Web(context) with ConductRApplicationComponents).application
  }
}
