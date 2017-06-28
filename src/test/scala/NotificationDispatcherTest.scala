import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action.{ok, resourceContent}
import com.xebialabs.restito.semantics.Condition.{get, parameter}
import com.xebialabs.restito.server.StubServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class NotificationDispatcherTest extends fixture.FunSuite with Matchers with ScalaFutures {

  case class FixtureParam(postcodeChecker: PostcodeChecker, dinnerChecker: DinnerChecker, stackpotChecker: StackpotChecker, emojiChecker: EmojiChecker, restitoServer: RestitoServer, testEmailClient: StubEmailClient, testConfig: Config, notificationDispatcher: NotificationDispatcher, users: List[User])

  val winningPostcodeFromWebpage = Postcode("DL11JU")

  val winnerUsersFromWebpage = List(
    DinnerUserName("Winner1"),
    DinnerUserName("Winner2"),
    DinnerUserName("Winner3"),
    DinnerUserName("Winner4"),
    DinnerUserName("Winner5"),
    DinnerUserName("Winner6"))

  val winningStackpotPostcodes = List(Postcode("E61QY"), Postcode("HA27LB"), Postcode("NE311AW"), Postcode("ST78PL"), Postcode("CH16HH"), Postcode("CV312DE"), Postcode("EN106HX"), Postcode("HU74PR"), Postcode("BN235AB"), Postcode("TN23FH"), Postcode("PR23DUU"), Postcode("L335YF"), Postcode("PR4OTS"), Postcode("ML74NG"), Postcode("SNT3LQ"), Postcode("EH44TO"), Postcode("RH68BJ"), Postcode("TN21ONJ"), Postcode("BT152LN"), Postcode("TF29FH"), Postcode("BB8OQU"), Postcode("BT473HN"), Postcode("BT179BB"), Postcode("CB62XE"), Postcode("BD133DY"), Postcode("CH446PD"), Postcode("B762SS"), Postcode("RM176AZ"), Postcode("PA28HS"), Postcode("PE3730"))

  val winningSurveyDrawPostcode = Postcode("B357LQ")

  val winningEmojis = Set("1f60a", "1f609", "1f60d", "1f911", "1f914").map(Emoji)

  def withFixture(test: OneArgTest) = {

    implicit val patienceConfig = PatienceConfig(2 minutes, 2 seconds)

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      postcodeCheckerConfig = defaultConfig.postcodeCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      dinnerCheckerConfig = defaultConfig.dinnerCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      stackpotCheckerConfig = defaultConfig.stackpotCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      surveyDrawCheckerConfig = defaultConfig.surveyDrawCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      emojiCheckerConfig = defaultConfig.emojiCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )
    val testEmailClient = new StubEmailClient
    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val visionAPIClient = new VisionAPIClient(testConfig.visionApiConfig)
    val screenshotAPIClient = new StubScreenshotApiClient(testConfig.screenshotApiConfig)
    val postcodeChecker = new PostcodeChecker(testConfig.postcodeCheckerConfig, users, visionAPIClient, screenshotAPIClient)
    val dinnerChecker = new DinnerChecker(testConfig.dinnerCheckerConfig, users)
    val stackpotChecker = new StackpotChecker(testConfig.stackpotCheckerConfig, users, visionAPIClient, screenshotAPIClient)
    val surveyDrawChecker = new SurveyDrawChecker(testConfig.surveyDrawCheckerConfig, users, visionAPIClient, screenshotAPIClient)
    val emojiChecker = new EmojiChecker(testConfig.emojiCheckerConfig, users)
    val notificationDispatcher = new NotificationDispatcher(testEmailClient)
    val testFixture = FixtureParam(postcodeChecker, dinnerChecker, stackpotChecker, emojiChecker, restitoServer, testEmailClient, testConfig, notificationDispatcher, users)

    try {
      dinnerWebpageIsRetrieved(restitoServer.server, testConfig.dinnerCheckerConfig.uuid, "dinner/dinner-test-webpage.html")
      surveyDrawWebpageIsRetrieved(restitoServer.server, testConfig.surveyDrawCheckerConfig.uuid, "survey-draw/survey-draw-test-webpage.html")
      emojiWebpageIsRetrieved(restitoServer.server, testConfig.emojiCheckerConfig.uuid, "emoji/emoji-test-webpage.html")

      (for {
        postCodeResults <- postcodeChecker.run
        dinnerResults <- dinnerChecker.run
        stackpotResults <- stackpotChecker.run
        surveyDrawResults <- surveyDrawChecker.run
        emojiResults <- emojiChecker.run
        _ <- notificationDispatcher.dispatchNotifications(users, postCodeResults._1, postCodeResults._2, dinnerResults._1, dinnerResults._2, stackpotResults._1, stackpotResults._2, surveyDrawResults._1, surveyDrawResults._2, emojiResults._1, emojiResults._2)
      } yield ()).futureValue

      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("Correct Number of emails are sent out") { f =>

    f.testEmailClient.emailsSent should have size 10
    f.testEmailClient.emailsSent.filter(_.subject.contains("CONGRATULATIONS YOU HAVE WON")) should have size 6
    f.testEmailClient.emailsSent.filter(_.subject.contains("Sorry, you have not won today")) should have size 4
  }

  test("User's watched values are included in email") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("You are watching the following postcode(s): AA67LJ, EF456GH")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("You are watching the following user(s): testuser1, testuser2")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("1f917")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("1f635")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("1f632")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("1f431")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("1f427")
  }

  test("Actual winning results included in email") { f =>
    winnerUsersFromWebpage.foreach(winningUser => {
      f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include(winningUser.value.toLowerCase)
    })
    winningStackpotPostcodes.foreach(winningPostcode => {
      f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include(winningPostcode.value)
    })
    winningEmojis.foreach(winningEmoji => {
      f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include(winningEmoji.id)
    })

    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include(winningPostcodeFromWebpage.value)
  }

  test("Test scenario where there is no win") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("Emoji Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include("Survey Draw: Not won")
  }

  test("Test scnario where postcode lottery is won") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include("Postcode Lottery: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include("Emoji Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include("Survey Draw: Not won")
  }

  test("Test scenario where dinner is won") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include("Win A Dinner: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include("Emoji Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include("Survey Draw: Not won")
  }

  test("Test scenario where postcode lottery, dinner and emoji lottery are won (not stackpot and survey draw") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include("Postcode Lottery: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include("Win A Dinner: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include("Emoji Lottery: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include("Survey Draw: Not won")
  }

  test("Test scenario where user is only playing the postcode draws") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should include("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should not include "Win A Dinner"
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should not include "Emoji Lottery"
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should include("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should include("Survey Draw: Not won")
  }

  test("Test scenario where user is only playing the dinner draw") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should not include "Postcode Lottery"
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should include("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should not include "Stackpot"
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should not include "Survey Draw"
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should not include "Emoji Lottery"
  }

  test("Test scenario where user wins stackpot") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include("Stackpot: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include("Emoji Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include("Survey Draw: Not won")
  }

  test("Test scenario where user wins stackpot and dinner") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotdinnerwin@test.com")).head.body should include("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotdinnerwin@test.com")).head.body should include("Survey Draw: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotdinnerwin@test.com")).head.body should include("Stackpot: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotdinnerwin@test.com")).head.body should include("Emoji Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotdinnerwin@test.com")).head.body should include("Win A Dinner: WON")
  }

  test("Test scenario where user wins survey draw") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("surveydrawwin@test.com")).head.body should include("Survey Draw: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("surveydrawwin@test.com")).head.body should include("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("surveydrawwin@test.com")).head.body should include("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("surveydrawwin@test.com")).head.body should include("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("surveydrawwin@test.com")).head.body should include("Emoji Lottery: Not won")
  }

  test("Test scenario where user is subscribed to no games") { f =>
    f.testEmailClient.emailsSent.filter(_.to.contains("none@test.com")) should have size 0
  }

  def dinnerWebpageIsRetrieved(server: StubServer, uuid: String, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("uuid", uuid))
      .`then`(ok, resourceContent(resourceName))
  }

  def surveyDrawWebpageIsRetrieved(server: StubServer, uuid: String, resourceName: String) = {
    whenHttp(server).`match`(
      get("/survey-draw/"),
      parameter("reminder", uuid))
      .`then`(ok, resourceContent(resourceName))
  }

  def emojiWebpageIsRetrieved(server: StubServer, uuid: String, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("uuid", uuid))
      .`then`(ok, resourceContent(resourceName))
  }

}