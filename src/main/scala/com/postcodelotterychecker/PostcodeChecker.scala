package com.postcodelotterychecker

import java.io.{BufferedOutputStream, FileOutputStream}
import java.net.URL
import sys.process._
import java.net.URL
import java.io.File
import com.postcodelotterychecker.utils.Utils
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

class PostcodeChecker(postcodeCheckerConfig: PostcodeCheckerConfig, users: List[User])(implicit executionContext: ExecutionContext) extends Checker[Postcode] with StrictLogging {

  override def run: Future[(UserResults, Postcode)] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, Postcode)] = {
    Future {
      logger.info("Postcode Checker: Starting using direct web address")
      val directWebAddress = postcodeCheckerConfig.directWebAddressPrefix + postcodeCheckerConfig.directWebAddressSuffix + postcodeCheckerConfig.uuid
      logger.info(s"using direct web address $directWebAddress")
      val winningPostcode = getWinningResult(directWebAddress)
      logger.info(s"winning postcode obtained: $winningPostcode")
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