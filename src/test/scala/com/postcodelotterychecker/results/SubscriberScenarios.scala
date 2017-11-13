package com.postcodelotterychecker.results

import java.util.UUID

import cats.effect.IO
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.Competitions._
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode, Subscriber}
import com.postcodelotterychecker.servlet.ServletTypes.EveryDay

import scala.util.Random

trait SubscriberScenarios {

  case class Scenario(description: String,
                      subscriber: Subscriber,
                      won: Map[Competition, Option[Boolean]] = Map.empty,
                      resultsNotReceivedFor: List[Competition] = List.empty)

  val defaultWinningPostcode = Postcode("TR18HJ")
  val defaultWinningDinnerUsers = List(DinnerUserName("user1"), DinnerUserName("user2"))
  val defaultWinningStackpotPostcodes = List(Postcode("DEF456"), Postcode("EFG567"), Postcode("FGH678"))
  val defaultWinningSurveyDrawPostcode = Postcode("ABC123")
  val defaultWinningEmojiSet = Set(Emoji("aaaaa"), Emoji("bbbbb"), Emoji("ccccc"), Emoji("ddddd"), Emoji("eeeee"))

  val defaultNonWinningSubscriber = Subscriber(
    uuid = UUID.randomUUID().toString,
    email = "default-subscriber@email.com",
    notifyWhen = EveryDay,
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
      won = Map(PostcodeCompetition -> Some(true))
    )

    val subscriberWinningPostcodeMultipleWatching = Scenario(
      "Postcode Scenario - Winning Subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = defaultNonWinningSubscriber.postcodesWatching.map(pc => pc :+ defaultWinningPostcode)),
      won = Map(PostcodeCompetition -> Some(true))
    )

    val subscriberNotSubscribedToPostcodes = Scenario(
      "Postcode Scenario - Subscriber not subscribed to postcodes",
      defaultNonWinningSubscriber.copy(postcodesWatching = None)
    )

    val noResultsReceivedForPostcodes = Scenario(
      "Postcode Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List(PostcodeCompetition),
      won = Map(PostcodeCompetition -> None)
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
      won = Map(DinnerCompetition -> Some(true)))

    val subscriberWinningDinnerMultipleWatching = Scenario(
      "Dinner Scenario - Winning Subscriber, multiple dinner users watching",
      defaultNonWinningSubscriber
        .copy(dinnerUsersWatching = defaultNonWinningSubscriber.dinnerUsersWatching.map(din => din :+ Random.shuffle(defaultWinningDinnerUsers).head)),
      won = Map(DinnerCompetition -> Some(true)))

    val subscriberMultipleWinningDinnerMultipleWatching = Scenario(
      "Dinner Scenario - Multiple winning subscriber, multiple dinner users watching",
      defaultNonWinningSubscriber
        .copy(dinnerUsersWatching = Some(defaultWinningDinnerUsers)),
      won = Map(DinnerCompetition -> Some(true)))

    val subscriberNotSubscribedToDinners = Scenario(
      "Dinner Scenario - Subscriber not subscribed to dinners",
      defaultNonWinningSubscriber.copy(dinnerUsersWatching = None)
    )

    val noResultsReceivedForDinner = Scenario(
      "Dinner Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List(DinnerCompetition),
      won = Map(DinnerCompetition -> None)
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
        .copy(postcodesWatching = Some(List(defaultWinningSurveyDrawPostcode))),
      won = Map(SurveyDrawCompetition -> Some(true)))

    val subscriberWinningSurveyDrawMultipleWatching = Scenario(
      "Survey Draw Scenario - Winning Subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = defaultNonWinningSubscriber.postcodesWatching.map(pc => pc :+ defaultWinningSurveyDrawPostcode)),
      won = Map(SurveyDrawCompetition -> Some(true)))

    val subscriberNotSubscribedToPostcodes = Scenario(
      "Survey Draw Scenario - Subscriber not subscribed to postcodes",
      defaultNonWinningSubscriber.copy(postcodesWatching = None)
    )

    val noResultsReceivedForSurveyDraw = Scenario(
      "Survey Draw Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List(SurveyDrawCompetition),
      won = Map(SurveyDrawCompetition -> None)
    )

