package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.Postcode
import com.postcodelotterychecker.models.ResultTypes.{PostcodeResultType, StackpotResultType}
import com.postcodelotterychecker.utils.Utils

import scala.collection.JavaConverters._

trait StackpotChecker extends CheckerRequestHandler[List[Postcode]] {

  override def getResult: IO[List[Postcode]] = {
    val webAddress = generateWebAddress
    logger.info(s"Stackpot Checker: Starting up using address $webAddress")
    getStackpotPostcodesFrom(webAddress)
  }

  private def getStackpotPostcodesFrom(webAddress: String): IO[List[Postcode]] = IO {
    logger.info(s"Stackpot Checker: Processing web address: $webAddress")

    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {

      val page = htmlUnitWebClient.getPage(webAddress)

      logger.debug(page.asXml().mkString)

      val elements1 = page.getElementById("result-header").getElementsByTagName("p")
      elements1.asScala.toList.map(htmlElem => {
        val textContent = htmlElem.getTextContent
        logger.debug(s"Text content: $textContent")

        val postcodeRetrieved = Postcode(textContent.trim().split("\n").map(_.trim).apply(0))
        if (postcodeRetrieved.isValid) postcodeRetrieved.trim
        else throw new RuntimeException(s"Postcode ${postcodeRetrieved.value} unable to be validated")
      })
    }
  }
}

object StackpotChecker extends StackpotChecker {
  override val config = ConfigLoader.defaultConfig.stackpotCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = new RedisResultCache[List[Postcode]] {
    override val resultType = StackpotResultType
    override val config = ConfigLoader.defaultConfig.redisConfig
  }
}