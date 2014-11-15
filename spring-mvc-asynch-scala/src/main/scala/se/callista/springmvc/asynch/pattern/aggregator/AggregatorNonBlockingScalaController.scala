package se.callista.springmvc.asynch.pattern.aggregator

import java.util.{TimerTask, Timer}
import java.util.concurrent.{ScheduledFuture, TimeUnit, Executors}

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping, RestController}
import org.springframework.web.context.request.async.DeferredResult
import se.callista.springmvc.asynch.commons.lambdasupport.AsyncHttpClientScala
import scala.concurrent.Future.{sequence, firstCompletedOf}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try


@RestController
class AggregatorNonBlockingScalaController {
	@Value("${sp.non_blocking.url}")
	private val SP_NON_BLOCKING_URL: String = null

	@Value("${aggregator.timeoutMs}")
	private val TIMEOUT_MS: Int = 0

	private val logger = AggregatorNonBlockingScalaController.logger
	private val dbThreadPoolExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadScheduledExecutor)
	private val scheduler = Executors.newScheduledThreadPool(1)
	private val timeoutTimer = new Timer()

	@RequestMapping(Array("/aggregate-non-blocking-scala"))
	def nonBlockingAggregator (@RequestParam(value = "dbLookupMs", required = false, defaultValue = "0") dbLookupMs: Int,
	                           @RequestParam(value = "dbHits", required = false, defaultValue = "3") dbHits: Int,
	                           @RequestParam(value = "minMs", required = false, defaultValue = "0") minMs: Int,
	                           @RequestParam(value = "maxMs", required = false, defaultValue = "0") maxMs: Int): DeferredResult[String] = {

		val deferredResult = new DeferredResult[String]()

		executeDbLookup(dbLookupMs, dbHits, minMs, maxMs)
				.flatMap(urls => sequence(urls.map(url => firstCompletedOf(asyncCall(url)::timeoutFuture::Nil))))
				.map(results => results.filterNot(result => result.isInstanceOf[Throwable]))
				.map(results => deferredResult.setResult(results.mkString("\n")))

		deferredResult
	}


	def asyncCall(url: String): Future[String] = {
		logger.debug(s"Remote call to: $url")
		AsyncHttpClientScala.execute(url).map(response => response.getResponseBody())
	}

	private def timeoutFuture(): Future[Throwable] = {
		val p = Promise[Throwable]
		timeoutTimer.schedule(new TimerTask {
			override def run() = {
				logger.debug("Future timed out!!");
				p.complete(Try(new TimeoutException("Timeout!")))
			}
		}, TIMEOUT_MS)
		p.future
	}

	private def executeDbLookup(dbLookupMs: Int, dbHits: Int, minMs: Int, maxMs: Int) = Future {
		Thread.sleep(dbLookupMs)
		List.fill(dbHits)(s"$SP_NON_BLOCKING_URL?minMs=$minMs&maxMs=$maxMs")
	}(dbThreadPoolExecutor)

}

object AggregatorNonBlockingScalaController {
	private val logger = LoggerFactory.getLogger(AggregatorNonBlockingScalaController.getClass)
}