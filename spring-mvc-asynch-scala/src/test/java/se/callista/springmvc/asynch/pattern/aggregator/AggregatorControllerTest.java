package se.callista.springmvc.asynch.pattern.aggregator;

import org.junit.Ignore;
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
     * Scala tests
     */

    @Test
    public void testAggregatorNonBlockingScala() throws Exception {
        String url = "/aggregate-non-blocking-scala?minMs=2000&maxMs=2000&dbHits=3";
        testNonBlocking(url, SC_OK, expectedResult.trim());
    }

    @Test
    public void testAggregatorNonBlockingScalaInvalidInput() throws Exception {
        String url = "/aggregate-non-blocking-scala?minMs=200&maxMs=100";
        testNonBlocking(url, SC_OK, expectedErrorResult.trim());
    }

    @Test
    public void testAggregatorNonBlockingScalaTimeout() throws Exception {
        String url = "/aggregate-non-blocking-scala" + getTimeoutQueryString();
        testNonBlockingTimeout(url, timeoutDbHits, timeoutMs);
    }


    private String getTimeoutQueryString() {
        int timeoutMinMs = (timeoutMs < 1000) ? 0 : timeoutMs - 1000;
        int timeoutMaxMs = timeoutMs + 1000;
        return "?dbHits=" + timeoutDbHits + "&minMs=" + timeoutMinMs + "&maxMs=" + timeoutMaxMs;
    }
}