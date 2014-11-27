package se.callista.springmvc.asynch.pattern.aggregator;

import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.AsyncHttpClientLambdaAware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@RestController
@Scope("prototype")
public class AggregatorNonBlockingCallbackController {

    private static final Logger LOG = LoggerFactory.getLogger(AggregatorNonBlockingCallbackController.class);

    @Value("${sp.non_blocking.url}")
    private String spUrl;

    @Value("${aggregator.timeoutMs}")
    private int aggTimeoutMs;

    @Autowired
    private AsyncHttpClientLambdaAware asyncHttpClient;

    @Autowired
    @Qualifier("dbThreadPoolExecutor")
    private TaskExecutor dbThreadPoolExecutor;

    @Autowired
    @Qualifier("timerService")
    private ScheduledExecutorService timerService;


    //
    // MUTABLE STATE (that's why we require prototype scope of our Spring bean!)
    //
    private ScheduledFuture timer = null;
    private List<ListenableFuture<Response>> concurrentRequests = new ArrayList<>();
    final AtomicInteger noOfResults = new AtomicInteger(0);
    private int noOfCalls = 0;
    private DeferredResult<String> deferredResult = null;
    private List<String> resultArr = new ArrayList<>();

    /**
     * Sample usage: curl "http://localhost:9080/aggregate-non-blocking-lambda?minMs=1000&maxMs=2000"
     *
     * @param dbLookupMs
     * @param dbHits
     * @param minMs
     * @param maxMs
     * @return
     */
    @RequestMapping("/aggregate-non-blocking-callback")
    public DeferredResult<String> nonBlockingAggregator(
        @RequestHeader(value = "Accept",     required = false, defaultValue = "*/*") String accept,
        @RequestParam (value = "dbLookupMs", required = false, defaultValue = "0")   int dbLookupMs,
        @RequestParam (value = "dbHits",     required = false, defaultValue = "3")   int dbHits,
        @RequestParam (value = "minMs",      required = false, defaultValue = "0")   int minMs,
        @RequestParam (value = "maxMs",      required = false, defaultValue = "0")   int maxMs) {

        deferredResult = new DeferredResult<String>();

        // Start with a blocking db-lookup of the urls to call
        dbThreadPoolExecutor.execute(() -> {
            LOG.debug("Start db-lookup");
            List<String> urls = new DbLookup(dbLookupMs, dbHits).lookupUrlsInDb(spUrl, minMs, maxMs);
            LOG.debug("Db found #{} urls", urls.size());

            // Now setup concurrent and asynch non-blocking calls the the urls
            noOfCalls = urls.size();
            int i = 0;
            for (String url: urls) {
                final int id = ++i;

                LOG.debug("Start asynch call #{}", id);
                concurrentRequests.add(asyncHttpClient.execute(url, accept,

                        (throwable) -> {
                            handleException(throwable, url, id);
                        },

                        (response) -> {
                            LOG.debug("Received asynch response #{}", id);
                            addResponse(id, response.getResponseBody());
                        }
                ));
            }

            // Setup a timer for the max wait-period
            timer = timerService.schedule(() -> onTimeout(), aggTimeoutMs, MILLISECONDS);
        });

        // Ok, everything is now setup for asynchronous processing, let the play begin...
        LOG.debug("Asynch processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }

    /**
     * Adds a response from the concurrent calls. The method is synchronized
     *
     * If many requests completes at the same time the following code must be executed in sequence for one thread at a time
     * Since we don't have any Actor-like mechanism to rely on (for the time being...) we simply ensure that the code block is executed by one thread at a time by an old school synchronized block
     * Since the processing in the block is very limited it will not cause a bottleneck.
     *
     * @param id
     * @param response
     */
    private synchronized void addResponse(int id, String response) {
        synchronized (resultArr) {
            // Count down, aggregate answer and return if all answers (also cancel timer)...
            int noOfRes = noOfResults.incrementAndGet();

            // Perform the aggregation...
            LOG.debug("Safely adding response/error-info for call #{}", id);
            resultArr.add(response);

            if (noOfRes >= noOfCalls) {
                onAllCompleted();
            }
        }
    }

    private void handleException(Throwable throwable, String url, int id) {
        if (throwable instanceof CancellationException) {
            LOG.debug("Request #{} cancelled by timeout", id);

        } else if (throwable instanceof TimeoutException) {
            String msg = "Request #" + id + " failed due to service provider not responding within the configured timeout. Url: " + url;
            LOG.error(msg);
            addResponse(id, msg);

        } else {
            String msg = "Request #" + id + " failed due to error: " + throwable;
            LOG.error(msg, throwable);
            addResponse(id, msg);
        }
    }

    private void onTimeout() {

        LOG.debug("Timeout! Cancel ongoing requests!");

        // complete missing answers and return ...
        int i = 0;
        for (ListenableFuture<Response> request : concurrentRequests) {
            if (!request.isDone()) {
                LOG.debug("Cancel a request due to timeout!");
                request.cancel(true);
            }
            i++;
        }
        onAllCompleted();
    }

    private void onAllCompleted() {
        timer.cancel(true);

        if (deferredResult.isSetOrExpired()) {
            // TODO: Handle already set or expired error
            LOG.warn("Result already set or expired");
        } else {
            LOG.debug("We are done, setting the result on the deferred result!");
            boolean deferredStatus = deferredResult.setResult(getTotalResult());
        }
    }

    private String getTotalResult() {
        String totalResult = "";
        for (String r : resultArr)
            totalResult += r + '\n';
        return totalResult;
    }
}