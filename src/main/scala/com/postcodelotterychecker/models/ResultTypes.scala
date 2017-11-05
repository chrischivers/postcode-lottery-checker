package com.postcodelotterychecker.models

import com.postcodelotterychecker.models.Competitions._
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object ResultTypes {

  //R Result Type, W Watch Type
  sealed abstract class ResultType[R, W] {
    val id: String
    val competition: Competition
    val resultToString: R => String
    val watchingToString: W => String
    val resultEncoder: Encoder[R]
    val resultDecoder: Decoder[R]
  }

  case object PostcodeResultType extends ResultType[Postcode, List[Postcode]] {

    override val id = "POSTCODE"
    override val competition: Competition = PostcodeCompetition
    override val resultToString: Postcode => String = _.value
    override val watchingToString: List[Postcode] => String = _.map(_.value).mkString(", ")

    implicit val resultEncoder: Encoder[Postcode] = deriveEncoder
    implicit val resultDecoder: Decoder[Postcode] = deriveDecoder

  }

  case object DinnerResultType extends ResultType[List[DinnerUserName], List[DinnerUserName]] {

    override val id = "DINNER"
    override val competition: Competition = DinnerCompetition
    override val resultToString: List[DinnerUserName] => String = _.map(_.value).mkString(", ")
    override val watchingToString: List[DinnerUserName] => String = _.map(_.value).mkString(", ")

    implicit val resultEncoder: Encoder[List[DinnerUserName]] = new Encoder[List[DinnerUserName]] {
      override def apply(a: List[DinnerUserName]): Json =
        Json.fromValues(a.map(dinnerUserName => Json.fromString(dinnerUserName.value)))
    }
    implicit val resultDecoder: Decoder[List[DinnerUserName]] = new Decoder[List[DinnerUserName]] {
      override def apply(c: HCursor): Result[List[DinnerUserName]] =
        c.as[List[String]].map(_.map(DinnerUserName(_)))
    }

  }

  case object EmojiResultType extends ResultType[Set[Emoji], List[Set[Emoji]]] {

    override val id = "EMOJI"
    override val competition: Competition = EmojiCompetition
    override val resultToString: Set[Emoji] => String = _.map(_.id).mkString(", ")
    override val watchingToString: List[Set[Emoji]] => String = _.map(_.map(_.id).mkString(", ")).mkString("\n")

    implicit val resultEncoder: Encoder[Set[Emoji]] =  new Encoder[Set[Emoji]] {
      override def apply(a: Set[Emoji]): Json =
        Json.fromValues(a.map(emoji => Json.fromString(emoji.id)))
    }

    implicit val resultDecoder: Decoder[Set[Emoji]]  = new Decoder[Set[Emoji]] {
      override def apply(c: HCursor): Result[Set[Emoji]] =
        c.as[List[String]].map(_.map(Emoji(_)).toSet)
    }
  }

  case object StackpotResultType extends ResultType[List[Postcode], List[Postcode]] {

    override val id = "STACKPOT"
    override val competition: Competition = StackpotCompetition
    override val resultToString: List[Postcode] => String = _.map(_.value).mkString(", ")
    override val watchingToString: List[Postcode] => String = _.map(_.value).mkString(", ")

    implicit val resultEncoder: Encoder[List[Postcode]] = new Encoder[List[Postcode]] {
      override def apply(a: List[Postcode]): Json =
        Json.fromValues(a.map(postcode => Json.fromString(postcode.value)))
    }
    implicit val resultDecoder: Decoder[List[Postcode]] = new Decoder[List[Postcode]] {
      override def apply(c: HCursor): Result[List[Postcode]] =
        c.as[List[String]].map(_.map(Postcode(_)))
    }

  }

  case object SurveyDrawResultType extends ResultType[List[Postcode], List[Postcode]] {

    override val id = "SURVEYDRAW"
    override val competition: Competition = SurveyDrawCompetition
    override val resultToString: List[Postcode] => String = _.map(_.value).mkString(", ")
    override val watchingToString: List[Postcode] => String = _.map(_.value).mkString(", ")

    implicit val resultEncoder: Encoder[List[Postcode]] = new Encoder[List[Postcode]] {
      override def apply(a: List[Postcode]): Json =
        Json.fromValues(a.map(postcode => Json.fromString(postcode.value)))
    }
    implicit val resultDecoder: Decoder[List[Postcode]] = new Decoder[List[Postcode]] {
      override def apply(c: HCursor): Result[List[Postcode]] =
        c.as[List[String]].map(_.map(Postcode(_)))
    }
  }
}
