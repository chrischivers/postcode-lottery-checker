package com.postcodelotterychecker

import scalaz._
import Scalaz._
import scala.concurrent.{ExecutionContext, Future}

class NotificationDispatcher(emailClient: EmailClient)(implicit executionContext: ExecutionContext) {

  def dispatchNotifications(allUsers: List[User], postcodeWinnersLosers: Map[User, Option[Boolean]], dinnerWinnersLosers: Map[User, Option[Boolean]]): Future[Unit] = {
    Future {
      allUsers.foreach(user => {
        val postcodeResult = postcodeWinnersLosers(user)
        val dinnerResult = dinnerWinnersLosers(user)
        val combinedResult: Option[Boolean] = postcodeResult.toList ::: dinnerResult.toList match {
          case Nil => None
          case list =>  Some(list.exists(identity))
          }

        combinedResult.foreach(result => {
          val email = Email(
            to = user.email,
            subject = if (result) "CONGRATULATIONS YOU HAVE WON!" else "Sorry, you have not won today",
            body = generateMessageBody(postcodeResult, dinnerResult)
          )
          emailClient.sendEmail(email)
        })
      })
    }
  }

  def generateMessageBody(postcodeResult: Option[Boolean], dinnerResult: Option[Boolean]): String = {
    s"""
       |Today's Results
       |
      |${postcodeResult.fold("") { result => s"Postcode Lottery: ${if (result) "WON" else "Not won"}" }}
       |${dinnerResult.fold("") { result => s"Win A Dinner: ${if (result) "WON" else "Not won"}" }}
       |
    """.stripMargin
  }

}
