import akka.actor.{Status, Props, ActorSystem}
import akka.io.IO
import akka.routing.FromConfig
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import akka.pattern.ask
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import se.callista.akka.async.AsyncService
import se.callista.akka.async.common.{RoutingSlipParams, HttpRequestActor, Settings, DbLookupWorker}
import se.callista.akka.async.pattern.RoutingSlipActor
import spray.can.Http
import spray.http
import spray.http.HttpMethods._
import spray.http.{StatusCodes, HttpResponse, HttpRequest, Uri}

import scala.concurrent.duration._

class RoutingSlipTest(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("Test"))

  val settings = Settings(system)

  val dbHits = 20

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "The routing slip" must {
    "return correct answer" in {
      val routingSlip = system.actorOf(RoutingSlipActor.props(testActor, RoutingSlipParams(HttpRequest(GET, "http://localhost:8080"))))

      routingSlip ! 1

      val msg = receiveOne(5.seconds)

      val expectedResult =
        """{"status":"Ok","processingTimeMs":100}
          |{"status":"Ok","processingTimeMs":200}
          |{"status":"Ok","processingTimeMs":400}
          |{"status":"Ok","processingTimeMs":500}
          |""".stripMargin

      msg shouldBe a[HttpResponse]

      val httpResponse = msg.asInstanceOf[HttpResponse]

      httpResponse.status shouldEqual StatusCodes.OK
      httpResponse.entity.asString shouldEqual expectedResult
    }
  }

  "The routing slip" must {
    "return internal server error when qry = error" in {
      val routingSlip = system.actorOf(
        RoutingSlipActor.props(testActor, RoutingSlipParams(HttpRequest(GET, "http://localhost:8080?qry=error")))
      )

      routingSlip ! 1

      val msg = receiveOne(5.seconds)

      val expectedResult = "Error: Invalid query parameter, qry=error"

      msg shouldBe a[HttpResponse]

      val httpResponse = msg.asInstanceOf[HttpResponse]

      httpResponse.status shouldEqual StatusCodes.InternalServerError
      httpResponse.entity.asString shouldEqual expectedResult
    }
  }

  "The routing slip" must {
    "return gateway error when qry = timeout" in {

      val routingSlip = system.actorOf(
        RoutingSlipActor.props(testActor, RoutingSlipParams(HttpRequest(GET, "http://localhost:8080?qry=timeout")))
      )
      routingSlip ! 1

      val msg = receiveOne(10.seconds)

      val expectedResult = "Request failed due to service provider not responding within 5 s. Url: http://localhost:9090?minMs=100&maxMs=100&qry=timeout"

      msg shouldBe a[HttpResponse]

      val httpResponse = msg.asInstanceOf[HttpResponse]

      httpResponse.status shouldEqual StatusCodes.GatewayTimeout
      httpResponse.entity.asString shouldEqual expectedResult
    }
  }
}
