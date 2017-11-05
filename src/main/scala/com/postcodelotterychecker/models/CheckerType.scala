package com.postcodelotterychecker.models

import io.circe.Decoder.Result
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor, Json}

object CheckerType {
  import models._

  sealed trait CheckerType[A] {
    val value: String
    val encoder: Encoder[A]
    val decoder: Decoder[A]
  }

  case object PostcodeType extends CheckerType[Postcode] {
    override val value = "POSTCODE"

    implicit val encoder: Encoder[Postcode] = deriveEncoder
    implicit val decoder: Decoder[Postcode] = deriveDecoder
  }

  case object DinnerType extends CheckerType[List[DinnerUserName]] {
    override val value = "DINNER"

    implicit val encoder: Encoder[List[DinnerUserName]] = new Encoder[List[DinnerUserName]] {
      override def apply(a: List[DinnerUserName]): Json =
        Json.fromValues(a.map(dinnerUserName => Json.fromString(dinnerUserName.value)))
    }
    implicit val decoder: Decoder[List[DinnerUserName]] = new Decoder[List[DinnerUserName]] {
      override def apply(c: HCursor): Result[List[DinnerUserName]] =
        c.as[List[String]].map(_.map(DinnerUserName(_)))
    }
  }

  case object EmojiType extends CheckerType[Set[Emoji]] {
    override val value = "EMOJI"

    implicit val encoder: Encoder[Set[Emoji]] =  new Encoder[Set[Emoji]] {
      override def apply(a: Set[Emoji]): Json =
        Json.fromValues(a.map(emoji => Json.fromString(emoji.id)))
    }

    implicit val decoder: Decoder[Set[Emoji]]  = new Decoder[Set[Emoji]] {
      override def apply(c: HCursor): Result[Set[Emoji]] =
        c.as[List[String]].map(_.map(Emoji(_)).toSet)
    }
  }

  case object StackpotType extends CheckerType[List[Postcode]] {
    override val value = "STACKPOT"

    implicit val encoder: Encoder[List[Postcode]] = new Encoder[List[Postcode]] {
      override def apply(a: List[Postcode]): Json =
        Json.fromValues(a.map(postcode => Json.fromString(postcode.value)))
    }
    implicit val decoder: Decoder[List[Postcode]] = new Decoder[List[Postcode]] {
      override def apply(c: HCursor): Result[List[Postcode]] =
        c.as[List[String]].map(_.map(Postcode(_)))
    }
  }

  case object SurveyDrawType extends CheckerType[List[Postcode]] {
    override val value = "SURVEYDRAW"

    implicit val encoder: Encoder[List[Postcode]] = new Encoder[List[Postcode]] {
      override def apply(a: List[Postcode]): Json =
        Json.fromValues(a.map(postcode => Json.fromString(postcode.value)))
    }
    implicit val decoder: Decoder[List[Postcode]] = new Decoder[List[Postcode]] {
      override def apply(c: HCursor): Result[List[Postcode]] =
        c.as[List[String]].map(_.map(Postcode(_)))
    }
  }

}
