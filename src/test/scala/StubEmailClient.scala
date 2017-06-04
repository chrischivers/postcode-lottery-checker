import com.postcodelotterychecker.{Email, EmailClient}
import com.typesafe.scalalogging.StrictLogging

class StubEmailClient extends EmailClient with StrictLogging {

  var emailsSent: List[Email] = List.empty

  override def sendEmail(email: Email): Unit = {
    emailsSent = emailsSent :+ email

  }
}
