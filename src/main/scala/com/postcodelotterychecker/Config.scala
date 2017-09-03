package com.postcodelotterychecker

import com.typesafe.config.ConfigFactory

case class Config(postcodeCheckerConfig: PostcodeCheckerConfig, dinnerCheckerConfig: DinnerCheckerConfig, stackpotCheckerConfig: StackpotCheckerConfig, surveyDrawCheckerConfig: SurveyDrawCheckerConfig, quidcoHitterConfig: QuidcoHitterConfig, emojiCheckerConfig: EmojiCheckerConfig,  emailerConfig: EmailerConfig, s3Config: S3Config, screenshotApiConfig: ScreenshotApiConfig)
case class ScreenshotApiConfig(url: String, apiKey: String, millisBetweenAttempts: Long)
case class EmailerConfig(fromAddress: String, smtpHost: String, smtpPort: Int, smtpUsername: String, smtpPassword: String, numberAttempts: Int, secondsBetweenAttempts: Int)
case class S3Config(accessKey: String, secretAccessKey: String, region: String, resultsBucketName: String, usersBucketName: String)

case class PostcodeCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String, lambdaTriggerUrl: String)
case class DinnerCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String, lambdaTriggerUrl: String)
case class StackpotCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String, lambdaTriggerUrl: String)
case class SurveyDrawCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String, lambdaTriggerUrl: String)
case class QuidcoHitterConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String, lambdaTriggerUrl: String)
case class EmojiCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String, lambdaTriggerUrl: String)

object ConfigLoader {

  private val defaultConfigFactory = ConfigFactory.load()

  val defaultConfig: Config = {
    Config(
      PostcodeCheckerConfig(
        defaultConfigFactory.getString("postcodeChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("postcodeChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("postcodeChecker.uuid"),
        defaultConfigFactory.getString("postcodeChecker.lambdaTriggerUrl")
      ),
      DinnerCheckerConfig(
        defaultConfigFactory.getString("dinnerChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("dinnerChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("dinnerChecker.uuid"),
        defaultConfigFactory.getString("dinnerChecker.lambdaTriggerUrl")
      ),
      StackpotCheckerConfig(
        defaultConfigFactory.getString("stackpotChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("stackpotChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("stackpotChecker.uuid"),
        defaultConfigFactory.getString("stackpotChecker.lambdaTriggerUrl")
      ),
      SurveyDrawCheckerConfig(
        defaultConfigFactory.getString("surveyDrawChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("surveyDrawChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("surveyDrawChecker.uuid"),
        defaultConfigFactory.getString("surveyDrawChecker.lambdaTriggerUrl")
      ),
      QuidcoHitterConfig(
        defaultConfigFactory.getString("quidcoHitter.directWebAddressPrefix"),
        defaultConfigFactory.getString("quidcoHitter.directWebAddressSuffix"),
        defaultConfigFactory.getString("quidcoHitter.uuid"),
        defaultConfigFactory.getString("quidcoHitter.lambdaTriggerUrl")
      ),
      EmojiCheckerConfig(
        defaultConfigFactory.getString("emojiChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("emojiChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("emojiChecker.uuid"),
        defaultConfigFactory.getString("emojiChecker.lambdaTriggerUrl")
      ),
      EmailerConfig(
        defaultConfigFactory.getString("email.fromAddress"),
        defaultConfigFactory.getString("email.smtpHost"),
        defaultConfigFactory.getInt("email.smtpPort"),
        defaultConfigFactory.getString("email.smtpUsername"),
        defaultConfigFactory.getString("email.smtpPassword"),
        defaultConfigFactory.getInt("email.numberAttempts"),
        defaultConfigFactory.getInt("email.secondsBetweenAttempts")
      ),
      S3Config(
        defaultConfigFactory.getString("s3.accessKey"),
        defaultConfigFactory.getString("s3.secretAccessKey"),
        defaultConfigFactory.getString("s3.region"),
        defaultConfigFactory.getString("s3.resultsBucketName"),
        defaultConfigFactory.getString("s3.usersBucketName")

      ),
      ScreenshotApiConfig(
        defaultConfigFactory.getString("screenshotApi.url"),
        defaultConfigFactory.getString("screenshotApi.apiKey"),
        defaultConfigFactory.getLong("screenshotApi.millisBetweenRequests")
      )
    )
  }
}