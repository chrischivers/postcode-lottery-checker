package com.postcodelotterychecker

import java.util.UUID

import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.db.SubscriberSchema
import com.postcodelotterychecker.db.sql.{PostgresDB, SubscribersTable}
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models._
import com.postcodelotterychecker.results._
import com.postcodelotterychecker.servlet.ServletTypes.EveryDay
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global


class CoordinatorTest extends FlatSpec with SubscriberScenarios with Matchers with BeforeAndAfterAll {

  "Coordinator" should "process results and email them" in new TestCoordinator {

    getDefaultSubscribers().foreach(subscriber => subscribersTable.insertSubscriber(subscriber).unsafeRunSync())

    writeWinningDataToCache(uuid, postcodeResultsCache, dinnerResultsCache, stackpotResultsCache, surveyDrawResultsCache, emojiResultsCache)

    aggregateAndProcessResults().unsafeRunSync()

    stubEmailClient.emailsSent should have size 5

    val subscribers = subscribersTable.getSubscribers().unsafeRunSync()
    subscribers.foreach {subscriber =>
      val email = stubEmailClient.emailsSent.find(_.to == subscriber.email).get
      subscriber.postcodesWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.dinnerUsersWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.emojiSetsWatching.map(watching => email.body should include (watching.map(_.map(_.id).mkString(", ")).mkString("\n")))
    }
    subscribersTable.dropTable.unsafeRunSync()
  }

  it should "process results and email them, retrying to wait for results to arrive" in new TestCoordinator {

    getDefaultSubscribers().foreach(subscriber => subscribersTable.insertSubscriber(subscriber).unsafeRunSync())

    writeWinningDataToCache(uuid, postcodeResultsCache, dinnerResultsCache, stackpotResultsCache, surveyDrawResultsCache, emojiResultsCache, delay = 10000)

    aggregateAndProcessResults().unsafeRunSync()

    stubEmailClient.emailsSent should have size 5

    val subscribers = subscribersTable.getSubscribers().unsafeRunSync()
    subscribers.foreach {subscriber =>
      val email = stubEmailClient.emailsSent.find(_.to == subscriber.email).get
      subscriber.postcodesWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.dinnerUsersWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.emojiSetsWatching.map(watching => email.body should include (watching.map(_.map(_.id).mkString(", ")).mkString("\n")))
      email.body should not include "Result: Unknown"
    }
    subscribersTable.dropTable.unsafeRunSync()
  }

