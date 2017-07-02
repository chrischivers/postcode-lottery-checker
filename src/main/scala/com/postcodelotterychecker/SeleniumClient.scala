package com.postcodelotterychecker

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage

class SeleniumClient extends App {

  val webClient = new WebClient

  webClient.waitForBackgroundJavaScript(30000)
  println("TITLE: " + currentPage.getTitleText)
  println(currentPage.getElementById("result-header").getElementsByTagName("p").get(0).getTextContent)

}
