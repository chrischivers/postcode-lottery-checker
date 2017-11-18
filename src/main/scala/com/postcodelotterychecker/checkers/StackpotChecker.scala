package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.gargoylesoftware.htmlunit.html.HtmlElement
import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.{Request, Response}
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

      val postcodeTags = page.getByXPath[HtmlElement]("//p[@class='postcode']").asScala.toList

      val retrievedPostcodes = postcodeTags.map(el => {
        el.removeChild("span", 0)
        Postcode(el.getTextContent).trim
      }).filter(maybePostcode => maybePostcode.isValid)

      if (retrievedPostcodes.isEmpty) throw new RuntimeException(s"Postcode unable to be fetched/validated. Postcode tags: [${postcodeTags.map(el => el.asXml())}]")
      else retrievedPostcodes
    }
  }
}

class _StackpotChecker extends RequestHandler[Request, Response] with StackpotChecker {
  override val config = ConfigLoader.defaultConfig.stackpotCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = new RedisResultCache[List[Postcode]] {
    override val resultType = StackpotResultType
    override val config = ConfigLoader.defaultConfig.redisConfig
  }

  override def handleRequest(input: CheckerRequestHandler.Request, context: Context) = {

    (for {
      result <- getResult
      _ <- cacheResult(input.uuid, result)
    } yield Response(true)).unsafeRunSync()
  }
}