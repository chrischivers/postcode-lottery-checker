package com.postcodelotterychecker.checkers

import java.util.UUID

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.CheckerRequestHandler.Request
import com.postcodelotterychecker.checkers.{HtmlUnitWebClient, SurveyDrawChecker}
import com.postcodelotterychecker.models.Postcode
import com.postcodelotterychecker.models.ResultTypes.SurveyDrawResultType
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, Matchers}

class SurveyDrawCheckerTest extends FlatSpec with Matchers {

  "Survey Draw checker" should "get result and store in cache" in new SurveyDrawChecker {
    override val config = ConfigLoader.defaultConfig.surveyDrawCheckerConfig
    override val htmlUnitWebClient = mock[HtmlUnitWebClient]
    override val redisResultCache = new RedisResultCache[Postcode] {
      override val resultType = SurveyDrawResultType
      override val config = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    }

    val uuid = UUID.randomUUID().toString
    val winningPostcode = Postcode("ABC123")

    override def getResult = IO.pure(winningPostcode)

    handleRequest(Request(uuid), mock[Context])

    redisResultCache.get(uuid).unsafeRunSync() shouldBe Some(winningPostcode)
  }
}


