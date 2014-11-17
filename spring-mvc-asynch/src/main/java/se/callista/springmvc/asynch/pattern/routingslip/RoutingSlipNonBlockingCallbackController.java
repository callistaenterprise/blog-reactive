package se.callista.springmvc.asynch.pattern.routingslip;

import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientLambdaAware;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RoutingSlipNonBlockingCallbackController {

    private class Result {
        List<String> resultList = new ArrayList<>();

        public boolean processResult(String result) {
            resultList.add(result);
            return true;
        }

        public String getTotalResult() {
            String totalResult = "";
            for (String r : resultList)
                totalResult += r + '\n';
            return totalResult;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RoutingSlipNonBlockingCallbackController.class);

    @Autowired
    private AsyncHttpClientLambdaAware asyncHttpClient;

    @Value("${sp.non_blocking.url}")
    private String spUrl;

    /**
     * Sample usage: curl "http://localhost:9181/routing-slip-non-blocking-lambds"
     *
     * @return
     */
    @RequestMapping("/routing-slip-non-blocking-callback")
    public DeferredResult<String> nonBlockingRoutingSlip(
        @RequestHeader(value = "Accept", required = false, defaultValue = "*/*") String accept) {

        DeferredResult<String> deferredResult = new DeferredResult<>();
        Result r = new Result();

        // Kick off the asynch processing of five sequentially executed asynch processing steps

        // Send request #1
        LOG.debug("Start req #1");
        ListenableFuture<Response> execute = asyncHttpClient.execute(getUrl(1),
            (Response r1) -> {
                LOG.debug("Got resp #1");

                // Process response #1
                r.processResult(r1.getResponseBody());

                // Send request #2
                LOG.debug("Start req #2");
                asyncHttpClient.execute(getUrl(2),
                    (Response r2) -> {
                        LOG.debug("Got resp #2");

                        // Process response #2
                        boolean ok = r.processResult(r2.getResponseBody());
                        int reqId = ok ? 4 : 3;

                        // Send request #3 or #4
                        LOG.debug("Start req #{}", reqId);
                        asyncHttpClient.execute(getUrl(reqId),
                            (Response r3or4) -> {
                                LOG.debug("Got resp #{}", reqId);

                                // Process response #3 or #4
                                r.processResult(r3or4.getResponseBody());

                                // Send request #5
                                LOG.debug("Start req #5");
                                asyncHttpClient.execute(getUrl(5),
                                    (Response r5) -> {
                                        LOG.debug("Got resp #5");

                                        // Process response #5
                                        r.processResult(r5.getResponseBody());

                                        // Get the total result and set it on the deferred result
                                        LOG.debug("We are done, setting the result on the deferred result!");
                                        boolean deferredStatus = deferredResult.setResult(r.getTotalResult());

                                    }
                                );
                            }
                        );
                    }
                );
            }
        );

        // Return to let go of the precious thread we are holding on to...
        LOG.debug("Asynch processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }

    private String getUrl(int processingStepNo) {
        int sleeptimeMs = 100 * processingStepNo;
        return spUrl + "?minMs=" + sleeptimeMs + "&maxMs=" + sleeptimeMs;
    }
}