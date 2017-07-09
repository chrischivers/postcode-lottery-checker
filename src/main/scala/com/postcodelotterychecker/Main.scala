package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object Main extends App with StrictLogging {

  def start {
    val config = ConfigLoader.defaultConfig
    val emailClient = new DefaultEmailClient(config.emailerConfig)
    val users = new UsersFetcher(config.s3Config).getUsers

    val postcodeChecker = new PostcodeChecker(config.postcodeCheckerConfig, users)
    val stackpotChecker = new StackpotChecker(config.stackpotCheckerConfig, users)
    val dinnerChecker = new DinnerChecker(config.dinnerCheckerConfig, users)
    val quidcoHitter = new QuidcoHitter(config.quidcoHitterConfig)
    val surveyDrawChecker = new SurveyDrawChecker(config.surveyDrawCheckerConfig, users)
    val emojiChecker = new EmojiChecker(config.emojiCheckerConfig, users)
    val notificationDispatcher = new NotificationDispatcher(emailClient)


    val postcodeCheckerResults = postcodeChecker.run
    val stackpotCheckerResults = stackpotChecker.run
    val dinnerCheckerResults = dinnerChecker.run
    val quidcoHitterResults = quidcoHitter.run
    val surveyDrawCheckerResults = surveyDrawChecker.run
    val emojiCheckerResults = emojiChecker.run

    val runner = for {
      postCodeResults <- postcodeCheckerResults
      stackpotResults <- stackpotCheckerResults
      dinnerResults <- dinnerCheckerResults
      surveyDrawResults <- surveyDrawCheckerResults
      emojiResults <- emojiCheckerResults
      _ <- quidcoHitterResults
      _ <- notificationDispatcher.dispatchNotifications(users, postCodeResults._1, postCodeResults._2, dinnerResults._1, dinnerResults._2, stackpotResults._1, stackpotResults._2, surveyDrawResults._1, surveyDrawResults._2, emojiResults._1, emojiResults._2)

    } yield ()

    Await.result(runner, 20 minute)
  }
  start
}