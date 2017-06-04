package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App with StrictLogging {

  def start {
    val config = ConfigLoader.defaultConfig
    val emailClient = new DefaultEmailClient(config.emailerConfig)
    val users = new UsersFetcher(config.s3Config).getUsers

    val postcodeChecker = new PostcodeChecker(config, users)
    val dinnerChecker = new DinnerChecker(config, users)

    val notificationDispatcher = new NotificationDispatcher(emailClient)

    for {
      postCodeResults <- postcodeChecker.run
      dinnerResults <- dinnerChecker.run
      _ <- notificationDispatcher.dispatchNotifications(users, postCodeResults, dinnerResults)
    } yield ()
  }
  start
}