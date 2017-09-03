import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class DinnerCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(dinnerChecker: DinnerChecker, restitoServer: RestitoServer, dinnerCheckerConfig: DinnerCheckerConfig)

  val winnerUsersFromWebpage = List(
    DinnerUserName("winner1"),
    DinnerUserName("winner2"),
    DinnerUserName("winner3"),
    DinnerUserName("winner4"),
    DinnerUserName("winner5"),
    DinnerUserName("winner6"))

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      dinnerCheckerConfig = defaultConfig.dinnerCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      s3Config = defaultConfig.s3Config.copy(usersBucketName = ConfigFactory.load().getString("s3.usersBucketName")))
    val users = new UsersFetcher(testConfig.s3Config).getUsers

    val dinnerChecker = new DinnerChecker(testConfig, users)

    val testFixture = FixtureParam(dinnerChecker, restitoServer, testConfig.dinnerCheckerConfig)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("list of winning users should be identified from webpage") { f =>

    webpageIsRetrieved(f.restitoServer.server, f.dinnerCheckerConfig.uuid, "dinner/dinner-test-webpage.html")

    val usersObtained = f.dinnerChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.dinnerCheckerConfig.directWebAddressSuffix + f.dinnerCheckerConfig.uuid)
    usersObtained should contain theSameElementsAs winnerUsersFromWebpage
  }

  test("Unknown webpage response should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, f.dinnerCheckerConfig.uuid, "dinner/invalid-dinner-test-webpage.html")

    assertThrows[RuntimeException] {
      f.dinnerChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.dinnerCheckerConfig.directWebAddressSuffix + f.dinnerCheckerConfig.uuid)
    }
  }
  def webpageIsRetrieved(server: StubServer, uuid: String, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("uuid", uuid))
      .`then`(ok, resourceContent(resourceName))
  }
}

