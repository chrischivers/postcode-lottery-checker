package com.postcodelotterychecker.models

import com.postcodelotterychecker.models.ResultTypes.ResultType

object Results {

  case class WinningResults(
                             postcodeResult: Option[Postcode],
                             dinnerResult: Option[List[DinnerUserName]],
                             surveyDrawResult: Option[Postcode],
                             stackpotResult: Option[List[Postcode]],
                             emojiResult: Option[Set[Emoji]]) {

    def allDefined: Boolean = {
      List(postcodeResult, dinnerResult, surveyDrawResult, stackpotResult, emojiResult).forall(_.isDefined)
    }
  }

  case class SubscriberResult[R, W](resultType: ResultType[R, W], watching: W, actualWinning: Option[R], won: Option[Boolean])

  case class SubscriberResults(
                                postcodeResult: Option[SubscriberResult[Postcode, List[Postcode]]],
                                dinnerResult: Option[SubscriberResult[List[DinnerUserName], List[DinnerUserName]]],
                                surveyDrawResult: Option[SubscriberResult[Postcode, List[Postcode]]],
                                stackpotResult: Option[SubscriberResult[List[Postcode], List[Postcode]]],
                                emojiResult: Option[SubscriberResult[Set[Emoji], List[Set[Emoji]]]]
                              )


}
