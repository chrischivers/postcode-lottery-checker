package com.postcodelotterychecker.results

import cats.effect.IO
import com.postcodelotterychecker.EmailerConfig
import com.postcodelotterychecker.models.Competitions.Competition
import com.postcodelotterychecker.models.Results.{SubscriberResult, SubscriberResults}
import com.postcodelotterychecker.models.Subscriber
import com.postcodelotterychecker.servlet.ServletTypes.{EveryDay, OnlyWhenWon}

trait ResultsEmailer {

  val emailClient: EmailClient
  val emailerConfig: EmailerConfig

  def sendEmails(resultsData: Map[Subscriber, SubscriberResults]): IO[Int] = IO {

    resultsData.foldLeft(0) { case (acc, (subscriber, subscriberResults)) =>
      val competitionsWon: List[Competition] = wonAnyCompetitions(subscriberResults)
      if (competitionsWon.nonEmpty || subscriber.notifyWhen == EveryDay) {
        val subject = if (competitionsWon.isEmpty) "Sorry you have not won today" else s"Congratulations you have won ${competitionsWon.map(_.name).mkString(", ")}"
        val emailBody =
          s"""
             |
             |TODAY'S RESULTS FOR ${subscriber.email}
             |
             |$subject
             |
             |${subscriberResults.postcodeResult.fold("")(pr => generateResultsBlock(pr))}
             |${subscriberResults.stackpotResult.fold("")(spr => generateResultsBlock(spr))}
             |${subscriberResults.surveyDrawResult.fold("")(sdr => generateResultsBlock(sdr))}
             |${subscriberResults.dinnerResult.fold("")(dr => generateResultsBlock(dr))}
             |${subscriberResults.emojiResult.fold("")(er => generateResultsBlock(er))}
             |
             |<a href="${emailerConfig.baseSubscribeUrl}/register/remove?uuid=${subscriber.uuid}">Unsubscribe here</a>
             |
     """.stripMargin

        val email = Email(subject, emailBody, subscriber.email)

        emailClient.sendEmail(email)
        acc + 1
      } else acc
    }
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

    def hasSubscriberResultWon[R, W](subscriberResult: Option[SubscriberResult[R, W]]): Option[Competition] = {
      println(subscriberResult)
      for {
        res <- subscriberResult
        won <- res.won
        if won
      } yield res.resultType.competition
    }

    List(
      hasSubscriberResultWon(subscriberResults.postcodeResult),
      hasSubscriberResultWon(subscriberResults.dinnerResult),
      hasSubscriberResultWon(subscriberResults.stackpotResult),
      hasSubscriberResultWon(subscriberResults.surveyDrawResult),
      hasSubscriberResultWon(subscriberResults.emojiResult),
    ).flatten
  }

}
