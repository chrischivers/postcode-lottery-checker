package com.postcodelotterychecker

import scalaz._
import Scalaz._
import scala.concurrent.{ExecutionContext, Future}

class NotificationDispatcher(emailClient: EmailClient)(implicit executionContext: ExecutionContext) {

  def dispatchNotifications(allUsers: List[User], postcodeUserResults: Map[User, Option[Boolean]], winningPostcode: Postcode, dinnerUserResults: Map[User, Option[Boolean]], winningDinnerUsers: List[DinnerUserName], stackPotUserResults: Map[User, Option[Boolean]], winningStackpotPostcodes: List[Postcode]): Future[Unit] = {
    Future {
    def generateMessageBody(postcodeResult: Option[Boolean], dinnerResult: Option[Boolean], stackpotResult: Option[Boolean]): String = {
      s"""
         |Today's Results
         |
         |${postcodeResult.fold("") { result =>
            s"Postcode Lottery: ${if (result) "WON" else "Not won"} \n" +
          s"Winning postcode was: ${winningPostcode.value} \n" }}

         |${dinnerResult.fold("") { result => s"Win A Dinner: ${if (result) "WON" else "Not won"} \n" +
            s"Winning users were: ${winningDinnerUsers.map(_.value).mkString(", ")} \n"}}

         |${stackpotResult.fold("") { result =>
          s"Stackpot: ${if (result) "WON" else "Not won"} \n" +
          s"Winning postcodes were: ${winningStackpotPostcodes.map(_.value).mkString(", ")}"
        }
      }
    """.stripMargin
    }

      allUsers.foreach(user => {
        val postcodeResult = postcodeUserResults(user)
        val dinnerResult = dinnerUserResults(user)
        val stackpotResult = stackPotUserResults(user)
        val combinedResult: Option[Boolean] = postcodeResult.toList ::: dinnerResult.toList ::: stackpotResult.toList match {
          case Nil => None
          case list =>  Some(list.exists(identity))
        }

        combinedResult.foreach(result => {
          val email = Email(
            to = user.email,
            subject = if (result) "CONGRATULATIONS YOU HAVE WON!" else "Sorry, you have not won today",
            body = generateMessageBody(postcodeResult, dinnerResult, stackpotResult)
          )
          emailClient.sendEmail(email)
        })
      })
    }
  }
}
