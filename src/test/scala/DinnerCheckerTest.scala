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
    DinnerUserName("Katyali"),
    DinnerUserName("HethShouse"),
    DinnerUserName("Alex Redmond"),
    DinnerUserName("LaurenCharlotte"),
    DinnerUserName("Littleimpney"),
    DinnerUserName("PastyRoastBeef78"))

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
    usersObtained should equal(winnerUsersFromWebpage)
  }

  test("Unknown webpage response should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, "dinner/invalid-dinner-test-webpage.html")

    assertThrows[RuntimeException] {
      f.dinnerChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.testConfig.dinnerCheckerConfig.directWebAddressSuffix)
    }
  }

//  test("Email is sent on successful match") { f =>
//    val usersPlaying = List(User("test@test.com", Some(List(winningPostcodeFromImage)), None))
//    val postcodeChecker = new PostcodeChecker(f.testConfig, f.testEmailClient, usersPlaying)
//
//    webpageIsRetrieved(f.restitoServer.server, "postcode-test-webpage.html")
//    imageIsRetrieved(f.restitoServer.server, "test-postcode-image.php")
//
//    postcodeChecker.run
//    f.testEmailClient.emailsSent.head.subject should include("WINNING POSTCODE")
//    f.testEmailClient.emailsSent.head.body should include(s"Postcode $winningPostcodeFromImage has won!")
//    f.testEmailClient.emailsSent.head.to should include ("test@test.com")
//  }
////
//  test("Email is sent on unsucessful match") { f =>
//    val usersPlaying = List(PostcodeUser("F89DJF", "test@test.com"))
//    val updatedConfig = f.testConfig.copy(postcodeCheckerConfig = f.testConfig.postcodeCheckerConfig.copy(users = usersPlaying))
//    val postcodeChecker = new PostcodeChecker(updatedConfig, f.testEmailClient)
//
//    webpageIsRetrieved(f.restitoServer.server, "postcode-test-webpage.html")
//    imageIsRetrieved(f.restitoServer.server, "test-postcode-image.php")
//
//    postcodeChecker.startWithDirectWebAddress
//    f.testEmailClient.emailsSent.head.subject should include("You have not won")
//    f.testEmailClient.emailsSent.head.body should include("You have not won")
//    f.testEmailClient.emailsSent.head.body should include(s"winning postcode was $winningPostcodeFromImage")
//    f.testEmailClient.emailsSent.head.to should include ("test@test.com")
//  }

  def webpageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/click.php/e970742/h39771/s121a5583e9/"),
      parameter("uuid", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }
}

