package com.postcodelotterychecker

import java.util.Properties
import javax.mail.Session
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail._
import javax.mail.internet._

import com.ditcherj.contextio.ContextIO
import com.ditcherj.contextio.dto.{Message, Type}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._

case class Email(subject: String, body: String, from: String, to: String)

class EmailClient(contextIoConfig: ContextIoConfig, emailerConfig: EmailerConfig) extends StrictLogging {

  val contextIO = new ContextIO(contextIoConfig.clientKey, contextIoConfig.secret)
  val accountId = contextIoConfig.accountId


  def getMostRecentMessageBody: String = {
    val messagesDescending = contextIO.getMessages(accountId).getMessages.asScala.toList.sortBy(msg => msg.getDate_received).reverse
    val postcodeMessages = messagesDescending.filter(msg => msg.getSubject.contains("Draw Alert"))
    val mostRecentMessageId = postcodeMessages.headOption.getOrElse(throw new RuntimeException("Cannot get most recent message")).getMessage_id
    contextIO.getMessageBody(accountId, mostRecentMessageId, Type.TXT).toString
  }

  def sendEmail(email: Email): Unit = {

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

    message.setFrom(new InternetAddress(email.from))
    message.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(email.to))
    message.setSubject(email.subject)
    message.setText(email.body)

    try {
      logger.info(s"Sending email to ${email.to}")
      Transport.send(message)
      Thread.sleep(5000)
    } catch {
      case e: Exception => logger.error(s"Error sending email to ${email.to}", e)
    }
  }
}
