package se.callista.springmvc.asynch.pattern.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.springmvc.asynch.common.lambdasupport.AsyncHttpClientLambdaAware;

import javax.annotation.PostConstruct;
import java.io.IOException;

@RestController
public class RouterNonBlockingLambdaController {

    private static final Logger LOG = LoggerFactory.getLogger(RouterNonBlockingLambdaController.class);
    private static final AsyncHttpClientLambdaAware asyncHttpClient = new AsyncHttpClientLambdaAware();

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    /**
     * Sample usage: curl "http://localhost:9080/router-non-blocking-lambda?minMs=1000&maxMs=2000"
     *
     * @param minMs
     * @param maxMs
     * @return
     * @throws java.io.IOException
     */
    @RequestMapping("/router-non-blocking-lambda")
    public DeferredResult<String> nonBlockingRouter(
        @RequestParam(value = "minMs", required = false, defaultValue = "0") int minMs,
        @RequestParam(value = "maxMs", required = false, defaultValue = "0") int maxMs) throws IOException {

        final DeferredResult<String> deferredResult = new DeferredResult<>();

        String url = SP_NON_BLOCKING_URL + "?minMs=" + minMs + "&maxMs=" + maxMs;

        LOG.debug("Start call...");
        asyncHttpClient.execute(url,

                (response) -> {
                    LOG.debug("Got response");
                    int httpStatus = response.getStatusCode();
                    // TODO: Handle status codes other than 200...

                    if (deferredResult.isSetOrExpired()) {
                        // TODO: Handle already set or expired error
                        LOG.warn("Result already set or expired");

                    } else {
                        LOG.debug("We are done, setting the result on the deferred result!");
                        boolean deferredStatus = deferredResult.setResult(response.getResponseBody());
                    }
                    return response;
                },

                (throwable) -> {
                    // TODO: Handle asynchronous processing errors...
                    LOG.warn("Asynchronous processing failed", throwable);
                }
        );

        // Return to let go of the precious thread we are holding on to...
        LOG.debug("Asynch processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }
}