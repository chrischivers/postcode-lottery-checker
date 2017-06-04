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

  case class FixtureParam(dinnerChecker: DinnerChecker, restitoServer: RestitoServer, testEmailClient: StubEmailClient, testConfig: Config)

  val winnerUsersFromWebpage = List(
    DinnerUserName("Winner1"),
    DinnerUserName("Winner2"),
    DinnerUserName("Winner3"),
    DinnerUserName("Winner4"),
    DinnerUserName("Winner5"),
    DinnerUserName("Winner6"))

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      dinnerCheckerConfig = defaultConfig.dinnerCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile")))
    val testEmailClient = new StubEmailClient
    val users = new UsersFetcher(testConfig.s3Config).getUsers

    val dinnerChecker = new DinnerChecker(testConfig, users)
    val testFixture = FixtureParam(dinnerChecker, restitoServer, testEmailClient, testConfig)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("list of winning users should be identified from webpage") { f =>

    webpageIsRetrieved(f.restitoServer.server, "dinner/dinner-test-webpage.html")

    val usersObtained = f.dinnerChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.testConfig.dinnerCheckerConfig.directWebAddressSuffix)
    usersObtained should contain theSameElementsAs winnerUsersFromWebpage
  }

  test("Unknown webpage response should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, "dinner/invalid-dinner-test-webpage.html")

    assertThrows[RuntimeException] {
      f.dinnerChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.testConfig.dinnerCheckerConfig.directWebAddressSuffix)
    }
  }
  def webpageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/click.php/e970742/h39771/s121a5583e9/"),
      parameter("uuid", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }
}

