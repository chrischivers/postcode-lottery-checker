package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.gargoylesoftware.htmlunit.html.{HtmlAnchor, HtmlElement}
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.checkers.CheckerRequestHandler.{Request, Response}
import com.postcodelotterychecker.models.Postcode
import com.postcodelotterychecker.models.ResultTypes.{StackpotResultType, SurveyDrawResultType}
import com.postcodelotterychecker.utils.Utils

import scala.collection.JavaConverters._
import scala.util.Try


trait SurveyDrawChecker extends CheckerRequestHandler[Postcode] {

  override def getResult: IO[Postcode] = {
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

      Try(Option(page.getFirstByXPath[HtmlAnchor]("//a[@class='reveal btn-link']")).map(_.click()))

      logger.info(page.asXml().mkString)

      val postcodeTagsFirstPass = page.getByXPath[HtmlElement]("//p[@class='postcode']").asScala

      if (postcodeTagsFirstPass.exists(_.getTextContent.contains("Looking for a"))) {
        logger.info("text contained 'Looking for a', waiting longer...")
        Thread.sleep(10000)
        htmlUnitWebClient.waitForBackgroundJS(page.getWebClient)
      }

      val postcodeTagsSecondPass = page.getByXPath[HtmlElement]("//p[@class='postcode']").asScala

      val retrievedPostcode = postcodeTagsSecondPass.map(el => {
        el.removeChild("span", 0)
        Postcode(el.getTextContent).trim
      }).find(maybePostcode => maybePostcode.isValid)

      retrievedPostcode.getOrElse(throw new RuntimeException(s"Postcode unable to be fetched/validated. Postcode tags: [${postcodeTagsSecondPass.map(el => el.asXml())}]"))

    }
  }
}

class _SurveyDrawChecker extends RequestHandler[Request, Response] with SurveyDrawChecker {
  override val config = ConfigLoader.defaultConfig.surveyDrawCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = new RedisResultCache[Postcode] {
    override val resultType = SurveyDrawResultType
    override val config = ConfigLoader.defaultConfig.redisConfig
  }

  override def handleRequest(input: CheckerRequestHandler.Request, context: Context) = {

    (for {
      result <- getResult
      _ <- cacheResult(input.uuid, result)
    } yield Response(true)).unsafeRunSync()
  }
}
