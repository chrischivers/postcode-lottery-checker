import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class StackpotCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(stackpotChecker: StackpotChecker, stackpotCheckerConfig: StackpotCheckerConfig, users: List[User])

  def withFixture(test: OneArgTest) = {

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )

    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val stackpotChecker = new StackpotChecker(testConfig.stackpotCheckerConfig, users)
    val testFixture = FixtureParam(stackpotChecker, testConfig.stackpotCheckerConfig, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
    }
  }

  test("Postcodes should be identified from Stackpot web address") { f =>

    noException should be thrownBy f.stackpotChecker.getWinningResult(f.stackpotCheckerConfig.directWebAddressPrefix + f.stackpotCheckerConfig.directWebAddressSuffix + f.stackpotCheckerConfig.uuid)
  }
}

