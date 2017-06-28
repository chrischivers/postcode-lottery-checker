package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App with StrictLogging {

  def start {
    val config = ConfigLoader.defaultConfig
    val emailClient = new DefaultEmailClient(config.emailerConfig)
    val users = new UsersFetcher(config.s3Config).getUsers

    val visionAPIClient = new VisionAPIClient(config.visionApiConfig)
    val screenshotAPIClient = new ScreenshotAPIClient(config.screenshotApiConfig)
    val postcodeChecker = new PostcodeChecker(config.postcodeCheckerConfig, users, visionAPIClient, screenshotAPIClient)
    val stackpotChecker = new StackpotChecker(config.stackpotCheckerConfig, users, visionAPIClient, screenshotAPIClient)
    val dinnerChecker = new DinnerChecker(config.dinnerCheckerConfig, users)
    val quidcoHitter = new QuidcoHitter(config.quidcoHitterConfig)
    val surveyDrawChecker = new SurveyDrawChecker(config.surveyDrawCheckerConfig, users, visionAPIClient, screenshotAPIClient)
    val emojiChecker = new EmojiChecker(config.emojiCheckerConfig, users)

    val notificationDispatcher = new NotificationDispatcher(emailClient)

    val runner = for {
      postCodeResults <- postcodeChecker.run
      stackpotResults <- stackpotChecker.run
      dinnerResults <- dinnerChecker.run
      surveyDrawResults <- surveyDrawChecker.run
      emojiResults <- emojiChecker.run
      _ <- notificationDispatcher.dispatchNotifications(users, postCodeResults._1, postCodeResults._2, dinnerResults._1, dinnerResults._2, stackpotResults._1, stackpotResults._2, surveyDrawResults._1, surveyDrawResults._2, emojiResults._1, emojiResults._2)
      _ <- quidcoHitter.run
    } yield ()

    Await.result(runner, 2 minute)
  }
  start
}