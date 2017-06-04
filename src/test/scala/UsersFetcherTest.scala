import com.postcodelotterychecker._
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.OptionValues._
import org.scalatest.{Matchers, fixture}

import scala.util.Random

class UsersFetcherTest extends fixture.FunSuite with Matchers {

  case class FixtureParam(restitoServer: RestitoServer, usersFetcher: UsersFetcher)

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val url = "http://localhost:" + port

    val usersFetcher = new UsersFetcher(S3Config(url))
    val testFixture = FixtureParam(restitoServer, usersFetcher)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("UsersFetcher should fetch users json for single user and parse (postcode and dinner)") { f =>

    val usersJson =
      """
        |{
        |  "users": [
        |    { "email" : "test@test.com",
        |      "postCodesWatching" : ["X122YU", "Y349UI"],
        |      "dinnerUsersWatching" : ["testuser1", "testuser2"]
        |    }
        |  ]
        |}
      """.stripMargin

    usersJsonIsRetrieved(usersJson, f.restitoServer.server)
    f.usersFetcher.getUsers should have size 1
    val user = f.usersFetcher.getUsers.head
    user.email shouldBe "test@test.com"
    user.postCodesWatching.value should contain (Postcode("X122YU"))
    user.postCodesWatching.value should contain (Postcode("Y349UI"))
    user.dinnerUsersWatching.value should contain (DinnerUserName("testuser1"))
    user.dinnerUsersWatching.value should contain (DinnerUserName("testuser2"))


  }

  test("UsersFetcher should fetch users json for multiple users and parse (postcode and dinner)") { f =>

    val usersJson =
      """
        |{
        |  "users": [
        |    { "email" : "test@test.com",
        |      "postCodesWatching" : ["X122YU", "Y349UI"],
        |      "dinnerUsersWatching" : ["testuser1", "testuser2"]
        |    },
        |    { "email" : "test2@test.com",
        |      "postCodesWatching" : ["X122YU", "Y349UI"],
        |      "dinnerUsersWatching" : ["testuser1", "testuser2"]
        |    }
        |  ]
        |}
      """.stripMargin

    usersJsonIsRetrieved(usersJson, f.restitoServer.server)


    f.usersFetcher.getUsers should have size 2
    f.usersFetcher.getUsers.head.email shouldBe "test@test.com"
    f.usersFetcher.getUsers(1).email shouldBe "test2@test.com"
    f.usersFetcher.getUsers.foreach(user => {
      user.postCodesWatching.value should contain (Postcode("X122YU"))
      user.postCodesWatching.value should contain (Postcode("Y349UI"))
      user.dinnerUsersWatching.value should contain (DinnerUserName("testuser1"))
      user.dinnerUsersWatching.value should contain (DinnerUserName("testuser2"))
    })
  }


  test("UsersFetcher should fetch users json and parse (postcode only)") { f =>

    val usersJson =
      """
        |{
        |  "users": [
        |    { "email" : "test@test.com",
        |      "postCodesWatching" : ["X122YU", "Y349UI"]
        |    }
        |  ]
        |}
      """.stripMargin

    usersJsonIsRetrieved(usersJson, f.restitoServer.server)

      f.usersFetcher.getUsers should have size 1
      val user = f.usersFetcher.getUsers.head
      user.email shouldBe "test@test.com"
      user.postCodesWatching.value should contain (Postcode("X122YU"))
      user.postCodesWatching.value should contain (Postcode("Y349UI"))
      user.dinnerUsersWatching shouldBe None
  }

  test("UsersFetcher should fetch users json and parse (dinner users only)") { f =>

    val usersJson =
      """
        |{
        |  "users": [
        |    { "email" : "test@test.com",
        |      "dinnerUsersWatching" : ["testuser1", "testuser2"]
        |    }
        |  ]
        |}
      """.stripMargin

    usersJsonIsRetrieved(usersJson, f.restitoServer.server)

      f.usersFetcher.getUsers should have size 1
      val user = f.usersFetcher.getUsers.head
      user.email shouldBe "test@test.com"
      user.dinnerUsersWatching.value should contain (DinnerUserName("testuser1"))
      user.dinnerUsersWatching.value should contain (DinnerUserName("testuser2"))
      user.postCodesWatching shouldBe None
  }


  def usersJsonIsRetrieved(usersJson: String, server: StubServer) = {
    whenHttp(server).`match`(
      get("/"))
      .`then`(ok, stringContent(usersJson))
  }
}
