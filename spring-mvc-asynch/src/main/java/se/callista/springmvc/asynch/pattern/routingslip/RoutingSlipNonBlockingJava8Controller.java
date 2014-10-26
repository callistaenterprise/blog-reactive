package se.callista.springmvc.asynch.pattern.routingslip;

import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientJava8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@RestController
public class RoutingSlipNonBlockingJava8Controller {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingSlipNonBlockingJava8Controller.class);

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

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

        doAsyncCall(new ArrayList<>(), 1)
                .thenCompose(result -> doAsyncCall(result, 2))
                .thenCompose(result -> doAsyncCall(result, 3))
                .thenCompose(result -> doAsyncCall(result, 4))
                .thenCompose(result -> doAsyncCall(result, 5))
                .thenApply(result -> deferredResult.setResult(getTotalResult(result)));

        return deferredResult;
    }

    private CompletableFuture<List<String>> doAsyncCall(List<String> result, int num) {
        LOG.debug("Start req #{}", num);
        return asyncHttpClientJava8
                .execute(getUrl(num))
                .thenApply(logResponse(num))
                .thenApply(getResponseBody())
                .thenApply(addBodyToResult(result));
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

    private Function<Response, Response> logResponse(int num) {
        return r -> {
            LOG.debug("Got resp #{}", num);
            return r;
        };
    }

    private Function<String, List<String>> addBodyToResult(List<String> result) {
        return response -> {
            result.add(response);
            return result;
        };
    }

    private String getUrl(int processingStepNo) {
        int sleeptimeMs = 100 * processingStepNo;
        return SP_NON_BLOCKING_URL + "?minMs=" + sleeptimeMs + "&maxMs=" + sleeptimeMs;
    }

    private String getTotalResult(List<String> resultList) {
        String totalResult = "";
        for (String r : resultList)
            totalResult += r + '\n';
        return totalResult;
    }

}