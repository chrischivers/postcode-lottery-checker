package com.postcodelotterychecker

import java.io.{BufferedOutputStream, FileOutputStream}

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

class StackpotChecker(stackpotCheckerConfig: StackpotCheckerConfig, users: List[User], visionAPIClient: VisionAPIClient, screenshotAPIClient: ScreenshotAPIClient)(implicit executionContext: ExecutionContext) extends Checker[List[Postcode]] with StrictLogging {

  override def run: Future[(UserResults, List[Postcode])] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, List[Postcode])] = {
    Future {
      logger.info("Stackpot: Starting using direct web address")
      val directWebAddress = stackpotCheckerConfig.directWebAddressPrefix + stackpotCheckerConfig.directWebAddressSuffix + stackpotCheckerConfig.uuid
      logger.info(s"using direct web address $directWebAddress")
      val winningPostcodes = getWinningResult(directWebAddress)
      logger.info(s"Stackpot: ${winningPostcodes.size} postcodes obtained")
      logger.info(s"Stackpot: winning postcodes obtained: $winningPostcodes")
      (processResult(winningPostcodes), winningPostcodes)
    }
  }

  override def getWinningResult(webAddress: String): List[Postcode] = {
    logger.info(s"Stackpot: Processing web address: $webAddress")

    logger.info(s"Stackpot: Getting screenshot byte array from web address: $webAddress")
    val imageByteArray = screenshotAPIClient.getScreenshotByteArray(webAddress, fullpage = false, viewPort = LongThinViewPort, userAgent = SafariMobile, delay = 2)

//    val bos = new BufferedOutputStream(new FileOutputStream("stackpot-screenshot-byte-array.png"))
//    bos.write(imageByteArray)
//    bos.close()

    val postCodesFromVisionApi = visionAPIClient.makeStackpotCheckerRequest(imageByteArray)
    logger.info(s"Stackpot: Postcode obtained from Vision API: $postCodesFromVisionApi")

    postCodesFromVisionApi match {
      case None => throw new RuntimeException("No stackpot winners found on webpage (none returned)")
      case Some(list) if list.isEmpty => throw new RuntimeException("No stackpot winners found on webpage (empty list)")
      case Some (list) => list.map(str => Postcode(str.replace(" ", "").toUpperCase))
    }
  }

  private def processResult(winningPostcodes: List[Postcode]): Map[User, Option[Boolean]] = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.intersect(winningPostcodes).nonEmpty
      })
    }).toMap
  }
}