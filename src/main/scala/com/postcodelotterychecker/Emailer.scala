package com.postcodelotterychecker
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.mail._
import javax.mail.internet.{MimeMessage, _}

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.{Message => GmailMessage}
import com.typesafe.scalalogging.StrictLogging

object Emailer extends StrictLogging {

  def sendEmail(to: String, from: String, subject: String, bodyText: String, service: Gmail, user: String) = {
    val email = createEmail(to, from, subject, bodyText)
    val message = createMessageWithEmail(email)
    val sentMessage = service.users().messages().send(user, message).execute()
    logger.info(s"Email sent with ID ${sentMessage.getId}, Email : ${sentMessage.toPrettyString}")
  }

  private def createEmail(to: String, from: String, subject: String, bodyText: String): MimeMessage = {
    val props = new Properties()
    val session = Session.getDefaultInstance(props, null)
    val email = new MimeMessage(session)
    email.setFrom(new InternetAddress(from))
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to))
    email.setSubject(subject)
    email.setText(bodyText)
    email
  }


  private def createMessageWithEmail(emailContent: MimeMessage): GmailMessage = {
    val buffer = new ByteArrayOutputStream
    emailContent.writeTo(buffer)
    val bytes = buffer.toByteArray
    val encodedEmail = Base64.encodeBase64URLSafeString(bytes)
    val message = new GmailMessage
    message.setRaw(encodedEmail)
    message
  }


}
