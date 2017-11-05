package results

import com.postcodelotterychecker.results.{Email, EmailClient}

import scala.collection.mutable.ListBuffer

class StubEmailClient extends EmailClient {

  val emailsSent = new ListBuffer[Email]

  override def sendEmail(email: Email): Unit =    {
    emailsSent += email
  }
}
