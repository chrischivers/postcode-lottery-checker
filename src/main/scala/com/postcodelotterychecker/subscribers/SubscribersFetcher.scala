package com.postcodelotterychecker.subscribers

import cats.effect.IO
import com.postcodelotterychecker.models.Subscriber
import com.typesafe.scalalogging.StrictLogging
import io.circe.parser._

import scala.io.Source

trait SubscribersFetcher extends StrictLogging {

  val subscribersFileName: String

  def getSubscribers: IO[List[Subscriber]] = IO {

    val rawStr = Source.fromResource(subscribersFileName).mkString

    (for {
      parsed <- parse(rawStr).right
      decoded <- parsed.as[List[Subscriber]].right
    } yield decoded) match {
      case Left(e) =>
        logger.error(s"Unable to parse/decode json $rawStr")
        throw e
      case Right(list) => list
    }
  }
}