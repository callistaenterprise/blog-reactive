package se.callista.springmvc.asynch.pattern.aggregator;

import akka.actor.ActorSystem;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import com.ning.http.client.Response;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.FiniteDuration;
import se.callista.springmvc.asynch.common.AsyncHttpClientAkka;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static akka.dispatch.Futures.future;
import static se.callista.springmvc.asynch.common.AkkaUtils.completer;
import static se.callista.springmvc.asynch.common.AkkaUtils.mapper;
import static se.callista.springmvc.asynch.common.AkkaUtils.runnable;

/**
 * @author Pär Wenåker <par.wenaker@callistaenterprise.se>
 */
@RestController
public class AggregatorNonBlockingAkkaFuturesController implements InitializingBean{

    @Value("${sp.non_blocking.url}")
    private String SP_NON_BLOCKING_URL;

    @Value("${aggregator.timeoutMs}")
    private int TIMEOUT_MS;

    @Autowired
    @Qualifier("dbThreadPoolExecutor")
    private TaskExecutor dbThreadPoolExecutor;

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private AsyncHttpClientAkka asyncHttpClientAkka;

    ExecutionContextExecutor ec;

    /**
     * Sample usage: curl "http://localhost:9080/aggregate-non-blocking-rx?minMs=1000&maxMs=2000"
     */
    @RequestMapping("/aggregate-non-blocking-akkafutures")
    public DeferredResult<String> nonBlockingAggregator(
        @RequestParam(value = "dbLookupMs", required = false, defaultValue = "0")    int dbLookupMs,
        @RequestParam(value = "dbHits",     required = false, defaultValue = "3")    int dbHits,
        @RequestParam(value = "minMs",      required = false, defaultValue = "0")    int minMs,
        @RequestParam(value = "maxMs",      required = false, defaultValue = "0")    int maxMs) throws IOException {

        DbLookup dbLookup = new DbLookup(dbLookupMs, dbHits);
        DeferredResult<String> deferredResult = new DeferredResult<>();

        String url = SP_NON_BLOCKING_URL + "?minMs=" + minMs + "&maxMs=" + maxMs;

        ExecutionContext dbEc = ExecutionContexts.fromExecutor(dbThreadPoolExecutor);

        future(dbLookup::executeDbLookup, dbEc)
            .map(mapper(noOfCalls -> IntStream.range(0, noOfCalls)), ec)
            .flatMap(mapper(intStream ->
                Futures.sequence(intStream.mapToObj(n ->
                    doAsyncCall(url, 3)
                ).collect(Collectors.toList()), ec)
            ), ec)
            .map(mapper(responses ->
                StreamSupport.stream(responses.spliterator(), false)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(this::getResponseBody)
                    .collect(Collectors.toList())
            ), ec)
            .onComplete(completer(
                    deferredResult::setErrorResult,
                    results -> deferredResult.setResult(getTotalResult(results))
            ), ec);

        return deferredResult;
    }

    private Future<Optional<Response>> doAsyncCall(String url, long seconds) {
        Promise<Optional<Response>> promise = Futures.promise();

        actorSystem.scheduler().scheduleOnce(
                new FiniteDuration(seconds, TimeUnit.SECONDS),
                runnable(() -> promise.success(Optional.empty())),
                ec);

        Future<Optional<Response>> response =
                asyncHttpClientAkka.execute(url).map(mapper(Optional::of), ec);

        return Futures.firstCompletedOf(Arrays.asList(response, promise.future()), ec);
    }

    private String getResponseBody(Response response) {
        try {
            return response.getResponseBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTotalResult(List<String> resultArr) {
        String totalResult = "";
        for (String r : resultArr)
            totalResult += r + '\n';
        return totalResult;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.ec = actorSystem.dispatcher();
    }
}
