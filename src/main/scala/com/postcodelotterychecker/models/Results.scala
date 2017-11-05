package com.postcodelotterychecker.models

import com.postcodelotterychecker.models.Competitions.Competition
import com.postcodelotterychecker.models.ResultTypes.{PostcodeResultType, ResultType}

object Results {

  case class WinningResults(
                             postcodeResult: Option[Postcode],
                             dinnerResult: Option[List[DinnerUserName]],
                             surveyDrawResult: Option[List[Postcode]],
                             stackpotResult: Option[List[Postcode]],
                             emojiResult: Option[Set[Emoji]])

  case class SubscriberResult[R, W](resultType: ResultType[R, W], watching: W, actualWinning: Option[R], won: Option[Boolean])

  case class SubscriberResults(
                                postcodeResult: Option[SubscriberResult[Postcode, List[Postcode]]],
                                dinnerResult: Option[SubscriberResult[List[DinnerUserName], List[DinnerUserName]]],
                                surveyDrawResult: Option[SubscriberResult[List[Postcode], List[Postcode]]],
                                stackpotResult: Option[SubscriberResult[List[Postcode], List[Postcode]]],
                                emojiResult: Option[SubscriberResult[Set[Emoji], List[Set[Emoji]]]]
                              )


}
