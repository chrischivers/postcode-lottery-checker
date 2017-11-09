package com.postcodelotterychecker

import java.util.UUID

import cats.effect.IO
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.Request
import com.postcodelotterychecker.checkers.{HtmlUnitWebClient, _}
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models._
import com.postcodelotterychecker.results._
import com.postcodelotterychecker.subscribers.SubscribersFetcher
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
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

  def triggerCheckers() = {
    logger.info("Triggering lambdas")
    Future(triggerLambda(mainConfig.postcodeCheckerConfig.lambdaTriggerUrl).unsafeRunSync())
    Future(triggerLambda(mainConfig.dinnerCheckerConfig.lambdaTriggerUrl).unsafeRunSync())
    Future(triggerLambda(mainConfig.stackpotCheckerConfig.lambdaTriggerUrl).unsafeRunSync())
    Future(triggerLambda(mainConfig.surveyDrawCheckerConfig.lambdaTriggerUrl).unsafeRunSync())
    Future(triggerLambda(mainConfig.emojiCheckerConfig.lambdaTriggerUrl).unsafeRunSync())
    Future(triggerLambda(mainConfig.quidcoHitterConfig.lambdaTriggerUrl).unsafeRunSync())
    IO.unit
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
        _ = if (!winningResults.allDefined && timeRemaining > 0) {
          logger.info(s"Not all winning results defined [$winningResults]")
          throw new RuntimeException("Blowing up...")
        }
        subscribersList <- subscribersFetcher.getSubscribers
        subscribersResultsMap = resultsProcessor.mapSubscribersToResults(subscribersList, winningResults)
        numberEmailsSent <- resultsEmailer.sendEmails(subscribersResultsMap)
        _ = logger.info(s"$numberEmailsSent emails successfully sent")
      } yield ()

      run.attempt.unsafeRunSync() match {
        case Left(err) =>
          val sleepTime = (attemptStartedAt + mainConfig.resultsProcessorConfig.timeBetweenRetries.toMillis) - System.currentTimeMillis()
          Thread.sleep(if (sleepTime < 0) 0 else sleepTime)
          logger.error(s"Error on attempt to aggregate and process results. Retrying...", err)
          helper(attemptNumber + 1, getTimeRemaining)
        case Right(_) => IO(logger.info("Aggregation and processing of results completed."))
      }
    }

    helper(attemptNumber = 1, timeRemaining = getTimeRemaining)

  }
}

class _Coordinator extends Coordinator {
  override val mainConfig = ConfigLoader.defaultConfig
  override val uuid = UUID.randomUUID().toString

  override val postcodeResultsCache = new RedisResultCache[Postcode] {
    override val resultType = PostcodeResultType
    override val config = mainConfig.redisConfig
  }
  override val dinnerResultsCache = new RedisResultCache[List[DinnerUserName]] {
    override val resultType = DinnerResultType
    override val config = mainConfig.redisConfig
  }
  override val stackpotResultsCache = new RedisResultCache[List[Postcode]] {
    override val resultType = StackpotResultType
    override val config = mainConfig.redisConfig
  }
  override val surveyDrawResultsCache = new RedisResultCache[Postcode] {
    override val resultType = SurveyDrawResultType
    override val config = mainConfig.redisConfig
  }
  override val emojiResultsCache = new RedisResultCache[Set[Emoji]] {
    override val resultType = EmojiResultType
    override val config = mainConfig.redisConfig
  }

  override val resultsProcessor = new ResultsProcessor {
    override val redisConfig: RedisConfig = mainConfig.redisConfig
  }
  override val subscribersFetcher = new SubscribersFetcher {
    override val subscribersFileName: String = "subscribers.json"
  }
  override val resultsEmailer = new ResultsEmailer {
    override val emailClient: EmailClient = new DefaultEmailClient(mainConfig.emailerConfig)
  }

  //Overridden to run locally
//  override def triggerCheckers() = {
//
//    val postcodeChecker = new PostcodeChecker {
//      override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
//      override val redisResultCache: RedisResultCache[Postcode] = postcodeResultsCache
//      override val config: CheckerConfig = mainConfig.postcodeCheckerConfig
//    }
//
//    val dinnerChecker = new DinnerChecker {
//      override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
//      override val redisResultCache: RedisResultCache[List[DinnerUserName]] = dinnerResultsCache
//      override val config: CheckerConfig = mainConfig.dinnerCheckerConfig
//    }
//
//    val stackpotChecker = new StackpotChecker {
//      override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
//      override val redisResultCache: RedisResultCache[List[Postcode]] = stackpotResultsCache
//      override val config: CheckerConfig = mainConfig.stackpotCheckerConfig
//    }
//
//    val surveyDrawChecker = new SurveyDrawChecker {
//      override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
//      override val redisResultCache: RedisResultCache[Postcode] = surveyDrawResultsCache
//      override val config: CheckerConfig = mainConfig.surveyDrawCheckerConfig
//    }
//
//    val emojiChecker = new EmojiChecker {
//      override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
//      override val redisResultCache: RedisResultCache[Set[Emoji]] = emojiResultsCache
//      override val config: CheckerConfig = mainConfig.emojiCheckerConfig
//    }
//
//    Future((for {
//      result <- postcodeChecker.getResult
//      _ <- postcodeChecker.cacheResult(uuid, result)
//    } yield ()).unsafeRunSync()).onComplete {
//      case Success(_) => logger.info("Postcode checker completed successfully")
//      case Failure(e) => logger.error("Postcode checker failed", e)
//    }
//
//    Future((for {
//      result <- dinnerChecker.getResult
//      _ <- dinnerChecker.cacheResult(uuid, result)
//    } yield ()).unsafeRunSync()).onComplete {
//      case Success(_) => logger.info("Dinner checker completed successfully")
//      case Failure(e) => logger.error("Dinner checker failed", e)
//    }
//
//    Future((for {
//      result <- stackpotChecker.getResult
//      _ <- stackpotChecker.cacheResult(uuid, result)
//    } yield ()).unsafeRunSync()).onComplete {
//      case Success(_) => logger.info("Stackpot checker completed successfully")
//      case Failure(e) => logger.error("Stackpot checker failed", e)
//    }
//
//    Future((for {
//      result <- surveyDrawChecker.getResult
//      _ <- surveyDrawChecker.cacheResult(uuid, result)
//    } yield ()).unsafeRunSync()).onComplete {
//      case Success(_) => logger.info("Survey Draw checker completed successfully")
//      case Failure(e) => logger.error("Survey Draw checker failed", e)
//    }
//
//    Future((for {
//      result <- emojiChecker.getResult
//      _ <- emojiChecker.cacheResult(uuid, result)
//    } yield ()).unsafeRunSync()).onComplete {
//      case Success(_) => logger.info("Emoji checker completed successfully")
//      case Failure(e) => logger.error("Emoji checker failed", e)
//    }
//
//    IO.unit
//  }

  def startMaster = {

    (for {
      _ <- triggerCheckers()
      _ = logger.info("Trigger checkers stated")
      _ <- aggregateAndProcessResults()
      _ = logger.info("Results processing complete")
    } yield ()).unsafeRunSync()
  }

  startMaster

}