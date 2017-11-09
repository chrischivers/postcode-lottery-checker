package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.gargoylesoftware.htmlunit.html.HtmlElement
import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.{Request, Response}
import com.postcodelotterychecker.models.ResultTypes.{EmojiResultType, PostcodeResultType}
import com.postcodelotterychecker.models.{Emoji, Postcode}
import com.postcodelotterychecker.utils.Utils

import scala.collection.JavaConverters._

trait PostcodeChecker extends CheckerRequestHandler[Postcode] {

  override def getResult: IO[Postcode] = {
    val webAddress = generateWebAddress
    logger.info(s"Postcode Checker: Starting up using address $webAddress")
    getPostcodeFrom(webAddress)
  }

  private def getPostcodeFrom(webAddress: String): IO[Postcode] = IO {
    logger.info(s"Postcode Checker: Processing web address: $webAddress")
    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {

      val page = htmlUnitWebClient.getPage(webAddress)

      logger.debug(page.asXml().mkString)

      val postcodeTags = page.getByXPath[HtmlElement]("//p[@class='postcode']").asScala

      val retrievedPostcode = postcodeTags.map(el => {
        el.removeChild("span", 0)
        Postcode(el.getTextContent).trim
      }).find(maybePostcode => maybePostcode.isValid)

      retrievedPostcode.getOrElse(throw new RuntimeException(s"Postcode unable to be fetched/validated. Postcode tags: [${postcodeTags.map(el => el.asXml())}]"))
    }
  }
}

class _PostcodeChecker extends RequestHandler[Request, Response] with PostcodeChecker {
  override val config = ConfigLoader.defaultConfig.postcodeCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = new RedisResultCache[Postcode] {
    override val resultType = PostcodeResultType
    override val config = ConfigLoader.defaultConfig.redisConfig
  }
  override def handleRequest(input: CheckerRequestHandler.Request, context: Context) = {

    (for {
      result <- getResult
      _ <- cacheResult(input.uuid, result)
    } yield Response(true)).unsafeRunSync()
  }
}