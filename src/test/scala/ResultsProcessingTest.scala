import java.util.UUID

import cats.effect.IO
import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.CheckerType.CheckerType
import com.postcodelotterychecker.models.Results.{SubscriberResult, WinningResults}
import com.postcodelotterychecker.models._
import com.postcodelotterychecker.results.ResultsProcessor
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.util.Random


class ResultsProcessingTest extends FlatSpec with ResultsProcessingFixtures with Matchers with BeforeAndAfterAll {

  val redisTestConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
  val testcaches = new TestCaches(redisTestConfig)

  override protected def beforeAll(): Unit = {
    new RedisResultCache[Postcode] {
      override val checkerType = CheckerType.PostcodeType
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
        winningPostcodeOpt = if (scenario.resultsNotReceivedFor.contains("POSTCODE")) None else Some(defaultWinningPostcode),
        winningDinnerUsersOpt = if (scenario.resultsNotReceivedFor.contains("DINNER")) None else Some(defaultWinningDinnerUsers),
        winningSurveyDrawPostcodesOpt = if (scenario.resultsNotReceivedFor.contains("SURVEYDRAW")) None else Some(defaultWinningSurveyDrawPostcodes),
        winningStackpotPostcodesOpt = if (scenario.resultsNotReceivedFor.contains("STACKPOT")) None else Some(defaultWinningStackpotPostcodes),
        winningEmojiSetOpt = if (scenario.resultsNotReceivedFor.contains("EMOJI")) None else Some(defaultWinningEmojiSet))
      val agregatedResults = aggregateResults(uuid).unsafeRunSync()

      val subscriberResults = mapSubscribersToResults(List(scenario.subscriber), agregatedResults)
      subscriberResults should have size 1
      subscriberResults(scenario.subscriber).postcodeResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult[List[Postcode]](scenario.won.getOrElse("POSTCODE", Some(false)), postcodesWatching, agregatedResults.postcodeResult.map(List(_))))
      subscriberResults(scenario.subscriber).dinnerResult shouldBe
        scenario.subscriber.dinnerUsersWatching.map(dinnerUsersWatching => SubscriberResult[List[DinnerUserName]](scenario.won.getOrElse("DINNER", Some(false)), dinnerUsersWatching, agregatedResults.dinnerResult))
      subscriberResults(scenario.subscriber).surveyDrawResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult[List[Postcode]](scenario.won.getOrElse("SURVEYDRAW", Some(false)), postcodesWatching, agregatedResults.surveyDrawResult))
      subscriberResults(scenario.subscriber).stackpotResult shouldBe
        scenario.subscriber.postcodesWatching.map(postcodesWatching => SubscriberResult[List[Postcode]](scenario.won.getOrElse("STACKPOT", Some(false)), postcodesWatching, agregatedResults.stackpotResult))
      subscriberResults(scenario.subscriber).emojiResult shouldBe
        scenario.subscriber.emojiSetsWatching.map(emojiSetsWatching => SubscriberResult[List[Set[Emoji]]](scenario.won.getOrElse("EMOJI", Some(false)), emojiSetsWatching, agregatedResults.emojiResult.map(List(_))))
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
    override val checkerType = CheckerType.PostcodeType
    override val config = redisConfig
  }
  val dinnerResultCache = new RedisResultCache[List[DinnerUserName]] {
    override val checkerType = CheckerType.DinnerType
    override val config = redisConfig
  }
  val stackpotResultCache = new RedisResultCache[List[Postcode]] {
    override val checkerType = CheckerType.StackpotType
    override val config = redisConfig
  }
  val surveyDrawResultCache = new RedisResultCache[List[Postcode]] {
    override val checkerType = CheckerType.SurveyDrawType
    override val config = redisConfig
  }
  val emojiResultCache = new RedisResultCache[Set[Emoji]] {
    override val checkerType = CheckerType.EmojiType
    override val config = redisConfig
  }
}

trait ResultsProcessingFixtures {

  case class Scenario(description: String,
                      subscriber: Subscriber,
                      won: Map[String, Option[Boolean]] = Map.empty,
                      resultsNotReceivedFor: List[String] = List.empty)

  val defaultWinningPostcode = Postcode("TR18HJ")
  val defaultWinningDinnerUsers = List(DinnerUserName("user1"), DinnerUserName("user2"))
  val defaultWinningStackpotPostcodes = List(Postcode("DEF456"), Postcode("EFG567"), Postcode("FGH678"))
  val defaultWinningSurveyDrawPostcodes = List(Postcode("ABC123"), Postcode("BCD234"), Postcode("CDE345"))
  val defaultWinningEmojiSet = Set(Emoji("aaaaa"), Emoji("bbbbb"), Emoji("ccccc"), Emoji("ddddd"), Emoji("eeeee"))

