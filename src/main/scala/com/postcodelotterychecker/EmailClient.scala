package com.postcodelotterychecker

import java.util.Properties
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Session, _}

import com.postcodelotterychecker.utils.Utils
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

case class Email(subject: String, body: String, to: String)

trait EmailClient {
  def sendEmail(email: Email): Unit
}

class DefaultEmailClient(emailerConfig: EmailerConfig) extends EmailClient with StrictLogging {

  val properties = new Properties()
  properties.put("mail.transport.protocol", "smtp")
  properties.put("mail.smtp.auth", "true")
  properties.put("mail.smtp.starttls.enable", "true")
  properties.put("mail.smtp.user", emailerConfig.smtpUsername)
  properties.put("mail.smtp.password", emailerConfig.smtpPassword)
  properties.put("mail.smtp.host", emailerConfig.smtpHost)
  properties.put("mail.smtp.port", emailerConfig.smtpPort.toString)
  val session: Session = Session.getInstance(properties, new Authenticator() {
    override protected def getPasswordAuthentication = new PasswordAuthentication(emailerConfig.smtpUsername, emailerConfig.smtpPassword)
  })
  session.setDebug(true)

  override def sendEmail(email: Email): Unit = {

    val message = new MimeMessage(session)

    message.setFrom(new InternetAddress(emailerConfig.fromAddress))
    message.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(email.to))
    message.setSubject(email.subject)
    message.setText(email.body)

    Utils.retry(emailerConfig.numberAttempts, emailerConfig.secondsBetweenAttempts) {
      logger.info(s"Sending email to ${email.to}")
      Transport.send(message)
    }
  }


}
