package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.concurrent.{ExecutionContext, Future}

class DinnerChecker(dinnerCheckerConfig: DinnerCheckerConfig, users: List[User])(implicit executionContext: ExecutionContext) extends Checker[List[DinnerUserName]] with StrictLogging {

  override def run: Future[(UserResults, List[DinnerUserName])] = startWithDirectWebAddress

  private def startWithDirectWebAddress = {
    Future {
      logger.info("Dinner Checker: Starting using direct web address")
      val directWebAddress = dinnerCheckerConfig.directWebAddressPrefix + dinnerCheckerConfig.directWebAddressSuffix
      val winnerList = getWinningResult(directWebAddress)
      (processResult(winnerList), winnerList)
    }
  }

  override def getWinningResult(webAddress: String): List[DinnerUserName] = {
    logger.info(s"Dinner Checker: Processing web address: $webAddress")

    val browser = JsoupBrowser()
    val doc = browser.get(webAddress)
    val list = (doc >> texts(".name")).toSet.toList
    logger.info("Winning User names: " + list)
    if (list.isEmpty) throw new RuntimeException("No dinner winners found on webpage")
    list.map(DinnerUserName)
  }

  private def processResult(listOfWinningNames: List[DinnerUserName]): Map[User, Option[Boolean]] = {

    logger.info(s"Winners obtained from webpage: $listOfWinningNames")
    users.map(user => {
      user -> user.dinnerUsersWatching.map(watching => {
        watching.intersect(listOfWinningNames).nonEmpty
      })
    }).toMap
  }
}