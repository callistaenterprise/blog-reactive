package se.callista.springmvc.asynch.pattern.routingslip

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.{RequestMapping, RestController}
import org.springframework.web.context.request.async.DeferredResult
import se.callista.springmvc.asynch.commons.lambdasupport.AsyncHttpClientScala

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{duration, Future}
import scala.concurrent.duration._
import scala.language.postfixOps


@RestController
class RoutingSlipNonBlockingScalaController {

	val logger = RoutingSlipNonBlockingScalaController.logger

	@Value("${sp.non_blocking.url}")
	val SP_NON_BLOCKING_URL: String = null


	@RequestMapping(Array("/routing-slip-non-blocking-scala"))
	def nonBlockingRoutingSlip = {
		val deferredResult = new DeferredResult[String]()
		for {
			r1 <- doAsyncCall(nonBlockingUrl(latency = 100 milliseconds))
			r2 <- doAsyncCall(nonBlockingUrl(latency = 200 milliseconds))
			r3 <- doAsyncCall(nonBlockingUrl(latency = 300 milliseconds))
			r4 <- doAsyncCall(nonBlockingUrl(latency = 400 milliseconds))
			r5 <- doAsyncCall(nonBlockingUrl(latency = 500 milliseconds))
		} yield deferredResult.setResult(List(r1, r2, r3, r4, r5).mkString("\n"))

		deferredResult
	}

	def doAsyncCall(url: String): Future[String] = {
		AsyncHttpClientScala.execute(url).map(response => response.getResponseBody())
	}

	def nonBlockingUrl(latency: Duration):String = s"$SP_NON_BLOCKING_URL?minMs=${latency.toMillis}&maxMs=${latency.toMillis}"
}

object RoutingSlipNonBlockingScalaController {
	private val logger = LoggerFactory.getLogger(RoutingSlipNonBlockingScalaController.getClass)
}