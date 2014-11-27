package se.callista.springmvc.asynch.pattern.router;

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

import java.util.concurrent.TimeoutException;

import static se.callista.springmvc.asynch.common.AsyncHttpClientLambdaAware.createResponseEntity;

@RestController
public class RouterNonBlockingCallbackController {

    private static final Logger LOG = LoggerFactory.getLogger(RouterNonBlockingCallbackController.class);

    @Autowired
    private AsyncHttpClientLambdaAware asyncHttpClient;

    @Value("${sp.non_blocking.url}")
    private String spUrl;

    @Value("${sp.requestTimeoutMs}")
    private String spRequestTimeoutMs;

    /**
     * Sample usage: curl "http://localhost:9080/router-non-blocking-lambda?minMs=1000&maxMs=2000"
     *
     * @param minMs
     * @param maxMs
     * @return
     */
    @RequestMapping("/router-non-blocking-callback")
    public DeferredResult<ResponseEntity<String>> nonBlockingRouter(
        @RequestHeader(value = "Accept", required = false, defaultValue = "*/*") String accept,
        @RequestParam (value = "minMs",  required = false, defaultValue = "0")   int minMs,
        @RequestParam (value = "maxMs",  required = false, defaultValue = "0")   int maxMs) {

        final DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();

        String url = spUrl + "?minMs=" + minMs + "&maxMs=" + maxMs;

        LOG.debug("Start call...");
        asyncHttpClient.execute(url, accept,

                (throwable) -> {
                    handleException(throwable, url, deferredResult);
                },

                (response) -> {
                    LOG.debug("Got a response ({}), setting the result on the deferred result!", response.getStatusCode());
                    deferredResult.setResult(createResponseEntity(response));
                }
        );

        // Return to let go of the precious thread we are holding on to...
        LOG.debug("Asynch processing setup, return the request thread to the thread-pool");
        return deferredResult;
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