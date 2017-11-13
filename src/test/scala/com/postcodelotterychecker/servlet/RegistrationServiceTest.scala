package com.postcodelotterychecker.servlet

import java.net.URLEncoder
import java.util.UUID
import cats.effect.IO
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.db.SubscriberSchema
import com.postcodelotterychecker.db.sql.{PostgresDB, SubscribersTable}
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode, Subscriber}
import com.postcodelotterychecker.servlet.ServletTypes.{EveryDay, JsonResponse, NotifyWhen, OnlyWhenWon}
import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, fixture}
import io.circe.parser._
import io.circe.generic.auto._

import scala.concurrent.duration._

class RegistrationServiceTest extends fixture.FlatSpec with ScalaFutures with Matchers {

  override implicit val patienceConfig = PatienceConfig(
    timeout = scaled(1 minute),
    interval = scaled(1 second)
  )
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  case class FixtureParam(subscribersTable: SubscribersTable, httpClient: Client[IO])

  val config = ConfigLoader.defaultConfig.postgresDBConfig
  val subscribersTestSchema = SubscriberSchema(tableName = "subscriberstest")


  override protected def withFixture(test: OneArgTest) = {

    val sqlDB = new PostgresDB(config)
    val subscribersTable = new SubscribersTable(sqlDB, subscribersTestSchema, createNewTable = true)
    val registrationService = new RegistrationService(subscribersTable)
    val httpService = registrationService.service
    val client = Client.fromHttpService(httpService)
    val testFixture = FixtureParam(subscribersTable, client)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      subscribersTable.dropTable.unsafeRunSync()
    }
  }

  it should "return a 400 if body is in unknown format" in { f =>

    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString("/register")
    ).withBody("Unknown String")
    f.httpClient.status(request).unsafeRunSync().code shouldBe 400
  }
  it should "return a 200 if body is in expected format" in { f =>

    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString("/register")
    ).withBody(generateIncomingBody())
    f.httpClient.status(request).unsafeRunSync().code shouldBe 200
  }

  it should "persist subscriber to DB" in { f =>

    val subscriberEmail = "subscriber1@gmail.com"
    val notifyWhen = OnlyWhenWon
    val postcodesWatching = List("AB18SH")
    val dinnerUsersWatching = List("DinnerUser1")
    val emojiSetsWatching = List(Set("aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee"))

    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString("/register")
    ).withBody(generateIncomingBody(subscriberEmail, notifyWhen, postcodesWatching, dinnerUsersWatching, emojiSetsWatching))
    f.httpClient.status(request).unsafeRunSync().code shouldBe 200
    val resultsFromDB = f.subscribersTable.getSubscribers().unsafeRunSync()
    resultsFromDB should have size 1
    resultsFromDB.head.email shouldBe subscriberEmail
    resultsFromDB.head.notifyWhen shouldBe notifyWhen
    resultsFromDB.head.postcodesWatching shouldBe Some(postcodesWatching.map(Postcode))
    resultsFromDB.head.dinnerUsersWatching shouldBe Some(dinnerUsersWatching.map(DinnerUserName))
    resultsFromDB.head.emojiSetsWatching shouldBe Some(emojiSetsWatching.map(_.map(Emoji)))
  }

  it should "delete subscriber from DB" in { f =>
    val subscriber = generateSubscriber()
    f.subscribersTable.insertSubscriber(subscriber).unsafeRunSync()
    f.subscribersTable.getSubscribers().unsafeRunSync() should have size 1

    val request = Request[IO](
      method = Method.GET,
      uri = Uri.unsafeFromString(s"/register/remove?uuid=${subscriber.uuid}")
    )
    f.httpClient.status(request).unsafeRunSync().code shouldBe 200
    f.subscribersTable.getSubscribers().unsafeRunSync() should have size 0
  }

  it should "return an error if email is not valid" in { f =>

    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString("/register")
    ).withBody(generateIncomingBody(emailAddress = "NotAnEmail"))
    val response = f.httpClient.expect[String](request).unsafeRunSync()
    val parsedResponse = parseResponseToJsonResponse(response)
    parsedResponse.`type` shouldBe "ERROR"
  }

  it should "return an error if postcode is not valid" in { f =>

    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString("/register")
    ).withBody(generateIncomingBody(postcodesWatching = List("123456")))
    val response = f.httpClient.expect[String](request).unsafeRunSync()
    val parsedResponse = parseResponseToJsonResponse(response)
    parsedResponse.`type` shouldBe "ERROR"
  }

  it should "return a success if postcode contains spaces" in { f =>

    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString("/register")
    ).withBody(generateIncomingBody(postcodesWatching = List("AB15 8SD")))
    val response = f.httpClient.expect[String](request).unsafeRunSync()
    val parsedResponse = parseResponseToJsonResponse(response)
    parsedResponse.`type` shouldBe "SUCCESS"
  }

  it should "return an error if emoji sets are not complete" in { f =>

    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString("/register")
    ).withBody(generateIncomingBody(emojiSetsWatching = List(Set("aaaaa"))))
    val response = f.httpClient.expect[String](request).unsafeRunSync()
    val parsedResponse = parseResponseToJsonResponse(response)
    parsedResponse.`type` shouldBe "ERROR"
  }

  def generateIncomingBody(emailAddress: String = "test@gmail.com",
                           notifyWhen: NotifyWhen = EveryDay,
                           postcodesWatching: List[String] = List("ABC123", "BCD234"),
                           dinnerUsersWatching: List[String] = List("TestUser1", "TestUser2"),
                           emojiSetsWatching: List[Set[String]] = List(Set("aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee"))) = {
    s"emailAddress=${URLEncoder.encode(emailAddress, "UTF-8")}" +
    s"&whenToNotify=${notifyWhen.value}" +
    s"${postcodesWatching.map{postcode =>
      s"&postcodesWatching%5B%5D=${URLEncoder.encode(postcode, "UTF-8")}"
    }.mkString}" +
      s"${dinnerUsersWatching.map{dinnerUser =>
        s"&dinnerUsersWatching%5B%5D=${URLEncoder.encode(dinnerUser, "UTF-8")}"
      }.mkString}" +
      s"${emojiSetsWatching.map{emojiSet =>
        emojiSet.zipWithIndex.map { case (emoji, index)  =>
          s"&emojiSetsWatching${('@' + index + 1).toChar}%5B%5D=${URLEncoder.encode(emoji, "UTF-8")}"
        }.mkString
      }.mkString}"
  }

  private def parseResponseToJsonResponse(str: String): JsonResponse =
    parse(str).right.get.as[JsonResponse].right.get


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
