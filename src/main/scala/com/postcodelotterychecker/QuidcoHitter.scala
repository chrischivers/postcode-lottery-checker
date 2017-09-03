package com.postcodelotterychecker

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.postcodelotterychecker.utils.Utils
import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class QuidcoHitterHandler extends RequestHandler[Request, Response] {

  val config: Config = ConfigLoader.defaultConfig
  val quidcoHitter = new QuidcoHitter(config)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  override def handleRequest(input: Request, context: Context): Response = {

    val result = for {
      _ <- quidcoHitter.run
    } yield ()

    Await.result(result, lambdaWaitTime)
    Response(true)
  }
}
class QuidcoHitter(config: Config) extends StrictLogging {

  val quidcoHitterConfig: QuidcoHitterConfig = config.quidcoHitterConfig

  def run = {
    Future {
      Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {
        logger.info("Quidco Hitter: Starting using direct web address")
        val directWebAddress = quidcoHitterConfig.directWebAddressPrefix + quidcoHitterConfig.directWebAddressSuffix + quidcoHitterConfig.uuid

        val browser = JsoupBrowser()
        val doc = browser.get(directWebAddress)
        doc.toHtml
      }
    }
  }
}
