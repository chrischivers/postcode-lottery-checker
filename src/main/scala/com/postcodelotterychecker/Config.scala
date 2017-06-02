package com.postcodelotterychecker

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

case class Config(postcodeCheckerConfig: PostcodeCheckerConfig, dinnerCheckerConfig: DinnerCheckerConfig, visionApiConfig: VisionApiConfig, emailerConfig: EmailerConfig, contextIOConfig: ContextIoConfig)
case class VisionApiConfig(apiKey: String)
case class PostcodeCheckerConfig(users: List[PostcodeUser], directWebAddressPrefix: String, directWebAddressSuffix: String)
case class DinnerCheckerConfig(users: List[DinnerUser], directWebAddressPrefix: String, directWebAddressSuffix: String)
case class EmailerConfig(fromAddress: String, smtpHost: String, smtpPort: Int, smtpUsername: String, smtpPassword: String)
case class ContextIoConfig(clientKey: String, secret: String, accountId: String, readRetries: Int, timeBetweenRetries: Long)

case class PostcodeUser(postcode: String, email: String)
case class DinnerUser(username: String, email: String)

object ConfigLoader {

  private val defaultConfigFactory = ConfigFactory.load()

  val defaultConfig: Config = {
    Config(
      PostcodeCheckerConfig(
        defaultConfigFactory.getStringList("postcodeChecker.postcodesToMatch").asScala.toList
          .map(str => PostcodeUser(str.split(",")(0), str.split(",")(1))),
        defaultConfigFactory.getString("postcodeChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("postcodeChecker.directWebAddressSuffix")
      ),
      DinnerCheckerConfig(
        defaultConfigFactory.getStringList("dinnerChecker.usernamesToMatch").asScala.toList
          .map(str => DinnerUser(str.split(",")(0), str.split(",")(1))),
        defaultConfigFactory.getString("dinnerChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("dinnerChecker.directWebAddressSuffix")
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
      ContextIoConfig(
        defaultConfigFactory.getString("contextIO.clientKey"),
        defaultConfigFactory.getString("contextIO.secret"),
        defaultConfigFactory.getString("contextIO.accountId"),
        defaultConfigFactory.getInt("contextIO.readRetries"),
        defaultConfigFactory.getLong("contextIO.timeBetweenRetries")
      )
    )
  }
}