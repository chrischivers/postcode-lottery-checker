package com.postcodelotterychecker

import java.util.Properties
import javax.mail.{Session, _}
import javax.mail.internet.{InternetAddress, MimeMessage}

import com.ditcherj.contextio.ContextIO
import com.ditcherj.contextio.dto.Type
import com.typesafe.scalalogging.StrictLogging

import scala.annotation.tailrec
import scala.collection.JavaConverters._

case class Email(subject: String, body: String, from: String, to: String)

class EmailClient(contextIoConfig: ContextIoConfig, emailerConfig: EmailerConfig) extends StrictLogging {

  val contextIO = new ContextIO(contextIoConfig.clientKey, contextIoConfig.secret)
  val accountId = contextIoConfig.accountId


  def getMostRecentMessageBody(subjectStr: String): String = {
    val messagesDescending = contextIO.getMessages(accountId).getMessages.asScala.toList.sortBy(msg => msg.getDate_received).reverse
    val matchingMessages = messagesDescending.filter(msg => msg.getSubject.contains(subjectStr))
    logger.info(s"Messages containing subject string $subjectStr are: ${matchingMessages.map(_.getSubject)}")
    val mostRecentMessageId = matchingMessages.headOption.getOrElse(throw new RuntimeException(s"Cannot get most recent message with subject $subjectStr")).getMessage_id
    logger.info(s"Most recent message ID: $mostRecentMessageId")

    @tailrec
    def getMessageBodyHelper(attempt: Int): String = {
      contextIO.getConnectTokens(accountId).getTokens
      val response = contextIO.getMessageBody(accountId, mostRecentMessageId, Type.TXT)
      if (response.getCode == 200) response.toString
      else if (attempt <= contextIoConfig.readRetries) {
        logger.info(s"Response code ${response.getCode} received. Waiting before trying again. Attempt $attempt of ${contextIoConfig.readRetries}")
        Thread.sleep(contextIoConfig.timeBetweenRetries)
        getMessageBodyHelper(attempt + 1)
      } else {
        throw new RuntimeException(s"Unable to get email after $attempt attempts. Response code: ${response.getCode}")
      }
    }
    getMessageBodyHelper(1)
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
    } catch {
      case e: Exception => logger.error(s"Error sending email to ${email.to}", e)
    }
  }
}
