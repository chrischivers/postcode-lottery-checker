package com.postcodelotterychecker

import com.postcodelotterychecker.NotificationDispatcher.ResultsBundle

import scalaz._
import Scalaz._
import scala.concurrent.{ExecutionContext, Future}

object NotificationDispatcher {
  type UserResults = Map[User, Option[Boolean]]

  case class ResultsBundle[T](userResults: UserResults, winningResult: T)
}

class NotificationDispatcher(emailClient: EmailClient)(implicit executionContext: ExecutionContext) {

  def dispatchNotifications(allUsers: List[User], postcodeBundle: Option[ResultsBundle[Postcode]], dinnerBundle: Option[ResultsBundle[List[DinnerUserName]]], stackpotBundle: Option[ResultsBundle[List[Postcode]]], surveyDrawBundle: Option[ResultsBundle[Postcode]], emojiBundle: Option[ResultsBundle[Set[Emoji]]]): Future[Unit] = {
    Future {
      allUsers.foreach(user => {
        val postcodeResult = postcodeBundle.flatMap(_.userResults(user))
        val dinnerResult = dinnerBundle.flatMap(_.userResults(user))
        val stackpotResult = stackpotBundle.flatMap(_.userResults(user))
        val surveyDrawResult = surveyDrawBundle.flatMap(_.userResults(user))
        val emojiResult = emojiBundle.flatMap(_.userResults(user))
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
                s"Winning postcode was: ${postcodeBundle.map(_.winningResult.value).getOrElse("Unknown")} \n" +
                s"You are watching the following postcode(s): ${user.postCodesWatching.map(_.map(_.value).mkString(", ")).getOrElse("N/A")} \n"
            }
          }

             |${
            dinnerResult.fold("") { result =>
              s"Win A Dinner: ${if (result) "WON" else "Not won"} \n" +
                s"Winning users were: ${dinnerBundle.map(_.winningResult.map(_.value).mkString(", ")).getOrElse("Unknown")} \n" +
                s"You are watching the following user(s): ${user.dinnerUsersWatching.map(_.map(_.value).mkString(", ")).getOrElse("N/A")}"
            }
          }

             |${
            stackpotResult.fold("") { result =>
              s"Stackpot: ${if (result) "WON" else "Not won"} \n" +
                s"Winning stackpot postcodes were: ${stackpotBundle.map(_.winningResult.map(_.value).mkString(", ")).getOrElse("Unknown")} \n" +
                s"You are watching the following postcode(s): ${user.postCodesWatching.map(_.map(_.value).mkString(", ")).getOrElse("N/A")} \n"
            }
          }

            |${
            surveyDrawResult.fold("") { result =>
              s"Survey Draw: ${if (result) "WON" else "Not won"} \n" +
                s"Winning survey draw postcode was: ${surveyDrawBundle.map(_.winningResult.value).getOrElse("Unknown")} \n" +
                s"You are watching the following postcode(s): ${user.postCodesWatching.map(_.map(_.value).mkString(", ")).getOrElse("N/A")} \n"
            }
          }

             |${
            emojiResult.fold("") { result =>
              s"Emoji Lottery: ${if (result) "WON" else "Not won"} \n" +
                s"Winning emojis were: ${emojiBundle.map(_.winningResult.map(_.id).mkString(",")).getOrElse("Unknown")} \n" +
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
