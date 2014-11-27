import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.Json

import play.api.test._
import play.api.test.Helpers._
import util.ConfigUtil

/**
 *
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification with ConfigUtil {

  "Application" should {

    "get aggregated result" in new WithApplication {
      val aggregate = route(FakeRequest(GET, "/aggregate?minMs=2000&maxMs=2000"))

      val expectedResult =
        """{"status":"Ok","processingTimeMs":2000}
          |{"status":"Ok","processingTimeMs":2000}
          |{"status":"Ok","processingTimeMs":2000}
          |""".stripMargin

      status(aggregate.get) must equalTo(OK)
      contentType(aggregate.get) must beSome.which(_ == "text/plain")
      contentAsString(aggregate.get) must equalTo(expectedResult)
    }

    "error on aggregated when invalid parameters" in new WithApplication {
      val aggregate = route(FakeRequest(GET, "/aggregate?minMs=2000&maxMs=1000"))

      val expectedResult =
        """Error: maxMs < minMs  (1000 < 2000)
          |Error: maxMs < minMs  (1000 < 2000)
          |Error: maxMs < minMs  (1000 < 2000)
          |""".stripMargin

      status(aggregate.get) must equalTo(OK)
      contentType(aggregate.get) must beSome.which(_ == "text/plain")
      contentAsString(aggregate.get) must equalTo(expectedResult)
    }

    "timeout on aggregated" in new WithApplication {
      val minMs = if (TIMEOUT_MS < 1000) 0 else TIMEOUT_MS - 1000
      val maxMs = TIMEOUT_MS + 1000
      val dbHits = 10

      val aggregate = route(FakeRequest(GET, s"/aggregate?minMs=$minMs&maxMs=$maxMs&dbHits=$dbHits"))

      val results = contentAsString(aggregate.get).split('\n')

      results.length must be_<=(dbHits)
      results.forall(r => (Json.parse(r) \ "processingTimeMs").as[Int] <= maxMs ) must beTrue
      status(aggregate.get) must equalTo(OK)
      contentType(aggregate.get) must beSome.which(_ == "text/plain")
    }

    "get routing slip result" in new WithApplication {
      val routingSlip = route(FakeRequest(GET, "/routing-slip"))

      val expectedResult =
        """{"status":"Ok","processingTimeMs":100}
          |{"status":"Ok","processingTimeMs":200}
          |{"status":"Ok","processingTimeMs":400}
          |{"status":"Ok","processingTimeMs":500}
          |""".stripMargin

      status(routingSlip.get) must equalTo(OK)
      contentType(routingSlip.get) must beSome.which(_ == "text/plain")
      contentAsString(routingSlip.get) must equalTo(expectedResult)
    }
  }
}
