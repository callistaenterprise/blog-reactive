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
import java.util.function.Function;

@RestController
public class RoutingSlipNonBlockingRxController {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingSlipNonBlockingRxController.class);

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    @Autowired
    AsyncHttpClientRx asyncHttpClientRx;

    /**
     * Sample usage: curl "http://localhost:9080/routing-slip-non-blocking-rx"
     *
     * @return
     */
    @RequestMapping("/routing-slip-non-blocking-rx")
    public DeferredResult<String> nonBlockingRoutingSlip() throws IOException {

        final DeferredResult<String> deferredResult = new DeferredResult<>();

        Subscription subscription = Observable.<List<String>>just(new ArrayList<>())
                .flatMap(result -> doAsyncCall(result, 1, this::processResult))
                .flatMap(result -> doAsyncCall(result, 2, this::processResult))
                .flatMap(result -> doAsyncCall(result, 3, this::processResult))
                .flatMap(result -> doAsyncCall(result, 4, this::processResult))
                .flatMap(result -> doAsyncCall(result, 5, this::processResult))
                .subscribe(v -> deferredResult.setResult(getTotalResult(v)));

        deferredResult.onCompletion(subscription::unsubscribe);

        return deferredResult;
    }

    private Observable<List<String>> doAsyncCall(List<String> result, int num, Function<String, String> processor) {
        LOG.debug("Start req #{}", num);
        return asyncHttpClientRx
                .observable(getUrl(num))
                .doOnNext(r -> LOG.debug("Got resp #{}", num))
                .map(this::getResponseBody)
                .map(response -> {
                    result.add(processor.apply(response));
                    return result;
                });
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

    private String processResult(String result) {
        return result;
    }

    private String getTotalResult(List<String> resultList) {
        String totalResult = "";
        for (String r : resultList)
            totalResult += r + '\n';
        return totalResult;
    }

}