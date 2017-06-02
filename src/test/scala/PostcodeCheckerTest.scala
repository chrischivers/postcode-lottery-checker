import com.postcodelotterychecker.{Config, ConfigLoader, PostcodeChecker}
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.{Matchers, fixture}

import scala.util.Random

class PostcodeCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(postcodeChecker: PostcodeChecker, restitoServer: RestitoServer, testConfig: Config)

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(postcodeCheckerConfig = defaultConfig.postcodeCheckerConfig.copy(directWebAddressPrefix = urlPrefix))

    val postcodeChecker = new PostcodeChecker(testConfig)
    val testFixture = FixtureParam(postcodeChecker, restitoServer, testConfig)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("Readable postcode should be identified from Postcode Checker web address") { f =>

    webpageIsRetrieved(f.restitoServer.server, "postcodetestwebpage.html")
    imageIsRetrieved(f.restitoServer.server, "test-postcode-image.php")

    val postcodeObtained = f.postcodeChecker.getPostcodeFromWebAddress("http://localhost:" + f.restitoServer.port + f.testConfig.postcodeCheckerConfig.directWebAddressSuffix)
    postcodeObtained should equal("PR67LJ")
  }

  test("Unreadable postcode should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, "postcodetestwebpage.html")
    imageIsRetrieved(f.restitoServer.server, "non-readable-postcode-image.php")

    assertThrows[RuntimeException] {
      f.postcodeChecker.getPostcodeFromWebAddress("http://localhost:" + f.restitoServer.port + f.testConfig.postcodeCheckerConfig.directWebAddressSuffix)
    }
  }

  def webpageIsRetrieved(server: StubServer,resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("reminder", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }

  def imageIsRetrieved(server: StubServer,resourceName: String) = {
    whenHttp(server).`match`(
      get("/speech/2.php"),
      parameter("s", "4"),
      parameter("amp;v", "1496434635"))
      .`then`(ok, resourceContent(resourceName))
  }
}

