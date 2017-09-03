package com.postcodelotterychecker

import awscala.Region
import com.amazonaws.services.s3.model.ObjectMetadata
import io.circe.generic.auto._
import io.circe.syntax._
import jp.co.bizreach.s3scala.S3


class ResultsToS3Uploader(s3Config: S3Config) {

  type UserResults = Map[User, Option[Boolean]]

  implicit private val region = Region.apply(s3Config.region)
  implicit private val s3 = S3(accessKeyId = s3Config.accessKey, secretAccessKey = s3Config.secretAccessKey)

  private val resultsBucket = s3.bucket(s3Config.resultsBucketName)
    .getOrElse(s3.createBucket(s3Config.resultsBucketName))

  private def uploadFile(fileName: String, contents: String): Unit = {

    val fileContentsBytes = contents.getBytes
    val metadata = new ObjectMetadata
    metadata.setContentType("application/json")
    metadata.setContentLength(fileContentsBytes.length)
    resultsBucket.putObject(fileName, fileContentsBytes, metadata)
  }


  def uploadPostcodeCheckerResults(userResults: UserResults, winningPostcode: Postcode, uuid: String) = {
    val resultsFileName = s"postcode-results-$uuid"
    val winnerFileName = s"postcode-winner-$uuid"
    uploadFile(resultsFileName, userResults.toList.asJson.noSpaces)
    uploadFile(winnerFileName, winningPostcode.asJson.noSpaces)
  }

  def uploadStackpotCheckerResults(userResults: UserResults, winningPostcodes: List[Postcode], uuid: String) = {
    val resultsFileName = s"stackpot-results-$uuid"
    val winnerFileName = s"stackpot-winner-$uuid"
    uploadFile(resultsFileName, userResults.toList.asJson.noSpaces)
    uploadFile(winnerFileName, winningPostcodes.asJson.noSpaces)
  }

  def uploadSurveyDrawCheckerResults(userResults: UserResults, winningPostcode: Postcode, uuid: String) = {
    val resultsFileName = s"survey-draw-results-$uuid"
    val winnerFileName = s"survey-draw-winner-$uuid"
    uploadFile(resultsFileName, userResults.toList.asJson.noSpaces)
    uploadFile(winnerFileName, winningPostcode.asJson.noSpaces)
  }

  def uploadDinnerCheckerResults(userResults: UserResults, winningDinnerUsers: List[DinnerUserName], uuid: String) = {
    val resultsFileName = s"dinner-results-$uuid"
    val winnerFileName = s"dinner-winner-$uuid"
    uploadFile(resultsFileName, userResults.toList.asJson.noSpaces)
    uploadFile(winnerFileName, winningDinnerUsers.asJson.noSpaces)
  }

  def uploadEmojiCheckerResults(userResults: UserResults, winningEmojiSet: Set[Emoji], uuid: String) = {
    val resultsFileName = s"emoji-results-$uuid"
    val winnerFileName = s"emoji-winner-$uuid"
    uploadFile(resultsFileName, userResults.toList.asJson.noSpaces)
    uploadFile(winnerFileName, winningEmojiSet.asJson.noSpaces)
  }
}
