package com.postcodelotterychecker

import java.io.ByteArrayOutputStream
import java.net.URL

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scalaj.http.{Http, HttpOptions}

class PostcodeChecker(config: Config, users: List[User])(implicit executionContext: ExecutionContext) extends Checker[Postcode] with StrictLogging {

  override def run: Future[Map[User, Option[Boolean]]] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[Map[User, Option[Boolean]]] = {
    Future {
      logger.info("Starting using direct web address")
      val directWebAddress = config.postcodeCheckerConfig.directWebAddressPrefix + config.postcodeCheckerConfig.directWebAddressSuffix
      logger.info(s"using direct web address $directWebAddress")
      val winningPostcode = getWinningResult(directWebAddress)
      logger.info(s"winning postcode obtained: $winningPostcode")
      processResult(winningPostcode)
    }
  }

  override def getWinningResult(webAddress: String): Postcode = {
      logger.info(s"Processing web address: $webAddress")

      val imageURL = getImageURLFromWebAddress(webAddress)
      logger.info(s"Using image address: $imageURL")

      val imageByteArray = getByteArrayFromImage(imageURL)

      val postCodeFromVisionApi = VisionAPI.makeRequest(imageByteArray)
      logger.info(s"Postcode obtained from Vision API: $postCodeFromVisionApi")
      postCodeFromVisionApi match {
        case None => throw new RuntimeException("No postcode returned from vision API")
        case Some(result) => Postcode(result)
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
    config.postcodeCheckerConfig.directWebAddressPrefix  + imageUrlSuffix
  }


  private def getByteArrayFromImage(imageUrl: String): Array[Byte] = {

    val url = new URL(imageUrl)
    val output = new ByteArrayOutputStream
      val inputStream = url.openStream
      try {
        val buffer = new Array[Byte](1024)
        var n = inputStream.read(buffer)
        do {
          output.write(buffer, 0, n)
          n = inputStream.read(buffer)
        } while (n != -1)

      } catch {
        case e: Exception => logger.error(s"Error converting image to byte array", e)
      } finally {
        if (inputStream != null) inputStream.close()
      }
    output.toByteArray
  }
}