package com.postcodelotterychecker

import java.util.UUID

import com.postcodelotterychecker.NotificationDispatcher.{ResultsBundle, UserResults}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaj.http.Http

object Main extends App with StrictLogging {

  def config = ConfigLoader.defaultConfig

  def totalNumberAttempts = 10

  def secondsBetweenRetries = 30

//    startMaster

  def startMaster = {

    val emailClient = new DefaultEmailClient(config.emailerConfig)
    val notificationDispatcher = new NotificationDispatcher(emailClient)
    val resultsFromS3Assembler = new ResultsFromS3Assembler(config.s3Config)
    val users = new UsersFetcher(config.s3Config).getUsers

    val sessionUUid = UUID.randomUUID().toString

    triggerLambdas
    retryHelper(1)

    def triggerLambdas = {
      logger.info("triggering lambdas")

      def generateHttpCall(url: String) = Http(url + "/" + sessionUUid)

      val httpEndpoints = List(
        generateHttpCall(config.postcodeCheckerConfig.lambdaTriggerUrl),
        generateHttpCall(config.dinnerCheckerConfig.lambdaTriggerUrl),
        generateHttpCall(config.stackpotCheckerConfig.lambdaTriggerUrl),
        generateHttpCall(config.surveyDrawCheckerConfig.lambdaTriggerUrl),
        generateHttpCall(config.emojiCheckerConfig.lambdaTriggerUrl),
        generateHttpCall(config.quidcoHitterConfig.lambdaTriggerUrl))

      httpEndpoints.map(endpoint => Future(endpoint.execute()))
    }

    def retryHelper(attemptNumber: Int): Unit = {
      val startTime = System.currentTimeMillis()
      logger.info(s"Main Attempt $attemptNumber of $totalNumberAttempts")
      if (attemptNumber > totalNumberAttempts) {
        logger.info("Reached last attempt. Sending anyway.")
        checkAndEmail(lastAttempt = true)
      }
      else {
        checkAndEmail() match {
          case Some(fn) => Await.result({
            fn
              .map(_ => logger.info("successfully completed"))
              .recoverWith { case _ =>
                logger.info(s"Main execution failed on attempt $attemptNumber. Recovering.")
                Future(sleepAndRetry)
              }
          }, 5.minutes)
          case None =>
            logger.info(s"Main execution failed on attempt $attemptNumber. Recovering.")
            sleepAndRetry
        }
      }

      def sleepAndRetry = {
        val sleepTime = ((secondsBetweenRetries * 1000) + startTime) - System.currentTimeMillis()
        logger.info(s"Sleeping for $sleepTime milliseconds")
        Thread.sleep(sleepTime)
        retryHelper(attemptNumber + 1)
      }
    }


    def checkAndEmail(lastAttempt: Boolean = false): Option[Future[Unit]] = {
      val postcodeUserResults = resultsFromS3Assembler.getUserResults("postcode-results", sessionUUid)
      val postcodeWinningResult = resultsFromS3Assembler.getPostcodeWinningResult(sessionUUid)
      val stackpotUserResults = resultsFromS3Assembler.getUserResults("stackpot-results", sessionUUid)
      val stackpotWinningResult = resultsFromS3Assembler.getStackpotWinningResult(sessionUUid)
      val surveyDrawUserResults = resultsFromS3Assembler.getUserResults("survey-draw-results", sessionUUid)
      val surveyDrawWinningResult = resultsFromS3Assembler.getSurveyDrawWinningResult(sessionUUid)
      val dinnerUserResults = resultsFromS3Assembler.getUserResults("dinner-results", sessionUUid)
      val dinnerWinningResult = resultsFromS3Assembler.getDinnerWinningResult(sessionUUid)
      val emojiUserResults = resultsFromS3Assembler.getUserResults("emoji-results", sessionUUid)
      val emojiWinningResult = resultsFromS3Assembler.getEmojiWinningResult(sessionUUid)


      def sendToNotificationDispatcher: Future[Unit] = {

        def resultsBundleOrNone[A](userResults: Option[UserResults], winningResult: Option[A]): Option[ResultsBundle[A]] = {
          (userResults, winningResult) match {
            case (Some(userRes), Some(winningRes)) => Some(ResultsBundle(userRes, winningRes))
            case _ => None
          }
        }
        notificationDispatcher.dispatchNotifications(
          users,
          resultsBundleOrNone(postcodeUserResults, postcodeWinningResult),
          resultsBundleOrNone(dinnerUserResults, dinnerWinningResult),
          resultsBundleOrNone(stackpotUserResults, stackpotWinningResult),
          resultsBundleOrNone(surveyDrawUserResults, surveyDrawWinningResult),
          resultsBundleOrNone(emojiUserResults, emojiWinningResult)
        )
      }

      if (lastAttempt) Some(sendToNotificationDispatcher)
      else {
        if (List(postcodeUserResults, postcodeWinningResult, stackpotUserResults,
          stackpotWinningResult, surveyDrawUserResults, surveyDrawWinningResult,
          dinnerUserResults, dinnerWinningResult, emojiUserResults, emojiWinningResult).exists(_.isEmpty)) None
        else Some(sendToNotificationDispatcher)
      }
    }
  }
}