package se.callista.springmvc.asynch.pattern.routingslip

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.{RequestMapping, RestController}
import org.springframework.web.context.request.async.DeferredResult
import se.callista.springmvc.asynch.commons.lambdasupport.AsyncHttpClientScala

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by anders on 14-11-04.
 */
@RestController
class RoutingSlipNonBlockingScalaController {

	val logger = RoutingSlipNonBlockingScalaController.logger

	@Value("${sp.non_blocking.url}")
	val SP_NON_BLOCKING_URL: String = null


	@RequestMapping(Array("/routing-slip-non-blocking-scala"))
	def nonBlockingRoutingSlip = {
		val deferredResult = new DeferredResult[String]()

		for {
			r1 <- doAsyncCall(Nil, 1)
			r2 <- doAsyncCall(r1, 2)
			r3 <- doAsyncCall(r2, 3)
			r4 <- doAsyncCall(r3, 4)
			r5 <- doAsyncCall(r4, 5)
		} yield deferredResult.setResult(r5.mkString("\n"))

		deferredResult
	}

	def doAsyncCall(result: List[String], num: Int) = {
		AsyncHttpClientScala.execute(nbUrl(num))
				.map(response => response.getResponseBody())
				.map(x => {logger.debug("Got resp #{}", num);x})
				.map(body => body :: result)
	}

	val nbUrl: Int => String = i => {
		val t = i * 100
		s"$SP_NON_BLOCKING_URL?minMs=$t&maxMs=$t"
	}

}

object RoutingSlipNonBlockingScalaController {
	private val logger = LoggerFactory.getLogger(RoutingSlipNonBlockingScalaController.getClass)
}