  it should "fail if results take too long to arrive" in new TestCoordinator {

    getDefaultSubscribers().foreach(subscriber => subscribersTable.insertSubscriber(subscriber).unsafeRunSync())

    writeWinningDataToCache(uuid, postcodeResultsCache, dinnerResultsCache, stackpotResultsCache, surveyDrawResultsCache, emojiResultsCache, delay = 20000)

    aggregateAndProcessResults().unsafeRunSync()

    stubEmailClient.emailsSent should have size 5

    val subscribers = subscribersTable.getSubscribers().unsafeRunSync()
    subscribers.foreach {subscriber =>
      val email = stubEmailClient.emailsSent.find(_.to == subscriber.email).get
      println(email.body)
      subscriber.postcodesWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.dinnerUsersWatching.map(watching => email.body should include (watching.map(_.value).mkString(", ")))
      subscriber.emojiSetsWatching.map(watching => email.body should include (watching.map(_.map(_.id).mkString(", ")).mkString("\n")))

      subscriber.postcodesWatching.map(_ => email.body should include ("**Stackpot Lottery**\nResult: Unknown. Please check...."))
      subscriber.postcodesWatching.map(_ => email.body should include ("**Survey Draw Lottery**\nResult: Unknown. Please check...."))
      subscriber.emojiSetsWatching.map(_ => email.body should include ("**Emoji Lottery**\nResult: Unknown. Please check...."))
    }
    subscribersTable.dropTable.unsafeRunSync()
  }
  def getDefaultSubscribers(): List[Subscriber] = {

    val subscriber1 = Subscriber(
      uuid = UUID.randomUUID().toString,
      email = "singlePostcode@test.com",
      notifyWhen = EveryDay,
      postcodesWatching = Some(List(Postcode("ABC123"))),
      dinnerUsersWatching = None,
      emojiSetsWatching = None)

    val subscriber2 = Subscriber(
      uuid = UUID.randomUUID().toString,
      email = "singleDinnerUser@test.com",
      notifyWhen = EveryDay,
      postcodesWatching = None,
      dinnerUsersWatching = Some(List(DinnerUserName("TestUser1"))),
      emojiSetsWatching = None)

    val subscriber3= Subscriber(
      uuid = UUID.randomUUID().toString,
      email = "singleEmojiSet@test.com",
      notifyWhen = EveryDay,
      postcodesWatching = None,
      dinnerUsersWatching = None,
      emojiSetsWatching = Some(List(Set("eeeee","ccccc","bbbbb","ddddd","aaaaa").map(Emoji))))

    val subscriber4= Subscriber(
      uuid = UUID.randomUUID().toString,
      email = "singleEverything@test.com",
      notifyWhen = EveryDay,
      postcodesWatching = Some(List(Postcode("ABC123"))),
      dinnerUsersWatching = Some(List(DinnerUserName("TestUser1"))),
      emojiSetsWatching = Some(List(Set("eeeee","ccccc","bbbbb","ddddd","aaaaa").map(Emoji))))

    val subscriber5 = Subscriber(
      uuid = UUID.randomUUID().toString,
      email = "\"multipleEverything@test.com",
      notifyWhen = EveryDay,
      postcodesWatching = Some(List(Postcode("ABC123"), Postcode("BCD234"))),
      dinnerUsersWatching = Some(List(DinnerUserName("TestUser1"), DinnerUserName("TestUser2"))),
      emojiSetsWatching = Some(List(
        Set("eeeee","ccccc","bbbbb","ddddd","aaaaa").map(Emoji),
        Set("fffff","ggggg","hhhhh","iiiii","jjjjj").map(Emoji))))

    List(subscriber1, subscriber2, subscriber3, subscriber4, subscriber5)
  }
}

class TestCoordinator extends Coordinator {
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  override val mainConfig: Config = ConfigLoader.defaultConfig.copy(
    redisConfig = ConfigLoader.defaultConfig.redisConfig.copy(dbIndex = 1),
    resultsProcessorConfig = ConfigLoader.defaultConfig.resultsProcessorConfig.copy(timeBetweenRetries = 5 seconds, timeCutoff = 15 seconds)
  )

  override val uuid: String = UUID.randomUUID().toString

  val sqlDB = new PostgresDB(mainConfig.postgresDBConfig)
  val subscribersTestSchema = SubscriberSchema(tableName = "subscriberstest")
  override val subscribersTable = new SubscribersTable(sqlDB, subscribersTestSchema, createNewTable = true)

  override val stackpotResultsCache: RedisResultCache[List[Postcode]] = new RedisResultCache[List[Postcode]] {
    override val resultType: ResultType[List[Postcode], _] = StackpotResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }
  override val surveyDrawResultsCache: RedisResultCache[Postcode] = new RedisResultCache[Postcode] {
    override val resultType: ResultType[Postcode, _] = SurveyDrawResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }
  override val dinnerResultsCache: RedisResultCache[List[DinnerUserName]] = new RedisResultCache[List[DinnerUserName]] {
    override val resultType: ResultType[List[DinnerUserName], _] = DinnerResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }
  override val postcodeResultsCache: RedisResultCache[Postcode] = new RedisResultCache[Postcode] {
    override val resultType: ResultType[Postcode, _] = PostcodeResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }
  override val emojiResultsCache: RedisResultCache[Set[Emoji]] = new RedisResultCache[Set[Emoji]] {
    override val resultType: ResultType[Set[Emoji], _] = EmojiResultType
    override val config: RedisConfig = mainConfig.redisConfig
  }

  val stubEmailClient = new StubEmailClient

  override val resultsEmailer: ResultsEmailer = new ResultsEmailer {
    override val emailClient: EmailClient = stubEmailClient
    override val emailerConfig = mainConfig.emailerConfig
  }
  override val resultsProcessor: ResultsProcessor = new ResultsProcessor {
    override val redisConfig: RedisConfig = mainConfig.redisConfig
  }

}




