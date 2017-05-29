package com.postcodelotterychecker

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

case class Config(postcodesToMatch: List[String], visionApiConfig: VisionApiConfig, emailerConfig: EmailerConfig)
case class VisionApiConfig(apiKey: String)
case class EmailerConfig(fromAddress: String, toAddress: String)

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
        defaultConfigFactory.getString("email.toAddress")
      )
    )
  }
}