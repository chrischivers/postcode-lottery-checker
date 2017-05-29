package com.postcodelotterychecker

import java.io.File
import java.net.URL

import com.google.api.client.repackaged.org.apache.commons.codec.binary.{Base64, StringUtils}
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.io.Source
import scala.sys.process._

class PostcodeChecker(config: Config) extends StrictLogging {

  private val service = GmailLoader.getGmailService
  private val user = "me"


  def startWithEmailChecker = {
    logger.info("Starting using email checker")
    val message = getMostRecentMessage
    val webAddress = getWebAddressFromMessage(message)
    processWebAddress(webAddress)
  }

  def startWithDirectWebAddress = {
    logger.info("Starting using direct web address")
    val webAddress = config.directWebAddress
    processWebAddress(webAddress)
  }

  private def processWebAddress(webAddress: String) = {
    val outputFileName = "output.png"
      logger.info(s"Using web address: $webAddress")

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
    Emailer.sendEmail(
      config.emailerConfig.toAddress,
      config.emailerConfig.fromAddress,
      "WINNING POSTCODE!", s"Postcode $winningPostcode has won!", service, user)
  }

  private def handleUnsuccessfulMatch(nonWinningPostcode: String) = {
    Emailer.sendEmail(
      config.emailerConfig.toAddress,
      config.emailerConfig.fromAddress,
      "You have not won", s"Today's winning postcode was $nonWinningPostcode", service, user)
  }

  private def getMostRecentMessage: Message = {
    println(service.users.messages().list(user).execute().getMessages.asScala)
    val msgIds = service.users.messages().list(user).execute().getMessages.asScala.map(_.getId)
    val messages = msgIds.map(msgId => {
      service.users().messages().get(user, msgId)
        .setFormat("FULL")
        .execute()
    })
    val mostRecentMessage = messages.filter(messages => messages.getLabelIds.asScala.contains("INBOX")).sortBy(msg => msg.getInternalDate).reverse.head
    logger.info(s"ID of most recent message is ${mostRecentMessage.getId}")
    mostRecentMessage
  }

  private def getWebAddressFromMessage(message: Message): String = {
    val msgLines = message.getPayload.getParts.asScala.foldLeft("")((acc, part) => {
      val str = Base64.decodeBase64(part.getBody.getData)
      acc + StringUtils.newString(str, "UTF-8")
    }).split("\n")
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
