package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.postcodelotterychecker.utils.Utils
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.{Request, Response}
import com.postcodelotterychecker.models.ResultTypes.QuidcoHitterResultType
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

trait QuidcoHitter extends CheckerRequestHandler[Unit] {

  override def getResult: IO[Unit] = {
    val webAddress = generateWebAddress
    logger.info(s"Survey Draw Checker: Starting up using address $webAddress")
    hitQuidco(webAddress)
  }

  def hitQuidco(webAddress: String): IO[Unit] = IO {
    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {
      logger.info(s"Quidco Hitter: Hitting web address $webAddress")

      val browser = JsoupBrowser()
      val doc = browser.get(webAddress)
      doc.toHtml
    }
  }
}

class _QuidcoHitter extends RequestHandler[Request, Response] with QuidcoHitter {
  override val config = ConfigLoader.defaultConfig.quidcoHitterConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = new RedisResultCache[Unit] {
    override val resultType = QuidcoHitterResultType
    override val config = ConfigLoader.defaultConfig.redisConfig
  }

  override def handleRequest(input: CheckerRequestHandler.Request, context: Context) = {

    (for {
      result <- getResult
      _ <- cacheResult(input.uuid, result)
    } yield Response(true)).unsafeRunSync()
  }
}
