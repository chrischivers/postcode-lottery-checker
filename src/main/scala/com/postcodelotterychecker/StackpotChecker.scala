package com.postcodelotterychecker

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.postcodelotterychecker.utils.Utils
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object StackpotCheckerHandlerManual extends App {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val stackpotChecker = new StackpotChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  val result = for {
    stackpotResults <- stackpotChecker.run
  } yield resultsToS3Uploader.uploadStackpotCheckerResults(stackpotResults._1, stackpotResults._2, "1234567")

  Await.result(result, lambdaWaitTime)
}


class StackpotCheckerHandler extends RequestHandler[Request, Response] {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val stackpotChecker = new StackpotChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  override def handleRequest(input: Request, context: Context): Response = {

    val result = for {
      stackpotResults <- stackpotChecker.run
    } yield resultsToS3Uploader.uploadStackpotCheckerResults(stackpotResults._1, stackpotResults._2, input.uuid)

    Await.result(result, lambdaWaitTime)
    Response(true)
  }
}

class StackpotChecker(config: Config, users: List[User]) extends Checker[List[Postcode]] {

  val stackpotCheckerConfig: StackpotCheckerConfig = config.stackpotCheckerConfig

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
    logger.info(s"Processing web address: $webAddress")

    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {
      val page = htmlUnitWebClient.getPage(webAddress)
//      logger.debug(page.asXml().mkString)
      val texts = page.getElementById("result-header").getElementsByTagName("p")
      texts.asScala.toList.map(htmlElem => {
        val text = htmlElem.getTextContent
        logger.info(s"text retrieved $text")
        val trimmedText = text.trim().split("\n").map(_.trim).apply(0)
        logger.info(s"trimmed text retrieved $trimmedText")
        val postcode = Postcode(trimmedText)
        if (Utils.validatePostcodeAgainstRegex(postcode)) Postcode(postcode.value.replace(" ", ""))
        else throw new RuntimeException(s"Postcode $postcode unable to be validated")
      })
    }
  }

  private def processResult(winningPostcodes: List[Postcode]): UserResults = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.intersect(winningPostcodes).nonEmpty
      })
    }).toMap
  }
}