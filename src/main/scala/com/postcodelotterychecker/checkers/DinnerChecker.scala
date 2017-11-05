package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.postcodelotterychecker._
import com.postcodelotterychecker.models.DinnerUserName
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._


trait DinnerChecker extends CheckerRequestHandler[List[DinnerUserName]] {

  override def getResult: IO[List[DinnerUserName]] = {
    logger.info("Dinner Checker: Starting using direct web address")
    val webAddress = generateWebAddress
    getDinnerUserNamesFrom(webAddress)
  }

  override def sendResult(result: List[DinnerUserName]) = ???

  private def getDinnerUserNamesFrom(webAddress: String): IO[List[DinnerUserName]] = IO {
      logger.info(s"Dinner Checker: Processing web address: $webAddress")

      val browser = JsoupBrowser()
      val doc = browser.get(webAddress)
      val list = (doc >> texts(".name")).toSet.toList
      logger.info("Winning User names: " + list)
      if (list.isEmpty) throw new RuntimeException("No dinner winners found on webpage")
      list.map(str => DinnerUserName(str.toLowerCase))
    }
}

object DinnerChecker extends DinnerChecker {
  override val config = ConfigLoader.defaultConfig.dinnerCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
}