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
    val winnerList = getListOfWinnersFromWebAddress(webAddress)
    processResult(winnerList)
  }

  def startWithDirectWebAddress = {
    logger.info("Dinner Checker: Starting using direct web address")
    val webAddress = config.dinnerCheckerConfig.directWebAddress
    val winnerList = getListOfWinnersFromWebAddress(webAddress)
    processResult(winnerList)
  }

  private def getListOfWinnersFromWebAddress(webAddress: String) = {
    logger.info(s"Dinner Checker: Processing web address: $webAddress")

    val browser = JsoupBrowser()
    val doc = browser.get(webAddress)
    (doc >> texts(".name")).toSet.toList
  }

  private def processResult(listOfWinners: List[String]) = {

    logger.info(s"Winners obtained from webpage: $listOfWinners")
    if (listOfWinners.isEmpty) throw new RuntimeException("No winners returned from website")
    else {
      val winnerLosingUsers = config.dinnerCheckerConfig.users
        .partition(user => listOfWinners.contains(user.username))
      handleWinningUsers(winnerLosingUsers._1)
      handleLosingUsers(winnerLosingUsers._2, listOfWinners)
    }

    def handleWinningUsers(winners: List[DinnerUser]): Unit = {
      winners.foreach(winner => {
        val email = Email(
          s"Dinner Lottery Checker ($todaysDate): WINNING USERNAME!",
          s"Username ${winner.username} has won!",
          config.emailerConfig.fromAddress,
          winner.email
        )
        emailClient.sendEmail(email)
      })
    }

    def handleLosingUsers(losers: List[DinnerUser], actualWinners: List[String]) = {
      losers.foreach(loser => {
        val email = Email(
          s"Dinner Lottery Checker ($todaysDate): You have not won",
          s"Today's winning usernames were ${actualWinners.mkString(", ")}. You have not won.",
          config.emailerConfig.fromAddress,
          loser.email
        )
        emailClient.sendEmail(email)
      })
    }
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