  val defaultNonWinningSubscriber = Subscriber(
    email = "default-subscriber@email.com",
    postcodesWatching = Some(List(Postcode("XYZ123"))),
    dinnerUsersWatching = Some(List(DinnerUserName("user3"))),
    emojiSetsWatching = Some(List(Set(Emoji("aaaaa"), Emoji("wwwww"), Emoji("xxxxx"), Emoji("yyyyy"), Emoji("zzzzz"))))
  )

  val postcodeSubscriberScenarios: List[Scenario] = {
    val nonWinningSubscriber = Scenario(
      "Postcode Scenario - Non Winning Subscriber",
      defaultNonWinningSubscriber)

    val subscriberWinningPostcodeSingleWatching = Scenario(
      "Postcode Scenario - Winning Subscriber, single postcode watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = Some(List(defaultWinningPostcode))),
      won = Map("POSTCODE" -> Some(true))
    )

    val subscriberWinningPostcodeMultipleWatching = Scenario(
      "Postcode Scenario - Winning Subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = defaultNonWinningSubscriber.postcodesWatching.map(pc => pc :+ defaultWinningPostcode)),
      won = Map("POSTCODE" -> Some(true))
    )

    val subscriberNotSubscribedToPostcodes = Scenario(
      "Postcode Scenario - Subscriber not subscribed to postcodes",
      defaultNonWinningSubscriber.copy(postcodesWatching = None)
    )

    val noResultsReceivedForPostcodes = Scenario(
      "Postcode Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List("POSTCODE"),
      won = Map("POSTCODE" -> None)
    )

    List(nonWinningSubscriber, subscriberWinningPostcodeSingleWatching, subscriberWinningPostcodeMultipleWatching, subscriberNotSubscribedToPostcodes, noResultsReceivedForPostcodes)
  }

  val dinnerSubscriberScenarios: List[Scenario] = {
    val nonWinningSubscriber = Scenario(
      "Dinner Scenario - Non Winning Subscriber",
      defaultNonWinningSubscriber)

    val subscriberWinningDinnerSingleWatching = Scenario(
      "Dinner Scenario - Winning Subscriber, single dinner user watching",
      defaultNonWinningSubscriber
        .copy(dinnerUsersWatching = Some(List(Random.shuffle(defaultWinningDinnerUsers).head))),
      won = Map("DINNER" -> Some(true)))

    val subscriberWinningDinnerMultipleWatching = Scenario(
      "Dinner Scenario - Winning Subscriber, multiple dinner users watching",
      defaultNonWinningSubscriber
        .copy(dinnerUsersWatching = defaultNonWinningSubscriber.dinnerUsersWatching.map(din => din :+ Random.shuffle(defaultWinningDinnerUsers).head)),
      won = Map("DINNER" -> Some(true)))

    val subscriberMultipleWinningDinnerMultipleWatching = Scenario(
      "Dinner Scenario - Multiple winning subscriber, multiple dinner users watching",
      defaultNonWinningSubscriber
        .copy(dinnerUsersWatching = Some(defaultWinningDinnerUsers)),
      won = Map("DINNER" -> Some(true)))

    val subscriberNotSubscribedToDinners = Scenario(
      "Dinner Scenario - Subscriber not subscribed to dinners",
      defaultNonWinningSubscriber.copy(dinnerUsersWatching = None)
    )

    val noResultsReceivedForDinner = Scenario(
      "Dinner Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List("DINNER"),
      won = Map("DINNER" -> None)
    )

    List(nonWinningSubscriber, subscriberWinningDinnerSingleWatching, subscriberWinningDinnerMultipleWatching, subscriberMultipleWinningDinnerMultipleWatching, subscriberNotSubscribedToDinners, noResultsReceivedForDinner)
  }

  val surveyDrawSubscriberScenarios: List[Scenario] = {
    val nonWinningSubscriber = Scenario(
      "Survey Draw Scenario - Non Winning Subscriber",
      defaultNonWinningSubscriber)

    val subscriberWinningSurveyDrawSingleWatching = Scenario(
      "Survey Draw Scenario - Winning Subscriber, single postcode watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = Some(List(Random.shuffle(defaultWinningSurveyDrawPostcodes).head))),
      won = Map("SURVEYDRAW" -> Some(true)))

    val subscriberWinningSurveyDrawMultipleWatching = Scenario(
      "Survey Draw Scenario - Winning Subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = defaultNonWinningSubscriber.postcodesWatching.map(pc => pc :+ Random.shuffle(defaultWinningSurveyDrawPostcodes).head)),
      won = Map("SURVEYDRAW" -> Some(true)))

    val subscriberMultipleWinningSurveyDrawMultipleWatching = Scenario(
      "Survey Draw Scenario - Multiple winning subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = Some(defaultWinningSurveyDrawPostcodes)),
      won = Map("SURVEYDRAW" -> Some(true)))

    val subscriberNotSubscribedToPostcodes = Scenario(
      "Survey Draw Scenario - Subscriber not subscribed to postcodes",
      defaultNonWinningSubscriber.copy(postcodesWatching = None)
    )

    val noResultsReceivedForSurveyDraw = Scenario(
      "Survey Draw Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List("SURVEYDRAW"),
      won = Map("SURVEYDRAW" -> None)
    )

    List(nonWinningSubscriber, subscriberWinningSurveyDrawSingleWatching, subscriberWinningSurveyDrawMultipleWatching, subscriberMultipleWinningSurveyDrawMultipleWatching, subscriberNotSubscribedToPostcodes, noResultsReceivedForSurveyDraw)
  }

