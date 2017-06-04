package com.postcodelotterychecker

import cats.syntax.either._
import io.circe._
import io.circe.parser.decode

import scala.io.Source

class UsersFetcher(s3Config: S3Config) {

  private implicit val decodeUser: Decoder[User] = new Decoder[User] {
    final def apply(c: HCursor): Decoder.Result[User] =
      for {
        email <- c.downField("email").as[String]
        postCodes <- c.downField("postCodesWatching").as[Option[List[String]]]
        dinnerUsers <- c.downField("dinnerUsersWatching").as[Option[List[String]]]
      } yield {
        User(email, postCodes.map(_.map(Postcode)), dinnerUsers.map(_.map(DinnerUserName)))
      }
  }

  private implicit val decodeUsers = Decoder[List[User]].prepare(
    _.downField("users"))

  lazy val getUsers: List[User] = {
    val usersjson = Source.fromURL(s3Config.usersAddress).getLines().mkString

    println(usersjson)
    decode(usersjson)(decodeUsers) match {
      case Left(e) => throw e
      case Right(list) => list
    }
  }
}
