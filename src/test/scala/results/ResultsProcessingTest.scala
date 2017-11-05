package results

import java.util.UUID

import cats.effect.IO
import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.Competitions._
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models.Results.{SubscriberResult, WinningResults}
import com.postcodelotterychecker.models._
import com.postcodelotterychecker.results.ResultsProcessor
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.util.Random


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
    writeWinningDataToCache(uuid, testcaches)

    aggregateResults(uuid).unsafeRunSync() shouldBe WinningResults(
      Some(defaultWinningPostcode),
      Some(defaultWinningDinnerUsers),
      Some(defaultWinningSurveyDrawPostcodes),
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
        testcaches,
        winningPostcodeOpt = if (scenario.resultsNotReceivedFor.contains(PostcodeCompetition)) None else Some(defaultWinningPostcode),
        winningDinnerUsersOpt = if (scenario.resultsNotReceivedFor.contains(DinnerCompetition)) None else Some(defaultWinningDinnerUsers),
        winningSurveyDrawPostcodesOpt = if (scenario.resultsNotReceivedFor.contains(SurveyDrawCompetition)) None else Some(defaultWinningSurveyDrawPostcodes),
        winningStackpotPostcodesOpt = if (scenario.resultsNotReceivedFor.contains(StackpotCompetition)) None else Some(defaultWinningStackpotPostcodes),
        winningEmojiSetOpt = if (scenario.resultsNotReceivedFor.contains(EmojiCompetition)) None else Some(defaultWinningEmojiSet))
      val agregatedResults = aggregateResults(uuid).unsafeRunSync()

      val subscriberResults = mapSubscribersToResults(List(scenario.subscriber), agregatedResults)
      subscriberResults should have size 1
      subscriberResults(scenario.subscriber).postcodeResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult(PostcodeResultType, postcodesWatching, agregatedResults.postcodeResult, scenario.won.getOrElse(PostcodeCompetition, Some(false))))
      subscriberResults(scenario.subscriber).dinnerResult shouldBe
        scenario.subscriber.dinnerUsersWatching.map(dinnerUsersWatching => SubscriberResult(DinnerResultType, dinnerUsersWatching, agregatedResults.dinnerResult, scenario.won.getOrElse(DinnerCompetition, Some(false))))
      subscriberResults(scenario.subscriber).surveyDrawResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult(SurveyDrawResultType, postcodesWatching, agregatedResults.surveyDrawResult, scenario.won.getOrElse(SurveyDrawCompetition, Some(false))))
      subscriberResults(scenario.subscriber).stackpotResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult(StackpotResultType, postcodesWatching, agregatedResults.stackpotResult, scenario.won.getOrElse(StackpotCompetition, Some(false))))
      subscriberResults(scenario.subscriber).emojiResult shouldBe
        scenario.subscriber.emojiSetsWatching.map(emojiSetsWatching => SubscriberResult(EmojiResultType, emojiSetsWatching, agregatedResults.emojiResult, scenario.won.getOrElse(EmojiCompetition, Some(false))))
    }
  }


  def writeWinningDataToCache(uuid: String,
                              testCaches: TestCaches,
                              winningPostcodeOpt: Option[Postcode] = Some(defaultWinningPostcode),
                              winningDinnerUsersOpt: Option[List[DinnerUserName]] = Some(defaultWinningDinnerUsers),
                              winningStackpotPostcodesOpt: Option[List[Postcode]] = Some(defaultWinningStackpotPostcodes),
                              winningSurveyDrawPostcodesOpt: Option[List[Postcode]] = Some(defaultWinningSurveyDrawPostcodes),
                              winningEmojiSetOpt: Option[Set[Emoji]] = Some(defaultWinningEmojiSet)) = {
    (for {
      _ <- winningPostcodeOpt.fold(IO.unit)(p => testCaches.postcodeResultCache.cache(uuid, p).map(_ => ()))
      _ <- winningDinnerUsersOpt.fold(IO.unit)(p => testCaches.dinnerResultCache.cache(uuid, p).map(_ => ()))
      _ <- winningStackpotPostcodesOpt.fold(IO.unit)(p => testCaches.stackpotResultCache.cache(uuid, p).map(_ => ()))
      _ <- winningSurveyDrawPostcodesOpt.fold(IO.unit)(p => testCaches.surveyDrawResultCache.cache(uuid, p).map(_ => ()))
      _ <- winningEmojiSetOpt.fold(IO.unit)(p => testCaches.emojiResultCache.cache(uuid, p).map(_ => ()))
    } yield ()).unsafeRunSync()
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
  val surveyDrawResultCache = new RedisResultCache[List[Postcode]] {
    override val resultType = ResultTypes.SurveyDrawResultType
    override val config = redisConfig
  }
  val emojiResultCache = new RedisResultCache[Set[Emoji]] {
    override val resultType = ResultTypes.EmojiResultType
    override val config = redisConfig
  }
}

