package com.postcodelotterychecker.models

import io.circe._

case class Subscriber(email: String,
                      postcodesWatching: Option[List[Postcode]],
                      dinnerUsersWatching: Option[List[DinnerUserName]],
                      emojiSetsWatching: Option[List[Set[Emoji]]])

object Subscriber {
  implicit val decodeSubscriber: Decoder[Subscriber] = new Decoder[Subscriber] {
    final def apply(c: HCursor): Decoder.Result[Subscriber] =
      for {
        email <- c.downField("email").as[String]
        postcodes <- c.downField("postCodesWatching").as[Option[List[String]]]
        dinnerUsers <- c.downField("dinnerUsersWatching").as[Option[List[String]]]
        emojis <- c.downField("emojisWatching").as[Option[List[Set[String]]]]
      } yield {
        new Subscriber(
          email.toLowerCase,
          postcodes.map(_.map(str => Postcode(str.toUpperCase))),
          dinnerUsers.map(_.map(str => DinnerUserName(str.toLowerCase))),
          emojis.map(_.map(_.map(str => Emoji(str.toLowerCase)))))
      }
  }

  implicit val encodeFoo: Encoder[Subscriber] = new Encoder[Subscriber] {
    final def apply(a: Subscriber): Json = Json.obj(
      ("email", Json.fromString(a.email)),
      ("postCodesWatching", a.postcodesWatching.fold(Json.Null)
            (ps => Json.fromValues(ps.map(p => Json.fromString(p.value))))),
      ("dinnerUsersWatching", a.dinnerUsersWatching.fold(Json.Null)
            (ds => Json.fromValues(ds.map(d => Json.fromString(d.value))))),
      ("emojisWatching", a.emojiSetsWatching.fold(Json.Null)
            (ess => Json.fromValues(ess.map(es => Json.fromValues(es.map(e => Json.fromString(e.id)))))))
    )
  }
}