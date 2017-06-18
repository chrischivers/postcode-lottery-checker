import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action.{ok, resourceContent}
import com.xebialabs.restito.semantics.Condition.{get, parameter}
import com.xebialabs.restito.server.StubServer
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, fixture}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class NotificationDispatcherTest extends fixture.FunSuite with Matchers with ScalaFutures {

  case class FixtureParam(postcodeChecker: PostcodeChecker, dinnerChecker: DinnerChecker, stackpotChecker: StackpotChecker, emojiChecker: EmojiChecker, restitoServer: RestitoServer, testEmailClient: StubEmailClient, testConfig: Config, notificationDispatcher: NotificationDispatcher, users: List[User])

  val winningPostcodeFromImage = Postcode("PR67LJ")

  val winnerUsersFromWebpage = List(
    DinnerUserName("Winner1"),
    DinnerUserName("Winner2"),
    DinnerUserName("Winner3"),
    DinnerUserName("Winner4"),
    DinnerUserName("Winner5"),
    DinnerUserName("Winner6"))

  val winningStackpotPostcodes = List(Postcode("NR31TT"), Postcode("YO104DD"), Postcode("SO316LJ"), Postcode("CH35QQ"), Postcode("BT521SS"), Postcode("GY101SB"), Postcode("DN156BA"), Postcode("SL36QA"), Postcode("IG45NF"), Postcode("PL53HW"), Postcode("ST27EB"), Postcode("BS207JP"), Postcode("BT538JY"), Postcode("SG86HL"), Postcode("SE171NJ"), Postcode("PO121GN"), Postcode("OL146QG"), Postcode("PE28TR"), Postcode("SA18RD"), Postcode("WS20DL"), Postcode("GL32AP"))

  val winningEmojis = Set("1f60a", "1f609", "1f60d", "1f911", "1f914").map(Emoji)

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      postcodeCheckerConfig = defaultConfig.postcodeCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      dinnerCheckerConfig = defaultConfig.dinnerCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      stackpotCheckerConfig = defaultConfig.stackpotCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      emojiCheckerConfig = defaultConfig.emojiCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      s3Config = S3Config(ConfigFactory.load().getString("s3.usersfile"))
    )
    val testEmailClient = new StubEmailClient
    val users = new UsersFetcher(testConfig.s3Config).getUsers
    val postcodeChecker = new PostcodeChecker(testConfig.postcodeCheckerConfig, users)
    val dinnerChecker = new DinnerChecker(testConfig.dinnerCheckerConfig, users)
    val stackpotChecker = new StackpotChecker(testConfig.stackpotCheckerConfig, users)
    val emojiChecker = new EmojiChecker(testConfig.emojiCheckerConfig, users)
    val notificationDispatcher = new NotificationDispatcher(testEmailClient)
    val testFixture = FixtureParam(postcodeChecker, dinnerChecker, stackpotChecker, emojiChecker, restitoServer, testEmailClient, testConfig, notificationDispatcher, users)

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
    stackPotWebpageIsRetrieved(f.restitoServer.server, "stackpot/stackpot-test-webpage.html")
    emojiWebpageIsRetrieved(f.restitoServer.server, "emoji/emoji-test-webpage.html")

    (for {
      postCodeResults <- f.postcodeChecker.run
      dinnerResults <- f.dinnerChecker.run
      stackpotResults <- f.stackpotChecker.run
      emojiResults <- f.emojiChecker.run
      _ <- f.notificationDispatcher.dispatchNotifications(f.users, postCodeResults._1, postCodeResults._2, dinnerResults._1, dinnerResults._2, stackpotResults._1, stackpotResults._2, emojiResults._1, emojiResults._2)
    } yield ()).futureValue

    f.testEmailClient.emailsSent should have size 9
    println(f.testEmailClient.emailsSent)
    f.testEmailClient.emailsSent.filter(_.subject.contains("CONGRATULATIONS YOU HAVE WON")) should have size 5
    f.testEmailClient.emailsSent.filter(_.subject.contains("Sorry, you have not won today")) should have size 4

    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("Emoji Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include (winningPostcodeFromImage.value)
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("AA67LJ, EF456GH")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("testuser1, testuser2")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("1f917")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("1f635")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("1f632")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("1f431")
    f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include ("1f427")

    winnerUsersFromWebpage.foreach(winningUser => {
      f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include (winningUser.value.toLowerCase)
    })
    winningStackpotPostcodes.foreach(winningPostcode => {
      f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include (winningPostcode.value)
    })
    winningEmojis.foreach(winningEmoji => {
      f.testEmailClient.emailsSent.filter(_.to.contains("nowin@test.com")).head.body should include (winningEmoji.id)
    })

    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include ("Postcode Lottery: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include ("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include ("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodewin@test.com")).head.body should include ("Emoji Lottery: Not won")

    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include ("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include ("Win A Dinner: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include ("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinnerwin@test.com")).head.body should include ("Emoji Lottery: Not won")

    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include ("Postcode Lottery: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include ("Win A Dinner: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include ("Stackpot: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("allwin@test.com")).head.body should include ("Emoji Lottery: WON")

    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should include ("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should not include "Win A Dinner"
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should not include "Emoji Lottery"
    f.testEmailClient.emailsSent.filter(_.to.contains("postcodeonlyplay@test.com")).head.body should include ("Stackpot: Not won")

    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should not include "Postcode Lottery"
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should include ("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should not include "Stackpot"
    f.testEmailClient.emailsSent.filter(_.to.contains("dinneronlyplay@test.com")).head.body should not include "Emoji Lottery"

    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include ("Stackpot: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include ("Win A Dinner: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include ("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotwin@test.com")).head.body should include ("Emoji Lottery: Not won")

    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotdinnerwin@test.com")).head.body should include ("Postcode Lottery: Not won")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotdinnerwin@test.com")).head.body should include ("Stackpot: WON")
    f.testEmailClient.emailsSent.filter(_.to.contains("stackpotdinnerwin@test.com")).head.body should include ("Win A Dinner: WON")

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

  def stackPotWebpageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/stackpot/"),
      parameter("reminder", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }

  def emojiWebpageIsRetrieved(server: StubServer, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("uuid", "***REMOVED***"))
      .`then`(ok, resourceContent(resourceName))
  }

}