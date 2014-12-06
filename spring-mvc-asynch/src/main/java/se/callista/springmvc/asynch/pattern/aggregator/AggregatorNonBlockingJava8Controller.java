package se.callista.springmvc.asynch.pattern.aggregator;

import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.AsyncHttpClientJava8;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static se.callista.springmvc.asynch.pattern.aggregator.callbacksupport.FutureSupport.sequence;

@RestController
public class AggregatorNonBlockingJava8Controller {

	@Value("${sp.non_blocking.url}")
	private String SP_NON_BLOCKING_URL;

	@Value("${aggregator.timeoutMs}")
	private int TIMEOUT_MS;

	@Autowired
	@Qualifier("dbThreadPoolExecutor")
	private TaskExecutor dbThreadPoolExecutor;

	@Autowired
	private AsyncHttpClientJava8 asyncHttpClientJava8;

	private static final Logger logger = LoggerFactory.getLogger(AggregatorNonBlockingJava8Controller.class);

	/**
	 * Sample usage: curl "http://localhost:9080/aggregate-non-blocking-java8?minMs=1000&maxMs=2000"
	 *
	 * @param dbLookupMs
	 * @param dbHits
	 * @param minMs
	 * @param maxMs
	 * @return
	 * @throws java.io.IOException
	 */
	@RequestMapping("/aggregate-non-blocking-java8")
	public DeferredResult<String> nonBlockingAggregator(
			@RequestParam(value = "dbLookupMs", required = false, defaultValue = "0") int dbLookupMs,
			@RequestParam(value = "dbHits", required = false, defaultValue = "3") int dbHits,
			@RequestParam(value = "minMs", required = false, defaultValue = "0") int minMs,
			@RequestParam(value = "maxMs", required = false, defaultValue = "0") int maxMs) throws IOException {

		final DeferredResult<String> deferredResult = new DeferredResult<>();
		final DbLookup dbLookup = new DbLookup(dbLookupMs, dbHits);

		final CompletableFuture<List<String>> urlsF =
				supplyAsync(() -> dbLookup.lookupUrlsInDb(SP_NON_BLOCKING_URL, minMs, maxMs), dbThreadPoolExecutor);

		urlsF
			.thenComposeAsync(this::executeHttpRequests)
			.whenComplete((result, throwable) -> {
				if (throwable == null) {
					populateDeferredResult(deferredResult, result);
				} else {
					deferredResult.setErrorResult(throwable);
				}
			});

		return deferredResult;
	}


	private CompletableFuture<List<String>> executeHttpRequests(List<String> urls) {
		CompletableFuture<List<Optional<String>>> futureResponses =
				sequence(
						IntStream.rangeClosed(1, urls.size()).mapToObj(i ->
								doAsyncCall(urls.get(i - 1), i)
						).collect(Collectors.toList()));

		return futureResponses.thenApply(this::filterResponses);
	}

	private CompletableFuture<Optional<String>> doAsyncCall(String url, int id) {
		logger.debug("Start asynch call #{}", id);
		return asyncHttpClientJava8.execute(url, id)
				.thenApply(response -> Optional.of(extractResponseBody(response)))
				.applyToEither(TimeoutFuture.after(TIMEOUT_MS), identity())
				.exceptionally(t -> Optional.of("Request #" + id + " failed due to error: " + t.getMessage()));
	}

	private List<String> filterResponses(List<Optional<String>> responses) {
		return responses.stream()
				.filter(Optional::isPresent)
				.map(Optional::get).collect(Collectors.toList());
	}

	private void populateDeferredResult(DeferredResult<String> deferredResult, List<String> result) {
		String collectedResponse = result.stream().collect(Collectors.joining("\n"));
		deferredResult.setResult(collectedResponse);
	}

	private String extractResponseBody(Response response) {
		try {
			return response.getResponseBody();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
