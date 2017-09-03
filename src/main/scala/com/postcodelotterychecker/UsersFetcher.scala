package com.postcodelotterychecker

import awscala.Region
import cats.syntax.either._
import io.circe._
import io.circe.parser.decode
import jp.co.bizreach.s3scala.S3
import scala.io.Source

class UsersFetcher(s3Config: S3Config) {

  implicit private val region = Region.apply(s3Config.region)
  implicit private val s3 = S3(accessKeyId = s3Config.accessKey, secretAccessKey = s3Config.secretAccessKey)

  private implicit val decodeUser: Decoder[User] = new Decoder[User] {
    final def apply(c: HCursor): Decoder.Result[User] =
      for {
        email <- c.downField("email").as[String]
        postCodes <- c.downField("postCodesWatching").as[Option[List[String]]]
        dinnerUsers <- c.downField("dinnerUsersWatching").as[Option[List[String]]]
        emojiSets <- c.downField("emojisWatching").as[Option[List[Set[String]]]]
      } yield {
        User(email.toLowerCase, postCodes.map(_.map(str => Postcode(str.toUpperCase))), dinnerUsers.map(_.map(str => DinnerUserName(str.toLowerCase))), emojiSets.map(_.map(_.map(str => Emoji(str.toLowerCase)))))
      }
  }

  private implicit val decodeUsers = Decoder[List[User]].prepare(
    _.downField("users"))

  lazy val getUsers: List[User] = {

    val usersBucket = s3.bucket(s3Config.usersBucketName).getOrElse(throw new RuntimeException("Unable to locate users bucket"))
    val usersFile = s3.get(usersBucket, "users.json").getOrElse(throw new RuntimeException("Unable to locate users file"))
    val usersJson = Source.fromInputStream(usersFile.content).getLines().mkString

    println(usersJson)
    decode(usersJson)(decodeUsers) match {
      case Left(e) => throw e
      case Right(list) => list
    }
  }
}
