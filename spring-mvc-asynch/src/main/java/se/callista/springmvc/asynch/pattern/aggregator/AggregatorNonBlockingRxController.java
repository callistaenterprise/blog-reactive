package se.callista.springmvc.asynch.pattern.aggregator;

import com.ning.http.client.Response;
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
import java.util.concurrent.TimeUnit;

/**
 * @author Pär Wenåker <par.wenaker@callistaenterprise.se>
 */
@RestController
public class AggregatorNonBlockingRxController {

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    @Value("${aggregator.timeoutMs}")
    private int TIMEOUT_MS;

    @Autowired
    @Qualifier("dbThreadPoolExecutor")
    private TaskExecutor dbThreadPoolExecutor;

    @Autowired
    private AsyncHttpClientRx asyncHttpClientRx;

    /**
     * Sample usage: curl "http://localhost:9080/aggregate-non-blocking-rx?minMs=1000&maxMs=2000"
     *
     * @param dbLookupMs
     * @param dbHits
     * @param minMs
     * @param maxMs
     * @return
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

        String url = SP_NON_BLOCKING_URL + "?minMs=" + minMs + "&maxMs=" + maxMs;

        Subscription subscription =
                Observable.<Integer>create(s ->
                        s.onNext(dbLookup.executeDbLookup())
                )
                .subscribeOn(Schedulers.from(dbThreadPoolExecutor))
                .flatMap(noOfCalls -> Observable.just(url).repeat(noOfCalls))
                .flatMap(u ->
                        asyncHttpClientRx.observable(u)
                                .map(this::getResponseBody)
                                .onErrorReturn(t -> "error")
                )
                .buffer(TIMEOUT_MS, TimeUnit.MILLISECONDS, dbHits)
                .subscribe(v -> deferredResult.setResult(getTotalResult(v)));

        deferredResult.onCompletion(subscription::unsubscribe);

        return deferredResult;
    }

    private String getResponseBody(Response response) {
        try {
            return response.getResponseBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTotalResult(List<String> resultArr) {
        String totalResult = "";
        for (String r : resultArr)
            totalResult += r + '\n';
        return totalResult;
    }

}
