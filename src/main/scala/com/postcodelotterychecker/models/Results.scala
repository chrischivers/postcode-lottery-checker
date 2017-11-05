package com.postcodelotterychecker.models

object Results {

  case class WinningResults(
                             postcodeResult: Option[Postcode],
                             dinnerResult: Option[List[DinnerUserName]],
                             surveyDrawResult: Option[List[Postcode]],
                             stackpotResult: Option[List[Postcode]],
                             emojiResult: Option[Set[Emoji]])

  case class SubscriberResult[A](won: Option[Boolean], watching: A, actualWinning: Option[A])

  case class SubscriberResults(
                                postcodeResult: Option[SubscriberResult[List[Postcode]]],
                                dinnerResult: Option[SubscriberResult[List[DinnerUserName]]],
                                surveyDrawResult: Option[SubscriberResult[List[Postcode]]],
                                stackpotResult: Option[SubscriberResult[List[Postcode]]],
                                emojiResult: Option[SubscriberResult[List[Set[Emoji]]]]
                              )

}
