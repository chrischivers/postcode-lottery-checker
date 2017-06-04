package com.postcodelotterychecker

import scalaz._
import Scalaz._
import scala.concurrent.{ExecutionContext, Future}

class NotificationDispatcher(emailClient: EmailClient)(implicit executionContext: ExecutionContext) {

  def dispatchNotifications(allUsers: List[User], postcodeUserResults: Map[User, Option[Boolean]], winningPostcode: Postcode, dinnerUserResults: Map[User, Option[Boolean]], winningDinnerUsers: List[DinnerUserName]): Future[Unit] = {
    Future {
    def generateMessageBody(postcodeResult: Option[Boolean], dinnerResult: Option[Boolean]): String = {
      s"""
         |Today's Results
         |
         |${postcodeResult.fold("") { result =>
            s"Postcode Lottery: ${if (result) "WON" else "Not won"} \n" +
          s"Winning postcode was: ${winningPostcode.value} \n" }}
         |${dinnerResult.fold("") { result => s"Win A Dinner: ${if (result) "WON" else "Not won"} \n" +
            s"Winning users were: ${winningDinnerUsers.map(_.value).mkString(", ")}" }}
         |
    """.stripMargin
    }

      allUsers.foreach(user => {
        val postcodeResult = postcodeUserResults(user)
        val dinnerResult = dinnerUserResults(user)
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
}
