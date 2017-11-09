import com.postcodelotterychecker.CheckerConfig
import com.postcodelotterychecker.caching.RedisResultCache
import com.postcodelotterychecker.checkers.{HtmlUnitWebClient, PostcodeChecker}
import com.postcodelotterychecker.models.Postcode
import com.postcodelotterychecker.subscribers.SubscribersFetcher

class IntegrationTest {

  val subscribersFetcher = new SubscribersFetcher {
    override val subscribersFileName: String = "subscribers-full-test.json"
  }

  val subscribers = subscribersFetcher.getSubscribers

  val postcodeChecker = new PostcodeChecker {
    override val htmlUnitWebClient: HtmlUnitWebClient = _
    override val redisResultCache: RedisResultCache[Postcode] = _
    override val config: CheckerConfig = _

    override def getResult = IO
  }

}
