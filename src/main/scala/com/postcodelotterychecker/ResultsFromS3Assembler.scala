package com.postcodelotterychecker

import awscala.Region
import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser.decode
import jp.co.bizreach.s3scala.S3

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.{Failure, Success}
import scalacache.guava.GuavaCache
import scalacache.{NoSerialization, ScalaCache, sync, _}


class ResultsFromS3Assembler(s3Config: S3Config)(implicit executionContext: ExecutionContext) extends StrictLogging {

  private implicit val region = Region.apply(s3Config.region)
  private implicit val s3 = S3(accessKeyId = s3Config.accessKey, secretAccessKey = s3Config.secretAccessKey)
  private implicit  val scalaCache = ScalaCache(GuavaCache())

  private val resultsBucket = s3.bucket(s3Config.resultsBucketName)
    .getOrElse(s3.createBucket(s3Config.resultsBucketName))

  private def getFileContents(fileName: String, uuid: String): Option[String] = {
    logger.info(s"getting file contents for $fileName and uuid: $uuid")
    val fileNameAndUuid = fileName + "-" + uuid

    def getFromS3: Option[String] = {
      resultsBucket.get(fileNameAndUuid).map(file => {
        val content = Source.fromInputStream(file.content).getLines().mkString
        put(fileNameAndUuid)(content).onComplete{
          case Success(_) => logger.info(s"added $fileNameAndUuid to cache")
          case Failure(e) => logger.error(s"unable to add $fileNameAndUuid to cache", e)
        }
        content
      })
    }

    sync.get[String, NoSerialization](fileNameAndUuid).orElse(getFromS3)
  }

  private implicit val decodeUserResult = Decoder[List[(User, Option[Boolean])]]
  private implicit val decodePostcode = Decoder[Postcode]
  private implicit val decodeStackpotPostcodes = Decoder[List[Postcode]]
  private implicit val decodeDinnerUserNames = Decoder[List[DinnerUserName]]
  private implicit val decodeEmojiSet = Decoder[Set[Emoji]]

  def getUserResults(fileName: String, uuid: String): Option[Map[User, Option[Boolean]]] = {
    logger.info(s"attempting to get user results for filename: $fileName and uuid $uuid")

    for {
      json <- getFileContents(fileName, uuid)
      res <- decode(json)(decodeUserResult).toOption
    } yield res.toMap
  }

  def getPostcodeWinningResult(uuid: String): Option[Postcode] = {
    logger.info(s"attempting to get postcode winner for uuid $uuid")
    for {
      json <- getFileContents("postcode-winner", uuid)
      res <- decode(json)(decodePostcode).toOption
    } yield res
  }

  def getStackpotWinningResult(uuid: String): Option[List[Postcode]] = {
    logger.info(s"attempting to get stackpot winner for uuid $uuid")
    for {
      json <- getFileContents("stackpot-winner", uuid)
      res <- decode(json)(decodeStackpotPostcodes).toOption
    } yield res
  }

  def getSurveyDrawWinningResult(uuid: String): Option[Postcode] = {
    logger.info(s"attempting to get survey draw winner for uuid $uuid")
    for {
      json <- getFileContents("survey-draw-winner", uuid)
      res <- decode(json)(decodePostcode).toOption
    } yield res
  }

  def getDinnerWinningResult(uuid: String): Option[List[DinnerUserName]] = {
    logger.info(s"attempting to get dinner winner for uuid $uuid")
    for {
      json <- getFileContents("dinner-winner", uuid)
      res <- decode(json)(decodeDinnerUserNames).toOption
    } yield res
  }

  def getEmojiWinningResult(uuid: String):Option[Set[Emoji]] = {
    logger.info(s"attempting to get emoji winner for uuid $uuid")
    for {
      json <- getFileContents("emoji-winner", uuid)
      res <- decode(json)(decodeEmojiSet).toOption
    } yield res
  }
}

