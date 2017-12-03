package com.postcodelotterychecker.local

import java.util.UUID

import cats.effect.IO
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers._
import com.postcodelotterychecker.db.SubscriberSchema
import com.postcodelotterychecker.db.sql.{PostgresDB, SubscribersTable}
import com.postcodelotterychecker.models.ResultTypes._
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode}
import com.postcodelotterychecker.results.{DefaultEmailClient, EmailClient, ResultsEmailer, ResultsProcessor}
import com.postcodelotterychecker._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object CoordinatorLocal {

    def apply(): Coordinator = new Coordinator() {
        override def triggerCheckers(): IO[Unit] = {

          val postcodeChecker = new PostcodeChecker {
            override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
            override val redisResultCache: RedisResultCache[Postcode] = postcodeResultsCache
            override val config: CheckerConfig = mainConfig.postcodeCheckerConfig
          }

          val dinnerChecker = new DinnerChecker {
            override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
            override val redisResultCache: RedisResultCache[List[DinnerUserName]] = dinnerResultsCache
            override val config: CheckerConfig = mainConfig.dinnerCheckerConfig
          }

          val stackpotChecker = new StackpotChecker {
            override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
            override val redisResultCache: RedisResultCache[List[Postcode]] = stackpotResultsCache
            override val config: CheckerConfig = mainConfig.stackpotCheckerConfig
          }

          val surveyDrawChecker = new SurveyDrawChecker {
            override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
            override val redisResultCache: RedisResultCache[Postcode] = surveyDrawResultsCache
            override val config: CheckerConfig = mainConfig.surveyDrawCheckerConfig
          }

          val emojiChecker = new EmojiChecker {
            override val htmlUnitWebClient: HtmlUnitWebClient = new HtmlUnitWebClient
            override val redisResultCache: RedisResultCache[Set[Emoji]] = emojiResultsCache
            override val config: CheckerConfig = mainConfig.emojiCheckerConfig
          }


          Future((for {
            result <- postcodeChecker.getResult
            _ <- postcodeChecker.cacheResult(uuid, result)
          } yield ()).unsafeToFuture()).onComplete {
            case Success(_) => logger.info("Postcode checker completed successfully")
            case Failure(e) => logger.error("Postcode checker failed", e)
          }


          Future((for {
            result <- dinnerChecker.getResult
            _ <- dinnerChecker.cacheResult(uuid, result)
          } yield ()).unsafeRunSync()).onComplete {
            case Success(_) => logger.info("Dinner checker completed successfully")
            case Failure(e) => logger.error("Dinner checker failed", e)
          }

          Future((for {
            result <- stackpotChecker.getResult
            _ <- stackpotChecker.cacheResult(uuid, result)
          } yield ()).unsafeRunSync()).onComplete {
            case Success(_) => logger.info("Stackpot checker completed successfully")
            case Failure(e) => logger.error("Stackpot checker failed", e)
          }

          Future((for {
            result <- surveyDrawChecker.getResult
            _ <- surveyDrawChecker.cacheResult(uuid, result)
          } yield ()).unsafeRunSync()).onComplete {
            case Success(_) => logger.info("Survey Draw checker completed successfully")
            case Failure(e) => logger.error("Survey Draw checker failed", e)
          }

          Future((for {
            result <- emojiChecker.getResult
            _ <- emojiChecker.cacheResult(uuid, result)
          } yield ()).unsafeRunSync()).onComplete {
            case Success(_) => logger.info("Emoji checker completed successfully")
            case Failure(e) => logger.error("Emoji checker failed", e)
          }

          IO.unit
        }

      override val mainConfig = ConfigLoader.defaultConfig
      override val uuid = UUID.randomUUID().toString

      val sqlDb = new PostgresDB(mainConfig.postgresDBConfig)
      override val subscribersTable = new SubscribersTable(sqlDb, SubscriberSchema())

      override val postcodeResultsCache = new RedisResultCache[Postcode] {
        override val resultType = PostcodeResultType
        override val config = mainConfig.redisConfig
      }
      override val dinnerResultsCache = new RedisResultCache[List[DinnerUserName]] {
        override val resultType = DinnerResultType
        override val config = mainConfig.redisConfig
      }
      override val stackpotResultsCache = new RedisResultCache[List[Postcode]] {
        override val resultType = StackpotResultType
        override val config = mainConfig.redisConfig
      }
      override val surveyDrawResultsCache = new RedisResultCache[Postcode] {
        override val resultType = SurveyDrawResultType
        override val config = mainConfig.redisConfig
      }
      override val emojiResultsCache = new RedisResultCache[Set[Emoji]] {
        override val resultType = EmojiResultType
        override val config = mainConfig.redisConfig
      }

      override val resultsProcessor = new ResultsProcessor {
        override val redisConfig: RedisConfig = mainConfig.redisConfig
      }

      override val resultsEmailer = new ResultsEmailer {
        override val emailClient: EmailClient = new DefaultEmailClient(mainConfig.emailerConfig)
        override val emailerConfig = mainConfig.emailerConfig
      }
    }
}
