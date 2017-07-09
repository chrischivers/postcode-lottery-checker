import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global

class SurveyDrawCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(surveyDrawChecker: SurveyDrawChecker, surveyDrawCheckerConfig: SurveyDrawCheckerConfig, users: List[User])

  def withFixture(test: OneArgTest) = {

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )

    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val surveyDrawChecker = new SurveyDrawChecker(testConfig.surveyDrawCheckerConfig, users)
    val testFixture = FixtureParam(surveyDrawChecker, testConfig.surveyDrawCheckerConfig, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
    }
  }

  test("Postcode should be identified from Survey Draw web address") { f =>
    noException should be thrownBy f.surveyDrawChecker.getWinningResult(f.surveyDrawCheckerConfig.directWebAddressPrefix + f.surveyDrawCheckerConfig.directWebAddressSuffix + f.surveyDrawCheckerConfig.uuid)
  }


}

