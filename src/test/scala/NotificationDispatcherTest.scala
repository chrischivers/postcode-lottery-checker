import com.postcodelotterychecker.NotificationDispatcher.ResultsBundle
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

  case class FixtureParam(testEmailClient: StubEmailClient, testConfig: Config, notificationDispatcher: NotificationDispatcher, users: List[User])

  def withFixture(test: OneArgTest) = {

    implicit val patienceConfig = PatienceConfig(2 minutes, 2 seconds)

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      s3Config = defaultConfig.s3Config.copy(usersBucketName = ConfigFactory.load().getString("s3.usersBucketName"))
    )
    val testEmailClient = new StubEmailClient
    val users = new UsersFetcher(testConfig.s3Config).getUsers

    val notificationDispatcher = new NotificationDispatcher(testEmailClient)
    val testFixture = FixtureParam(testEmailClient, testConfig, notificationDispatcher, users)

    val postcodeUserList = Map(User("dinnerwin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("nowin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("postcodeonlyplay@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),None,None) -> Some(false), User("allwin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f914"), Emoji("1f60d"), Emoji("1f609"), Emoji("1f60a"), Emoji("1f911"))))) -> Some(true), User("postcodewin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(true), User("stackpotdinnerwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("none@test.com",None,None,None) -> None, User("dinneronlyplay@test.com",None,Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),None) -> None, User("emojionlyplay@test.com",None,None,Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> None, User("surveydrawwin@test.com",Some(List(Postcode("HD58NA"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false))
    val postCodeResults = Postcode("PR67LJ")
    val dinnerUserList = Map(User("dinnerwin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(true), User("nowin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("postcodeonlyplay@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),None,None) -> None, User("allwin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f914"), Emoji("1f60d"), Emoji("1f609"), Emoji("1f60a"), Emoji("1f911"))))) -> Some(true), User("postcodewin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotdinnerwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(true), User("none@test.com",None,None,None) -> None, User("dinneronlyplay@test.com",None,Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),None) -> Some(false), User("emojionlyplay@test.com",None,None,Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> None, User("surveydrawwin@test.com",Some(List(Postcode("HD58NA"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false))
    val dinnerResults = List(DinnerUserName("winner4"), DinnerUserName("winner1"), DinnerUserName("winner5"), DinnerUserName("winner2"), DinnerUserName("winner6"), DinnerUserName("winner3"))
    val stackPotUserList = Map(User("dinnerwin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("nowin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(true), User("postcodeonlyplay@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),None,None) -> Some(false), User("allwin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f914"), Emoji("1f60d"), Emoji("1f609"), Emoji("1f60a"), Emoji("1f911"))))) -> Some(false), User("postcodewin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotdinnerwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(true), User("none@test.com",None,None,None) -> None, User("dinneronlyplay@test.com",None,Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),None) -> None, User("emojionlyplay@test.com",None,None,Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> None, User("surveydrawwin@test.com",Some(List(Postcode("HD58NA"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false))
    val stackpotResults = List(Postcode("NR31TT"), Postcode("YO104DD"), Postcode("SO316LJ"), Postcode("CH35QQ"), Postcode("BT521SS"), Postcode("GY101SB"), Postcode("DN156BA"), Postcode("SL36QA"), Postcode("IG45NF"), Postcode("PL53HW"), Postcode("ST27EB"), Postcode("BS207JP"), Postcode("BT538JY"), Postcode("SG86HL"), Postcode("SE171NJ"), Postcode("PO121GN"), Postcode("OL146QG"), Postcode("PE28TR"), Postcode("SA18RD"), Postcode("WS20DL"), Postcode("GL32AP"))
    val surveyDrawUserList = Map(User("dinnerwin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("nowin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("postcodeonlyplay@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),None,None) -> Some(false), User("allwin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f914"), Emoji("1f60d"), Emoji("1f609"), Emoji("1f60a"), Emoji("1f911"))))) -> Some(false), User("postcodewin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotdinnerwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("none@test.com",None,None,None) -> None, User("dinneronlyplay@test.com",None,Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),None) -> None, User("emojionlyplay@test.com",None,None,Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> None, User("surveydrawwin@test.com",Some(List(Postcode("HD58NA"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(true))
    val surveyDrawResults = Postcode("HD58NA")
    val emojiUserList = Map(User("dinnerwin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("nowin@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("postcodeonlyplay@test.com",Some(List(Postcode("AA67LJ"), Postcode("EF456GH"))),None,None) -> None, User("allwin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f914"), Emoji("1f60d"), Emoji("1f609"), Emoji("1f60a"), Emoji("1f911"))))) -> Some(true), User("postcodewin@test.com",Some(List(Postcode("PR67LJ"), Postcode("EF456GH"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("stackpotdinnerwin@test.com",Some(List(Postcode("NR31TT"))),Some(List(DinnerUserName("winner1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("none@test.com",None,None,None) -> None, User("dinneronlyplay@test.com",None,Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),None) -> None, User("emojionlyplay@test.com",None,None,Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false), User("surveydrawwin@test.com",Some(List(Postcode("HD58NA"))),Some(List(DinnerUserName("testuser1"), DinnerUserName("testuser2"))),Some(List(Set(Emoji("1f917"), Emoji("1f635"), Emoji("1f427"), Emoji("1f431"), Emoji("1f632"))))) -> Some(false))
    val emojiResults = Set(Emoji("1f914"), Emoji("1f60d"), Emoji("1f609"), Emoji("1f60a"), Emoji("1f911"))

    (for {
        _ <- notificationDispatcher.dispatchNotifications(users,
          Some(ResultsBundle(postcodeUserList, postCodeResults)),
          Some(ResultsBundle(dinnerUserList, dinnerResults)),
          Some(ResultsBundle(stackPotUserList, stackpotResults)),
          Some(ResultsBundle(surveyDrawUserList, surveyDrawResults)),
          Some(ResultsBundle(emojiUserList, emojiResults)))
      } yield ()).futureValue

      withFixture(test.toNoArgTest(testFixture))
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