package com.postcodelotterychecker

import cats.effect.IO
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models._
import com.postcodelotterychecker.results.{ResultsEmailer, ResultsProcessor}
import com.postcodelotterychecker.subscribers.SubscribersFetcher
import com.typesafe.scalalogging.StrictLogging

import scala.annotation.tailrec
import scalaj.http.Http

trait Coordinator extends StrictLogging {

  val mainConfig: Config
  val uuid: String

  val resultsProcessor: ResultsProcessor

  val subscribersFetcher: SubscribersFetcher

  val resultsEmailer: ResultsEmailer

  val postcodeResultsCache: RedisResultCache[Postcode]

  val dinnerResultsCache:  RedisResultCache[List[DinnerUserName]]

  val stackpotResultsCache: RedisResultCache[List[Postcode]]

  val surveyDrawResultsCache: RedisResultCache[Postcode]

  val emojiResultsCache: RedisResultCache[Set[Emoji]]

  def triggerLambdas() = {
    logger.info("Triggering lambdas")
    triggerLambda(mainConfig.postcodeCheckerConfig.lambdaTriggerUrl).runAsync(_ => IO.pure())
    triggerLambda(mainConfig.dinnerCheckerConfig.lambdaTriggerUrl).runAsync(_ => IO.pure())
    triggerLambda(mainConfig.stackpotCheckerConfig.lambdaTriggerUrl).runAsync(_ => IO.pure())
    triggerLambda(mainConfig.surveyDrawCheckerConfig.lambdaTriggerUrl).runAsync(_ => IO.pure())
    triggerLambda(mainConfig.emojiCheckerConfig.lambdaTriggerUrl).runAsync(_ => IO.pure())
    triggerLambda(mainConfig.quidcoHitterConfig.lambdaTriggerUrl).runAsync(_ => IO.pure())
  }

  private def triggerLambda(url: String): IO[Unit] = {
    logger.info(s"Triggering lambda for url $url")
    IO(Http(url + "/" + uuid).execute())
  }


  def aggregateAndProcessResults(): IO[Unit] = {
    val startingTime = System.currentTimeMillis()
    val cutOffTime = startingTime + mainConfig.resultsProcessorConfig.timeCutoff.toMillis
    def getTimeRemaining = cutOffTime - System.currentTimeMillis()

    @tailrec
    def helper(attemptNumber: Int, timeRemaining: Long): IO[Unit] = {
      val attemptStartedAt = System.currentTimeMillis()
      logger.info(s"Results processing attempt $attemptNumber, time remaining: $timeRemaining milliseconds")

      val run: IO[Unit] = for {
        winningResults <- resultsProcessor.aggregateResults(uuid)
        _ = if (!winningResults.allDefined && timeRemaining > 0) throw new RuntimeException("Blowing up...")
        subscribersList <- subscribersFetcher.getSubscribers
        subscribersResultsMap = resultsProcessor.mapSubscribersToResults(subscribersList, winningResults)
        numberEmailsSent <- resultsEmailer.sendEmails(subscribersResultsMap)
        _ = logger.info(s"$numberEmailsSent emails successfully sent")
      } yield ()

      run.attempt.unsafeRunSync() match {
        case Left(err) =>
          Thread.sleep((attemptStartedAt + mainConfig.resultsProcessorConfig.timeBetweenRetries.toMillis) - System.currentTimeMillis())
          logger.error(s"Error on attempt to aggregate and process results. Retrying...", err)
          helper(attemptNumber + 1, getTimeRemaining)
        case Right(_) => IO(logger.info("Aggregation and processing of results completed."))
      }
    }

    helper(attemptNumber = 1, timeRemaining = getTimeRemaining)

  }
}
