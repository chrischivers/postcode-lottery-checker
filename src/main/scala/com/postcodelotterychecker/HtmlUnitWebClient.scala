package com.postcodelotterychecker

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.util.FalsifyingWebConnection
import com.typesafe.scalalogging.StrictLogging


class HtmlUnitWebClient extends StrictLogging {

  def getNewWebClient = {
    val webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER)
    webClient.getOptions.setThrowExceptionOnScriptError(false)
    webClient.getOptions.setThrowExceptionOnFailingStatusCode(false)
    webClient.getOptions.setRedirectEnabled(true)
    webClient.getOptions.setGeolocationEnabled(false)
    webClient.getOptions.setMaxInMemory(1024 * 1024 * 1000)
    webClient.getOptions.setDoNotTrackEnabled(true)
    webClient.getOptions.setDownloadImages(false)
    webClient.getOptions.setPopupBlockerEnabled(false)
    webClient.getOptions.setUseInsecureSSL(true)
    webClient.getOptions.setCssEnabled(false)
    webClient.setWebConnection(new InterceptWebConnection(webClient))
    webClient
  }

  def getPage(webAddress: String): HtmlPage = {
    val webClient = getNewWebClient
    logger.info(s"Html unit web client getting page from web address $webAddress")
    val currentPage = webClient.getPage[HtmlPage](webAddress)
    waitForBackgroundJS(webClient)
    logger.info("refreshing page...")
    val refreshedPage = currentPage.refresh()
    waitForBackgroundJS(webClient)
    refreshedPage.asInstanceOf[HtmlPage]
  }

  def waitForBackgroundJS(webClient: WebClient) = {
    webClient.waitForBackgroundJavaScriptStartingBefore(30000)
  }
}

class InterceptWebConnection(webClient: WebClient) extends FalsifyingWebConnection(webClient) {

  override def getResponse(request: WebRequest): WebResponse = {
    super.getResponse(request)
        val response = super.getResponse(request)
        if (response.getWebRequest.getUrl.toString.contains("delivery.d.switchadhub.com/adserver"))
          createWebResponse(response.getWebRequest, "", "application/javascript", 200, "Ok")
        else super.getResponse(request)
  }
}