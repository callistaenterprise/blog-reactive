package se.callista.springmvc.asynch.pattern.aggregator;

import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientLambdaAware;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by magnus on 20/07/14.
 */
public class Executor {

    final private String baseUrl;
    final private int timeoutMs;
    final private TaskExecutor dbThreadPoolExecutor;
    final private int dbLookupMs;
    final private int dbHits;
    final private int minMs;
    final private int maxMs;

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);
    private static final AsyncHttpClientLambdaAware asyncHttpClient = new AsyncHttpClientLambdaAware();
    private static Timer timer = new Timer();
    private TimerTask timeoutTask = null;
    private List<ListenableFuture<Response>> concurrentExecutors = new ArrayList<>();
    final AtomicInteger noOfResults = new AtomicInteger(0);
    private int noOfCalls = 0;
    private DeferredResult<String> deferredResult = null;
    private List<String> resultArr = new ArrayList<>();

    public Executor(String baseUrl, int timeoutMs, TaskExecutor dbThreadPoolExecutor, int dbLookupMs, int dbHits, int minMs, int maxMs) {
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.dbThreadPoolExecutor = dbThreadPoolExecutor;
        this.dbLookupMs = dbLookupMs;
        this.dbHits = dbHits;
        this.minMs = minMs;
        this.maxMs = maxMs;
    }

    public DeferredResult<String> startNonBlockingExecution() {

        DbLookup dbLookup = new DbLookup(dbLookupMs, dbHits);
        deferredResult = new DeferredResult<String>();

        dbThreadPoolExecutor.execute(() -> {
            noOfCalls = dbLookup.executeDbLookup();
            try {
                for (int i = 0; i < noOfCalls; i++) {
                    final int id = i+1;

                    String url = baseUrl + "?minMs=" + minMs + "&maxMs=" + maxMs;
                    LOG.debug("Start asynch call #{}", id);
                    concurrentExecutors.add(asyncHttpClient.execute(url, (response) -> {
                        LOG.debug("Received asynch response #{}", id);

                        int httpStatus = response.getStatusCode();
                        // TODO: Handle status codes other than 200...

                        // If many requests completes at the same time the following code must be executed in sequence for one thread at a time
                        // Since we don't have any Actor-like mechanism to rely on (for the time being...) we simply ensure that the code block is executed by one thread at a time by an old school synchronized block
                        // Since the processing in the block is very limited it will not cause a bottleneck.
                        synchronized (resultArr) {
                            // Count down, aggregate answer and return if all answers (also cancel timer)...
                            int noOfRes = noOfResults.incrementAndGet();

                            // Perform the aggregation...
                            LOG.debug("Safely adding response #{}", id);
                            resultArr.add(response.getResponseBody());

                            if (noOfRes >= noOfCalls) {
                                onAllCompleted();
                            }
                        }
                        return response;
                    }));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Setup a timer for the max wait-period
            timeoutTask = new TimerTask() {

                @Override
                public void run() {
                    onTimeout();
                }
            };

            // Schedule the timeout task
            timer.schedule(timeoutTask, timeoutMs);

        });

        // Ok, everything is now setup for asynchronous processing, let the play begin...
        LOG.debug("Asynch processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }

    private void onTimeout() {

        LOG.debug("Timeout! Cancel ongoing requests!");

        // complete missing answers and return ...
        int i = 0;
        for (ListenableFuture<Response> executor : concurrentExecutors) {
            if (!executor.isDone()) {
                LOG.debug("Cancel a request due to timeout!");
                executor.cancel(true);
            }
            i++;
        }
        onAllCompleted();
    }

    private void onAllCompleted() {
        timeoutTask.cancel();

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