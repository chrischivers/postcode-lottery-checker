package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.{DinnerUserName, Emoji}
import com.postcodelotterychecker.models.ResultTypes.{DinnerResultType, EmojiResultType}
import com.postcodelotterychecker.ConfigLoader
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

trait EmojiChecker extends CheckerRequestHandler[Set[Emoji]] {

  override def getResult: IO[Set[Emoji]] = {
    val webAddress = generateWebAddress
    logger.info(s"Emoji Checker: Starting up using address $webAddress")
    getEmojiListFrom(webAddress)
  }

  private def getEmojiListFrom(webAddress: String): IO[Set[Emoji]] = IO {
    logger.info(s"Emoji Checker: Processing web address: $webAddress")
    val browser = JsoupBrowser()
    val doc = browser.get(webAddress)
    val emojiUrls = (doc >> elementList(".results-panel") >> element(".freemoji-display-name") >> elements(".emojione") >> attrs("data")).flatten.toSet
    val regex = "svg\\/(.*?)\\.svg".r
    emojiUrls.map(url => Emoji(regex.findAllMatchIn(url).next().group(1).toLowerCase))
  }
}

object EmojiChecker extends EmojiChecker {
  override val config = ConfigLoader.defaultConfig.emojiCheckerConfig
  override val htmlUnitWebClient = new HtmlUnitWebClient
  override val redisResultCache = new RedisResultCache[Set[Emoji]] {
    override val resultType = EmojiResultType
    override val config = ConfigLoader.defaultConfig.redisConfig
  }
}