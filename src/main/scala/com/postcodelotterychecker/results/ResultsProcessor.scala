package com.postcodelotterychecker.results

import cats.effect.IO
import com.postcodelotterychecker.RedisConfig
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.CheckerType._
import com.postcodelotterychecker.models.Results.{SubscriberResult, SubscriberResults, WinningResults}
import com.postcodelotterychecker.models._

trait ResultsProcessor {

  val redisConfig: RedisConfig

  lazy val postcodeResultsCache: RedisResultCache[Postcode] = new RedisResultCache[Postcode] {
    override val config: RedisConfig = redisConfig
    override val checkerType = PostcodeType
  }

  lazy val dinnerResultsCache: RedisResultCache[List[DinnerUserName]] = new RedisResultCache[List[DinnerUserName]] {

    override val config: RedisConfig = redisConfig
    override val checkerType = DinnerType
  }

  lazy val surveyDrawResultsCache: RedisResultCache[List[Postcode]] = new RedisResultCache[List[Postcode]] {
    override val config: RedisConfig = redisConfig
    override val checkerType = SurveyDrawType
  }

  lazy val stackpotResultsCache: RedisResultCache[List[Postcode]] = new RedisResultCache[List[Postcode]] {
    override val config: RedisConfig = redisConfig
    override val checkerType = StackpotType
  }

  lazy val emojisResultsCache: RedisResultCache[Set[Emoji]] = new RedisResultCache[Set[Emoji]] {
    override val config: RedisConfig = redisConfig
    override val checkerType = EmojiType
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
            postcodeResult = handleSinglePostcodeResult(subscriber.postcodesWatching, winningResults.postcodeResult),
            dinnerResult = handleDinnerResult(subscriber.dinnerUsersWatching, winningResults.dinnerResult),
            surveyDrawResult = handleMultiplePostcodeResult(subscriber.postcodesWatching, winningResults.surveyDrawResult),
            stackpotResult = handleMultiplePostcodeResult(subscriber.postcodesWatching, winningResults.stackpotResult),
            emojiResult = handleEmojiResult(subscriber.emojiSetsWatching, winningResults.emojiResult)
          )
      }.toMap
  }

  private def handleSinglePostcodeResult(subscriberPostcodeOpt: Option[List[Postcode]], winningResultOpt: Option[Postcode]): Option[SubscriberResult[List[Postcode]]] = {
    subscriberPostcodeOpt.map(subPostcodes => {
      winningResultOpt match {
          case Some(winningPostcode) if subPostcodes.contains(winningPostcode) => SubscriberResult(Some(true), subPostcodes, Some(List(winningPostcode)))
          case Some(winningPostcode) if !subPostcodes.contains(winningPostcode) => SubscriberResult(Some(false), subPostcodes, Some(List(winningPostcode)))
          case None => SubscriberResult(None, subPostcodes, None)
        }
    })
  }

  private def handleMultiplePostcodeResult(subscriberPostcodesOpt: Option[List[Postcode]], winningResultOpt: Option[List[Postcode]]): Option[SubscriberResult[List[Postcode]]] = {
    subscriberPostcodesOpt.map(subPostcodes => {
      winningResultOpt match {
        case Some(winningPostcodes) if winningPostcodes.intersect(subPostcodes).nonEmpty => SubscriberResult(Some(true), subPostcodes, Some(winningPostcodes))
        case Some(winningPostcodes) if winningPostcodes.intersect(subPostcodes).isEmpty  => SubscriberResult(Some(false), subPostcodes, Some(winningPostcodes))
        case None => SubscriberResult(None, subPostcodes, None)
      }
    })
  }

  private def handleDinnerResult(subscriberDinnerOpt: Option[List[DinnerUserName]], winningResultOpt: Option[List[DinnerUserName]]): Option[SubscriberResult[List[DinnerUserName]]] = {
    subscriberDinnerOpt.map(subDinnerUsers => {
      winningResultOpt match {
        case Some(winningDinnerUsers) if winningDinnerUsers.intersect(subDinnerUsers).nonEmpty => SubscriberResult(Some(true), subDinnerUsers, Some(winningDinnerUsers))
        case Some(winningPostcode) if winningPostcode.intersect(subDinnerUsers).isEmpty => SubscriberResult(Some(false), subDinnerUsers, Some(winningPostcode))
        case None => SubscriberResult(None, subDinnerUsers, None)
      }
    })
  }

  private def handleEmojiResult(subscriberEmojiSetsOpt: Option[List[Set[Emoji]]], winningResultOpt: Option[Set[Emoji]]): Option[SubscriberResult[List[Set[Emoji]]]] = {
    subscriberEmojiSetsOpt.map(subEmojiSets => {
      winningResultOpt match {
        case Some(winningEmojiSet) if subEmojiSets.contains(winningEmojiSet) => SubscriberResult(Some(true), subEmojiSets, Some(List(winningEmojiSet)))
        case Some(winningEmojiSet) if !subEmojiSets.contains(winningEmojiSet) => SubscriberResult(Some(false), subEmojiSets, Some(List(winningEmojiSet)))
        case None => SubscriberResult(None, subEmojiSets, None)
      }
    })
  }
}
