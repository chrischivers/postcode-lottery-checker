package com.postcodelotterychecker.checkers

import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.{DinnerChecker, HtmlUnitWebClient}
import com.postcodelotterychecker.models.DinnerUserName
import com.postcodelotterychecker.models.ResultTypes.DinnerResultType
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

trait DinnerCheckerTestSetup {

  val port = 7000 + Random.nextInt(1000)
  val restitoServer = new StubServer(port)
  val urlPrefix = s"http://localhost:$port"
  restitoServer.start()

  val dinnerConfig = ConfigLoader.defaultConfig.dinnerCheckerConfig.copy(directWebAddressPrefix = urlPrefix)

  val dinnerChecker = new DinnerChecker {
    override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
    override val config: CheckerConfig = dinnerConfig
    override val redisResultCache = new RedisResultCache[List[DinnerUserName]] {
      override val resultType = DinnerResultType
      override val config = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    }
  }
}

class DinnerCheckerIntegrationTest extends FlatSpec with Matchers {

  it should "identify winning users from webpage" in new DinnerCheckerTestSetup {

    webpageIsRetrieved(restitoServer, dinnerConfig.uuid, "dinner/dinner-test-webpage.html")

    dinnerChecker.getResult.unsafeRunSync() should contain theSameElementsAs  winnerUsersFromWebpage

  }

  it should "throw an exception when webpage is invalid " in new DinnerCheckerTestSetup {

    webpageIsRetrieved(restitoServer, dinnerConfig.uuid, "dinner/invalid-dinner-test-webpage.html")

    dinnerChecker.getResult.attempt.unsafeRunSync() shouldBe 'left
  }

  def webpageIsRetrieved(server: StubServer, uuid: String, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("uuid", uuid))
      .`then`(ok, resourceContent(resourceName))
  }

  val winnerUsersFromWebpage = List(
    DinnerUserName("winner1"),
    DinnerUserName("winner2"),
    DinnerUserName("winner3"),
    DinnerUserName("winner4"),
    DinnerUserName("winner5"),
    DinnerUserName("winner6"))
}

