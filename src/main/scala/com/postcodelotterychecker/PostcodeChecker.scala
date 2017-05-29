package com.postcodelotterychecker

import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

import com.ditcherj.contextio.dto.Message
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.io.Source
import scala.sys.process._

class PostcodeChecker(config: Config) extends StrictLogging {

  val todaysDate = new SimpleDateFormat("dd/MM/yyyy").format(new Date())
  val emailClient = new EmailClient(config.contextIOConfig, config.emailerConfig)

  def startWithEmailChecker = {
    logger.info("Starting using email checker")
    val messageBody = emailClient.getMostRecentMessageBody
    val webAddress = getWebAddressFromMessage(messageBody)
    logger.info(s"Web address $webAddress found in email")
    processWebAddress(webAddress)
  }

  def startWithDirectWebAddress = {
    logger.info("Starting using direct web address")
    val webAddress = config.directWebAddress
    processWebAddress(webAddress)
  }

  private def processWebAddress(webAddress: String) = {
    val outputFileName = "output.png"
      logger.info(s"Processing web address: $webAddress")

      val imageURL = getImageURLFromWebAddress(webAddress)
      logger.info(s"Using image address: $imageURL")

      writeImageToDisk(imageURL, outputFileName)

      val postCodeFromVisionApi = VisionAPI.makeRequest(outputFileName)
      logger.info(s"Postcode obtained from Vision API: $postCodeFromVisionApi")
      postCodeFromVisionApi match {
        case None => logger.error("No postcode returned from vision API")
        case Some(result) =>
          if (config.postcodesToMatch contains result) handleSuccessfulMatch(result)
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

    def getLines(url: String): List[String] = {
      val lines = Source.fromURL(url).getLines().toList
      lines.find(_.contains("The document has moved")) match {
        case None => lines
        case Some(line) => getLines(line.split("\"")(1))
      }
    }

    val imageUrlSuffix = getLines(url).find(_.contains("The current winning postcode")) match {
      case None => throw new RuntimeException("Text 'The current winning postcode' not found in webpage retrieved")
      case Some(line) => line.split("src=\"")(1).split("\"/>")(0)
    }

    logger.info(s"Image url suffix retrieved: $imageUrlSuffix")
    "https://freepostcodelottery.com" + imageUrlSuffix
  }

  private def writeImageToDisk(imageURL: String, outputFileName: String): Unit = {
    logger.info(s"Attempting to write image file $outputFileName to disk")
    new URL(imageURL) #> new File(outputFileName) !!
  }
}
