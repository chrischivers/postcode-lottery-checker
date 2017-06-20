package com.postcodelotterychecker

import scalaz._
import Scalaz._
import scala.concurrent.{ExecutionContext, Future}

class NotificationDispatcher(emailClient: EmailClient)(implicit executionContext: ExecutionContext) {

  def dispatchNotifications(allUsers: List[User], postcodeUserResults: Map[User, Option[Boolean]], winningPostcode: Postcode, dinnerUserResults: Map[User, Option[Boolean]], winningDinnerUsers: List[DinnerUserName], stackPotUserResults: Map[User, Option[Boolean]], winningStackpotPostcodes: List[Postcode], surveyDrawUserResults: Map[User, Option[Boolean]], winningSurveyDrawPostCode: Postcode, emojiUserResults: Map[User, Option[Boolean]], winningEmojiSequence: Set[Emoji]): Future[Unit] = {
    Future {
      allUsers.foreach(user => {
        val postcodeResult = postcodeUserResults(user)
        val dinnerResult = dinnerUserResults(user)
        val stackpotResult = stackPotUserResults(user)
        val surveyDrawResult = surveyDrawUserResults(user)
        val emojiResult = emojiUserResults(user)
        val combinedResult: Option[Boolean] = postcodeResult.toList ::: dinnerResult.toList ::: stackpotResult.toList ::: surveyDrawResult.toList ::: emojiResult.toList match {
          case Nil => None
          case list => Some(list.exists(identity))
        }

        def generateMessageBody(postcodeResult: Option[Boolean], dinnerResult: Option[Boolean], stackpotResult: Option[Boolean], surveyDrawResult: Option[Boolean], emojiResult: Option[Boolean]): String = {
          s"""
             |Today's Results
             |
              |${
            postcodeResult.fold("") { result =>
              s"Postcode Lottery: ${if (result) "WON" else "Not won"} \n" +
                s"Winning postcode was: ${winningPostcode.value} \n" +
                s"You are watching the following postcode(s): ${user.postCodesWatching.map(_.map(_.value).mkString(", ")).getOrElse("N/A")} \n"
            }
          }

             |${
            dinnerResult.fold("") { result =>
              s"Win A Dinner: ${if (result) "WON" else "Not won"} \n" +
                s"Winning users were: ${winningDinnerUsers.map(_.value).mkString(", ")} \n" +
                s"You are watching the following user(s): ${user.dinnerUsersWatching.map(_.map(_.value).mkString(", ")).getOrElse("N/A")}"
            }
          }

             |${
            stackpotResult.fold("") { result =>
              s"Stackpot: ${if (result) "WON" else "Not won"} \n" +
                s"Winning stackpot postcodes were: ${winningStackpotPostcodes.map(_.value).mkString(", ")} \n" +
                s"You are watching the following postcode(s): ${user.postCodesWatching.map(_.map(_.value).mkString(", ")).getOrElse("N/A")} \n"
            }
          }

            |${
            surveyDrawResult.fold("") { result =>
              s"Survey Draw: ${if (result) "WON" else "Not won"} \n" +
                s"Winning survey draw postcode was: ${winningSurveyDrawPostCode.value} \n" +
                s"You are watching the following postcode(s): ${user.postCodesWatching.map(_.map(_.value).mkString(", ")).getOrElse("N/A")} \n"
            }
          }

             |${
            emojiResult.fold("") { result =>
              s"Emoji Lottery: ${if (result) "WON" else "Not won"} \n" +
                s"Winning emojis were: ${winningEmojiSequence.map(_.id).mkString(", ")} \n" +
                s"You are watching the following emoji sets: ${user.emojiSetsWatching.map(_.map(_.map(_.id).mkString(", ")).mkString("; ")).getOrElse("N/A")} \n"
            }
          }
          """.stripMargin
        }

        combinedResult.foreach(result => {

          val email = Email(
            to = user.email,
            subject = if (result) "CONGRATULATIONS YOU HAVE WON!" else "Sorry, you have not won today",
            body = generateMessageBody(postcodeResult, dinnerResult, stackpotResult, surveyDrawResult, emojiResult)
          )
          emailClient.sendEmail(email)
        })
      })
    }
  }
}
