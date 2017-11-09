package com.postcodelotterychecker.checkers

import java.util.UUID

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.Request
import com.postcodelotterychecker.checkers.{HtmlUnitWebClient, StackpotChecker}
import com.postcodelotterychecker.models.Postcode
import com.postcodelotterychecker.models.ResultTypes.StackpotResultType
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, Matchers}

class StackpotCheckerTest extends FlatSpec with Matchers {

  "Stackpot checker" should "get result and store in cache" in new StackpotChecker {
    override val config = ConfigLoader.defaultConfig.stackpotCheckerConfig
    override val htmlUnitWebClient = mock[HtmlUnitWebClient]
    override val redisResultCache = new RedisResultCache[List[Postcode]] {
      override val resultType = StackpotResultType
      override val config = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    }

    val uuid = UUID.randomUUID().toString
    val winningPostcodes = List(Postcode("ABC123"), Postcode("CDE456"), Postcode("EFG678"))

    override def getResult = IO.pure(winningPostcodes)

    (for {
      result <- getResult
      _ <- cacheResult(uuid, result)
    } yield ()).unsafeRunSync()

    redisResultCache.get(uuid).unsafeRunSync() shouldBe Some(winningPostcodes)
  }
}