  val stackpotSubscriberScenarios: List[Scenario] = {
    val nonWinningSubscriber = Scenario(
      "Stackpot Scenario - Non Winning Subscriber",
      defaultNonWinningSubscriber)

    val subscriberWinningStackpotSingleWatching = Scenario(
      "Stackpot Scenario - Winning Subscriber, single postcode watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = Some(List(Random.shuffle(defaultWinningStackpotPostcodes).head))),
      won = Map("STACKPOT" -> Some(true)))

    val subscriberWinningStackpotMultipleWatching = Scenario(
      "Stackpot Scenario - Winning Subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = defaultNonWinningSubscriber.postcodesWatching.map(pc => pc :+ Random.shuffle(defaultWinningStackpotPostcodes).head)),
      won = Map("STACKPOT" -> Some(true)))

    val subscriberMultipleWinningStackpotMultipleWatching = Scenario(
      "Stackpot Scenario - Multiple winning subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = Some(defaultWinningStackpotPostcodes)),
      won = Map("STACKPOT" -> Some(true)))

    val subscriberNotSubscribedToPostcodes = Scenario(
      "Stackpot Scenario - Subscriber not subscribed to postcodes",
      defaultNonWinningSubscriber.copy(postcodesWatching = None)
    )

    val noResultsReceivedForStackpot = Scenario(
      "Stackpot Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List("STACKPOT"),
      won = Map("STACKPOT" -> None)
    )

    List(nonWinningSubscriber, subscriberWinningStackpotSingleWatching, subscriberWinningStackpotMultipleWatching, subscriberMultipleWinningStackpotMultipleWatching, subscriberNotSubscribedToPostcodes, noResultsReceivedForStackpot)
  }

  val emojiSubscriberScenarios: List[Scenario] = {
    val nonWinningSubscriber = Scenario(
      "Emoji Scenario - Non Winning Subscriber",
      defaultNonWinningSubscriber)

    val subscriberWinningEmojiSingleSetWatching = Scenario(
      "Emoji Scenario - Winning Subscriber, single emoji set watching",
      defaultNonWinningSubscriber
        .copy(emojiSetsWatching = Some(List(defaultWinningEmojiSet))),
      won = Map("EMOJI" -> Some(true)))

    val subscriberWinningEmojiSetMultipleWatching = Scenario(
      "Emoji Scenario - Winning Subscriber, multiple emoji sets watching",
      defaultNonWinningSubscriber
        .copy(emojiSetsWatching = defaultNonWinningSubscriber.emojiSetsWatching.map(em => em :+ defaultWinningEmojiSet)),
      won = Map("EMOJI" -> Some(true)))

    val subscriberNotSubscribedToEmojis = Scenario(
      "Emoji Scenario - Subscriber not subscribed to postcodes",
      defaultNonWinningSubscriber.copy(emojiSetsWatching = None)
    )

    val noResultsReceivedForEmoji = Scenario(
      "Emoji Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List("EMOJI"),
      won = Map("EMOJI" -> None)
    )

    List(nonWinningSubscriber, subscriberWinningEmojiSingleSetWatching, subscriberWinningEmojiSetMultipleWatching, subscriberNotSubscribedToEmojis, noResultsReceivedForEmoji)
  }

  val multipleWinningScenarios: List[Scenario] = {
    val winPostcodeDinnerEmojiSubscriber = Scenario(
      "Multiple Winning Scenario - Win Postcode, Dinner Emoji Subscriber",
      defaultNonWinningSubscriber.copy(
        postcodesWatching = Some(List(defaultWinningPostcode)),
        dinnerUsersWatching = Some(List(Random.shuffle(defaultWinningDinnerUsers).head)),
        emojiSetsWatching = Some(List(defaultWinningEmojiSet))),
      won = Map(
        "POSTCODE" -> Some(true),
        "DINNER" -> Some(true),
        "EMOJI" -> Some(true))
    )

    val winPostcodeStackpotSurveyDrawSubscriber = Scenario(
      "Multiple Winning Scenario - Win Postcode, Stackpot, Survey Draw Subscriber",
      defaultNonWinningSubscriber.copy(
        postcodesWatching = Some(List(
          defaultWinningPostcode,
          Random.shuffle(defaultWinningStackpotPostcodes).head,
          Random.shuffle(defaultWinningSurveyDrawPostcodes).head))),
      won = Map(
        "POSTCODE" -> Some(true),
        "STACKPOT" -> Some(true),
        "SURVEYDRAW" -> Some(true))
    )
    List(winPostcodeDinnerEmojiSubscriber, winPostcodeStackpotSurveyDrawSubscriber)
  }
}
