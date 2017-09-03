package com.postcodelotterychecker

import java.util.UUID

import com.postcodelotterychecker.NotificationDispatcher.ResultsBundle
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaj.http.Http

object Main extends App with StrictLogging {

  def config = ConfigLoader.defaultConfig

  def totalNumberAttempts = 10

  def secondsBetweenRetries = 30

//  startMaster

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
      if (attemptNumber > totalNumberAttempts) throw new RuntimeException(s"Retried $totalNumberAttempts times. No more retries")
      else {
        checkAndEmail match {
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


    def checkAndEmail: Option[Future[Unit]] = for {
      postcodeUserResults <- resultsFromS3Assembler.getUserResults("postcode-results", sessionUUid)
      _ = logger.info("got postcode user results")
      postcodeWinningResult <- resultsFromS3Assembler.getPostcodeWinningResult(sessionUUid)
      _ = logger.info("got postcode winning result")
      stackpotUserResults <- resultsFromS3Assembler.getUserResults("stackpot-results", sessionUUid)
      _ = logger.info("got stackpot user results")
      stackpotWinningResult <- resultsFromS3Assembler.getStackpotWinningResult(sessionUUid)
      _ = logger.info("got stackpot winning result")
      surveyDrawUserResults <- resultsFromS3Assembler.getUserResults("survey-draw-results", sessionUUid)
      _ = logger.info("got survey draw user results")
      surveyDrawWinningResult <- resultsFromS3Assembler.getSurveyDrawWinningResult(sessionUUid)
      _ = logger.info("got survey draw winning result")
      dinnerUserResults <- resultsFromS3Assembler.getUserResults("dinner-results", sessionUUid)
      _ = logger.info("got dinner user results")
      dinnerWinningResult <- resultsFromS3Assembler.getDinnerWinningResult(sessionUUid)
      _ = logger.info("got dinner winning result")
      emojiUserResults <- resultsFromS3Assembler.getUserResults("emoji-results", sessionUUid)
      _ = logger.info("got emoji user results")
      emojiWinningResult <- resultsFromS3Assembler.getEmojiWinningResult(sessionUUid)
      _ = logger.info("got emoji winning result")
    } yield {
      notificationDispatcher.dispatchNotifications(
        users,
        Some(ResultsBundle(postcodeUserResults, postcodeWinningResult)),
        Some(ResultsBundle(dinnerUserResults, dinnerWinningResult)),
        Some(ResultsBundle(stackpotUserResults, stackpotWinningResult)),
        Some(ResultsBundle(surveyDrawUserResults, surveyDrawWinningResult)),
        Some(ResultsBundle(emojiUserResults, emojiWinningResult))
      )
    }
  }
}