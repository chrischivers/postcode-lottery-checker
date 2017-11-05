package com.postcodelotterychecker.caching

import akka.actor.ActorSystem
import akka.util.ByteString
import cats.Eval
import cats.effect.IO
import com.postcodelotterychecker.RedisConfig
import com.postcodelotterychecker.models.ResultTypes.ResultType
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import redis.{ByteStringDeserializer, ByteStringSerializer}

import scala.concurrent.ExecutionContext

trait RedisResultCache[R] {

  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit val actorSystem: ActorSystem = ActorSystem()

  val resultType: ResultType[R, _]

  implicit lazy val encoder: Encoder[R] = resultType.resultEncoder
  implicit lazy val decoder: Decoder[R] = resultType.resultDecoder

  implicit val serializer = new ByteStringSerializer[R] {

    override def serialize(data: R) = {
      ByteString(data.asJson.noSpaces)
    }
  }

  implicit val deserializer = new ByteStringDeserializer[R] {
    override def deserialize(bs: ByteString) =
      (for {
        parsed <- parse(bs.utf8String).right
        decoded <- parsed.as[R].right
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

  def cache(uuid: String, result: R): IO[Boolean] = {
    IO.fromFuture(Eval.later(client.set(getKey(uuid), result, None, pxMilliseconds = Some(config.resultsTTL.toMillis))))
  }

  def get(uuid: String): IO[Option[R]] = {
    IO.fromFuture(Eval.later(client.get[R](getKey(uuid))))
  }

  private def getKey(uuid: String) = s"$uuid-${resultType.id}"

  def flushDB() = {
    client.flushdb()
  }
}
