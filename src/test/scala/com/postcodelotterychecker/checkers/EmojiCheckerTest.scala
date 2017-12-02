package com.postcodelotterychecker.checkers

import java.util.UUID

import cats.effect.IO
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.caching.RedisResultCache
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

    (for {
      result <- getResult
      _ <- cacheResult(uuid, result)
    } yield ()).unsafeRunSync()

    redisResultCache.get(uuid).unsafeRunSync() shouldBe Some(winningEmojiSet)
  }
}


