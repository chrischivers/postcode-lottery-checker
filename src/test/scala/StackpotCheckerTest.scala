import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class StackpotCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(stackpotChecker: StackpotChecker, stackpotCheckerConfig: StackpotCheckerConfig, users: List[User])
  val winningPostcodes = List(Postcode("E61QY"), Postcode("HA27LB"), Postcode("NE311AW"), Postcode("ST78PL"), Postcode("CH16HH"), Postcode("CV312DE"), Postcode("EN106HX"), Postcode("HU74PR"), Postcode("BN235AB"), Postcode("TN23FH"), Postcode("PR23DUU"), Postcode("L335YF"), Postcode("PR4OTS"), Postcode("ML74NG"), Postcode("SNT3LQ"), Postcode("EH44TO"), Postcode("RH68BJ"), Postcode("TN21ONJ"), Postcode("BT152LN"), Postcode("TF29FH"), Postcode("BB8OQU"), Postcode("BT473HN"), Postcode("BT179BB"), Postcode("CB62XE"), Postcode("BD133DY"), Postcode("CH446PD"), Postcode("B762SS"), Postcode("RM176AZ"), Postcode("PA28HS"), Postcode("PE3730"))
  def withFixture(test: OneArgTest) = {

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )

    val visionAPIClient = new VisionAPIClient(testConfig.visionApiConfig)
    val screenshotAPIClient = new StubScreenshotApiClient(testConfig.screenshotApiConfig)
    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val stackpotChecker = new StackpotChecker(testConfig.stackpotCheckerConfig, users, visionAPIClient, screenshotAPIClient)
    val testFixture = FixtureParam(stackpotChecker, testConfig.stackpotCheckerConfig, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
    }
  }

  test("Postcodes should be identified from Stackpot web address") { f =>

    val postcodesObtained = f.stackpotChecker.getWinningResult(f.stackpotCheckerConfig.directWebAddressPrefix + f.stackpotCheckerConfig.directWebAddressSuffix + f.stackpotCheckerConfig.uuid)

    postcodesObtained should contain theSameElementsAs winningPostcodes
  }
}

