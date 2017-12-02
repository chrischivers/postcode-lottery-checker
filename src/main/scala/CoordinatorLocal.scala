import cats.effect.IO
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.{CheckerConfig, _Coordinator}
import com.postcodelotterychecker.checkers._
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object CoordinatorLocal extends App {

    val localCoordinator = new _Coordinator() {
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
    }



      localCoordinator.startMaster

}
