package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.concurrent.{ExecutionContext, Future}

class EmojiChecker(emojiCheckerConfig: EmojiCheckerConfig, users: List[User])(implicit executionContext: ExecutionContext) extends Checker[Set[Emoji]] with StrictLogging {

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

  private def processResult(winningEmojis: Set[Emoji]): Map[User, Option[Boolean]] = {
    users.map(user => {
      user -> user.emojiSetsWatching.map(watchingSets => {
        watchingSets.contains(winningEmojis)
      })
    }).toMap
  }
}