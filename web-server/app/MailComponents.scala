import akka.actor.ActorSystem
import com.softwaremill.macwire.wire
import play.api.Configuration
import play.api.libs.mailer.{SMTPConfiguration, SMTPMailer}
import utils.{MailService, Mailer}

trait MailComponents {
  def actorSystem: ActorSystem
  def configuration: Configuration
  lazy val smtpConfig = SMTPConfiguration("smtp.gmail.com", 587, false, true, true, Some("upijcy@gmail.com"), Some("3chili94"), true, None, None, false)
  lazy val mailClient = wire[SMTPMailer]
  lazy val mailerService = wire[MailService]
  lazy val mailer = wire[Mailer]
}
