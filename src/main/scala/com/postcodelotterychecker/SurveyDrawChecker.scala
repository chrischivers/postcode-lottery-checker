package com.postcodelotterychecker

import java.io.{BufferedOutputStream, FileOutputStream}

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.concurrent.{ExecutionContext, Future}

class SurveyDrawChecker(surveyDrawCheckerConfig: SurveyDrawCheckerConfig, users: List[User], visionAPIClient: VisionAPIClient, screenshotAPIClient: ScreenshotAPIClient)(implicit executionContext: ExecutionContext) extends Checker[Postcode] with StrictLogging {

  override def run: Future[(UserResults, Postcode)] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, Postcode)] = {
    Future {
      logger.info("Survey Draw: Starting using direct web address")
      val directWebAddress = surveyDrawCheckerConfig.directWebAddressPrefix + surveyDrawCheckerConfig.directWebAddressSuffix + surveyDrawCheckerConfig.uuid
      logger.info(s"Survey Draw: using direct web address $directWebAddress")
      val winningPostcode = getWinningResult(directWebAddress)
      logger.info(s"Survey Draw: winning postcode obtained: $winningPostcode")
      (processResult(winningPostcode), winningPostcode)
    }
  }

  override def getWinningResult(webAddress: String): Postcode = {
    logger.info(s"Processing web address: $webAddress")

    logger.info(s"Getting screenshot byte array from web address: $webAddress")
    val imageByteArray = screenshotAPIClient.getScreenshotByteArray(webAddress, fullpage = false, viewPort = SmallSquareViewPort, userAgent = SafariMobile, delay = 0)

//    val bos = new BufferedOutputStream(new FileOutputStream("survey-draw-screenshot-byte-array.png"))
//        bos.write(imageByteArray)
//        bos.close()

    val postCodeFromVisionApi = visionAPIClient.makeSurveyDrawCheckerRequest(imageByteArray)
    logger.info(s"Postcode obtained from Vision API: $postCodeFromVisionApi")

    postCodeFromVisionApi match {
      case None => throw new RuntimeException("Survey draw: No postcode returned from vision API")
      case Some(result) => Postcode(result.toUpperCase)
    }
  }

  private def processResult(winningPostcode: Postcode): Map[User, Option[Boolean]] = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.contains(winningPostcode)
      })
    }).toMap
  }
}
