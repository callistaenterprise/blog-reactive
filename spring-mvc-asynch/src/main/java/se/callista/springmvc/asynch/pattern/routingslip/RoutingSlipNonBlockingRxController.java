package se.callista.springmvc.asynch.pattern.routingslip;

import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import rx.Observable;
import rx.Subscription;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientRx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class RoutingSlipNonBlockingRxController {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingSlipNonBlockingRxController.class);

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    @Autowired
    AsyncHttpClientRx asyncHttpClientRx;

    private class Result {
        private final List<String> resultList = new ArrayList<>();
        private boolean lastCallStatus = true;

        private Result processResult(String result) {
            resultList.add(result);
            lastCallStatus = true;
            return this;
        }

        private String getTotalResult() {
            String totalResult = "";
            for (String r : resultList)
                totalResult += r + '\n';
            return totalResult;
        }
    }

    /**
     * Sample usage: curl "http://localhost:9080/routing-slip-non-blocking-rx"
     *
     * @return a deferred result
     */
    @RequestMapping("/routing-slip-non-blocking-rx")
    public DeferredResult<String> nonBlockingRoutingSlip() throws IOException {

        final DeferredResult<String> deferredResult = new DeferredResult<>();

        Subscription subscription = Observable.just(new Result())
                .flatMap(result -> doAsyncCall(result, 1))
                .flatMap(result -> doAsyncCall(result, 2))
                .flatMap(result ->
                    result.lastCallStatus ?
                        doAsyncCall(result, 4) :
                        doAsyncCall(result, 3)
                )
                .flatMap(result -> doAsyncCall(result, 5))
                .subscribe(result -> deferredResult.setResult(result.getTotalResult()));

        deferredResult.onCompletion(subscription::unsubscribe);

        return deferredResult;
    }

    private Observable<Result> doAsyncCall(Result result, int num) {
        LOG.debug("Start req #{}", num);
        return asyncHttpClientRx
                .observable(getUrl(num))
                .doOnNext(r -> LOG.debug("Got resp #{}", num))
                .map(this::getResponseBody)
                .map(result::processResult);
    }

    private String getResponseBody(Response response) {
        try {
            return response.getResponseBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUrl(int processingStepNo) {
        int sleeptimeMs = 100 * processingStepNo;
        return SP_NON_BLOCKING_URL + "?minMs=" + sleeptimeMs + "&maxMs=" + sleeptimeMs;
    }


}