    List(nonWinningSubscriber, subscriberWinningSurveyDrawSingleWatching, subscriberWinningSurveyDrawMultipleWatching, subscriberNotSubscribedToPostcodes, noResultsReceivedForSurveyDraw)
  }

  val stackpotSubscriberScenarios: List[Scenario] = {
    val nonWinningSubscriber = Scenario(
      "Stackpot Scenario - Non Winning Subscriber",
      defaultNonWinningSubscriber)

    val subscriberWinningStackpotSingleWatching = Scenario(
      "Stackpot Scenario - Winning Subscriber, single postcode watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = Some(List(Random.shuffle(defaultWinningStackpotPostcodes).head))),
      won = Map(StackpotCompetition -> Some(true)))

    val subscriberWinningStackpotMultipleWatching = Scenario(
      "Stackpot Scenario - Winning Subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = defaultNonWinningSubscriber.postcodesWatching.map(pc => pc :+ Random.shuffle(defaultWinningStackpotPostcodes).head)),
      won = Map(StackpotCompetition -> Some(true)))

    val subscriberMultipleWinningStackpotMultipleWatching = Scenario(
      "Stackpot Scenario - Multiple winning subscriber, multiple postcodes watching",
      defaultNonWinningSubscriber
        .copy(postcodesWatching = Some(defaultWinningStackpotPostcodes)),
      won = Map(StackpotCompetition -> Some(true)))

    val subscriberNotSubscribedToPostcodes = Scenario(
      "Stackpot Scenario - Subscriber not subscribed to postcodes",
      defaultNonWinningSubscriber.copy(postcodesWatching = None)
    )

    val noResultsReceivedForStackpot = Scenario(
      "Stackpot Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List(StackpotCompetition),
      won = Map(StackpotCompetition -> None)
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
      won = Map(EmojiCompetition -> Some(true)))

    val subscriberWinningEmojiSetMultipleWatching = Scenario(
      "Emoji Scenario - Winning Subscriber, multiple emoji sets watching",
      defaultNonWinningSubscriber
        .copy(emojiSetsWatching = defaultNonWinningSubscriber.emojiSetsWatching.map(em => em :+ defaultWinningEmojiSet)),
      won = Map(EmojiCompetition -> Some(true)))

    val subscriberNotSubscribedToEmojis = Scenario(
      "Emoji Scenario - Subscriber not subscribed to postcodes",
      defaultNonWinningSubscriber.copy(emojiSetsWatching = None)
    )

    val noResultsReceivedForEmoji = Scenario(
      "Emoji Scenario - No results received",
      defaultNonWinningSubscriber,
      resultsNotReceivedFor = List(EmojiCompetition),
      won = Map(EmojiCompetition -> None)
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
        PostcodeCompetition -> Some(true),
        DinnerCompetition -> Some(true),
        EmojiCompetition -> Some(true))
    )

    val winPostcodeStackpotSurveyDrawSubscriber = Scenario(
      "Multiple Winning Scenario - Win Postcode, Stackpot, Survey Draw Subscriber",
      defaultNonWinningSubscriber.copy(
        postcodesWatching = Some(List(
          defaultWinningPostcode,
          Random.shuffle(defaultWinningStackpotPostcodes).head,
          defaultWinningSurveyDrawPostcode))),
      won = Map(
        PostcodeCompetition -> Some(true),
        StackpotCompetition -> Some(true),
        SurveyDrawCompetition -> Some(true))
    )
    List(winPostcodeDinnerEmojiSubscriber, winPostcodeStackpotSurveyDrawSubscriber)
  }

  def writeWinningDataToCache(uuid: String,
                              postcodeResultsCache: RedisResultCache[Postcode],
                              dinnerResultsCache: RedisResultCache[List[DinnerUserName]],
                              stackpotResultsCache: RedisResultCache[List[Postcode]],
                              surveyDrawResultsCache: RedisResultCache[Postcode],
                              emojiResultsCache: RedisResultCache[Set[Emoji]],
                              winningPostcodeOpt: Option[Postcode] = Some(defaultWinningPostcode),
                              winningDinnerUsersOpt: Option[List[DinnerUserName]] = Some(defaultWinningDinnerUsers),
                              winningStackpotPostcodesOpt: Option[List[Postcode]] = Some(defaultWinningStackpotPostcodes),
                              winningSurveyDrawPostcodesOpt: Option[Postcode] = Some(defaultWinningSurveyDrawPostcode),
                              winningEmojiSetOpt: Option[Set[Emoji]] = Some(defaultWinningEmojiSet),
                              delay: Long = 0) = {
    (for {
      _ <- winningPostcodeOpt.fold(IO.unit)(p => postcodeResultsCache.cache(uuid, p).map(_ => ()))
      _ <- winningDinnerUsersOpt.fold(IO.unit)(p => dinnerResultsCache.cache(uuid, p).map(_ => ()))
      _ = Thread.sleep(delay) //simulate waiting/hanging
      _ <- winningStackpotPostcodesOpt.fold(IO.unit)(p => stackpotResultsCache.cache(uuid, p).map(_ => ()))
      _ <- winningSurveyDrawPostcodesOpt.fold(IO.unit)(p => surveyDrawResultsCache.cache(uuid, p).map(_ => ()))
      _ <- winningEmojiSetOpt.fold(IO.unit)(p => emojiResultsCache.cache(uuid, p).map(_ => ()))
    } yield ()).unsafeRunAsync(_ => ())
  }
}
