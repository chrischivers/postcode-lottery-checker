package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging

import scalaj.http.{Http, HttpOptions}

sealed trait ViewPort {
  def x: Int

  def y: Int

  override def toString: String = x.toString + "x" + y.toString
}

case object SmallSquareViewPort extends ViewPort {
  override def x: Int = 350

  override def y: Int = 350
}

case object LongThinViewPort extends ViewPort {
  override def x: Int = 350

  override def y: Int = 3000
}

sealed trait UserAgent {
  def value: String
}

case object SafariMobile extends UserAgent {
  override def value: String = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_0_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12A366 Safari/600.1.4"
}

class ScreenshotAPIClient(screenshotApiConfig: ScreenshotApiConfig) extends StrictLogging {

  var lastTimeRun: Long = 0

  def getScreenshotByteArray(webAddress: String, fullpage: Boolean, viewPort: ViewPort, userAgent: UserAgent, delay: Int) = {

    val timeToSleep = calculateSleepTime
    logger.info(s"Sleeping for $timeToSleep milliseconds before running next Screenshot Api call")
    Thread.sleep(calculateSleepTime)

    val response = Http(screenshotApiConfig.url)
      .params(Seq(
        ("access_key", screenshotApiConfig.apiKey),
        ("url", webAddress),
        ("user_agent", userAgent.value),
        ("viewport", viewPort.toString),
        ("force", "1"),
        ("fullpage", if (fullpage) "1" else "0"),
        ("delay", delay.toString)))
      .options(HttpOptions.followRedirects(true))
      .timeout(30000, 1200000)
      .asBytes

    lastTimeRun = System.currentTimeMillis()

    if (response.isSuccess) response.body
    else throw new RuntimeException(s"Response from screenshot API not successful: ${response.code}, ${response.headers}")
  }

  private def calculateSleepTime: Long = {
    val timeSinceLastRun: Long = System.currentTimeMillis() - lastTimeRun
    if (timeSinceLastRun < screenshotApiConfig.millisBetweenAttempts) screenshotApiConfig.millisBetweenAttempts - timeSinceLastRun
    else 0
  }
}
