package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.models.Postcode
import com.postcodelotterychecker.models.ResultTypes.{StackpotResultType, SurveyDrawResultType}
import com.postcodelotterychecker.utils.Utils


trait SurveyDrawChecker extends CheckerRequestHandler[Postcode] {

  override def getResult: IO[Postcode]  = {
    val webAddress = generateWebAddress
    logger.info(s"Survey Draw Checker: Starting up using address $webAddress")
    getSurveyDrawResultFrom(webAddress)
  }

  private def getSurveyDrawResultFrom(webAddress: String): IO[Postcode] = IO {
    logger.info(s"Survey Draw Checker: Processing web address: $webAddress")

    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {
      val page = htmlUnitWebClient.getPage(webAddress)

      logger.debug(page.asXml().mkString)
      logger.debug("clicking link...")

      page.getFirstByXPath[HtmlAnchor]("//a[@class='reveal btn-link']").click()

      logger.debug(page.asXml().mkString)

      val textContent = {
        val t = {
          val res = page.getElementById("result-header").getElementsByTagName("p").get(0)
          res.removeChild("span", 0)
          res.getTextContent
        }
        if (t.contains("Looking for a")) {
          logger.info("text contained 'Looking for a', waiting longer...")
          Thread.sleep(10000)
          htmlUnitWebClient.waitForBackgroundJS(page.getWebClient)

          val res = page.getElementById("result-header").getElementsByTagName("p").get(0)
          res.removeChild("span", 0)
          res.getTextContent

        } else t
      }
      logger.info(s"textContent retrieved $textContent")

      val postcodeRetrieved = Postcode(textContent.trim().split("\n").map(_.trim).apply(0))

      if (postcodeRetrieved.isValid) postcodeRetrieved.trim
      else throw new RuntimeException(s"Postcode $postcodeRetrieved unable to be validated")
    }
  }
}

object SurveyDrawChecker extends SurveyDrawChecker {
  override val config = ConfigLoader.defaultConfig.surveyDrawCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = new RedisResultCache[Postcode] {
    override val resultType = SurveyDrawResultType
    override val config = ConfigLoader.defaultConfig.redisConfig
  }
}
