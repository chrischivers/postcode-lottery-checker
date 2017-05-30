package com.postcodelotterychecker

import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import com.typesafe.scalalogging.StrictLogging
import scalaj.http.{Http, HttpOptions}

class PostcodeChecker(config: Config) extends StrictLogging {

  val todaysDate = new SimpleDateFormat("dd/MM/yyyy").format(new Date())
  val emailClient = new EmailClient(config.contextIOConfig, config.emailerConfig)

  def startWithEmailChecker = {
    logger.info("Starting using email checker")
    val messageBody = emailClient.getMostRecentMessageBody("Draw Alert")
    val webAddress = getWebAddressFromMessage(messageBody)
    logger.info(s"Web address $webAddress found in email")
    getPostcodeFromWebAddress(webAddress)
  }

  def startWithDirectWebAddress = {
    logger.info("Starting using direct web address")
    val webAddress = config.postcodeCheckerConfig.directWebAddress
    getPostcodeFromWebAddress(webAddress)
  }

  private def getPostcodeFromWebAddress(webAddress: String) = {
      logger.info(s"Processing web address: $webAddress")

      val imageURL = getImageURLFromWebAddress(webAddress)
      logger.info(s"Using image address: $imageURL")

      val imageByteArray = getByteArrayFromImage(imageURL)

      val postCodeFromVisionApi = VisionAPI.makeRequest(imageByteArray)
      logger.info(s"Postcode obtained from Vision API: $postCodeFromVisionApi")
      postCodeFromVisionApi match {
        case None => logger.error("No postcode returned from vision API")
        case Some(result) =>
          if (config.postcodeCheckerConfig.postcodesToMatch contains result) handleSuccessfulMatch(result)
          else handleUnsuccessfulMatch(result)
      }
    }


  private def handleSuccessfulMatch(winningPostcode: String): Unit = {
    logger.info("Successful match!")
    val email = Email(
      s"Postcode Lottery Checker ($todaysDate): WINNING POSTCODE!",
      s"Postcode $winningPostcode has won!",
      config.emailerConfig.fromAddress,
      config.emailerConfig.toAddress
    )
    emailClient.sendEmail(email)
  }

  private def handleUnsuccessfulMatch(nonWinningPostcode: String) = {

    logger.info("Unsuccessful match!")
    val email = Email(
      s"Postcode Lottery Checker ($todaysDate): You have not won",
      s"Today's winning postcode was $nonWinningPostcode. You have not won.",
      config.emailerConfig.fromAddress,
      config.emailerConfig.toAddress
    )
    emailClient.sendEmail(email)
  }

  private def getWebAddressFromMessage(messageBody: String): String = {

    println(s"Message body: $messageBody")

    val msgLines = messageBody.split("\n")
    val webAddressLine = msgLines.indexWhere(_.startsWith("See if you're a winner")) match {
      case -1 => throw new RuntimeException("Line 'See if you're a winner' not found in email")
      case n => n + 1
    }
    msgLines(webAddressLine).replaceAll("[<> ]", "")
  }

  private def getImageURLFromWebAddress(url: String): String = {

    val response = Http(url).options(HttpOptions.followRedirects(true)).asString
    val responseLines = response.body.split("\n")

    val imageUrlSuffix = responseLines.find(_.contains("The current winning postcode")) match {
      case None => throw new RuntimeException("Text 'The current winning postcode' not found in webpage retrieved")
      case Some(line) => line.split("src=\"")(1).split("\"/>")(0)
    }

    logger.info(s"Image url suffix retrieved: $imageUrlSuffix")
    "https://freepostcodelottery.com" + imageUrlSuffix
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