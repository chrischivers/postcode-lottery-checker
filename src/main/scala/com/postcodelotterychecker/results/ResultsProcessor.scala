package com.postcodelotterychecker.results

import cats.effect.IO
import com.postcodelotterychecker.RedisConfig
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models.Results.{SubscriberResult, SubscriberResults, WinningResults}
import com.postcodelotterychecker.models._

trait ResultsProcessor {

  val redisConfig: RedisConfig

  lazy val postcodeResultsCache: RedisResultCache[Postcode] = new RedisResultCache[Postcode] {
    override val config: RedisConfig = redisConfig
    override val resultType = PostcodeResultType
  }

  lazy val dinnerResultsCache: RedisResultCache[List[DinnerUserName]] = new RedisResultCache[List[DinnerUserName]] {

    override val config: RedisConfig = redisConfig
    override val resultType = DinnerResultType
  }

  lazy val surveyDrawResultsCache: RedisResultCache[List[Postcode]] = new RedisResultCache[List[Postcode]] {
    override val config: RedisConfig = redisConfig
    override val resultType = SurveyDrawResultType
  }

  lazy val stackpotResultsCache: RedisResultCache[List[Postcode]] = new RedisResultCache[List[Postcode]] {
    override val config: RedisConfig = redisConfig
    override val resultType = StackpotResultType
  }

  lazy val emojisResultsCache: RedisResultCache[Set[Emoji]] = new RedisResultCache[Set[Emoji]] {
    override val config: RedisConfig = redisConfig
    override val resultType = EmojiResultType
  }

  def aggregateResults(uuid: String): IO[WinningResults] = {

    for {
      winningPostcode <- postcodeResultsCache.get(uuid)
      winningDinnerResult <- dinnerResultsCache.get(uuid)
      winningSurveyDrawResult <- surveyDrawResultsCache.get(uuid)
      winningStackpotResult <- stackpotResultsCache.get(uuid)
      winningEmojiResult <- emojisResultsCache.get(uuid)
    } yield {
      WinningResults(
        winningPostcode,
        winningDinnerResult,
        winningSurveyDrawResult,
        winningStackpotResult,
        winningEmojiResult)
    }
  }

  def mapSubscribersToResults(subscribers: List[Subscriber], winningResults: WinningResults): Map[Subscriber, SubscriberResults] = {

      subscribers.map { subscriber =>
        subscriber ->
          SubscriberResults(
            postcodeResult = handlePostcodeResult(subscriber.postcodesWatching, winningResults.postcodeResult),
            dinnerResult = handleDinnerResult(subscriber.dinnerUsersWatching, winningResults.dinnerResult),
            surveyDrawResult = handleSurveyDrawResult(subscriber.postcodesWatching, winningResults.surveyDrawResult),
            stackpotResult = handleStackpotResult(subscriber.postcodesWatching, winningResults.stackpotResult),
            emojiResult = handleEmojiResult(subscriber.emojiSetsWatching, winningResults.emojiResult)
          )
      }.toMap
  }

  private def handlePostcodeResult(subscriberPostcodeOpt: Option[List[Postcode]], winningResultOpt: Option[Postcode]): Option[SubscriberResult[Postcode, List[Postcode]]] = {
    subscriberPostcodeOpt.map(subPostcodes => {
      winningResultOpt match {
          case Some(winningPostcode) if subPostcodes.contains(winningPostcode) => SubscriberResult(PostcodeResultType, subPostcodes, Some(winningPostcode), Some(true))
          case Some(winningPostcode) if !subPostcodes.contains(winningPostcode) => SubscriberResult(PostcodeResultType, subPostcodes, Some(winningPostcode), Some(false))
          case None => SubscriberResult(PostcodeResultType, subPostcodes, None, None)
        }
    })
  }

  private def handleStackpotResult(subscriberPostcodesOpt: Option[List[Postcode]], winningResultOpt: Option[List[Postcode]]): Option[SubscriberResult[List[Postcode], List[Postcode]]] = {
    subscriberPostcodesOpt.map(subPostcodes => {
      winningResultOpt match {
        case Some(winningPostcodes) if winningPostcodes.intersect(subPostcodes).nonEmpty => SubscriberResult(StackpotResultType, subPostcodes, Some(winningPostcodes), Some(true))
        case Some(winningPostcodes) if winningPostcodes.intersect(subPostcodes).isEmpty  => SubscriberResult(StackpotResultType, subPostcodes, Some(winningPostcodes), Some(false))
        case None => SubscriberResult(StackpotResultType, subPostcodes, None, None)
      }
    })
  }

  private def handleSurveyDrawResult(subscriberPostcodesOpt: Option[List[Postcode]], winningResultOpt: Option[List[Postcode]]): Option[SubscriberResult[List[Postcode], List[Postcode]]] = {
    subscriberPostcodesOpt.map(subPostcodes => {
      winningResultOpt match {
        case Some(winningPostcodes) if winningPostcodes.intersect(subPostcodes).nonEmpty => SubscriberResult(SurveyDrawResultType, subPostcodes, Some(winningPostcodes), Some(true))
        case Some(winningPostcodes) if winningPostcodes.intersect(subPostcodes).isEmpty  => SubscriberResult(SurveyDrawResultType, subPostcodes, Some(winningPostcodes), Some(false))
        case None => SubscriberResult(SurveyDrawResultType, subPostcodes, None, None)
      }
    })
  }

  private def handleDinnerResult(subscriberDinnerOpt: Option[List[DinnerUserName]], winningResultOpt: Option[List[DinnerUserName]]): Option[SubscriberResult[List[DinnerUserName], List[DinnerUserName]]] = {
    subscriberDinnerOpt.map(subDinnerUsers => {
      winningResultOpt match {
        case Some(winningDinnerUsers) if winningDinnerUsers.intersect(subDinnerUsers).nonEmpty => SubscriberResult(DinnerResultType, subDinnerUsers, Some(winningDinnerUsers), Some(true))
        case Some(winningPostcode) if winningPostcode.intersect(subDinnerUsers).isEmpty => SubscriberResult(DinnerResultType, subDinnerUsers, Some(winningPostcode), Some(false))
        case None => SubscriberResult(DinnerResultType, subDinnerUsers, None, None)
      }
    })
  }

  private def handleEmojiResult(subscriberEmojiSetsOpt: Option[List[Set[Emoji]]], winningResultOpt: Option[Set[Emoji]]): Option[SubscriberResult[Set[Emoji], List[Set[Emoji]]]] = {
    subscriberEmojiSetsOpt.map(subEmojiSets => {
      winningResultOpt match {
        case Some(winningEmojiSet) if subEmojiSets.contains(winningEmojiSet) => SubscriberResult(EmojiResultType, subEmojiSets, Some(winningEmojiSet), Some(true))
        case Some(winningEmojiSet) if !subEmojiSets.contains(winningEmojiSet) => SubscriberResult(EmojiResultType, subEmojiSets, Some(winningEmojiSet), Some(false))
        case None => SubscriberResult(EmojiResultType, subEmojiSets, None, None)
      }
    })
  }
}
