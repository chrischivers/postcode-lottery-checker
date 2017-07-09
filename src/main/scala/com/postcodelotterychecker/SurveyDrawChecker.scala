package com.postcodelotterychecker

import com.postcodelotterychecker.utils.Utils
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

class SurveyDrawChecker(surveyDrawCheckerConfig: SurveyDrawCheckerConfig, users: List[User])(implicit executionContext: ExecutionContext) extends Checker[Postcode] with StrictLogging {

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
      logger.debug(page.asXml().mkString)

      val text = page.getElementById("result-header").getElementsByTagName("p").get(0).getTextContent
      logger.info(s"text retrieved $text")
      val trimmedText = text.trim().split("\n").map(_.trim).apply(0)
      logger.info(s"trimmed text retrieved $trimmedText")
      val postcode = Postcode(trimmedText)
      if (Utils.validatePostcodeAgainstRegex(postcode)) Postcode(postcode.value.replace(" ", ""))
      else throw new RuntimeException(s"Postcode $postcode unable to be validated")
    }
  }

  private def processResult(winningPostcode: Postcode): Map[User, Option[Boolean]] = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.contains(winningPostcode)
      })
    }).toMap
  }
}
