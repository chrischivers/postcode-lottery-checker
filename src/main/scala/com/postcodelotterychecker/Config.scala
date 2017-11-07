package com.postcodelotterychecker

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.{Duration, FiniteDuration}

case class Config(postcodeCheckerConfig: CheckerConfig,
                  dinnerCheckerConfig: CheckerConfig,
                  stackpotCheckerConfig: CheckerConfig,
                  surveyDrawCheckerConfig: CheckerConfig,
                  quidcoHitterConfig: CheckerConfig,
                  emojiCheckerConfig: CheckerConfig,
                  emailerConfig: EmailerConfig,
                  redisConfig: RedisConfig,
                  resultsProcessorConfig: ResultsProcessorConfig)


case class EmailerConfig(fromAddress: String, smtpHost: String, smtpPort: Int, smtpUsername: String, smtpPassword: String, numberAttempts: Int, secondsBetweenAttempts: Int)

case class PostgresDBConfig(host: String, port: Int, username: String, password: String, dbName: String)

case class RedisConfig(host: String, port: Int, dbIndex: Int, resultsTTL: FiniteDuration)

case class CheckerConfig(directWebAddressPrefix: String, directWebAddressSuffix: String, uuid: String, lambdaTriggerUrl: String)

case class ResultsProcessorConfig(timeBetweenRetries: FiniteDuration, timeCutoff: FiniteDuration)

object ConfigLoader {

  private val defaultConfigFactory = ConfigFactory.load()
  //  private val postgresDBParamsPrefix = "db.postgres."

  val defaultConfig: Config = {
    Config(
      CheckerConfig(
        defaultConfigFactory.getString("postcodeChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("postcodeChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("postcodeChecker.uuid"),
        defaultConfigFactory.getString("postcodeChecker.lambdaTriggerUrl")
      ),
      CheckerConfig(
        defaultConfigFactory.getString("dinnerChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("dinnerChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("dinnerChecker.uuid"),
        defaultConfigFactory.getString("dinnerChecker.lambdaTriggerUrl")
      ),
      CheckerConfig(
        defaultConfigFactory.getString("stackpotChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("stackpotChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("stackpotChecker.uuid"),
        defaultConfigFactory.getString("stackpotChecker.lambdaTriggerUrl")
      ),
      CheckerConfig(
        defaultConfigFactory.getString("surveyDrawChecker.directWebAddressPrefix"),
        defaultConfigFactory.getString("surveyDrawChecker.directWebAddressSuffix"),
        defaultConfigFactory.getString("surveyDrawChecker.uuid"),
        defaultConfigFactory.getString("surveyDrawChecker.lambdaTriggerUrl")
      ),
      CheckerConfig(
        defaultConfigFactory.getString("quidcoHitter.directWebAddressPrefix"),
        defaultConfigFactory.getString("quidcoHitter.directWebAddressSuffix"),
        defaultConfigFactory.getString("quidcoHitter.uuid"),
        defaultConfigFactory.getString("quidcoHitter.lambdaTriggerUrl")
      ),
      CheckerConfig(
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
      RedisConfig(
        defaultConfigFactory.getString("redis.host"),
        defaultConfigFactory.getInt("redis.port"),
        defaultConfigFactory.getInt("redis.dbIndex"),
        FiniteDuration(defaultConfigFactory.getDuration("redis.resultsTTL").toMillis, TimeUnit.MILLISECONDS)
      ),
      ResultsProcessorConfig(
        FiniteDuration(defaultConfigFactory.getDuration("resultsProcessor.timeBetweenRetries").toMillis, TimeUnit.MILLISECONDS),
        FiniteDuration(defaultConfigFactory.getDuration("resultsProcessor.timeCutoff").toMillis, TimeUnit.MILLISECONDS)
      )
    )
  }
}