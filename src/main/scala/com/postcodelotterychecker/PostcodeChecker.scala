package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scalaj.http.{Http, HttpOptions}

class PostcodeChecker(postcodeCheckerConfig: PostcodeCheckerConfig, users: List[User], visionAPIClient: VisionAPIClient)(implicit executionContext: ExecutionContext) extends Checker[Postcode] with StrictLogging {

  override def run: Future[(UserResults, Postcode)] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, Postcode)] = {
    Future {
      logger.info("Postcode Checker: Starting using direct web address")
      val directWebAddress = postcodeCheckerConfig.directWebAddressPrefix + postcodeCheckerConfig.directWebAddressSuffix + postcodeCheckerConfig.uuid
      logger.info(s"using direct web address $directWebAddress")
      val winningPostcode = getWinningResult(directWebAddress)
      logger.info(s"winning postcode obtained: $winningPostcode")
      (processResult(winningPostcode), winningPostcode)
    }
  }

  override def getWinningResult(webAddress: String): Postcode = {
      logger.info(s"Processing web address: $webAddress")

      val imageURL = getImageURLFromWebAddress(webAddress)
      logger.info(s"Using image address: $imageURL")

      val imageByteArray = getByteArrayFromImage(imageURL)

      val postCodeFromVisionApi = visionAPIClient.makeRequest(imageByteArray)
      logger.info(s"Postcode obtained from Vision API: $postCodeFromVisionApi")
      postCodeFromVisionApi match {
        case None => throw new RuntimeException("No postcode returned from vision API")
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

  private def getImageURLFromWebAddress(url: String): String = {

    val response = Http(url).options(HttpOptions.followRedirects(true)).asString
    val responseLines = response.body.split("\n")

    val imageUrlSuffix = responseLines.find(_.contains("The current winning postcode")) match {
      case None => throw new RuntimeException("Text 'The current winning postcode' not found in webpage retrieved")
      case Some(line) => line.split("src=\"")(1).split("\"/>")(0)
    }

    logger.info(s"Image url suffix retrieved: $imageUrlSuffix")
    postcodeCheckerConfig.directWebAddressPrefix  + imageUrlSuffix
  }

  private def getByteArrayFromImage(imageUrl: String): Array[Byte] = {
    Http(imageUrl).asBytes.body
  }
}