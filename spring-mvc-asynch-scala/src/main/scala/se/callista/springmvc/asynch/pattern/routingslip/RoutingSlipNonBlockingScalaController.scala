package se.callista.springmvc.asynch.pattern.routingslip

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.{RequestMapping, RestController}
import org.springframework.web.context.request.async.DeferredResult
import se.callista.springmvc.asynch.commons.AsyncHttpClientScala

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps


@RestController
class RoutingSlipNonBlockingScalaController {

	val logger = RoutingSlipNonBlockingScalaController.logger

	@Value("${sp.non_blocking.url}")
	val SP_NON_BLOCKING_URL: String = null

	@RequestMapping(Array("/routing-slip-non-blocking-scala"))
	def nonBlockingRoutingSlip: DeferredResult[String] = {
		val deferredResult = new DeferredResult[String]()
		val result =
			for {
				r1 <- doAsyncCall(1)
				r2 <- doAsyncCall(2)
				r3 <- doAsyncCall(routeCall(r2))
				r4 <- doAsyncCall(5)
			} yield List(r1, r2, r3, r4)
		result.map(v => deferredResult.setResult(v.mkString("\n")))
		deferredResult
	}

	def doAsyncCall(num: Integer) = AsyncHttpClientScala.get(nonBlockingUrl(num)).map(r => r.getResponseBody)

	def nonBlockingUrl(id: Integer) = s"$SP_NON_BLOCKING_URL?minMs=${id*100}&maxMs=${id*100}"
	def routeCall(result: String) = 4
}

object RoutingSlipNonBlockingScalaController {
	private val logger = LoggerFactory.getLogger(RoutingSlipNonBlockingScalaController.getClass)
}
