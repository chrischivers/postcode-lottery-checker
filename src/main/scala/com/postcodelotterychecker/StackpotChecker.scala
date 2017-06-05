package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.concurrent.{ExecutionContext, Future}

class StackpotChecker(stackpotCheckerConfig: StackpotCheckerConfig, users: List[User])(implicit executionContext: ExecutionContext) extends Checker[List[Postcode]] with StrictLogging {

  override def run: Future[(UserResults, List[Postcode])] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, List[Postcode])] = {
    Future {
      logger.info("Stackpot: Starting using direct web address")
      val directWebAddress = stackpotCheckerConfig.directWebAddressPrefix + stackpotCheckerConfig.directWebAddressSuffix
      logger.info(s"using direct web address $directWebAddress")
      val winningPostcodes = getWinningResult(directWebAddress)
      logger.info(s"Stackpot: ${winningPostcodes.size} postcodes obtained")
      logger.info(s"Stackpot: winning postcodes obtained: $winningPostcodes")
      (processResult(winningPostcodes), winningPostcodes)
    }
  }

  override def getWinningResult(webAddress: String): List[Postcode] = {

    val browser = JsoupBrowser()
    val doc = browser.get(webAddress)
    val list = (doc >> texts(".result-text")).toSet.toList
    logger.info("Stackpot: Winning Postcodes: " + list)
    if (list.isEmpty) throw new RuntimeException("No stackpot winners found on webpage")
    list.map(str => Postcode(str.replace(" ", "")))
  }

  private def processResult(winningPostcodes: List[Postcode]): Map[User, Option[Boolean]] = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.intersect(winningPostcodes).nonEmpty
      })
    }).toMap
  }
}