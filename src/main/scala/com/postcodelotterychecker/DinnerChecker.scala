package com.postcodelotterychecker

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

class DinnerChecker(config: Config) extends StrictLogging {

  val todaysDate = new SimpleDateFormat("dd/MM/yyyy").format(new Date())
  val emailClient = new EmailClient(config.contextIOConfig, config.emailerConfig)

  def startWithEmailChecker = {
    logger.info("Dinner Checker: Starting using email checker")
    val messageBody = emailClient.getMostRecentMessageBody("Dinner is served")
    val webAddress = getWebAddressFromMessage(messageBody)
    println(s"Web address $webAddress found in email")
    getWinnersFromWebAddress(webAddress)
  }

  def startWithDirectWebAddress = {
    logger.info("Dinner Checker: Starting using direct web address")
    val webAddress = config.dinnerCheckerConfig.directWebAddress
    getWinnersFromWebAddress(webAddress)
  }

  private def getWinnersFromWebAddress(webAddress: String) = {
    logger.info(s"Dinner Checker: Processing web address: $webAddress")

    val browser = JsoupBrowser()
    val doc = browser.get(webAddress)
    val listOfWinners = (doc >> texts(".name")).toSet.toList

    logger.info(s"Winners obtained from webpage: $listOfWinners")
    if (listOfWinners.isEmpty) throw new RuntimeException("No winners returned from website")
    else {
    val matchingUsernames: List[String] = listOfWinners.foldLeft(List[String]())((acc, winner) => {
          if (config.dinnerCheckerConfig.usernamesToMatch.contains(winner)) acc :+ winner
          else acc
        })
    if (matchingUsernames.nonEmpty) handleSuccessfulMatch(matchingUsernames)
    else handleUnsuccessfulMatch(listOfWinners)
    }
  }

  private def handleSuccessfulMatch(matchingUsernames: List[String]): Unit = {
    logger.info("Successful match!")
    val email = Email(
      s"Dinner Lottery Checker ($todaysDate): WINNING USERNAME(S)!",
      s"Username(s) ${matchingUsernames.mkString(", ")} has won!",
      config.emailerConfig.fromAddress,
      config.emailerConfig.toAddress
    )
    emailClient.sendEmail(email)
  }

  private def handleUnsuccessfulMatch(winners: List[String]) = {
    logger.info("Unsuccessful match!")
    val email = Email(
      s"Dinner Lottery Checker ($todaysDate): You have not won",
      s"Today's winning usernames were ${winners.mkString(", ")}. You have not won.",
      config.emailerConfig.fromAddress,
      config.emailerConfig.toAddress
    )
    emailClient.sendEmail(email)
  }

  private def getWebAddressFromMessage(messageBody: String): String = {
    println(s"Message body: $messageBody")

    val msgLines = messageBody.split("\n")
    msgLines.foreach(println)
    val webAddressLine = msgLines.indexWhere(_.startsWith("See if dinner's on us")) match {
      case -1 => throw new RuntimeException("Line 'See if dinner's on us' not found in email")
      case n => n + 1
    }
    msgLines(webAddressLine).replaceAll("[<> ]", "")
  }
}