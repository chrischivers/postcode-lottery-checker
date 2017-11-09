package com.postcodelotterychecker.checkers

import java.io.{File, PrintWriter}

import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode, Subscriber}
import com.postcodelotterychecker.subscribers.SubscribersFetcher
import org.scalatest.{FlatSpec, Matchers}

class SubscribersFetcherTest extends FlatSpec with Matchers {

  it should "read subscriber from Json" in new SubscribersFetcher {

    override val subscribersFileName = "subscribers-test.json"

    val subscriber = Subscriber("test@test.com",
      Some(List(Postcode("ABC123"))),
      Some(List(DinnerUserName("testuser1"))),
      Some(List(Set(Emoji("aaaaa"), Emoji("bbbbb"), Emoji("ccccc"), Emoji("ddddd"), Emoji("eeeee")))))

    writeNewSubscribersFile(subscribersFileName, generateJsonStr(List(subscriber)))

    getSubscribers.unsafeRunSync() shouldBe List(subscriber)
  }

  it should "read subscribers from Json if they are not participating in one competition" in new SubscribersFetcher {

    override val subscribersFileName = "subscribers-test.json"

    val subscriber = Subscriber("test@test.com",
      None,
      Some(List(DinnerUserName("testuser1"))),
      Some(List(Set(Emoji("aaaaa"), Emoji("bbbbb"), Emoji("ccccc"), Emoji("ddddd"), Emoji("eeeee")))))

    writeNewSubscribersFile(subscribersFileName, generateJsonStr(List(subscriber)))

    getSubscribers.unsafeRunSync() shouldBe List(subscriber)
  }

  it should "read multiple subscribers from Json" in new SubscribersFetcher {

    override val subscribersFileName = "subscribers-test.json"

    val subscriber1 = Subscriber("test@test.com",
      None,
      Some(List(DinnerUserName("testuser1"))),
      Some(List(Set(Emoji("aaaaa"), Emoji("bbbbb"), Emoji("ccccc"), Emoji("ddddd"), Emoji("eeeee")))))

    val subscriber2 = Subscriber("test@test.com",
      Some(List(Postcode("ABC123"))),
      Some(List(DinnerUserName("testuser1"))),
      None)

    writeNewSubscribersFile(subscribersFileName, generateJsonStr(List(subscriber1, subscriber2)))

    getSubscribers.unsafeRunSync() shouldBe List(subscriber1, subscriber2)
  }

  def writeNewSubscribersFile(fileName: String, content: String) = {
    val writer = new PrintWriter(new File(getClass.getResource("/").getFile + "/" + fileName))
    writer.write(content)
    writer.close()
  }

  def generateJsonStr(subscribers: List[Subscriber]): String = {
    "[" + subscribers.map(subscriber =>
      s"""
        |    { "email" : "${subscriber.email}",
        |      "postCodesWatching" : ${subscriber.postcodesWatching.fold("null")(lst => "[" + lst.map(p => "\"" + p.value + "\"").mkString(",") + "]")},
        |      "dinnerUsersWatching" : ${subscriber.dinnerUsersWatching.fold("null")(lst => "[" + lst.map(d => "\"" + d.value + "\"").mkString(",") + "]")},
        |      "emojisWatching": ${subscriber.emojiSetsWatching.fold("null")(lst => "[" + lst.map(el =>
                                  s"[${el.map(e => "\"" + e.id + "\"").mkString(",")}]").mkString(",") + "]")}
        |    }
      """.stripMargin).mkString(",") + "]"
  }

}

