import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification {

  "Application" should {

    "aggregate from a browser" in new WithBrowser {

      val expectedResult =
        """{"status":"Ok","processingTimeMs":2000}
          |{"status":"Ok","processingTimeMs":2000}
          |{"status":"Ok","processingTimeMs":2000}
          |""".stripMargin


      browser.goTo(s"http://localhost:$port/aggregate?minMs=2000&maxMs=2000")

      browser.pageSource must equalTo(expectedResult)
    }

    "routing slip from a browser" in new WithBrowser {

      val expectedResult =
        """{"status":"Ok","processingTimeMs":100}
          |{"status":"Ok","processingTimeMs":200}
          |{"status":"Ok","processingTimeMs":400}
          |{"status":"Ok","processingTimeMs":500}
          |""".stripMargin

      browser.goTo(s"http://localhost:$port/routing-slip")

      browser.pageSource must equalTo(expectedResult)
    }
  }
}
