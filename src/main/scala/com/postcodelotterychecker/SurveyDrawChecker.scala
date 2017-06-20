package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.concurrent.{ExecutionContext, Future}

class SurveyDrawChecker(surveyDrawCheckerConfig: SurveyDrawCheckerConfig, users: List[User])(implicit executionContext: ExecutionContext) extends Checker[Postcode] with StrictLogging {

  override def run: Future[(UserResults, Postcode)] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, Postcode)] = {
    Future {
      logger.info("Survey Draw Checker: Starting using direct web address")
      val directWebAddress = surveyDrawCheckerConfig.directWebAddressPrefix + surveyDrawCheckerConfig.directWebAddressSuffix + surveyDrawCheckerConfig.uuid
      logger.info(s"using direct web address $directWebAddress")
      val winningPostcode = getWinningResult(directWebAddress)
      logger.info(s"winning postcode obtained: $winningPostcode")
      (processResult(winningPostcode), winningPostcode)
    }
  }

  override def getWinningResult(webAddress: String): Postcode = {
    logger.info(s"Processing web address: $webAddress")

    val browser = JsoupBrowser()
    val doc = browser.get(webAddress)
    val winningPostcode = doc >> text(".result-text")
    logger.info("Survey Draw: Winning Postcode: " + winningPostcode)
    Postcode(winningPostcode.replace(" ", "").toUpperCase)
  }

  private def processResult(winningPostcode: Postcode): Map[User, Option[Boolean]] = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.contains(winningPostcode)
      })
    }).toMap
  }
}
