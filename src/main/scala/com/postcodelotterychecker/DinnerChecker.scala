package com.postcodelotterychecker

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object DinnerCheckerHandlerManual extends App {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val dinnerChecker = new DinnerChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  val result = for {
    dinnerResults <- dinnerChecker.run
  } yield resultsToS3Uploader.uploadDinnerCheckerResults(dinnerResults._1, dinnerResults._2, "1234567")

  Await.result(result, lambdaWaitTime)

}


class DinnerCheckerHandler extends RequestHandler[Request, Response] {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val dinnerChecker = new DinnerChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  override def handleRequest(input: Request, context: Context): Response = {

    val result = for {
      dinnerResults <- dinnerChecker.run
    } yield resultsToS3Uploader.uploadDinnerCheckerResults(dinnerResults._1, dinnerResults._2, input.uuid)

    Await.result(result, lambdaWaitTime)
    Response(true)
  }
}

class DinnerChecker(config: Config, users: List[User]) extends Checker[List[DinnerUserName]] {

  val dinnerCheckerConfig: DinnerCheckerConfig = config.dinnerCheckerConfig

  override def run: Future[(UserResults, List[DinnerUserName])] = startWithDirectWebAddress

  private def startWithDirectWebAddress = {
    Future {
      logger.info("Dinner Checker: Starting using direct web address")
      val directWebAddress = dinnerCheckerConfig.directWebAddressPrefix + dinnerCheckerConfig.directWebAddressSuffix + dinnerCheckerConfig.uuid
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
    list.map(str => DinnerUserName(str.toLowerCase))
  }

  private def processResult(listOfWinningNames: List[DinnerUserName]): UserResults = {

    logger.info(s"Winners obtained from webpage: $listOfWinningNames")
    users.map(user => {
      user -> user.dinnerUsersWatching.map(watching => {
        watching.intersect(listOfWinningNames).nonEmpty
      })
    }).toMap
  }
}