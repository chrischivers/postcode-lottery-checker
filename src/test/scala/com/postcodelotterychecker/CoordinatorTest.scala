package com.postcodelotterychecker

import java.util.UUID

import cats.effect.IO
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models._
import com.postcodelotterychecker.results._
import com.postcodelotterychecker.subscribers.SubscribersFetcher
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}


class CoordinatorTest extends FlatSpec with SubscriberScenarios with Matchers with BeforeAndAfterAll {

  "Coordinator" should "process results and email them" in new TestCoordinator {

    writeWinningDataToCache(uuid, postcodeResultsCache, dinnerResultsCache, stackpotResultsCache, surveyDrawResultsCache, emojiResultsCache)

    aggregateAndProcessResults().unsafeRunSync()

    stubEmailClient.emailsSent should have size 5

    val subscribers = subscribersFetcher.getSubscribers.unsafeRunSync()
    subscribers.foreach {subscriber =>
      val email = stubEmailClient.emailsSent.find(_.to == subscriber.email).get
      subscriber.postcodesWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.dinnerUsersWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.emojiSetsWatching.map(watching => email.body should include (watching.map(_.map(_.id).mkString(", ")).mkString("\n")))
    }
  }

  it should "process results and email them, retrying to wait for results to arrive" in new TestCoordinator {

    writeWinningDataToCache(uuid, postcodeResultsCache, dinnerResultsCache, stackpotResultsCache, surveyDrawResultsCache, emojiResultsCache, delay = 10000)

    println("After sleep")

    aggregateAndProcessResults().unsafeRunSync()

    stubEmailClient.emailsSent should have size 5

    val subscribers = subscribersFetcher.getSubscribers.unsafeRunSync()
    subscribers.foreach {subscriber =>
      val email = stubEmailClient.emailsSent.find(_.to == subscriber.email).get
      subscriber.postcodesWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.dinnerUsersWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.emojiSetsWatching.map(watching => email.body should include (watching.map(_.map(_.id).mkString(", ")).mkString("\n")))
      email.body should not include "Result: Unknown"
    }
  }

  it should "fail if results take too long to arrive" in new TestCoordinator {

    writeWinningDataToCache(uuid, postcodeResultsCache, dinnerResultsCache, stackpotResultsCache, surveyDrawResultsCache, emojiResultsCache, delay = 20000)

    aggregateAndProcessResults().unsafeRunSync()

    stubEmailClient.emailsSent should have size 5

    val subscribers = subscribersFetcher.getSubscribers.unsafeRunSync()
    subscribers.foreach {subscriber =>
      val email = stubEmailClient.emailsSent.find(_.to == subscriber.email).get
      println(email.body)
      subscriber.postcodesWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.dinnerUsersWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.emojiSetsWatching.map(watching => email.body should include (watching.map(_.map(_.id).mkString(", ")).mkString("\n")))

      subscriber.postcodesWatching.map(_ => email.body should include ("**Stackpot Lottery**\nResult: Unknown. Please check...."))
      subscriber.postcodesWatching.map(_ => email.body should include ("**Survey Draw Lottery**\nResult: Unknown. Please check...."))
      subscriber.emojiSetsWatching.map(_ => email.body should include ("**Emoji Lottery**\nResult: Unknown. Please check...."))
    }
  }
}

class TestCoordinator extends Coordinator {
  import scala.concurrent.duration._
  override val mainConfig: Config = ConfigLoader.defaultConfig.copy(
    redisConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1),
    resultsProcessorConfig = ConfigLoader.defaultConfig.resultsProcessorConfig.copy(timeBetweenRetries = 5 seconds, timeCutoff = 15 seconds)
  )

  override val uuid: String = UUID.randomUUID().toString
  override val subscribersFetcher: SubscribersFetcher = new SubscribersFetcher {
    override val subscribersFileName: String = "subscribers-full-test.json"
  }
  override val stackpotResultsCache: RedisResultCache[List[Postcode]] = new RedisResultCache[List[Postcode]] {
    override val resultType: ResultType[List[Postcode], _] = StackpotResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }
  override val surveyDrawResultsCache: RedisResultCache[Postcode] = new RedisResultCache[Postcode] {
    override val resultType: ResultType[Postcode, _] = SurveyDrawResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }
  override val dinnerResultsCache: RedisResultCache[List[DinnerUserName]] = new RedisResultCache[List[DinnerUserName]] {
    override val resultType: ResultType[List[DinnerUserName], _] = DinnerResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }
  override val postcodeResultsCache: RedisResultCache[Postcode] = new RedisResultCache[Postcode] {
    override val resultType: ResultType[Postcode, _] = PostcodeResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }
  override val emojiResultsCache: RedisResultCache[Set[Emoji]] = new RedisResultCache[Set[Emoji]] {
    override val resultType: ResultType[Set[Emoji], _] = EmojiResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }

  val stubEmailClient = new StubEmailClient

  override val resultsEmailer: ResultsEmailer = new ResultsEmailer {
    override val emailClient: EmailClient = stubEmailClient
  }
  override val resultsProcessor: ResultsProcessor = new ResultsProcessor {
    override val redisConfig: RedisConfig = mainConfig.redisConfig
  }
}




