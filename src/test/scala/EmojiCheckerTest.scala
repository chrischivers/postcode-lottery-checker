import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.jsoup.Jsoup
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class EmojiCheckerTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(emojiChecker: EmojiChecker, restitoServer: RestitoServer, emojiCheckerConfig: EmojiCheckerConfig, users: List[User])

  val winningEmojisFromPage: Set[Emoji] = Set("1f60a", "1f609", "1f60d", "1f911", "1f914").map(Emoji)

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      emojiCheckerConfig  = defaultConfig.emojiCheckerConfig.copy(directWebAddressPrefix = urlPrefix),
      s3Config = defaultConfig.s3Config.copy(usersBucketName = ConfigFactory.load().getString("s3.usersBucketName"))
    )
    val testUsers = new UsersFetcher(testConfig.s3Config).getUsers
    val emojiChecker = new EmojiChecker(testConfig, testUsers)
    val testFixture = FixtureParam(emojiChecker, restitoServer, testConfig.emojiCheckerConfig, testUsers)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("Winning emoji set should be identified from Postcode Checker web address") { f =>

    webpageIsRetrieved(f.restitoServer.server, f.emojiCheckerConfig.uuid, "emoji/emoji-test-webpage.html")

    val emojisObtained = f.emojiChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.emojiCheckerConfig.directWebAddressSuffix + f.emojiCheckerConfig.uuid)
    emojisObtained should contain theSameElementsAs winningEmojisFromPage
  }


  test("Unknown webpage response should throw an exception") { f =>

    webpageIsRetrieved(f.restitoServer.server, f.emojiCheckerConfig.uuid, "emoji/invalid-emoji-test-webpage.html")

    assertThrows[RuntimeException] {
      f.emojiChecker.getWinningResult("http://localhost:" + f.restitoServer.port + f.emojiCheckerConfig.directWebAddressSuffix + f.emojiCheckerConfig.uuid)
    }
  }

  def webpageIsRetrieved(server: StubServer, uuid: String, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("uuid", uuid))
      .`then`(ok, resourceContent(resourceName))
  }
}

