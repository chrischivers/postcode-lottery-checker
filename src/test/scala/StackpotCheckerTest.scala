import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class StackpotCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(stackpotChecker: StackpotChecker, restitoServer: RestitoServer, testConfig: Config, users: List[User])
  val winningPostcodes = List(Postcode("NR31TT"), Postcode("YO104DD"), Postcode("SO316LJ"), Postcode("CH35QQ"), Postcode("BT521SS"), Postcode("GY101SB"), Postcode("DN156BA"), Postcode("SL36QA"), Postcode("IG45NF"), Postcode("PL53HW"), Postcode("ST27EB"), Postcode("BS207JP"), Postcode("BT538JY"), Postcode("SG86HL"), Postcode("SE171NJ"), Postcode("PO121GN"), Postcode("OL146QG"), Postcode("PE28TR"), Postcode("SA18RD"), Postcode("WS20DL"), Postcode("GL32AP"))
  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      stackpotCheckerConfig = defaultConfig.stackpotCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )
    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val stackpotChecker = new StackpotChecker(testConfig.stackpotCheckerConfig, users)
    val testFixture = FixtureParam(stackpotChecker, restitoServer, testConfig, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("Postcodes should be identified from Stackpot web address") { f =>

    webpageIsRetrieved(f.restitoServer.server, "stackpot/stackpot-test-webpage.html")

    val postcodesObtained = f.stackpotChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.testConfig.postcodeCheckerConfig.directWebAddressSuffix)
    postcodesObtained should contain theSameElementsAs winningPostcodes
  }

  test("Unknown webpage response should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, "stackpot/invalid-stackpot-test-webpage.html")

    assertThrows[RuntimeException] {
      f.stackpotChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.testConfig.postcodeCheckerConfig.directWebAddressSuffix)
    }
  }

  def webpageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("reminder", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }
}

