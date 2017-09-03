package com.postcodelotterychecker

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.gargoylesoftware.htmlunit.html.{HtmlAnchor, HtmlPage}
import com.postcodelotterychecker.utils.Utils
import org.w3c.dom.html.HTMLElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object SurveyDrawCheckerHandlerManual extends App {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val surveyDrawChecker = new SurveyDrawChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  val result = for {
    surveyDrawResults <- surveyDrawChecker.run
  } yield resultsToS3Uploader.uploadPostcodeCheckerResults(surveyDrawResults._1, surveyDrawResults._2, "1234567")

  Await.result(result, lambdaWaitTime)
}

class SurveyDrawCheckerHandler extends RequestHandler[Request, Response] {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val surveyDrawChecker = new SurveyDrawChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  override def handleRequest(input: Request, context: Context): Response = {

    val result = for {
      surveyDrawResults <- surveyDrawChecker.run
    } yield resultsToS3Uploader.uploadSurveyDrawCheckerResults(surveyDrawResults._1, surveyDrawResults._2, input.uuid)

    Await.result(result, lambdaWaitTime)
    Response(true)
  }
}

class SurveyDrawChecker(config: Config, users: List[User]) extends Checker[Postcode] {

  val surveyDrawCheckerConfig: SurveyDrawCheckerConfig = config.surveyDrawCheckerConfig

  override def run: Future[(UserResults, Postcode)] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, Postcode)] = {
    Future {
      logger.info("Survey Draw: Starting using direct web address")
      val directWebAddress = surveyDrawCheckerConfig.directWebAddressPrefix + surveyDrawCheckerConfig.directWebAddressSuffix + surveyDrawCheckerConfig.uuid
      logger.info(s"Survey Draw: using direct web address $directWebAddress")
      val winningPostcode = getWinningResult(directWebAddress)
      logger.info(s"Survey Draw: winning postcode obtained: $winningPostcode")
      (processResult(winningPostcode), winningPostcode)
    }
  }

  override def getWinningResult(webAddress: String): Postcode = {
    logger.info(s"Processing web address: $webAddress")

    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {
      val htmlUnitWebClient = new HtmlUnitWebClient
      val page = htmlUnitWebClient.getPage(webAddress)
//      logger.debug(page.asXml().mkString)
      logger.info("clicking link...")
      page.getFirstByXPath[HtmlAnchor]("//a[@class='reveal btn-link']").click()
//      logger.debug(page.asXml().mkString)

      val text = {
        val t = page.getElementById("result-header").getElementsByTagName("p").get(0).getTextContent
        if (t.contains("Looking for a")) {
          logger.info("text contained 'Looking for a', waiting longer...")
          Thread.sleep(10000)
          htmlUnitWebClient.waitForBackgroundJS(page.getWebClient)
          page.getElementById("result-header").getElementsByTagName("p").get(0).getTextContent
        } else t
      }
      logger.info(s"text retrieved $text")
      val trimmedText = text.trim().split("\n").map(_.trim).apply(0)
      logger.info(s"trimmed text retrieved $trimmedText")
      val postcode = Postcode(trimmedText)
      if (Utils.validatePostcodeAgainstRegex(postcode)) Postcode(postcode.value.replace(" ", ""))
      else throw new RuntimeException(s"Postcode $postcode unable to be validated")
    }
  }

  private def processResult(winningPostcode: Postcode): UserResults = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.contains(winningPostcode)
      })
    }).toMap
  }
}
