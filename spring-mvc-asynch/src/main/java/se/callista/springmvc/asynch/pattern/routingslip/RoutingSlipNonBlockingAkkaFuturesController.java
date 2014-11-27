package se.callista.springmvc.asynch.pattern.routingslip;

import akka.actor.ActorSystem;
import akka.dispatch.Mapper;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import se.callista.springmvc.asynch.common.AsyncHttpClientAkka;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static se.callista.springmvc.asynch.common.AkkaUtils.completer;
import static se.callista.springmvc.asynch.common.AkkaUtils.mapper;

/**
 * @author Pär Wenåker <par.wenaker@callistaenterprise.se>
 */
@RestController
public class RoutingSlipNonBlockingAkkaFuturesController {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingSlipNonBlockingAkkaFuturesController.class);

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    @Autowired
    AsyncHttpClientAkka asyncHttpClientAkka;

    @Autowired
    ActorSystem actorSystem;

    /**
     * Sample usage: curl "http://localhost:9080/routing-slip-non-blocking-java8"
     *
     * @return
     */
    @RequestMapping("/routing-slip-non-blocking-akkafutures")
    public DeferredResult<String> nonBlockingRoutingSlip() throws IOException {

        final DeferredResult<String> deferredResult = new DeferredResult<>();

        ExecutionContextExecutor ec = actorSystem.dispatcher();

        doAsyncCall(new ArrayList<>(), 1)
                .flatMap(mapper(result -> doAsyncCall(result, 2)), ec)
                .flatMap(mapper(result -> doAsyncCall(result, 3)), ec)
                .flatMap(mapper(result -> doAsyncCall(result, 4)), ec)
                .flatMap(mapper(result -> doAsyncCall(result, 5)), ec)
                .onComplete(completer(
                        deferredResult::setErrorResult,
                        result -> deferredResult.setResult(getTotalResult(result))), ec);

        return deferredResult;
    }

    private Future<List<String>> doAsyncCall(List<String> result, int num) {
        LOG.debug("Start req #{}", num);
        ExecutionContextExecutor ec = actorSystem.dispatcher();

        return asyncHttpClientAkka
                .execute(getUrl(num))
                .map(logResponse(num), ec)
                .map(getResponseBody(), ec)
                .map(addBodyToResult(result), ec);
    }

    private Mapper<Response, String> getResponseBody() {
        return mapper(response -> {
            try {
                return response.getResponseBody();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Mapper<Response, Response> logResponse(int num) {
        return mapper(r -> {
            LOG.debug("Got resp #{}", num);
            return r;
        });
    }

    private Mapper<String, List<String>> addBodyToResult(List<String> result) {
        return mapper(response -> {
            result.add(response);
            return result;
        });
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