package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.{Request, Response}
import com.postcodelotterychecker.models.DinnerUserName
import com.postcodelotterychecker.models.ResultTypes.DinnerResultType
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._


trait DinnerChecker extends CheckerRequestHandler[List[DinnerUserName]] {

  override def getResult: IO[List[DinnerUserName]] = {
    logger.info("Dinner Checker: Starting using direct web address")
    val webAddress = generateWebAddress
    getDinnerUserNamesFrom(webAddress)
  }

  private def getDinnerUserNamesFrom(webAddress: String): IO[List[DinnerUserName]] = IO {
    logger.info(s"Dinner Checker: Processing web address: $webAddress")

    val browser = JsoupBrowser()
    val doc = browser.get(webAddress)
    val list = (doc >> elementList(".name")).map(_.text).distinct
    logger.info("Winning User names: " + list)
    if (list.isEmpty) throw new RuntimeException("No dinner winners found on webpage")
    list.map(str => DinnerUserName(str.toLowerCase))
  }
}

class _DinnerChecker extends RequestHandler[Request, Response] with DinnerChecker {
  override val config = ConfigLoader.defaultConfig.dinnerCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = new RedisResultCache[List[DinnerUserName]] {
    override val resultType = DinnerResultType
    override val config = ConfigLoader.defaultConfig.redisConfig
  }

  override def handleRequest(input: CheckerRequestHandler.Request, context: Context) = {

    (for {
      result <- getResult
      _ <- cacheResult(input.uuid, result)
    } yield Response(true)).unsafeRunSync()
  }
}