package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.postcodelotterychecker._
import com.postcodelotterychecker.models.Postcode
import com.postcodelotterychecker.utils.Utils

trait PostcodeChecker extends CheckerRequestHandler[Postcode] {

  override def getResult: IO[Postcode] = {
    val webAddress = generateWebAddress
    logger.info(s"Postcode Checker: Starting up using address $webAddress")
    getPostcodeFrom(webAddress)
  }

  override def sendResult(result: Postcode) = ???


  private def getPostcodeFrom(webAddress: String): IO[Postcode] = IO {
    logger.info(s"Postcode Checker: Processing web address: $webAddress")
    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {

      val page = htmlUnitWebClient.getPage(webAddress)

      logger.debug(page.asXml().mkString)

      val textContent = {
        val res = page.getElementById("result").getElementsByTagName("p").get(0)
        res.removeChild("span", 0)
        res.getTextContent
      }

      logger.debug(s"Text content: $textContent")
      val postcodeRetrieved = Postcode(textContent.trim().split("\n").map(_.trim).apply(0))

      if (postcodeRetrieved.isValid) postcodeRetrieved.trim
      else throw new RuntimeException(s"Postcode ${postcodeRetrieved.value} unable to be validated")
    }
  }
}

object PostcodeChecker extends PostcodeChecker {
  override val config = ConfigLoader.defaultConfig.postcodeCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
}