package com.postcodelotterychecker.results

import java.util.UUID

import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.Competitions._
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models.Results.{SubscriberResult, WinningResults}
import com.postcodelotterychecker.models._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}


class ResultsProcessingTest extends FlatSpec with SubscriberScenarios with Matchers with BeforeAndAfterAll {

  val redisTestConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
  val testcaches = new TestCaches(redisTestConfig)

  override protected def beforeAll(): Unit = {
    new RedisResultCache[Postcode] {
      override val resultType = ResultTypes.PostcodeResultType
      override val config = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    }.flushDB()
  }

  "Results Processor" should "aggregate winning results" in new ResultsProcessor {

    override val redisConfig = redisTestConfig
    val uuid = UUID.randomUUID().toString

    val testcaches = new TestCaches(redisConfig)
    writeWinningDataToCache(uuid, testcaches.postcodeResultCache, testcaches. dinnerResultCache, testcaches.stackpotResultCache, testcaches.surveyDrawResultCache, testcaches.emojiResultCache)

    aggregateResults(uuid).unsafeRunSync() shouldBe WinningResults(
      Some(defaultWinningPostcode),
      Some(defaultWinningDinnerUsers),
      Some(defaultWinningSurveyDrawPostcode),
      Some(defaultWinningStackpotPostcodes),
      Some(defaultWinningEmojiSet))
  }

  (postcodeSubscriberScenarios ++
    dinnerSubscriberScenarios ++
    surveyDrawSubscriberScenarios ++
    stackpotSubscriberScenarios ++
    emojiSubscriberScenarios ++
    multipleWinningScenarios).foreach { scenario =>

    it should s"generate subscriber results for ${scenario.description}" in new ResultsProcessor {
      override val redisConfig: RedisConfig = redisTestConfig

      val uuid = UUID.randomUUID().toString
      writeWinningDataToCache(
        uuid,
        testcaches.postcodeResultCache,
        testcaches. dinnerResultCache,
        testcaches.stackpotResultCache,
        testcaches.surveyDrawResultCache,
        testcaches.emojiResultCache,
        winningPostcodeOpt = if (scenario.resultsNotReceivedFor.contains(PostcodeCompetition)) None else Some(defaultWinningPostcode),
        winningDinnerUsersOpt = if (scenario.resultsNotReceivedFor.contains(DinnerCompetition)) None else Some(defaultWinningDinnerUsers),
        winningSurveyDrawPostcodesOpt = if (scenario.resultsNotReceivedFor.contains(SurveyDrawCompetition)) None else Some(defaultWinningSurveyDrawPostcode),
        winningStackpotPostcodesOpt = if (scenario.resultsNotReceivedFor.contains(StackpotCompetition)) None else Some(defaultWinningStackpotPostcodes),
        winningEmojiSetOpt = if (scenario.resultsNotReceivedFor.contains(EmojiCompetition)) None else Some(defaultWinningEmojiSet))
      val aggregatedResults = aggregateResults(uuid).unsafeRunSync()

      val subscriberResults = mapSubscribersToResults(List(scenario.subscriber), aggregatedResults)
      subscriberResults should have size 1
      subscriberResults(scenario.subscriber).postcodeResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult(PostcodeResultType, postcodesWatching, aggregatedResults.postcodeResult, scenario.won.getOrElse(PostcodeCompetition, Some(false))))
      subscriberResults(scenario.subscriber).dinnerResult shouldBe
        scenario.subscriber.dinnerUsersWatching.map(dinnerUsersWatching => SubscriberResult(DinnerResultType, dinnerUsersWatching, aggregatedResults.dinnerResult, scenario.won.getOrElse(DinnerCompetition, Some(false))))
      subscriberResults(scenario.subscriber).surveyDrawResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult(SurveyDrawResultType, postcodesWatching, aggregatedResults.surveyDrawResult, scenario.won.getOrElse(SurveyDrawCompetition, Some(false))))
      subscriberResults(scenario.subscriber).stackpotResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult(StackpotResultType, postcodesWatching, aggregatedResults.stackpotResult, scenario.won.getOrElse(StackpotCompetition, Some(false))))
      subscriberResults(scenario.subscriber).emojiResult shouldBe
        scenario.subscriber.emojiSetsWatching.map(emojiSetsWatching => SubscriberResult(EmojiResultType, emojiSetsWatching, aggregatedResults.emojiResult, scenario.won.getOrElse(EmojiCompetition, Some(false))))
    }
  }

}

class TestCaches(redisConfig: RedisConfig) {
  val postcodeResultCache = new RedisResultCache[Postcode] {
    override val resultType = ResultTypes.PostcodeResultType
    override val config = redisConfig
  }
  val dinnerResultCache = new RedisResultCache[List[DinnerUserName]] {
    override val resultType = ResultTypes.DinnerResultType
    override val config = redisConfig
  }
  val stackpotResultCache = new RedisResultCache[List[Postcode]] {
    override val resultType = ResultTypes.StackpotResultType
    override val config = redisConfig
  }
  val surveyDrawResultCache = new RedisResultCache[Postcode] {
    override val resultType = ResultTypes.SurveyDrawResultType
    override val config = redisConfig
  }
  val emojiResultCache = new RedisResultCache[Set[Emoji]] {
    override val resultType = ResultTypes.EmojiResultType
    override val config = redisConfig
  }
}

