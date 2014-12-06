package se.callista.springmvc.asynch.pattern.aggregator

import java.util.concurrent._

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, RestController}
import org.springframework.web.context.request.async.DeferredResult
import se.callista.springmvc.asynch.commons.AsyncHttpClientScala

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{firstCompletedOf, sequence}
import scala.concurrent.{Future, TimeoutException, _}
import scala.util.{Failure, Success, Try}


@RestController
class AggregatorNonBlockingScalaController {
	@Value("${sp.non_blocking.url}") private val SP_NON_BLOCKING_URL: String = null
	@Value("${threadPool.db.max_size}") private val THREAD_POOL_DB_MAX_SIZE: Int = 0
	@Value("${threadPool.db.init_size}") private val THREAD_POOL_DB_INIT_SIZE: Int = 0
	@Value("${threadPool.db.queue_size}") private val THREAD_POOL_DB_QUEUE_SIZE: Int = 0
	@Value("${aggregator.timeoutMs}") private val TIMEOUT_MS: Int = 0

	private lazy val taskExecutor = {
		val e = new ThreadPoolTaskExecutor()
		e.setCorePoolSize(THREAD_POOL_DB_INIT_SIZE)
		e.setMaxPoolSize(THREAD_POOL_DB_MAX_SIZE)
		e.setQueueCapacity(THREAD_POOL_DB_QUEUE_SIZE)
		e.setThreadNamePrefix("db-thread")
		e.initialize()
		e
	}

	private val logger = LoggerFactory.getLogger(classOf[AggregatorNonBlockingScalaController])
	private lazy val timeoutScheduler = Executors.newSingleThreadScheduledExecutor

	@RequestMapping(Array("/aggregate-non-blocking-scala"))
	def nonBlockingAggregator(@RequestParam(value = "dbLookupMs", required = false, defaultValue = "0") dbLookupMs: Int,
	                          @RequestParam(value = "dbHits", required = false, defaultValue = "3") dbHits: Int,
	                          @RequestParam(value = "minMs", required = false, defaultValue = "0") minMs: Int,
	                          @RequestParam(value = "maxMs", required = false, defaultValue = "0") maxMs: Int): DeferredResult[String] = {

		val deferredResult = new DeferredResult[String]

		val urlsF = doDbLookup(dbLookupMs, dbHits, minMs, maxMs)(ExecutionContext.fromExecutor(taskExecutor))

		val resultsF = urlsF.flatMap(urls => sequence(urls.map(url => firstCompletedOf(asyncCall(url) :: timeoutFuture :: Nil))))
				.map(results => results.filterNot(result => result.isInstanceOf[Throwable]))
				.andThen {
			case Success(results) => deferredResult.setResult(results.mkString("\n"))
			case Failure(t) => deferredResult.setErrorResult(t)
		}

		resultsF.map(results => deferredResult.setResult(results.mkString("\n")))

		deferredResult
	}

	def asyncCall(url: String): Future[String] = {
		logger.debug(s"Remote call to: $url")
		AsyncHttpClientScala.get(url).map(response => response.getResponseBody)
	}

	def doDbLookup(dbLookupMs: Int, dbHits: Int, minMs: Int, maxMs: Int)(dbThreadPoolExecutor: ExecutionContext): Future[List[String]] = Future {
		logger.debug("Fake db lookup")
		Thread.sleep(dbLookupMs)
		List.fill(dbHits)(s"$SP_NON_BLOCKING_URL?minMs=$minMs&maxMs=$maxMs")
	}(dbThreadPoolExecutor)

	private def timeoutFuture() = {
		val p = Promise[Throwable]()

		timeoutScheduler.schedule(new Runnable {
			override def run() = {
				logger.debug("Future timed out!!")
				p.complete(Try(new TimeoutException("Timeout!!")))
			}
		}, TIMEOUT_MS, TimeUnit.MILLISECONDS)

		p.future
	}
}