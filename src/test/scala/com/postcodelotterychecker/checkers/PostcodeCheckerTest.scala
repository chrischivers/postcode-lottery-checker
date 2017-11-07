package com.postcodelotterychecker.checkers

import java.util.UUID
import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.Request
import com.postcodelotterychecker.checkers.{HtmlUnitWebClient, PostcodeChecker}
import com.postcodelotterychecker.models.Postcode
import com.postcodelotterychecker.models.ResultTypes.PostcodeResultType
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, Matchers}

class PostcodeCheckerTest extends FlatSpec with Matchers {

  "Postcode checker" should "get result and store in cache" in new PostcodeChecker {
    override val config = ConfigLoader.defaultConfig.postcodeCheckerConfig
    override val htmlUnitWebClient = mock[HtmlUnitWebClient]
    override val redisResultCache = new RedisResultCache[Postcode] {
      override val resultType = PostcodeResultType
      override val config = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    }

    val uuid = UUID.randomUUID().toString
    val winningPostcode = Postcode("TW29UI")

    override def getResult = IO.pure(winningPostcode)

    handleRequest(Request(uuid), mock[Context])

    redisResultCache.get(uuid).unsafeRunSync() shouldBe Some(winningPostcode)
  }
}


