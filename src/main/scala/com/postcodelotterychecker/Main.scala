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

    val postcodeChecker = new PostcodeChecker(config.postcodeCheckerConfig, users)
    val stackpotChecker = new StackpotChecker(config.stackpotCheckerConfig, users)
    val dinnerChecker = new DinnerChecker(config.dinnerCheckerConfig, users)
    val quidcoHitter = new QuidcoHitter(config.quidcoHitterConfig)

    val notificationDispatcher = new NotificationDispatcher(emailClient)

    val runner = for {
      postCodeResults <- postcodeChecker.run
      stackpotResults <- stackpotChecker.run
      dinnerResults <- dinnerChecker.run
      _ <- notificationDispatcher.dispatchNotifications(users, postCodeResults._1, postCodeResults._2, dinnerResults._1, dinnerResults._2, stackpotResults._1, stackpotResults._2)
      _ <- quidcoHitter.run
    } yield ()

    Await.result(runner, 2 minute)
  }
  start
}