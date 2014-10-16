package se.callista.springmvc.asynch.pattern.routingslip;

import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientLambdaAware;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class RoutingSlipNonBlockingLambdaController {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingSlipNonBlockingLambdaController.class);
    private static final AsyncHttpClientLambdaAware asyncHttpClient = new AsyncHttpClientLambdaAware();

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    private List<String> resultList = new ArrayList<>();

    /**
     * Sample usage: curl "http://localhost:9181/routing-slip-non-blocking-lambds"
     *
     * @return
     */
    @RequestMapping("/routing-slip-non-blocking-lambda")
    public DeferredResult<String> nonBlockingRoutingSlip() throws IOException {

        final DeferredResult<String> deferredResult = new DeferredResult<>();

        // Kick off the asynch processing of five sequentially executed asynch processing steps

        // Send request #1
        LOG.debug("Start req #1");
        ListenableFuture<Response> execute = asyncHttpClient.execute(getUrl(1),
                (Response r1) -> {
                    LOG.debug("Got resp #1");

                    // Process response #1
                    processResult(r1.getResponseBody());

                    // Send request #2
                    LOG.debug("Start req #2");
                    asyncHttpClient.execute(getUrl(2),
                            (Response r2) -> {
                                LOG.debug("Got resp #2");

                                // Process response #2
                                processResult(r2.getResponseBody());

                                // Send request #3
                                LOG.debug("Start req #3");
                                asyncHttpClient.execute(getUrl(3),
                                        (Response r3) -> {
                                            LOG.debug("Got resp #3");

                                            // Process response #3
                                            processResult(r3.getResponseBody());

                                            // Send request #4
                                            LOG.debug("Start req #4");
                                            asyncHttpClient.execute(getUrl(4),
                                                    (Response r4) -> {
                                                        LOG.debug("Got resp #4");

                                                        // Process response #4
                                                        processResult(r4.getResponseBody());

                                                        // Send request #5
                                                        LOG.debug("Start req #5");
                                                        asyncHttpClient.execute(getUrl(5),
                                                            (Response r5) -> {
                                                                LOG.debug("Got resp #5");

                                                                // Process response #5
                                                                processResult(r5.getResponseBody());

                                                                // Get the total result and set it on the deferred result
                                                                LOG.debug("We are done, setting the result on the deferred result!");
                                                                boolean deferredStatus = deferredResult.setResult(getTotalResult());

                                                                return r5;
                                                            });
                                                        return r4;
                                                    });
                                            return r3;
                                        });
                                return r2;
                            });
                    return r1;
                });

        // Return to let go of the precious thread we are holding on to...
        LOG.debug("Asynch processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }

    private String getUrl(int processingStepNo) {
        int sleeptimeMs = 100 * processingStepNo;
        return SP_NON_BLOCKING_URL + "?minMs=" + sleeptimeMs + "&maxMs=" + sleeptimeMs;
    }

    private void processResult(String result) {
        resultList.add(result);
    }

    private String getTotalResult() {
        String totalResult = "";
        for (String r : resultList)
            totalResult += r + '\n';
        return totalResult;
    }

}