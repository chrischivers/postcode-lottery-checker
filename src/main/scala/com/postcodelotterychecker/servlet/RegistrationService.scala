package com.postcodelotterychecker.servlet


import java.util.UUID

import cats.data.Validated._
import cats.data.ValidatedNel
import cats.effect.IO
import cats.implicits._
import com.postcodelotterychecker.ConfigLoader
import com.postcodelotterychecker.db.SubscriberSchema
import com.postcodelotterychecker.db.sql.{PostgresDB, SubscribersTable}
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode, Subscriber}
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.commons.validator.routines.EmailValidator
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp
import org.http4s.{Method, _}

import scala.concurrent.ExecutionContext
import scala.util.Properties.envOrNone

case class JsonResponse(`type`: String, message: String)

class RegistrationService(subscribersTable: SubscribersTable)(implicit executionContext: ExecutionContext) extends StrictLogging {

  object UUIDQueryParameter extends QueryParamDecoderMatcher[String]("uuid")

  type ValidationResult[A] = ValidatedNel[String, A]

  private val supportedAssetTypes = List("css", "js", "images")

  val service = HttpService[IO] {
    case request@Method.GET -> Root / "registration" =>
      StaticFile.fromResource("/static/html/registration.html", Some(request))
        .getOrElseF(NotFound())

    case request@POST -> Root / "register" =>
      println(request.as[String].unsafeRunSync())
      request.decode[UrlForm] { m =>
        val email = m.getFirst("emailAddress")
        val postcodesWatching = m.get("postcodesWatching[]").toList.filterNot(_ == "").map(Postcode)
        val dinnerUsersWatching = m.get("dinnerUsersWatching[]").toList.filterNot(_ == "").map(DinnerUserName)
        val emojiSetsWatchingA = m.get("emojiSetsWatchingA[]").toList
        val emojiSetsWatchingB = m.get("emojiSetsWatchingB[]").toList
        val emojiSetsWatchingC = m.get("emojiSetsWatchingC[]").toList
        val emojiSetsWatchingD = m.get("emojiSetsWatchingD[]").toList
        val emojiSetsWatchingE = m.get("emojiSetsWatchingE[]").toList

        val emojiSetsWatching = (emojiSetsWatchingA, emojiSetsWatchingB, emojiSetsWatchingC, emojiSetsWatchingD, emojiSetsWatchingE)
          .mapN(Set(_, _, _, _, _)).map(_.map(Emoji))


        validateInput(email, postcodesWatching, dinnerUsersWatching, emojiSetsWatching).toEither match {
          case Left(errors) =>
            logger.info(s"Errors validating input [${errors.toList.mkString(",")}]")
            Ok(JsonResponse("ERROR", errors.toList.mkString(",")).asJson.noSpaces)
          case Right(subscriber) =>
            logger.debug(s"Successfully validated $subscriber")
            subscribersTable.insertSubscriber(subscriber)
            .flatMap {
              result =>
                if (result.rowsAffected == 0) Ok(JsonResponse("ERROR", s"E: Unable to register ${subscriber.uuid}. Something has gone wrong...").asJson.noSpaces)
                else Ok(JsonResponse("SUCCESS", s"Successfully registered $email").asJson.noSpaces)
            }
        }
      }

    case _@GET -> Root / "register" / "remove" :? UUIDQueryParameter(uuid) =>
      subscribersTable.deleteSubscriber(uuid)
        .flatMap {
          result =>
            if (result.rowsAffected == 0) Ok(JsonResponse("ERROR", s"Error. Id $uuid does not exist").asJson.noSpaces)
            else Ok(JsonResponse("SUCCESS", s"Success. ID $uuid has been unsubscribed").asJson.noSpaces)
        }

    case request@GET -> Root / "assets" / assetType / file if supportedAssetTypes.contains(assetType) =>
      StaticFile.fromResource(s"/static/assets/$assetType/$file", Some(request))
        .getOrElseF(NotFound())
  }

  private def listToOption[A](list: List[A]): Option[List[A]] = {
    if (list.isEmpty) None
    else Some(list)
  }

  def validateInput(email: Option[String], postcodesWatching: List[Postcode], dinnerUsersWatching: List[DinnerUserName], emojiSetsWatching: List[Set[Emoji]]): ValidationResult[Subscriber] = {

    (validateEmail(email),
      validatePostcodes(postcodesWatching),
    validateEmojiSets(emojiSetsWatching)).mapN { case (validatedEmail, validatedPostcodesWatching, validatedEmojiSetsWatching) =>
      val uuid = UUID.randomUUID().toString
      Subscriber(uuid, validatedEmail, listToOption(validatedPostcodesWatching), listToOption(dinnerUsersWatching), listToOption(validatedEmojiSetsWatching))
    }
  }


  private def validateEmail(email: Option[String]): ValidationResult[String] = {

    email.fold[ValidationResult[String]]("No email address found".invalidNel) { email =>
      if (EmailValidator.getInstance().isValid(email)) email.validNel
      else "Invalid email address".invalidNel
    }
  }

  private def validatePostcodes(postcodeList: List[Postcode]): ValidationResult[List[Postcode]] = {
    if (postcodeList.isEmpty) postcodeList.validNel
    else {
      val trimmedPostcodes = postcodeList.map(_.trim)
      if (trimmedPostcodes.forall(_.isValid)) trimmedPostcodes.validNel
      else "Invalid postcodes".invalidNel
    }
  }

  private def validateEmojiSets(emojiSetsList: List[Set[Emoji]]): ValidationResult[List[Set[Emoji]]] = {
    if (emojiSetsList.isEmpty) emojiSetsList.validNel
    else {
      val trimmedEmojiSets = emojiSetsList.map(_.map(x => Emoji(x.id.trim)))
      if (trimmedEmojiSets.forall(_.forall(_ != ""))) trimmedEmojiSets.validNel
      else "Invalid emoji sets".invalidNel
    }
  }
}

object RegistrationService extends StreamApp[IO] with StrictLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = ConfigLoader.defaultConfig.postgresDBConfig
  val sqlDB = new PostgresDB(config)
  val subscribersTable = new SubscribersTable(sqlDB, SubscriberSchema(), createNewTable = false)

  val registrationService = new RegistrationService(subscribersTable)
  val port: Int = envOrNone("HTTP_PORT") map (_.toInt) getOrElse 8080
  val ip: String = "0.0.0.0"

  override def stream(args: List[String], requestShutdown: IO[Unit]) = {
    BlazeBuilder[IO]
      .bindHttp(port, "localhost")
      .mountService(registrationService.service, "/")
      .serve
  }
}
