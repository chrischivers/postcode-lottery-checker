package results

import com.postcodelotterychecker.models.Competitions._
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models.Results.{SubscriberResult, SubscriberResults}
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode, Subscriber}
import com.postcodelotterychecker.results.ResultsEmailer
import org.scalatest.{FlatSpec, Matchers}


class ResultsEmailerTest extends FlatSpec with SubscriberScenarios with Matchers {

  (postcodeSubscriberScenarios ++
    dinnerSubscriberScenarios ++
    surveyDrawSubscriberScenarios ++
    stackpotSubscriberScenarios ++
    emojiSubscriberScenarios ++
    multipleWinningScenarios).foreach { scenario =>

    it should s"Send correct results emails to clients for ${scenario.description}" in new ResultsEmailer {

      override val resultsData: Map[Subscriber, SubscriberResults] = scenarioToSubscriberResults(scenario)
      override val emailClient = new StubEmailClient

      sendEmails().unsafeRunSync()

      emailClient.emailsSent should have size 1
      val emailSent = emailClient.emailsSent.head
      emailSent.to shouldBe scenario.subscriber.email

      val competitionsWon = scenario.won.toList.filter(_._2.contains(true))
      if (competitionsWon.isEmpty) {
        emailSent.subject should include("Sorry you have not won today")
      } else {
        emailSent.subject should include(s"Congratulations you have won ${competitionsWon.map(_._1.name).mkString(", ")}")
      }

      List(PostcodeCompetition,
        DinnerCompetition,
        SurveyDrawCompetition,
        StackpotCompetition,
        EmojiCompetition).foreach { competition =>

        val isWatching = competition match {
          case PostcodeCompetition | SurveyDrawCompetition | StackpotCompetition => scenario.subscriber.postcodesWatching.isDefined
          case DinnerCompetition => scenario.subscriber.dinnerUsersWatching.isDefined
          case EmojiCompetition => scenario.subscriber.emojiSetsWatching.isDefined
        }

        if (isWatching) {
          emailSent.body should include(
            s"**${competition.name}**\n" +
              s"Result: ${scenario.won.get(competition).fold("Not won")(_.fold("Unknown. Please check....")(if (_) "WON" else "Not won"))}"
                .stripMargin
          )
        } else emailSent.body should not include s"**${competition.name}**"
      }

    }
  }

  def scenarioToSubscriberResults(scenario: Scenario): Map[Subscriber, SubscriberResults] = {
    def postcodeResult(postcodesWatching: List[Postcode]) = SubscriberResult(
      PostcodeResultType,
      postcodesWatching,
      if (scenario.resultsNotReceivedFor.contains(PostcodeCompetition)) None else Some(defaultWinningPostcode),
      scenario.won.get(PostcodeCompetition).fold[Option[Boolean]](Some(false))(identity))

    def dinnerResult(dinnerUsersWatching: List[DinnerUserName]) = SubscriberResult(
      DinnerResultType,
      dinnerUsersWatching,
      if (scenario.resultsNotReceivedFor.contains(DinnerCompetition)) None else Some(defaultWinningDinnerUsers),
      scenario.won.get(DinnerCompetition).fold[Option[Boolean]](Some(false))(identity))

    def surveyDrawResult(postcodesWatching: List[Postcode]) = SubscriberResult(
      SurveyDrawResultType,
      postcodesWatching,
      if (scenario.resultsNotReceivedFor.contains(SurveyDrawCompetition)) None else Some(defaultWinningSurveyDrawPostcodes),
      scenario.won.get(SurveyDrawCompetition).fold[Option[Boolean]](Some(false))(identity))

    def stackpotResult(postcodesWatching: List[Postcode]) = SubscriberResult(
      StackpotResultType,
      postcodesWatching,
      if (scenario.resultsNotReceivedFor.contains(StackpotCompetition)) None else Some(defaultWinningStackpotPostcodes),
      scenario.won.get(StackpotCompetition).fold[Option[Boolean]](Some(false))(identity))

    def emojiResult(emojiSetsWatching: List[Set[Emoji]]) = SubscriberResult(
      EmojiResultType,
      emojiSetsWatching,
      if (scenario.resultsNotReceivedFor.contains(EmojiCompetition)) None else Some(defaultWinningEmojiSet),
      scenario.won.get(EmojiCompetition).fold[Option[Boolean]](Some(false))(identity))

    Map(scenario.subscriber -> SubscriberResults(
      scenario.subscriber.postcodesWatching.map(postcodesWatching => postcodeResult(postcodesWatching)),
      scenario.subscriber.dinnerUsersWatching.map(dinnerUsersWatching => dinnerResult(dinnerUsersWatching)),
      scenario.subscriber.postcodesWatching.map(postcodesWatching => stackpotResult(postcodesWatching)),
      scenario.subscriber.postcodesWatching.map(postcodesWatching => surveyDrawResult(postcodesWatching)),
      scenario.subscriber.emojiSetsWatching.map(emojiSetsWatching => emojiResult(emojiSetsWatching))))
  }
}

