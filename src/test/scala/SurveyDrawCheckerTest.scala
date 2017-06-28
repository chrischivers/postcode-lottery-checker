import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global

class SurveyDrawCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(surveyDrawChecker: SurveyDrawChecker, surveyDrawCheckerConfig: SurveyDrawCheckerConfig, users: List[User])
  val winningPostcode = Postcode("B357LQ")

  def withFixture(test: OneArgTest) = {

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )
    val visionAPIClient = new VisionAPIClient(testConfig.visionApiConfig)
    val screenshotAPIClient = new StubScreenshotApiClient(testConfig.screenshotApiConfig)
    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val surveyDrawChecker = new SurveyDrawChecker(testConfig.surveyDrawCheckerConfig, users, visionAPIClient, screenshotAPIClient)
    val testFixture = FixtureParam(surveyDrawChecker, testConfig.surveyDrawCheckerConfig, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
    }
  }

  test("Postcode should be identified from Survey Draw web address") { f =>
    val postcodeObtained = f.surveyDrawChecker.getWinningResult(f.surveyDrawCheckerConfig.directWebAddressPrefix + f.surveyDrawCheckerConfig.directWebAddressSuffix + f.surveyDrawCheckerConfig.uuid)
    postcodeObtained should equal (winningPostcode)
  }


}

