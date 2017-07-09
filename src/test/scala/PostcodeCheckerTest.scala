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

  case class FixtureParam(postcodeChecker: PostcodeChecker, postcodeCheckerConfig: PostcodeCheckerConfig, users: List[User])

  def withFixture(test: OneArgTest) = {

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )
    val users = new UsersFetcher(testConfig.s3Config).getUsers

    val postcodeChecker = new PostcodeChecker(testConfig.postcodeCheckerConfig, users)
    val testFixture = FixtureParam(postcodeChecker, testConfig.postcodeCheckerConfig, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
    }
  }

  test("Valid postcode should be identified from Postcode Checker web address") { f =>

    noException should be thrownBy f.postcodeChecker.getWinningResult(f.postcodeCheckerConfig.directWebAddressPrefix + f.postcodeCheckerConfig.directWebAddressSuffix + f.postcodeCheckerConfig.uuid)
  }
}

