package com.postcodelotterychecker

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.postcodelotterychecker.utils.Utils
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object PostcodeCheckerHandlerManual extends App {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val postcodeChecker = new PostcodeChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

    val result = for {
      postCodeResults <- postcodeChecker.run
    } yield resultsToS3Uploader.uploadPostcodeCheckerResults(postCodeResults._1, postCodeResults._2, "1234567")

    Await.result(result, lambdaWaitTime)
}


class PostcodeCheckerHandler extends RequestHandler[Request, Response] {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val postcodeChecker = new PostcodeChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  override def handleRequest(input: Request, context: Context): Response = {

    val result = for {
      postCodeResults <- postcodeChecker.run
    } yield resultsToS3Uploader.uploadPostcodeCheckerResults(postCodeResults._1, postCodeResults._2, input.uuid)

    Await.result(result, lambdaWaitTime)
    Response(true)
  }
}

class PostcodeChecker(config: Config, users: List[User]) extends Checker[Postcode] {

  private val postcodeCheckerConfig: PostcodeCheckerConfig = config.postcodeCheckerConfig

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

    Utils.retry(totalNumberOfAttempts = 3, secondsBetweenAttempts = 2) {
      val page = htmlUnitWebClient.getPage(webAddress)
//            logger.debug(page.asXml().mkString)
      val text = page.getElementById("result").getElementsByTagName("p").get(0).getTextContent
      logger.info(s"text retrieved $text")
      val trimmedText = text.trim().split("\n").map(_.trim).apply(0)
      logger.info(s"trimmed text retrieved $trimmedText")
      val postcode = Postcode(trimmedText)
      if (Utils.validatePostcodeAgainstRegex(postcode)) Postcode(postcode.value.replace(" ", ""))
      else throw new RuntimeException(s"Postcode $postcode unable to be validated")
    }
  }

  private def processResult(winningPostcode: Postcode): UserResults = {
    users.map(user => {
      user -> user.postCodesWatching.map(watching => {
        watching.contains(winningPostcode)
      })
    }).toMap
  }
}