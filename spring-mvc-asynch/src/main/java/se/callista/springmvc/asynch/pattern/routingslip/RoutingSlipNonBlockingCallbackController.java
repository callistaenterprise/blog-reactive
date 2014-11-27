package se.callista.springmvc.asynch.pattern.routingslip;

import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.AsyncHttpClientLambdaAware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static se.callista.springmvc.asynch.common.AsyncHttpClientLambdaAware.createResponseEntity;

@RestController
public class RoutingSlipNonBlockingCallbackController {

    private class State {
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

    @Value("${sp.requestTimeoutMs}")
    private String spRequestTimeoutMs;

    /**
     * Sample usage: curl "http://localhost:9181/routing-slip-non-blocking-lambds"
     *
     * @return
     */
    @RequestMapping("/routing-slip-non-blocking-callback")
    public DeferredResult<ResponseEntity<String>> nonBlockingRoutingSlip(
        @RequestHeader(value = "Accept", required = false, defaultValue = "*/*") String accept,
        @RequestParam (value = "qry",    required = false, defaultValue = "")    String qry) {

        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();
        State state = new State();

        // Kick off the asynch processing of five sequentially executed asynch processing steps

        // Send request #1
        LOG.debug("Start req #1");
        String url1 = getUrl(1, qry);
        ListenableFuture<Response> execute = asyncHttpClient.execute(url1, accept,

            (throwable) -> {
                handleException(throwable, url1, deferredResult);
            },

            (Response r1) -> {
                LOG.debug("Got resp #1 ({})", r1.getStatusCode());

                // Check response #1, if not ok simply pass back the error and abort further processing
                if (!isResponseOk(r1)) {
                    deferredResult.setResult(createResponseEntity(r1));
                    return;
                }

                state.processResult(r1.getResponseBody());

                // Send request #2
                LOG.debug("Start req #2");
                String url2 = getUrl(2, qry);
                asyncHttpClient.execute(url2, accept,

                    (throwable) -> {
                        handleException(throwable, url2, deferredResult);
                    },

                    (Response r2) -> {
                        LOG.debug("Got resp #2 ({})", r1.getStatusCode());

                        // Check response #2, if not ok simply pass back the error and abort further processing
                        if (!isResponseOk(r2)) {
                            deferredResult.setResult(createResponseEntity(r1));
                            return;
                        }

                        // Process response #2
                        boolean ok = state.processResult(r2.getResponseBody());

                        // Select the next step depending on the outcome from the previous step
                        int reqId = ok ? 4 : 3;
                        String url3or4 = getUrl(reqId, qry);

                        // Send request #3 or #4
                        LOG.debug("Start req #{}", reqId);
                        asyncHttpClient.execute(url3or4, accept,

                            (throwable) -> {
                                handleException(throwable, url3or4, deferredResult);
                            },

                            (Response r3or4) -> {
                                LOG.debug("Got resp #{} ({})", reqId, r1.getStatusCode());

                                // Check response #3/4, if not ok simply pass back the error and abort further processing
                                if (!isResponseOk(r3or4)) {
                                    deferredResult.setResult(createResponseEntity(r1));
                                    return;
                                }


                                // Process response #3 or #4
                                state.processResult(r3or4.getResponseBody());

                                // Send request #5
                                LOG.debug("Start req #5");
                                String url5 = getUrl(5, qry);
                                asyncHttpClient.execute(url5, accept,

                                    (throwable) -> {
                                        handleException(throwable, url5, deferredResult);
                                    },

                                    (Response r5) -> {
                                        // Check response #5, if not ok simply pass back the error and abort further processing
                                        if (!isResponseOk(r5)) {
                                            deferredResult.setResult(createResponseEntity(r1));
                                            return;
                                        }

                                        LOG.debug("Got resp #5 ({})", r1.getStatusCode());

                                        // Process response #5
                                        state.processResult(r5.getResponseBody());

                                        // Get the total result and set it on the deferred result
                                        LOG.debug("We are done, setting the result on the deferred result!");
                                        boolean deferredStatus = deferredResult.setResult(createResponseEntity(state.getTotalResult(), HttpStatus.OK));
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

    private String getUrl(int processingStepNo, String qry) {
        int sleeptimeMs = 100 * processingStepNo;
        String url = spUrl + "?minMs=" + sleeptimeMs + "&maxMs=" + sleeptimeMs;
        if (qry != null && qry.length() > 0) url += "&qry=" + qry;
        return url;
    }

    private boolean isResponseOk(Response r1) {
        return HttpStatus.valueOf(r1.getStatusCode()).is2xxSuccessful();
    }

    private void handleException(Throwable throwable, String url, DeferredResult<ResponseEntity<String>> deferredResult) {
        if (throwable instanceof TimeoutException) {
            String msg = "Request failed due to service provider not responding within " + spRequestTimeoutMs + " ms. Url: " + url;
            LOG.error(msg);
            deferredResult.setResult(new ResponseEntity<String>(msg, HttpStatus.GATEWAY_TIMEOUT));

        } else {
            String msg = "Request failed due to error: " + throwable;
            LOG.error(msg);
            deferredResult.setResult(new ResponseEntity<String>(msg, HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
}