package com.postcodelotterychecker.results

import scala.collection.mutable.ListBuffer

class StubEmailClient extends EmailClient {

  val emailsSent = new ListBuffer[Email]

  override def sendEmail(email: Email): Unit =    {
    emailsSent += email
  }
}
