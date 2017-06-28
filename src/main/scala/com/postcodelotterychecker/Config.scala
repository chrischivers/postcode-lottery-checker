package com.postcodelotterychecker

import com.typesafe.config.ConfigFactory

case class Config(postcodeCheckerConfig: PostcodeCheckerConfig, dinnerCheckerConfig: DinnerCheckerConfig, stackpotCheckerConfig: StackpotCheckerConfig, surveyDrawCheckerConfig: SurveyDrawCheckerConfig, quidcoHitterConfig: QuidcoHitterConfig, emojiCheckerConfig: EmojiCheckerConfig, visionApiConfig: VisionApiConfig, emailerConfig: EmailerConfig, s3Config: S3Config, screenshotApiConfig: ScreenshotApiConfig)
case class VisionApiConfig(apiKey: String)
case class ScreenshotApiConfig(url: String, apiKey: String)
case class EmailerConfig(fromAddress: String, smtpHost: String, smtpPort: Int, smtpUsername: String, smtpPassword: String, numberAttempts: Int, secondsBetweenAttempts: Int)
case class S3Config(usersAddress: String)

case class PostcodeCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String)
case class DinnerCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String)
case class StackpotCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String)
case class SurveyDrawCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String)
case class QuidcoHitterConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String)
case class EmojiCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String)

object ConfigLoader {

  private val defaultConfigFactory = ConfigFactory.load()

  val defaultConfig: Config = {
    Config(
      PostcodeCheckerConfig(
        defaultConfigFactory.getString("postcodeChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("postcodeChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("postcodeChecker.uuid")
      ),
      DinnerCheckerConfig(
        defaultConfigFactory.getString("dinnerChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("dinnerChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("dinnerChecker.uuid")
      ),
      StackpotCheckerConfig(
        defaultConfigFactory.getString("stackpotChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("stackpotChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("stackpotChecker.uuid")
      ),
      SurveyDrawCheckerConfig(
        defaultConfigFactory.getString("surveyDrawChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("surveyDrawChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("surveyDrawChecker.uuid")
      ),
      QuidcoHitterConfig(
        defaultConfigFactory.getString("quidcoHitter.directWebAddressPrefix"),
        defaultConfigFactory.getString("quidcoHitter.directWebAddressSuffix"),
        defaultConfigFactory.getString("quidcoHitter.uuid")
      ),
      EmojiCheckerConfig(
        defaultConfigFactory.getString("emojiChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("emojiChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("emojiChecker.uuid")
      ),
      VisionApiConfig(
        defaultConfigFactory.getString("visionApiKey")
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
        defaultConfigFactory.getString("s3.usersfile")
      ),
      ScreenshotApiConfig(
        defaultConfigFactory.getString("screenshotApi.url"),
        defaultConfigFactory.getString("screenshotApi.apiKey")
      )
    )
  }
}