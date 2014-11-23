package se.callista.springmvc.asynch.pattern.aggregator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import se.callista.springmvc.asynch.Application;
import se.callista.springmvc.asynch.common.AsynchTestBase;

import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Created by magnus on 29/05/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class AggregatorControllerTest extends AsynchTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(AggregatorControllerTest.class);

    @Value("${aggregator.timeoutMs}")
    private int timeoutMs;

    private final int timeoutDbHits = 20;

    private final String expectedResultSingle = "{\"status\":\"Ok\",\"processingTimeMs\":2000}";
    private final String expectedResult =
        expectedResultSingle + '\n' +
        expectedResultSingle + '\n' +
        expectedResultSingle + '\n';

    private final String expectedErrorResultSingle = "Error: maxMs < minMs  (100 < 200)";
    private final String expectedErrorResult =
        expectedErrorResultSingle + '\n' +
        expectedErrorResultSingle + '\n' +
        expectedErrorResultSingle + '\n';

    /*
     * Callback tests
     */

    @Test
    public void testAggregatorNonBlockingCallback() throws Exception {
        String url = "/aggregate-non-blocking-callback?minMs=2000&maxMs=2000&dbHits=3";
        testNonBlocking(url, SC_OK, expectedResult);
    }

    @Test
    public void testAggregatorNonBlockingCallbackInvalidInput() throws Exception {
        String url = "/aggregate-non-blocking-callback?minMs=200&maxMs=100";
        testNonBlocking(url, SC_OK, expectedErrorResult);
    }

    @Test
    public void testAggregatorNonBlockingCallbackTimeout() throws Exception {
        String url = "/aggregate-non-blocking-callback" + getTimeoutQueryString();
        testNonBlockingTimeout(url, timeoutDbHits, timeoutMs);
    }


    /*
     * RX tests
     */

    @Test
    public void testAggregatorNonBlockingRx() throws Exception {
        String url = "/aggregate-non-blocking-rx?minMs=2000&maxMs=2000&dbHits=3";
        testNonBlocking(url, SC_OK, expectedResult);
    }

    @Test
    public void testAggregatorNonBlockingRxInvalidInput() throws Exception {
        String url = "/aggregate-non-blocking-rx?minMs=200&maxMs=100";
        testNonBlocking(url, SC_OK, expectedErrorResult);
    }

    @Test
    public void testAggregatorNonBlockingRxTimeout() throws Exception {
        String url = "/aggregate-non-blocking-rx" + getTimeoutQueryString();
        testNonBlockingTimeout(url, timeoutDbHits, timeoutMs);
    }


    /*
     * Java8 tests
     */

    @Test
    public void testAggregatorNonBlockingJava8() throws Exception {
        String url = "/aggregate-non-blocking-java8?minMs=2000&maxMs=2000&dbHits=3";
        testNonBlocking(url, SC_OK, expectedResult.trim());
    }

    @Test
    public void testAggregatorNonBlockingJava8InvalidInput() throws Exception {
        String url = "/aggregate-non-blocking-java8?minMs=200&maxMs=100";
        testNonBlocking(url, SC_OK, expectedErrorResult.trim());
    }

    @Test
    public void testAggregatorNonBlockingJava8Timeout() throws Exception {
        String url = "/aggregate-non-blocking-java8" + getTimeoutQueryString();
        testNonBlockingTimeout(url, timeoutDbHits, timeoutMs);
    }


    /*
     * Akka Futures tests
     */

    @Test
    public void testAggregatorNonBlockingAkkaFutures() throws Exception {
        String url = "/aggregate-non-blocking-akkafutures?minMs=2000&maxMs=2000&dbHits=3";
        testNonBlocking(url, SC_OK, expectedResult);
    }

    @Test
    public void testAggregatorNonBlockingAkkaInvalidInput() throws Exception {
        String url = "/aggregate-non-blocking-akkafutures?minMs=200&maxMs=100";
        testNonBlocking(url, SC_OK, expectedErrorResult);
    }

    @Test
    public void testAggregatorNonBlockingAkkaFuturesTimeout() throws Exception {
        String url = "/aggregate-non-blocking-akkafutures" + getTimeoutQueryString();
        testNonBlockingTimeout(url, timeoutDbHits, timeoutMs);
    }


    private String getTimeoutQueryString() {
        int timeoutMinMs = (timeoutMs < 1000) ? 0 : timeoutMs - 1000;
        int timeoutMaxMs = timeoutMs + 1000;
        return "?dbHits=" + timeoutDbHits + "&minMs=" + timeoutMinMs + "&maxMs=" + timeoutMaxMs;
    }
}