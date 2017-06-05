package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import scala.concurrent.{ExecutionContext, Future}


class QuidcoHitter(quidcoHitterConfig: QuidcoHitterConfig)(implicit val executionContext: ExecutionContext) extends StrictLogging {
  
  def run = {
    Future {
      logger.info("Quidco Hitter: Starting using direct web address")
      val directWebAddress = quidcoHitterConfig.directWebAddressPrefix + quidcoHitterConfig.directWebAddressSuffix

      val browser = JsoupBrowser()
      val doc = browser.get(directWebAddress)
      doc.toHtml
    }
  }
}
