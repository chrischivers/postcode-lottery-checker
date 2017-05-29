package com.postcodelotterychecker

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

case class Config(postcodesToMatch: List[String], visionApiConfig: VisionApiConfig, emailerConfig: EmailerConfig, contextIOConfig: ContextIoConfig, directWebAddress: String)
case class VisionApiConfig(apiKey: String)
case class EmailerConfig(fromAddress: String, toAddress: String, smtpHost: String, smtpPort: Int, smtpUsername: String, smtpPassword: String)
case class ContextIoConfig(clientKey: String, secret: String, accountId: String)


object ConfigLoader {

  private val defaultConfigFactory = ConfigFactory.load()

  val defaultConfig: Config = {
    Config(
      defaultConfigFactory.getStringList("postcodesToMatch").asScala.toList,
      VisionApiConfig(
        defaultConfigFactory.getString("visionApiKey")
      ),
      EmailerConfig(
        defaultConfigFactory.getString("email.fromAddress"),
        defaultConfigFactory.getString("email.toAddress"),
        defaultConfigFactory.getString("email.smtpHost"),
        defaultConfigFactory.getInt("email.smtpPort"),
        defaultConfigFactory.getString("email.smtpUsername"),
        defaultConfigFactory.getString("email.smtpPassword")
      ),
      ContextIoConfig(
        defaultConfigFactory.getString("contextIO.clientKey"),
        defaultConfigFactory.getString("contextIO.secret"),
        defaultConfigFactory.getString("contextIO.accountId")
      ),
      defaultConfigFactory.getString("directWebAddress")
    )
  }
}