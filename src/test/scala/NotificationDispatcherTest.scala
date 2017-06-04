import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action.{ok, resourceContent}
import com.xebialabs.restito.semantics.Condition.{get, parameter}
import com.xebialabs.restito.server.StubServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, fixture}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class NotificationDispatcherTest extends fixture.FunSuite with Matchers with ScalaFutures {

  case class FixtureParam(postcodeChecker: PostcodeChecker, dinnerChecker: DinnerChecker, restitoServer: RestitoServer, testEmailClient: StubEmailClient, testConfig: Config, notificationDispatcher: NotificationDispatcher, users: List[User])

  val winningPostcodeFromImage = Postcode("PR67LJ")

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      postcodeCheckerConfig = defaultConfig.postcodeCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      dinnerCheckerConfig = defaultConfig.dinnerCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )
    val testEmailClient = new StubEmailClient
    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val postcodeChecker = new PostcodeChecker(testConfig, users)
    val dinnerChecker = new DinnerChecker(testConfig, users)
    val notificationDispatcher = new NotificationDispatcher(testEmailClient)
    val testFixture = FixtureParam(postcodeChecker, dinnerChecker, restitoServer, testEmailClient, testConfig, notificationDispatcher, users)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("Emails are sent out to all users") { f =>

    implicit val patienceConfig = PatienceConfig(2 minutes, 2 seconds)

    postCodeWebpageIsRetrieved(f.restitoServer.server, "postcode/postcode-test-webpage.html")
    postCodeImageIsRetrieved(f.restitoServer.server, "postcode/test-postcode-image.php")
    dinnerWebpageIsRetrieved(f.restitoServer.server, "dinner/dinner-test-webpage.html")

    (for {
      postCodeResults <- f.postcodeChecker.run
      dinnerResults <- f.dinnerChecker.run
      _ <- f.notificationDispatcher.dispatchNotifications(f.users, postCodeResults, dinnerResults)
    } yield ()).futureValue

    f.testEmailClient.emailsSent should have size 6
    println(f.testEmailClient.emailsSent)
    f.testEmailClient.emailsSent.filter(_.subject.contains("CONGRATULATIONS YOU HAVE WON")) should have size 3
    f.testEmailClient.emailsSent.filter(_.subject.contains("Sorry, you have not won today")) should have size 3

    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("Win A Dinner: Not won")

    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include ("Postcode Lottery: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include ("Win A Dinner: Not won")

    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include ("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include ("Win A Dinner: WON")

    f.testEmailClient.emailsSent.filter(_.to.contains("bothwin@test.com")).head.body should include ("Postcode Lottery: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("bothwin@test.com")).head.body should include ("Win A Dinner: WON")

    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should include ("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should not include "Win A Dinner"

    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should not include "Postcode Lottery"
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should include ("Win A Dinner: Not won")

    f.testEmailClient.emailsSent.filter(_.to.contains("none@test.com")) should have size 0
  }


  def postCodeWebpageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("reminder", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }

  def postCodeImageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/speech/2.php"),
      parameter("s", "4"),
      parameter("amp;v", "1496434635"))
      .`then`(ok, resourceContent(resourceName))
  }

  def dinnerWebpageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/click.php/e970742/h39771/s121a5583e9/"),
      parameter("uuid", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }

}