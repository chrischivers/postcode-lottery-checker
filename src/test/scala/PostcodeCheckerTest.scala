import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class PostcodeCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(postcodeChecker: PostcodeChecker, restitoServer: RestitoServer, testEmailClient: StubEmailClient, testConfig: Config, notificationDispatcher: NotificationDispatcher, users: List[User])

  val winningPostcodeFromImage = Postcode("PR67LJ")

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      postcodeCheckerConfig = defaultConfig.postcodeCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )
    val testEmailClient = new StubEmailClient
    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val postcodeChecker = new PostcodeChecker(testConfig, users)
    val notificationDispatcher = new NotificationDispatcher(testEmailClient)
    val testFixture = FixtureParam(postcodeChecker, restitoServer, testEmailClient, testConfig, notificationDispatcher, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("Readable postcode should be identified from Postcode Checker web address") { f =>

    webpageIsRetrieved(f.restitoServer.server, "postcode/postcode-test-webpage.html")
    imageIsRetrieved(f.restitoServer.server, "postcode/test-postcode-image.php")

    val postcodeObtained = f.postcodeChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.testConfig.postcodeCheckerConfig.directWebAddressSuffix)
    postcodeObtained should equal(winningPostcodeFromImage)
  }

  test("Unreadable postcode should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, "postcode/postcode-test-webpage.html")
    imageIsRetrieved(f.restitoServer.server, "postcode/non-readable-postcode-image.php")

    assertThrows[RuntimeException] {
      f.postcodeChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.testConfig.postcodeCheckerConfig.directWebAddressSuffix)
    }
  }

  test("Unknown webpage response should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, "postcode/invalid-postcode-test-webpage.html")

    assertThrows[RuntimeException] {
      f.postcodeChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.testConfig.postcodeCheckerConfig.directWebAddressSuffix)
    }
  }

  def webpageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("reminder", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }

  def imageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/speech/2.php"),
      parameter("s", "4"),
      parameter("amp;v", "1496434635"))
      .`then`(ok, resourceContent(resourceName))
  }
}

