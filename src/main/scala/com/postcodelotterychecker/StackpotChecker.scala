package com.postcodelotterychecker

import java.io.{BufferedOutputStream, FileOutputStream, ObjectOutputStream}

import collection.JavaConverters._
import com.postcodelotterychecker.utils.Utils
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

class StackpotChecker(stackpotCheckerConfig: StackpotCheckerConfig, users: List[User])(implicit executionContext: ExecutionContext) extends Checker[List[Postcode]] with StrictLogging {

  override def run: Future[(UserResults, List[Postcode])] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, List[Postcode])] = {
    Future {
      logger.info("Stackpot: Starting using direct web address")
      val directWebAddress = stackpotCheckerConfig.directWebAddressPrefix + stackpotCheckerConfig.directWebAddressSuffix + stackpotCheckerConfig.uuid
      logger.info(s"using direct web address $directWebAddress")
      val winningPostcodes = getWinningResult(directWebAddress)
      logger.info(s"Stackpot: ${winningPostcodes.size} postcodes obtained")
      logger.info(s"Stackpot: winning postcodes obtained: $winningPostcodes")
      (processResult(winningPostcodes), winningPostcodes)
    }
  }

  override def getWinningResult(webAddress: String): List[Postcode] = {
    logger.info(s"Processing web address: $webAddress")

    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {
      val htmlUnitWebClient = new HtmlUnitWebClient
      val page = htmlUnitWebClient.getPage(webAddress)
      logger.debug(page.asXml().mkString)

      val texts = page.getElementById("result-header").getElementsByTagName("p")
      texts.asScala.toList.map(htmlElem => {
        val text = htmlElem.getTextContent
        logger.info(s"text retrieved $text")
        val trimmedText = text.trim().split("\n").map(_.trim).apply(0)
        logger.info(s"trimmed text retrieved $trimmedText")
        val postcode = Postcode(trimmedText)
        if (Utils.validatePostcodeAgainstRegex(postcode)) Postcode(postcode.value.replace(" ", ""))
        else throw new RuntimeException(s"Postcode $postcode unable to be validated")
      })
    }
  }

  private def processResult(winningPostcodes: List[Postcode]): Map[User, Option[Boolean]] = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.intersect(winningPostcodes).nonEmpty
      })
    }).toMap
  }
}