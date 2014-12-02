import akka.actor.{ActorSystem, Props, Status}
import akka.io.IO
import akka.pattern.ask
import akka.routing.FromConfig
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.libs.json.Json
import se.callista.akka.async.AsyncService
import se.callista.akka.async.common.{AggregateParams, DbLookupWorker, HttpRequestActor, Settings}
import se.callista.akka.async.pattern.{AggregateRequest, AggregatorActor}
import spray.can.Http
import spray.http
import spray.http.HttpMethods._
import spray.http._

import scala.concurrent.duration._

/**
 *
 */
class AggregatorTest(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("Test"))

  val settings = Settings(system)

  val dbHits = 20

  val dbLookup = system.actorOf(FromConfig.props(Props[DbLookupWorker]), "dblookup")

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "The aggregator" must {
    "return three results aggregated" in {
      val request = HttpRequest(GET, "http://localhost:8080?minMs=2000&maxMs=2000")
      val aggregator = system.actorOf(AggregatorActor.props(testActor, dbLookup, AggregateParams("http://127.0.0.1:9090")(request)))
      aggregator ! AggregateRequest()

      val msg = receiveOne(5.seconds)

      val expectedResult =
        """{"status":"Ok","processingTimeMs":2000}
          |{"status":"Ok","processingTimeMs":2000}
          |{"status":"Ok","processingTimeMs":2000}
          |""".stripMargin

      msg shouldBe a[HttpResponse]

      val httpResponse = msg.asInstanceOf[HttpResponse]

      httpResponse.status shouldEqual StatusCodes.OK
      httpResponse.entity.asString shouldEqual expectedResult
    }
  }

  "The aggregator" must {
    "return error on invalid input" in {
      val request = HttpRequest(GET, "http://localhost:8080?minMs=2000&maxMs=1000")
      val aggregator = system.actorOf(AggregatorActor.props(testActor, dbLookup, AggregateParams("http://127.0.0.1:9090")(request)))
      aggregator ! AggregateRequest()

      val msg = receiveOne(5.seconds)

      val expectedResult =
        """Error: maxMs < minMs  (1000 < 2000)
          |Error: maxMs < minMs  (1000 < 2000)
          |Error: maxMs < minMs  (1000 < 2000)
          |""".stripMargin

      msg shouldBe a[HttpResponse]

      val httpResponse = msg.asInstanceOf[HttpResponse]

      httpResponse.status shouldEqual StatusCodes.OK
      httpResponse.entity.asString shouldEqual expectedResult
    }
  }

  "The aggregator" must {
    "return error on timeout" in {
      val timeoutMinMs = if (settings.AGG_TMO_MS < 1000) 0 else settings.AGG_TMO_MS - 1000
      val timeoutMaxMs = settings.AGG_TMO_MS + 1000

      val request = HttpRequest(GET, s"http://localhost:8080?minMs=$timeoutMinMs&maxMs=$timeoutMinMs&dbHits=$dbHits")
      val aggregator = system.actorOf(AggregatorActor.props(testActor, dbLookup, AggregateParams("http://127.0.0.1:9090")(request)))
      aggregator ! AggregateRequest()

      val msg = receiveOne(5.seconds)

      msg shouldBe a[HttpResponse]

      val httpResponse = msg.asInstanceOf[HttpResponse]

      httpResponse.status shouldEqual StatusCodes.OK
      val results = httpResponse.entity.asString.split("\n")

      results.length should be <= dbHits
      results.forall(r => (Json.parse(r) \ "processingTimeMs").as[Int] <= timeoutMaxMs ) shouldBe true

    }
  }
}
