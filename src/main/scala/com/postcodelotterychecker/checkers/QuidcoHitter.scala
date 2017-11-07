package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.postcodelotterychecker.utils.Utils
import com.postcodelotterychecker.ConfigLoader
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

object QuidcoHitter extends QuidcoHitter {
  override val config = ConfigLoader.defaultConfig.quidcoHitterConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = ???
}
