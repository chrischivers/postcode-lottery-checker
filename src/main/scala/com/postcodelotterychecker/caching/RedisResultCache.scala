package com.postcodelotterychecker.caching

import akka.actor.ActorSystem
import akka.util.ByteString
import cats.Eval
import cats.effect.IO
import com.postcodelotterychecker.RedisConfig
import com.postcodelotterychecker.models.CheckerType.CheckerType
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import redis.{ByteStringDeserializer, ByteStringSerializer}

import scala.concurrent.ExecutionContext

trait RedisResultCache[A] {

  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit val actorSystem: ActorSystem = ActorSystem()

  val checkerType: CheckerType[A]

  implicit lazy val encoder: Encoder[A] = checkerType.encoder
  implicit lazy val decoder: Decoder[A] = checkerType.decoder

  implicit val serializer = new ByteStringSerializer[A] {

    override def serialize(data: A) = {
      ByteString(data.asJson.noSpaces)
    }
  }

  implicit val deserializer = new ByteStringDeserializer[A] {
    override def deserialize(bs: ByteString) =
      (for {
        parsed <- parse(bs.utf8String).right
        decoded <- parsed.as[A].right
      } yield decoded) match {
        case Left(e) => throw e
        case Right(p) => p
      }
  }

  val config: RedisConfig

  lazy private val client: redis.RedisClient = {
    val cl = redis.RedisClient(host = config.host, port = config.port)
    cl.select(config.dbIndex)
    cl
  }

  def cache(uuid: String, result: A): IO[Boolean] = {
    IO.fromFuture(Eval.later(client.set(getKey(uuid), result, None, pxMilliseconds = Some(config.resultsTTL.toMillis))))
  }

  def get(uuid: String): IO[Option[A]] = {
    IO.fromFuture(Eval.later(client.get[A](getKey(uuid))))
  }

  private def getKey(uuid: String) = s"$uuid-${checkerType.value}"

  def flushDB() = {
    client.flushdb()
  }
}
