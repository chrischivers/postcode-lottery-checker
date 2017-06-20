import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class SurveyDrawCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(surveyDrawChecker: SurveyDrawChecker, restitoServer: RestitoServer, surveyDrawCheckerConfig: SurveyDrawCheckerConfig, users: List[User])
  val winningPostcode = Postcode("HD58NA")

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
    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val surveyDrawChecker = new SurveyDrawChecker(testConfig.surveyDrawCheckerConfig, users)
    val testFixture = FixtureParam(surveyDrawChecker, restitoServer, testConfig.surveyDrawCheckerConfig, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("Postcode should be identified from Survey Draw web address") { f =>

    webpageIsRetrieved(f.restitoServer.server, f.surveyDrawCheckerConfig.uuid, "survey-draw/survey-draw-test-webpage.html")

    val postcodeObtained = f.surveyDrawChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.surveyDrawCheckerConfig.directWebAddressSuffix + f.surveyDrawCheckerConfig.uuid)
    postcodeObtained should equal (winningPostcode)
  }

  test("Unknown webpage response should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, f.surveyDrawCheckerConfig.uuid, "survey-draw/invalid-survey-draw-test-webpage.html")

    assertThrows[RuntimeException] {
      f.surveyDrawChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.surveyDrawCheckerConfig.directWebAddressSuffix + f.surveyDrawCheckerConfig.uuid)
    }
  }

  def webpageIsRetrieved(server: StubServer, uuid: String, resourceName: String) = {
    whenHttp(server).`match`(
      get("/survey-draw/"),
      parameter("reminder", uuid))
      .`then`(ok, resourceContent(resourceName))
  }
}

