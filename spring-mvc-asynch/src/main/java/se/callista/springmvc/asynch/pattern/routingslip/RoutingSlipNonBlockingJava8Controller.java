package se.callista.springmvc.asynch.pattern.routingslip;

import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.AsyncCallException;
import se.callista.springmvc.asynch.common.AsyncHttpClientJava8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class RoutingSlipNonBlockingJava8Controller {

	private static final Logger LOG = LoggerFactory.getLogger(RoutingSlipNonBlockingJava8Controller.class);

	@Value("${sp.non_blocking.url}")
	private String SP_NON_BLOCKING_URL;

	@Value("${sp.requestTimeoutMs}")
	private String spRequestTimeoutMs;

	@Autowired
	AsyncHttpClientJava8 asyncHttpClientJava8;

	/**
	 * Sample usage: curl "http://localhost:9080/routing-slip-non-blocking-java8"
	 *
	 * @return
	 */
	@RequestMapping("/routing-slip-non-blocking-java8")
	public DeferredResult<String> nonBlockingRoutingSlip() throws IOException {

		final DeferredResult<String> deferredResult = new DeferredResult<>();

		final CompletableFuture<List<String>> f1 = doAsyncCall(new ArrayList<>(), 1)
				.thenCompose(result -> doAsyncCall(result, 2))
				.thenCompose(result -> doAsyncCall(result, routeCall(result)))
				.thenCompose(result -> doAsyncCall(result, 5))
				.whenComplete((result, error) -> {
					if (error == null) {
						deferredResult.setResult(getTotalResult(result));
					} else {
						deferredResult.setErrorResult(handleException(error));
					}
				});

		return deferredResult;
	}

	private CompletableFuture<List<String>> doAsyncCall(List<String> result, int num) {
		LOG.debug("Start req #{}", num);
		String url = getUrl(num);
		return asyncHttpClientJava8
				.execute(url, num).exceptionally(t -> throwNewException(url, t))
				.thenApply(logResponse(num))
				.thenApply(getResponseBody())
				.thenApply(body -> addBodyToResult(body, result));
	}

	private Function<Response, String> getResponseBody() {
		return (response -> {
			try {
				return response.getResponseBody();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}


	private int routeCall(List<String> result) {
		return 4;
	}

	private Function<Response, Response> logResponse(int num) {
		return r -> {
			LOG.debug("Got resp #{}", num);
			return r;
		};
	}

	private List<String> addBodyToResult(String response, List<String> result) {
		result.add(response);
		return result;
	}

	private String getUrl(int processingStepNo) {
		int sleeptimeMs = 100 * processingStepNo;
		return SP_NON_BLOCKING_URL + "?minMs=" + sleeptimeMs + "&maxMs=" + sleeptimeMs;
	}

	private String getTotalResult(List<String> resultList) {
		return resultList.stream().collect(Collectors.joining("\n"));
	}

	private String handleException(Throwable t) {
		String url = "";
		Throwable cause = t;

		if (t.getCause() instanceof AsyncCallException) {
			AsyncCallException ae = (AsyncCallException) t.getCause();
			url = ae.getUrl();
			cause = ae.getCause();
		}
		String msg = (cause instanceof TimeoutException) ?
			"Request failed due to service provider not responding within " + spRequestTimeoutMs + " ms. Url: " + url:
			"Request failed due to error. Url: " + url + " Cause: " + cause;

		LOG.error(msg);
		return msg;
	}

	private static <T> T throwNewException(String url, Throwable t) {
		LOG.error("Error on aync call to {}. {}", url, t);
		throw new AsyncCallException(url, t);
	}

}