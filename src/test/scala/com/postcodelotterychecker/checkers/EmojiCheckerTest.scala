package com.postcodelotterychecker.checkers

import java.util.UUID

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.Request
import com.postcodelotterychecker.checkers.{EmojiChecker, HtmlUnitWebClient}
import com.postcodelotterychecker.models.Emoji
import com.postcodelotterychecker.models.ResultTypes.EmojiResultType
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, Matchers}

class EmojiCheckerTest extends FlatSpec with Matchers {

  "Emoji checker" should "get result and store in cache" in new EmojiChecker {
    override val config = ConfigLoader.defaultConfig.emojiCheckerConfig
    override val htmlUnitWebClient = mock[HtmlUnitWebClient]
    override val redisResultCache = new RedisResultCache[Set[Emoji]] {
      override val resultType = EmojiResultType
      override val config = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    }

    val uuid = UUID.randomUUID().toString
    val winningEmojiSet = Set(Emoji("aaaaa"), Emoji("bbbbb"), Emoji("ccccc"), Emoji("ddddd"), Emoji("eeeee"))

    override def getResult = IO.pure(winningEmojiSet)

    handleRequest(Request(uuid), mock[Context])

    redisResultCache.get(uuid).unsafeRunSync() shouldBe Some(winningEmojiSet)
  }
}


