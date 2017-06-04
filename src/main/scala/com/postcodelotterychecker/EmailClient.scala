package com.postcodelotterychecker

import java.util.Properties
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Session, _}

import com.typesafe.scalalogging.StrictLogging

case class Email(subject: String, body: String, to: String)

trait EmailClient {
  def sendEmail(email: Email): Unit
}

class DefaultEmailClient(emailerConfig: EmailerConfig) extends EmailClient with StrictLogging {

  override def sendEmail(email: Email): Unit = {

    val properties = new Properties()
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.starttls.enable", "true")
    properties.put("mail.smtp.user", emailerConfig.smtpUsername)
    properties.put("mail.smtp.password", emailerConfig.smtpPassword)
    properties.put("mail.smtp.host", emailerConfig.smtpHost)
    properties.put("mail.smtp.port", emailerConfig.smtpPort.toString)
    val session = Session.getInstance(properties, new Authenticator() {
      override protected def getPasswordAuthentication = new PasswordAuthentication(emailerConfig.smtpUsername, emailerConfig.smtpPassword)
    })
    session.setDebug(true)
    val message = new MimeMessage(session)

    message.setFrom(new InternetAddress(emailerConfig.fromAddress))
    message.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(email.to))
    message.setSubject(email.subject)
    message.setText(email.body)

    try {
      logger.info(s"Sending email to ${email.to}")
      Transport.send(message)
    } catch {
      case e: Exception => logger.error(s"Error sending email to ${email.to}", e)
    }
  }
}
