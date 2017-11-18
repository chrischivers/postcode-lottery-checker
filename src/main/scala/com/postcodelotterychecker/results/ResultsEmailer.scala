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
        val subject = s"Free Postcode Lottery Checker ${if (competitionsWon.isEmpty) "- Not won" else "Winner!"}"
        val firstLine = if (competitionsWon.isEmpty) "Sorry you have not won today" else s"Congratulations you have won the ${competitionsWon.map(_.name).mkString(", ")}"
        val emailBody = generateEmailBody(
          subscriber.email,
          firstLine,
          subscriberResults.postcodeResult.map(pr => generateResultsBlock(pr)),
          subscriberResults.dinnerResult.map(dr => generateResultsBlock(dr)),
          subscriberResults.stackpotResult.map(spr => generateResultsBlock(spr)),
          subscriberResults.surveyDrawResult.map(sdr => generateResultsBlock(sdr)),
          subscriberResults.emojiResult.map(er => generateResultsBlock(er)),
          s"${emailerConfig.baseSubscribeUrl}/register/remove?uuid=${subscriber.uuid}"
          )

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
       |<strong>$competitionName</strong><br/>
       |Result: ${wonOpt.fold("Unknown. Please check....")(res => if (res) "<strong>WON</strong>" else "Not won")}<br/>
       |You are watching: ${subscriberResult.resultType.watchingToString(subscriberWatching)}<br/>
       |Actual results were: ${actualResultsOpt.fold("Unknown. Please check....")(res => subscriberResult.resultType.resultToString(res))}
      """.stripMargin
  }

  private def wonAnyCompetitions(subscriberResults: SubscriberResults): List[Competition] = {

    def hasSubscriberResultWon[R, W](subscriberResult: Option[SubscriberResult[R, W]]): Option[Competition] = {
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

  //Template used from https://www.leemunroe.com/responsive-html-email-template/
  def generateEmailBody(subscriberEmail: String,
                firstLine: String,
                postcodeResultBlock: Option[String],
                dinnerResultBlock: Option[String],
                stackpotResultBlock: Option[String],
                surveyDrawResultBlock: Option[String],
                emojiResultBlock: Option[String],
                unsubscribeUrl: String): String = {
    s"""
      |<!doctype html>
      |<html>
      |  <head>
      |    <meta name="viewport" content="width=device-width" />
      |    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
      |    <title>Free Postcode Lottery Checker = Results</title>
      |    <style>
      |      /* -------------------------------------
      |          GLOBAL RESETS
      |      ------------------------------------- */
      |      img {
      |        border: none;
      |        -ms-interpolation-mode: bicubic;
      |        max-width: 100%; }
      |
      |      body {
      |        background-color: #f6f6f6;
      |        font-family: sans-serif;
      |        -webkit-font-smoothing: antialiased;
      |        font-size: 14px;
      |        line-height: 1.4;
      |        margin: 0;
      |        padding: 0;
      |        -ms-text-size-adjust: 100%;
      |        -webkit-text-size-adjust: 100%; }
      |
      |      table {
      |        border-collapse: separate;
      |        mso-table-lspace: 0pt;
      |        mso-table-rspace: 0pt;
      |        width: 100%; }
      |        table td {
      |          font-family: sans-serif;
      |          font-size: 14px;
      |          vertical-align: top; }
      |
      |      /* -------------------------------------
      |          BODY & CONTAINER
      |      ------------------------------------- */
      |
      |      .body {
      |        background-color: #f6f6f6;
      |        width: 100%; }
      |
      |      /* Set a max-width, and make it display as block so it will automatically stretch to that width, but will also shrink down on a phone or something */
      |      .container {
      |        display: block;
      |        Margin: 0 auto !important;
      |        /* makes it centered */
      |        max-width: 580px;
      |        padding: 10px;
      |        width: 580px; }
      |
      |      /* This should also be a block element, so that it will fill 100% of the .container */
      |      .content {
      |        box-sizing: border-box;
      |        display: block;
      |        Margin: 0 auto;
      |        max-width: 580px;
      |        padding: 10px; }
      |
      |      /* -------------------------------------
      |          HEADER, FOOTER, MAIN
      |      ------------------------------------- */
      |      .main {
      |        background: #ffffff;
      |        border-radius: 3px;
      |        width: 100%; }
      |
      |      .wrapper {
      |        box-sizing: border-box;
      |        padding: 20px; }
      |
      |      .content-block {
      |        padding-bottom: 10px;
      |        padding-top: 10px;
      |      }
      |
      |      .footer {
      |        clear: both;
      |        Margin-top: 10px;
      |        text-align: center;
      |        width: 100%; }
      |        .footer td,
      |        .footer p,
      |        .footer span,
      |        .footer a {
      |          color: #999999;
      |          font-size: 12px;
      |          text-align: center; }
      |
      |      /* -------------------------------------
      |          TYPOGRAPHY
      |      ------------------------------------- */
      |      h1,
      |      h2,
      |      h3,
      |      h4 {
      |        color: #000000;
      |        font-family: sans-serif;
      |        font-weight: 400;
      |        line-height: 1.4;
      |        margin: 0;
      |        Margin-bottom: 30px; }
      |
      |      h1 {
      |        font-size: 35px;
      |        font-weight: 300;
      |        text-align: center;
      |        text-transform: capitalize; }
      |
      |      p,
      |      ul,
      |      ol {
      |        font-family: sans-serif;
      |        font-size: 14px;
      |        font-weight: normal;
      |        margin: 0;
      |        Margin-bottom: 15px; }
      |        p li,
      |        ul li,
      |        ol li {
      |          list-style-position: inside;
      |          margin-left: 5px; }
      |
      |      a {
      |        color: #3498db;
      |        text-decoration: underline; }
      |
      |      /* -------------------------------------
      |          BUTTONS
      |      ------------------------------------- */
      |      .btn {
      |        box-sizing: border-box;
      |        width: 100%; }
      |        .btn > tbody > tr > td {
      |          padding-bottom: 15px; }
      |        .btn table {
      |          width: auto; }
      |        .btn table td {
      |          background-color: #ffffff;
      |          border-radius: 5px;
      |          text-align: center; }
      |        .btn a {
      |          background-color: #ffffff;
      |          border: solid 1px #3498db;
      |          border-radius: 5px;
      |          box-sizing: border-box;
      |          color: #3498db;
      |          cursor: pointer;
      |          display: inline-block;
      |          font-size: 14px;
      |          font-weight: bold;
      |          margin: 0;
      |          padding: 12px 25px;
      |          text-decoration: none;
      |          text-transform: capitalize; }
      |
      |      .btn-primary table td {
      |        background-color: #3498db; }
      |
      |      .btn-primary a {
      |        background-color: #3498db;
      |        border-color: #3498db;
      |        color: #ffffff; }
      |
      |      /* -------------------------------------
      |          OTHER STYLES THAT MIGHT BE USEFUL
      |      ------------------------------------- */
      |      .last {
      |        margin-bottom: 0; }
      |
      |      .first {
      |        margin-top: 0; }
      |
      |      .align-center {
      |        text-align: center; }
      |
      |      .align-right {
      |        text-align: right; }
      |
      |      .align-left {
      |        text-align: left; }
      |
      |      .clear {
      |        clear: both; }
      |
      |      .mt0 {
      |        margin-top: 0; }
      |
      |      .mb0 {
      |        margin-bottom: 0; }
      |
      |      .preheader {
      |        color: transparent;
      |        display: none;
      |        height: 0;
      |        max-height: 0;
      |        max-width: 0;
      |        opacity: 0;
      |        overflow: hidden;
      |        mso-hide: all;
      |        visibility: hidden;
      |        width: 0; }
      |
      |      .powered-by a {
      |        text-decoration: none; }
      |
      |      hr {
      |        border: 0;
      |        border-bottom: 1px solid #f6f6f6;
      |        Margin: 20px 0; }
      |
      |      /* -------------------------------------
      |          RESPONSIVE AND MOBILE FRIENDLY STYLES
      |      ------------------------------------- */
      |      @media only screen and (max-width: 620px) {
      |        table[class=body] h1 {
      |          font-size: 28px !important;
      |          margin-bottom: 10px !important; }
      |        table[class=body] p,
      |        table[class=body] ul,
      |        table[class=body] ol,
      |        table[class=body] td,
      |        table[class=body] span,
      |        table[class=body] a {
      |          font-size: 16px !important; }
      |        table[class=body] .wrapper,
      |        table[class=body] .article {
      |          padding: 10px !important; }
      |        table[class=body] .content {
      |          padding: 0 !important; }
      |        table[class=body] .container {
      |          padding: 0 !important;
      |          width: 100% !important; }
      |        table[class=body] .main {
      |          border-left-width: 0 !important;
      |          border-radius: 0 !important;
      |          border-right-width: 0 !important; }
      |        table[class=body] .btn table {
      |          width: 100% !important; }
      |        table[class=body] .btn a {
      |          width: 100% !important; }
      |        table[class=body] .img-responsive {
      |          height: auto !important;
      |          max-width: 100% !important;
      |          width: auto !important; }}
      |
      |      /* -------------------------------------
      |          PRESERVE THESE STYLES IN THE HEAD
      |      ------------------------------------- */
      |      @media all {
      |        .ExternalClass {
      |          width: 100%; }
      |        .ExternalClass,
      |        .ExternalClass p,
      |        .ExternalClass span,
      |        .ExternalClass font,
      |        .ExternalClass td,
      |        .ExternalClass div {
      |          line-height: 100%; }
      |        .apple-link a {
      |          color: inherit !important;
      |          font-family: inherit !important;
      |          font-size: inherit !important;
      |          font-weight: inherit !important;
      |          line-height: inherit !important;
      |          text-decoration: none !important; }
      |        .btn-primary table td:hover {
      |          background-color: #34495e !important; }
      |        .btn-primary a:hover {
      |          background-color: #34495e !important;
      |          border-color: #34495e !important; } }
      |
      |    </style>
      |  </head>
      |  <body class="">
      |    <table border="0" cellpadding="0" cellspacing="0" class="body">
      |      <tr>
      |        <td>&nbsp;</td>
      |        <td class="container">
      |          <div class="content">
      |
      |            <!-- START CENTERED WHITE CONTAINER -->
      |            <span class="preheader">Free Postcode Lottery Checker: Results</span>
      |            <table class="main">
      |
      |              <!-- START MAIN CONTENT AREA -->
      |              <tr>
      |                <td class="wrapper">
      |                  <table border="0" cellpadding="0" cellspacing="0">
      |                    <tr>
      |                      <td>
      |                        <h2>Free Postcode Lottery Checker<br/>Today's Results for $subscriberEmail<h2/>
      |                        <p>$firstLine</p>
      |                        ${postcodeResultBlock.fold("")(str => s"<p>$str</p>")}
      |                        ${stackpotResultBlock.fold("")(str => s"<p>$str</p>")}
      |                        ${surveyDrawResultBlock.fold("")(str => s"<p>$str</p>")}
      |                        ${dinnerResultBlock.fold("")(str => s"<p>$str</p>")}
      |                        ${emojiResultBlock.fold("")(str => s"<p>$str</p>")}
      |                      </td>
      |                    </tr>
      |                  </table>
      |                </td>
      |              </tr>
      |
      |            <!-- END MAIN CONTENT AREA -->
      |            </table>
      |
      |            <!-- START FOOTER -->
      |            <div class="footer">
      |              <table border="0" cellpadding="0" cellspacing="0">
      |                <tr>
      |                  <td class="content-block">
      |                    <br><a href="$unsubscribeUrl">Unsubscribe here</a>.
      |                  </td>
      |                </tr>
      |                <tr>
      |                  <td class="content-block powered-by">
      |                    Powered by <a href="http://htmlemail.io">HTMLemail</a>.
      |                  </td>
      |                </tr>
      |              </table>
      |            </div>
      |            <!-- END FOOTER -->
      |
      |          <!-- END CENTERED WHITE CONTAINER -->
      |          </div>
      |        </td>
      |        <td>&nbsp;</td>
      |      </tr>
      |    </table>
      |  </body>
      |</html>
      |
    """.stripMargin
  }
}
