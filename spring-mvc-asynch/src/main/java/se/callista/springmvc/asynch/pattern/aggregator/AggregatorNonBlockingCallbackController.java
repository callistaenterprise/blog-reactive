package se.callista.springmvc.asynch.pattern.aggregator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientLambdaAware;
import se.callista.springmvc.asynch.pattern.aggregator.callbacksupport.Executor;

import java.util.concurrent.ScheduledExecutorService;

@RestController
public class AggregatorNonBlockingCallbackController {

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

        // Delegate the whole processing to a executor-instance to avoid concurrency problems with other concurrent requests
        Executor exec = new Executor(spUrl, accept, aggTimeoutMs, dbThreadPoolExecutor, dbLookupMs, dbHits, minMs, maxMs, asyncHttpClient, timerService);

        DeferredResult<String> deferredResult = exec.startNonBlockingExecution();

        // Return to let go of the precious thread we are holding on to...
        return deferredResult;
    }
}