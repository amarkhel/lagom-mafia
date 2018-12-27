package utils

import play.twirl.api.Html
import play.api.i18n.Messages
import views.html.mails
import javax.inject.{Inject, Singleton}

import com.amarkhel.user.api.User

@Singleton
class Mailer @Inject() (ms: MailService) {

  implicit def html2String(html: Html): String = html.toString

  def welcome(user: User, link: String)(implicit m: Messages) {
    ms.sendEmailAsync(user.email)(
      subject = Messages("mail.welcome.subject"),
      bodyHtml = mails.welcome(user.name, link),
      bodyText = mails.welcomeTxt(user.name, link)
    )
  }

  def forgotPassword(email: String, link: String)(implicit m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("mail.forgotpwd.subject"),
      bodyHtml = mails.forgotPassword(email, link),
      bodyText = mails.forgotPasswordTxt(email, link)
    )
  }
}