package com.postcodelotterychecker.db

import java.util.UUID

import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.db.sql.{PostgresDB, SubscribersTable}
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode, Subscriber}
import com.postcodelotterychecker.servlet.RegistrationService
import com.postcodelotterychecker.servlet.ServletTypes.{EveryDay, NotifyWhen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, fixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DBTest extends fixture.FlatSpec with ScalaFutures with Matchers {

  override implicit val patienceConfig = PatienceConfig(
    timeout = scaled(1 minute),
    interval = scaled(1 second)
  )

  case class FixtureParam(subscribersTable: SubscribersTable)

  val config = ConfigLoader.defaultConfig.postgresDBConfig
  val subscribersTestSchema = SubscriberSchema(tableName = "subscriberstest")

  override protected def withFixture(test: OneArgTest) = {

    val sqlDB = new PostgresDB(config)
    val subscribersTable = new SubscribersTable(sqlDB, subscribersTestSchema, createNewTable = true)
    val testFixture = FixtureParam(subscribersTable)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      subscribersTable.dropTable.unsafeRunSync()
    }
  }

  it should "create new subscribers in DB" in { f =>
    val subscriber1 = generateSubscriber()
    val subscriber2 = generateSubscriber()

    f.subscribersTable.insertSubscriber(subscriber1).unsafeRunSync()
    f.subscribersTable.insertSubscriber(subscriber2).unsafeRunSync()
    val subscribersFromDb = f.subscribersTable.getSubscribers().unsafeRunSync()
    subscribersFromDb should contain allElementsOf List(subscriber1, subscriber2)
  }

  it should "create new subscribers in DB without all watching values defined" in { f =>
    val subscriber1 = generateSubscriber(postcodesWatching = None)
    val subscriber2 = generateSubscriber(emojiSetsWatching = None)
    f.subscribersTable.insertSubscriber(subscriber1).unsafeRunSync()
    f.subscribersTable.insertSubscriber(subscriber2).unsafeRunSync()
    val subscribersFromDb = f.subscribersTable.getSubscribers().unsafeRunSync()
    subscribersFromDb should contain allElementsOf List(subscriber1, subscriber2)
  }

  it should "delete subscribers from DB" in { f =>
    val subscriber = generateSubscriber(postcodesWatching = None)
    f.subscribersTable.insertSubscriber(subscriber).unsafeRunSync()
    f.subscribersTable.getSubscribers().unsafeRunSync() should have size 1
    f.subscribersTable.deleteSubscriber(subscriber.uuid).unsafeRunSync()
    f.subscribersTable.getSubscribers().unsafeRunSync() should have size 0
  }

  def generateSubscriber(
                          uuid: String = UUID.randomUUID().toString,
                          email: String = "test@gmail.com",
                          notifyWhen: NotifyWhen = EveryDay,
                          postcodesWatching: Option[List[Postcode]] = Some(List(Postcode("ABC123"), Postcode("BCD234"))),
                          dinnerUsersWatching: Option[List[DinnerUserName]] = Some(List(DinnerUserName("User1"), DinnerUserName("User2"))),
                          emojiSetsWatching: Option[List[Set[Emoji]]] = Some(List(Set(Emoji("aaaaa"), Emoji("bbbbb"), Emoji("ccccc"), Emoji("ddddd"), Emoji("eeeee"))))
                        ): Subscriber =
    Subscriber(uuid, email, notifyWhen, postcodesWatching, dinnerUsersWatching, emojiSetsWatching)

}
