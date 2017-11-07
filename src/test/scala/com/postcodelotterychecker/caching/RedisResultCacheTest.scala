package com.postcodelotterychecker.caching

import java.util.UUID

import com.postcodelotterychecker._
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode}
import org.scalatest.{FlatSpec, Matchers}


class RedisResultCacheTest extends FlatSpec with Matchers {

  "Redis cache" should "store winning postcode" in new RedisResultCache[Postcode] {

    override val config: RedisConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    override val resultType = PostcodeResultType

    val uuid = UUID.randomUUID().toString
    val postcode = Postcode("TR18HJ")

    cache(uuid,postcode).unsafeRunSync()
    get(uuid).unsafeRunSync() shouldBe Some(postcode)
  }

  it should "store winning dinner users" in new RedisResultCache[List[DinnerUserName]] {

    override val config: RedisConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    override val resultType = DinnerResultType

    val uuid = UUID.randomUUID().toString
    val dinnerUsers = List(DinnerUserName("user1"), DinnerUserName("user2"))

    cache(uuid, dinnerUsers).unsafeRunSync()
    get(uuid).unsafeRunSync() shouldBe Some(dinnerUsers)

  }

  it should "store winning stackpot postcodes" in new RedisResultCache[List[Postcode]] {

    override val config: RedisConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    override val resultType = StackpotResultType

    val uuid = UUID.randomUUID().toString
    val stackpotPostcodes = List(Postcode("ABC123"), Postcode("BCD234"), Postcode("CDE345"))

    cache(uuid, stackpotPostcodes).unsafeRunSync()
    get(uuid).unsafeRunSync() shouldBe Some(stackpotPostcodes)

  }

  it should "store winning survey draw postcodes" in new RedisResultCache[Postcode] {

    override val config: RedisConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    override val resultType = SurveyDrawResultType

    val uuid = UUID.randomUUID().toString
    val surveyDrawPostcode = Postcode("ABC123")

    cache(uuid, surveyDrawPostcode).unsafeRunSync()
    get(uuid).unsafeRunSync() shouldBe Some(surveyDrawPostcode)

  }

  it should "identify store winning emoji sets to Redis" in new RedisResultCache[Set[Emoji]] {

    override val config: RedisConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1)
    override val resultType = EmojiResultType

    val uuid = UUID.randomUUID().toString
    val emojiSet = Set(Emoji("aaaaa"), Emoji("bbbbb"), Emoji("ccccc"), Emoji("ddddd"), Emoji("eeeee")
    )

    cache(uuid, emojiSet).unsafeRunSync()
    get(uuid).unsafeRunSync() shouldBe Some(emojiSet)

  }

  it should "expire cached records after period of time" in new RedisResultCache[Postcode] {
    import scala.concurrent.duration._

    override val config: RedisConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1, resultsTTL = 2 seconds)
    override val resultType = PostcodeResultType

    val uuid = UUID.randomUUID().toString
    val postcode = Postcode("TR18HJ")

    cache(uuid, postcode).unsafeRunSync()
    get(uuid).unsafeRunSync() shouldBe Some(postcode)
    Thread.sleep(2000)
    get(uuid).unsafeRunSync() shouldBe None

  }
}