package com.postcodelotterychecker.checkers

import java.util.UUID

import cats.effect.IO
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.models.DinnerUserName
import com.postcodelotterychecker.models.ResultTypes.DinnerResultType
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, Matchers}

class DinnerCheckerTest extends FlatSpec with Matchers {

  "Dinner checker" should "get result and store in cache" in new DinnerChecker {
    override val config = ConfigLoader.defaultConfig.dinnerCheckerConfig
    override val htmlUnitWebClient = mock[HtmlUnitWebClient]
    override val redisResultCache = new RedisResultCache[List[DinnerUserName]] {
      override val resultType = DinnerResultType
      override val config = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    }

    val uuid = UUID.randomUUID().toString
    val winningDinnerUsers = List(DinnerUserName("User1"), DinnerUserName("User2"))

    override def getResult = IO.pure(winningDinnerUsers)

    (for {
      result <- getResult
      _ <- cacheResult(uuid, result)
    } yield ()).unsafeRunSync()

    redisResultCache.get(uuid).unsafeRunSync() shouldBe Some(winningDinnerUsers)
  }
}


