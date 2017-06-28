import java.io.BufferedInputStream

import com.postcodelotterychecker.{ScreenshotAPIClient, ScreenshotApiConfig, UserAgent, ViewPort}

import scala.io.Source

class StubScreenshotApiClient(screenshotApiConfig: ScreenshotApiConfig) extends ScreenshotAPIClient(screenshotApiConfig) {

  override def getScreenshotByteArray(webAddress: String, fullpage: Boolean, viewPort: ViewPort, userAgent: UserAgent, delay: Int): Array[Byte] = {
    val resource: String = {
      if (webAddress.contains("stackpot")) "/stackpot/stackpot-screenshot-byte-array.png"
      else if (webAddress.contains("survey-draw")) "/survey-draw/survey-draw-screenshot-byte-array.png"
      else "/postcode/postcode-screenshot-byte-array.png"
    }
    val bis = new BufferedInputStream(getClass.getResource(resource).openStream())
    try Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
    finally bis.close()
  }
}
