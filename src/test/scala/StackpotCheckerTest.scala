//import com.postcodelotterychecker._
//import com.postcodelotterychecker.checkers.StackpotChecker
//import com.typesafe.config.ConfigFactory
//import org.scalatest.{Matchers, fixture}
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.util.Random
//
//class StackpotCheckerTest extends fixture.FunSuite with Matchers {
//
//  case class FixtureParam(stackpotChecker: StackpotChecker, stackpotCheckerConfig: StackpotCheckerConfig, users: List[User])
//
//  def withFixture(test: OneArgTest) = {
//
//    val defaultConfig = ConfigLoader.defaultConfig
//    val testConfig = defaultConfig.copy(
//      s3Config = defaultConfig.s3Config.copy(usersBucketName = ConfigFactory.load().getString("s3.usersBucketName"))
//    )
//
//    val testUsers = new SubscribersFetcher(testConfig.s3Config).getSubscribers
//    val stackpotChecker = new StackpotChecker(testConfig, testUsers)
//    val testFixture = FixtureParam(stackpotChecker, testConfig.stackpotCheckerConfig, testUsers)
//
//    try {
//      withFixture(test.toNoArgTest(testFixture))
//    }
//    finally {
//    }
//  }
//
//  test("Postcodes should be identified from Stackpot web address") { f =>
//
//    noException should be thrownBy f.stackpotChecker.getWinningResult(f.stackpotCheckerConfig.directWebAddressPrefix + f.stackpotCheckerConfig.directWebAddressSuffix + f.stackpotCheckerConfig.uuid)
//  }
//}
//
