package com.postcodelotterychecker.results

import com.postcodelotterychecker.models.Competitions.Competition
import com.postcodelotterychecker.models.Results.{SubscriberResult, SubscriberResults}
import com.postcodelotterychecker.models.Subscriber

trait ResultsEmailer {

  val resultsData: Map[Subscriber, SubscriberResults]
  val emailClient: EmailClient

  resultsData.foreach { case (subscriber, subscriberResults) =>
    val competitionsWon: List[Competition] = wonAnyCompetitions(subscriberResults)
    val subject = if (competitionsWon.isEmpty) "Sorry you have not won anything today" else s"Congratulations you have won ${competitionsWon.mkString(", ")}"
    val emailBody =
      s"""
         |
         |TODAYS RESULTS FOR ${subscriber.email}
         |
         |$subject
         |
         |${subscriberResults.postcodeResult.fold("")(pr => generateResultsBlock(pr))}
         |${subscriberResults.stackpotResult.fold("")(spr => generateResultsBlock(spr))}
         |${subscriberResults.surveyDrawResult.fold("")(sdr => generateResultsBlock(sdr))}
         |${subscriberResults.dinnerResult.fold("")(dr => generateResultsBlock(dr))}
         |${subscriberResults.emojiResult.fold("")(er => generateResultsBlock(er))}
         |
     """.stripMargin

    val email = Email(subject, emailBody, subscriber.email)

    emailClient.sendEmail(email)

  }


  private def generateResultsBlock[R, W](subscriberResult: SubscriberResult[R, W]): String = {
    val resultType = subscriberResult.resultType
    val competitionName = resultType.competition.name
    val wonOpt = subscriberResult.won
    val actualResultsOpt = subscriberResult.actualWinning
    val subscriberWatching = subscriberResult.watching

    s"""
       |**$competitionName**
       |Result: ${wonOpt.fold("Unknown. Please check....")(res => if (res) "WON" else "Not won")}
       |You are watching: ${subscriberResult.resultType.watchingToString(subscriberWatching)}
       |Actual results were: ${actualResultsOpt.fold("Unknown. Please check....")(res => subscriberResult.resultType.resultToString(res))}
      """.stripMargin
  }

  private def wonAnyCompetitions(subscriberResults: SubscriberResults): List[Competition] = {

    def hasSubscriberResultWon[R, W](subscriberResult: Option[SubscriberResult[R, W]]) = {
      for {
        res <- subscriberResult
        won <- res.won
        if won
      } yield res.resultType.competition
    }

    List(
      hasSubscriberResultWon(subscriberResults.postcodeResult),
      hasSubscriberResultWon(subscriberResults.dinnerResult),
      hasSubscriberResultWon(subscriberResults.surveyDrawResult),
      hasSubscriberResultWon(subscriberResults.stackpotResult),
      hasSubscriberResultWon(subscriberResults.emojiResult),
    ).flatten
  }

}
