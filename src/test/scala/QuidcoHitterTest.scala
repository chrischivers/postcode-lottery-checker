import com.postcodelotterychecker._
import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, fixture}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import scala.concurrent.duration._

class QuidcoHitterTest extends fixture.FunSuite with Matchers with ScalaFutures {

  case class FixtureParam(quidcoHitter: QuidcoHitter, restitoServer: RestitoServer, testConfig: Config)

  override implicit val patienceConfig = PatienceConfig(2 minutes, 2 seconds)

  def withFixture(test: OneArgTest) = {

    val port = 7000 + Random.nextInt(1000)
    val restitoServer = new RestitoServer(port)
    restitoServer.start()
    val urlPrefix = "http://localhost:" + port

    val defaultConfig = ConfigLoader.defaultConfig
    val testConfig = defaultConfig.copy(
      quidcoHitterConfig = defaultConfig.quidcoHitterConfig.copy(directWebAddressPrefix = urlPrefix)
    )
    val quidcoHitter = new QuidcoHitter(testConfig.quidcoHitterConfig)

    val testFixture = FixtureParam(quidcoHitter, restitoServer,  testConfig)

    try {
      withFixture(test.toNoArgTest(testFixture))
    }
    finally {
      restitoServer.stop()
    }
  }

  test("Quidco webpage is hit and returns status code 200") { f =>

    webpageIsRetrieved(f.restitoServer.server)

    f.quidcoHitter.run.futureValue
    f.restitoServer.server.getCalls.asScala should have size 1
  }

  def webpageIsRetrieved(server: StubServer) = {
    whenHttp(server).`match`(
      get("/quidco/"),
      parameter("reminder", "***REMOVED***"))
      .`then`(ok)
  }
}

