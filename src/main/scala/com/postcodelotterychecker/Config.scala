package com.postcodelotterychecker

import com.typesafe.config.ConfigFactory

case class Config(postcodeCheckerConfig: PostcodeCheckerConfig, dinnerCheckerConfig: DinnerCheckerConfig, stackpotCheckerConfig: StackpotCheckerConfig, quidcoHitterConfig: QuidcoHitterConfig, emojiCheckerConfig: EmojiCheckerConfig, visionApiConfig: VisionApiConfig, emailerConfig: EmailerConfig, s3Config: S3Config)
case class VisionApiConfig(apiKey: String)
case class EmailerConfig(fromAddress: String, smtpHost: String, smtpPort: Int, smtpUsername: String, smtpPassword: String)
case class S3Config(usersAddress: String)

case class PostcodeCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String)
case class DinnerCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String)
case class StackpotCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String)
case class QuidcoHitterConfig(directWebAddressPrefix: String, directWebAddressSuffix: String)
case class EmojiCheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String)

object ConfigLoader {

  private val defaultConfigFactory = ConfigFactory.load()

  val defaultConfig: Config = {
    Config(
      PostcodeCheckerConfig(
        defaultConfigFactory.getString("postcodeChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("postcodeChecker.directWebAddressSuffix")
      ),
      DinnerCheckerConfig(
        defaultConfigFactory.getString("dinnerChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("dinnerChecker.directWebAddressSuffix")
      ),
      StackpotCheckerConfig(
        defaultConfigFactory.getString("stackpotChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("stackpotChecker.directWebAddressSuffix")
      ),
      QuidcoHitterConfig(
        defaultConfigFactory.getString("quidcoHitter.directWebAddressPrefix"),
        defaultConfigFactory.getString("quidcoHitter.directWebAddressSuffix")
      ),
      EmojiCheckerConfig(
        defaultConfigFactory.getString("emojiChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("emojiChecker.directWebAddressSuffix")
      ),
      VisionApiConfig(
        defaultConfigFactory.getString("visionApiKey")
      ),
      EmailerConfig(
        defaultConfigFactory.getString("email.fromAddress"),
        defaultConfigFactory.getString("email.smtpHost"),
        defaultConfigFactory.getInt("email.smtpPort"),
        defaultConfigFactory.getString("email.smtpUsername"),
        defaultConfigFactory.getString("email.smtpPassword")
      ),
      S3Config(
        defaultConfigFactory.getString("s3.usersfile")
      )
    )
  }
}