package com.postcodelotterychecker

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object EmojiCheckerHandlerManual extends App {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val emojiChecker = new EmojiChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes

  val result = for {
    emojiResults <- emojiChecker.run
  } yield resultsToS3Uploader.uploadEmojiCheckerResults(emojiResults._1, emojiResults._2, "1234567")

  Await.result(result, lambdaWaitTime)

}


class EmojiCheckerHandler extends RequestHandler[Request, Response] {

  val config: Config = ConfigLoader.defaultConfig
  val users = new UsersFetcher(config.s3Config).getUsers
  val emojiChecker = new EmojiChecker(config, users)
  val resultsToS3Uploader = new ResultsToS3Uploader(config.s3Config)
  val lambdaWaitTime = 4 minutes


  override def handleRequest(input: Request, context: Context): Response = {

    val result = for {
      emojiResults <- emojiChecker.run
    } yield resultsToS3Uploader.uploadEmojiCheckerResults(emojiResults._1, emojiResults._2, input.uuid)

    Await.result(result, lambdaWaitTime)
    Response(true)
  }
}

class EmojiChecker(config: Config, users: List[User]) extends Checker[Set[Emoji]] {

  val emojiCheckerConfig: EmojiCheckerConfig = config.emojiCheckerConfig

  override def run: Future[(UserResults, Set[Emoji])] = startWithDirectWebAddress

  private def startWithDirectWebAddress: Future[(UserResults, Set[Emoji])] = {
    Future {
      logger.info("Emoji Checker: Starting using direct web address")
      val directWebAddress = emojiCheckerConfig.directWebAddressPrefix + emojiCheckerConfig.directWebAddressSuffix + emojiCheckerConfig.uuid
      logger.info(s"using direct web address $directWebAddress")
      val winningEmojis = getWinningResult(directWebAddress)
      logger.info(s"winning emoji sequence obtained: $winningEmojis")
      (processResult(winningEmojis), winningEmojis)
    }
  }

  override def getWinningResult(webAddress: String): Set[Emoji] = {
      logger.info(s"Processing web address: $webAddress")
      val browser = JsoupBrowser()
    val doc = browser.get(webAddress)
    val emojiUrls = (doc >> elementList(".results-panel") >> element(".freemoji-display-name") >> elements(".emojione") >> attrs("data")).flatten.toSet
    val regex = "svg\\/(.*?)\\.svg".r
    emojiUrls.map(url => Emoji(regex.findAllMatchIn(url).next().group(1).toLowerCase))
    }

  private def processResult(winningEmojis: Set[Emoji]): UserResults = {
    users.map(user => {
      user -> user.emojiSetsWatching.map(watchingSets => {
        watchingSets.contains(winningEmojis)
      })
    }).toMap
  }
}