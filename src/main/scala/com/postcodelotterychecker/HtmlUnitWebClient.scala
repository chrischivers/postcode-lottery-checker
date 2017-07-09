package com.postcodelotterychecker

import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit._
import com.typesafe.scalalogging.StrictLogging
import com.gargoylesoftware.htmlunit.util.FalsifyingWebConnection


class HtmlUnitWebClient extends StrictLogging {

  def getPage(webAddress: String): HtmlPage = {
    logger.info(s"Html unit web client getting page from web address $webAddress")

    val webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER)
    webClient.getOptions.setThrowExceptionOnScriptError(false)
    webClient.getOptions.setThrowExceptionOnFailingStatusCode(false)
    webClient.getOptions.setRedirectEnabled(true)
    webClient.getOptions.setGeolocationEnabled(false)
    webClient.getOptions.setMaxInMemory(1024 * 1024 * 500)
    webClient.getOptions.setDoNotTrackEnabled(true)
    webClient.getOptions.setDownloadImages(false)
    webClient.getOptions.setPopupBlockerEnabled(true)
    webClient.getOptions.setUseInsecureSSL(true)

    val currentPage = webClient.getPage[HtmlPage](webAddress)
    webClient.waitForBackgroundJavaScriptStartingBefore(30000)
    currentPage
  }
}