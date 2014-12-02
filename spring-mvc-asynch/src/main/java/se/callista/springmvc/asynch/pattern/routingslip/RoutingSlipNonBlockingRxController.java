package se.callista.springmvc.asynch.pattern.routingslip;

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
import rx.Observable;
import rx.Subscription;
import se.callista.springmvc.asynch.common.AsyncCallException;
import se.callista.springmvc.asynch.common.AsyncHttpClientRx;
import se.callista.springmvc.asynch.common.RequestFailureException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static se.callista.springmvc.asynch.common.AsyncHttpClientRx.createResponseEntity;

@RestController
public class RoutingSlipNonBlockingRxController {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingSlipNonBlockingRxController.class);

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    @Value("${sp.requestTimeoutMs}")
    private String spRequestTimeoutMs;

    @Autowired
    private AsyncHttpClientRx asyncHttpClientRx;

    private class Result {
        final List<String> resultList = new ArrayList<>();
        final String accept;
        final String qry;
        boolean lastCallStatus = true;

        Result(String accept, String qry) {
            this.accept = accept;
            this.qry = qry;
        }

        Result processResponse(Response response) {
            //if(!isResponseOk(response)) throw new RequestFailureException(response);

            resultList.add(getResponseBody(response));
            lastCallStatus = true;
            return this;
        }

        ResponseEntity<String> getTotalResult() {
            return createResponseEntity(resultList.stream().collect(Collectors.joining("\n"))+"\n", HttpStatus.OK);
        }

    }

    /**
     * Sample usage: curl "http://localhost:9080/routing-slip-non-blocking-rx"
     *
     * @return a deferred result
     */
    @RequestMapping("/routing-slip-non-blocking-rx")
    public DeferredResult<ResponseEntity<String>> nonBlockingRoutingSlip(
            @RequestHeader(value = "Accept", required = false, defaultValue = "*/*") String accept,
            @RequestParam(value = "qry",    required = false, defaultValue = "")    String qry) throws IOException {

        final DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();

        Subscription subscription = Observable.just(new Result(accept, qry))
                .flatMap(result -> doAsyncCall(result, 1))
                .flatMap(result -> doAsyncCall(result, 2))
                .flatMap(result ->
                    result.lastCallStatus ?
                        doAsyncCall(result, 4) :
                        doAsyncCall(result, 3)
                )
                .flatMap(result -> doAsyncCall(result, 5))
                .subscribe(
                        result -> deferredResult.setResult(result.getTotalResult()),
                        t -> deferredResult.setErrorResult(handleException(t))
                );

        deferredResult.onCompletion(subscription::unsubscribe);

        return deferredResult;
    }

    private Observable<Result> doAsyncCall(Result result, int num) {
        LOG.debug("Start req #{}", num);
        String url = getUrl(num, result.qry);
        return asyncHttpClientRx
                .observable(url, result.accept)
                .doOnNext(r -> LOG.debug("Got resp #{}", num))
                .flatMap(r -> isResponseOk(r) ?
                        Observable.just(r) :
                        Observable.error(new RequestFailureException(r)))
                .map(result::processResponse)
                .onErrorResumeNext(t -> Observable.error(new AsyncCallException(url, t)));
    }

    private String getResponseBody(Response response) {
        try {
            return response.getResponseBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUrl(int processingStepNo, String qry) {
        int sleeptimeMs = 100 * processingStepNo;
        String url = SP_NON_BLOCKING_URL + "?minMs=" + sleeptimeMs + "&maxMs=" + sleeptimeMs;
        if (qry != null && qry.length() > 0) url += "&qry=" + qry;
        return url;
    }

    private boolean isResponseOk(Response r) {
        return HttpStatus.valueOf(r.getStatusCode()).is2xxSuccessful();
    }

    private ResponseEntity<String> handleException(Throwable t) {
        String url = "";
        Throwable cause = t;

      	if (t instanceof AsyncCallException) {
      	    AsyncCallException ae = (AsyncCallException) t;
      		url = ae.getUrl();
      		cause = ae.getCause();
      	}

        if(cause instanceof RequestFailureException) {
            return createResponseEntity(((RequestFailureException)cause).getResponse());
        } else {
            String msg;
            HttpStatus status;
            if (cause instanceof TimeoutException) {
                msg = "Request failed due to service provider not responding within " + spRequestTimeoutMs + " ms. Url: " + url;
                status = HttpStatus.GATEWAY_TIMEOUT;
            } else {
                msg = "Request failed due to error: " + cause;
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            LOG.error(msg);
            return new ResponseEntity<>(msg, status);
        }
    }
}