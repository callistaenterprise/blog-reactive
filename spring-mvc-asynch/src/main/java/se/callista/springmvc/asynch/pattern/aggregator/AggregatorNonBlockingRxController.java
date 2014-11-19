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
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientRx;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Pär Wenåker <par.wenaker@callistaenterprise.se>
 */
@RestController
public class AggregatorNonBlockingRxController {

    private static final Logger LOG = LoggerFactory.getLogger(AggregatorNonBlockingRxController.class);

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    @Value("${aggregator.timeoutMs}")
    private int TIMEOUT_MS;

    @Autowired
    @Qualifier("dbThreadPoolExecutor")
    private TaskExecutor dbThreadPoolExecutor;

    @Autowired
    private AsyncHttpClientRx asyncHttpClientRx;

    private class Result {
        private String url = "";
        private int id;

        Result() {}

        Result(int id, String url) {
            this.id = id;
            this.url = url;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "url='" + url + '\'' +
                    ", index=" + id +
                    '}';
        }
    }

    /**
     * Sample usage: curl "http://localhost:9080/aggregate-non-blocking-rx?minMs=1000&maxMs=2000"
     *
     * @param dbLookupMs time for db lookup
     * @param dbHits number of results from db
     * @param minMs min execution time of remote
     * @param maxMs max execution time of remote
     *
     * @return deferred result
     *
     * @throws java.io.IOException
     */
    @RequestMapping("/aggregate-non-blocking-rx")
    public DeferredResult<String> nonBlockingAggregator(
        @RequestParam(value = "dbLookupMs", required = false, defaultValue = "0")    int dbLookupMs,
        @RequestParam(value = "dbHits",     required = false, defaultValue = "3")    int dbHits,
        @RequestParam(value = "minMs",      required = false, defaultValue = "0")    int minMs,
        @RequestParam(value = "maxMs",      required = false, defaultValue = "0")    int maxMs) throws IOException {

        DbLookup dbLookup = new DbLookup(dbLookupMs, dbHits);
        DeferredResult<String> deferredResult = new DeferredResult<>();

        Subscription subscription =
            Observable.from(dbLookup.lookupUrlsInDb(SP_NON_BLOCKING_URL, minMs, maxMs))
                .subscribeOn(Schedulers.from(dbThreadPoolExecutor))
                .scan(new Result(), (result, url) -> new Result(result.id + 1, url))
                .filter(result -> result.id > 0)
                .flatMap(result ->
                    asyncHttpClientRx
                        .observable(result.url)
                        .map(this::getResponseBody)
                        .map(Optional::of)
                        .timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS, Observable.empty())
                        .onErrorReturn(t -> handleException(t, result))
                )
                .doOnNext(this::logThread)
                .observeOn(Schedulers.computation())
                .doOnNext(this::logThread)
                .buffer(dbHits)
                .subscribe(v -> deferredResult.setResult(getTotalResult(v)));

        deferredResult.onCompletion(subscription::unsubscribe);

        return deferredResult;
    }

    private void logThread(Optional<String> r) {
        LOG.debug("Thread:[" + Thread.currentThread().getName()+"] : " + r);
    }

    private String getResponseBody(Response response) {
        try {
            return response.getResponseBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTotalResult(List<Optional<String>> resultArr) {
        String totalResult = "";
        for (Optional<String> r : resultArr)
            totalResult += r.map(v -> v + '\n').orElse("");
        return totalResult;
    }

    private Optional<String> handleException(Throwable throwable, Result result) {
        String msg;
        if (throwable instanceof TimeoutException) {
           msg = "Request #" + throwable + " failed due to service provider not responding within the configured timeout. Url: " + result.url;
           LOG.error(msg);
        } else {
           msg = "Request #" + result.id + " failed due to error: " + throwable;
           LOG.error(msg, throwable);
        }
        return Optional.ofNullable(msg);
     }
}
