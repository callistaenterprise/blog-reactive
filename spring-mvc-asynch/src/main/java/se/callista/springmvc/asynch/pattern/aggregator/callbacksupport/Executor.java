package se.callista.springmvc.asynch.pattern.aggregator.callbacksupport;

import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientLambdaAware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by magnus on 20/07/14.
 */
public class Executor {

    final private String baseUrl;
    final private String accept;
    final private int timeoutMs;
    final private TaskExecutor dbThreadPoolExecutor;
    final private int dbLookupMs;
    final private int dbHits;
    final private int minMs;
    final private int maxMs;

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private AsyncHttpClientLambdaAware asyncHttpClient;
    private ScheduledExecutorService timerService;

    private ScheduledFuture timer = null;
    private List<ListenableFuture<Response>> concurrentRequests = new ArrayList<>();
    final AtomicInteger noOfResults = new AtomicInteger(0);
    private int noOfCalls = 0;
    private DeferredResult<String> deferredResult = null;
    private List<String> resultArr = new ArrayList<>();

    public Executor(String baseUrl, String accept, int timeoutMs, TaskExecutor dbThreadPoolExecutor, int dbLookupMs, int dbHits, int minMs, int maxMs, AsyncHttpClientLambdaAware asyncHttpClient, ScheduledExecutorService timerService) {
        this.baseUrl = baseUrl;
        this.accept = accept;
        this.timeoutMs = timeoutMs;
        this.dbThreadPoolExecutor = dbThreadPoolExecutor;
        this.dbLookupMs = dbLookupMs;
        this.dbHits = dbHits;
        this.minMs = minMs;
        this.maxMs = maxMs;
        this.asyncHttpClient = asyncHttpClient;
        this.timerService = timerService;
    }

    public DeferredResult<String> startNonBlockingExecution() {

        deferredResult = new DeferredResult<String>();

        // Start with a blocking db-lookup of the urls to call
        dbThreadPoolExecutor.execute(() -> {
            LOG.debug("Start dblookup");
            List<String> urls = lookupUrlsInDb();
            LOG.debug("Db found #{} urls", urls.size());

            // Now setup concurrent and asynch non-blocking calls the the urls
            noOfCalls = urls.size();
            int i = 0;
            for (String url: urls) {
                final int id = ++i;

                LOG.debug("Start asynch call #{}", id);
                concurrentRequests.add(asyncHttpClient.execute(url, accept,

                        (response) -> {
                            LOG.debug("Received asynch response #{}", id);
                            addResponse(id, response.getResponseBody());
                        },

                        (throwable) -> {
                            handleException(throwable, url, id);
                        }
                ));
            }

            // Setup a timer for the max wait-period
            timer = timerService.schedule(() -> onTimeout(), timeoutMs, MILLISECONDS);
        });

        // Ok, everything is now setup for asynchronous processing, let the play begin...
        LOG.debug("Asynch processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }

    /**
     * Performs a blocking db-lookup to find urls to make the calls to
     *
     * @return
     */
    private List<String> lookupUrlsInDb() {

        // Start of blocking db-lookup
        List<String> urls = new ArrayList<String>();

        // Simulate a blocking db-lookup by putting the current thread to sleep for a while...
        try {
            Thread.sleep(dbLookupMs);
        } catch (InterruptedException e) {}

        for (int i = 0; i < dbHits; i++) {
            // Use one and the same address for all returned URL's
            urls.add(baseUrl + "?minMs=" + minMs + "&maxMs=" + maxMs);
        }

        // Processing of blocking db-lookup done

        return urls;
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