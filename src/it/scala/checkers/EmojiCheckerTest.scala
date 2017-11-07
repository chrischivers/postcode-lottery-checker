package com.postcodelotterychecker.checkers

import com.postcodelotterychecker._
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.{EmojiChecker, HtmlUnitWebClient}
import com.postcodelotterychecker.models.Emoji
import com.postcodelotterychecker.models.ResultTypes.EmojiResultType
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

trait EmojiCheckerTestSetup {

  val port = 7000 + Random.nextInt(1000)
  val restitoServer = new StubServer(port)
  val urlPrefix = s"http://localhost:$port"
  restitoServer.start()

  val emojiConfig = ConfigLoader.defaultConfig.emojiCheckerConfig.copy(directWebAddressPrefix = urlPrefix)

  val emojiChecker = new EmojiChecker {
    override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
    override val config: CheckerConfig = emojiConfig
    override val redisResultCache = new RedisResultCache[Set[Emoji]] {
      override val resultType = EmojiResultType
      override val config =  ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    }
  }
}


class EmojiCheckerTest extends FlatSpec with Matchers {

 it should "identify winning emojis from webpage" in new EmojiCheckerTestSetup {

    webpageIsRetrieved(restitoServer, emojiConfig.uuid, "emoji/emoji-test-webpage.html")

    emojiChecker.getResult.unsafeRunSync() should contain theSameElementsAs winningEmojisFromPage

  }

  it should "throw an exception when webpage is invalid " in new EmojiCheckerTestSetup  {

    webpageIsRetrieved(restitoServer, emojiConfig.uuid, "emoji/invalid-emoji-test-webpage.html")

    emojiChecker.getResult.attempt.unsafeRunSync() shouldBe 'left
  }

  def webpageIsRetrieved(server: StubServer, uuid: String, resourceName: String) = {
    whenHttp(server).`match`(
      get("/"),
      parameter("uuid", uuid))
      .`then`(ok, resourceContent(resourceName))
  }

  val winningEmojisFromPage: Set[Emoji] = Set("1f60a", "1f609", "1f60d", "1f911", "1f914").map(Emoji(_))
}

