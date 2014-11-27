package se.callista.springmvc.asynch.pattern.routingslip;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import se.callista.springmvc.asynch.Application;
import se.callista.springmvc.asynch.common.AsynchTestBase;

import static javax.servlet.http.HttpServletResponse.*;


/**
 * Created by magnus on 29/05/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class RoutingSlipControllerTest extends AsynchTestBase {

    private static final String expectedResult =
            "{\"status\":\"Ok\",\"processingTimeMs\":100}\n" +
                    "{\"status\":\"Ok\",\"processingTimeMs\":200}\n" +
                    "{\"status\":\"Ok\",\"processingTimeMs\":400}\n" +
                    "{\"status\":\"Ok\",\"processingTimeMs\":500}\n";


    private static final String expectedInvalidInputError = "Error: Invalid query parameter, qry=error";

    private static final String expectedTImeoutError = "Request failed due to service provider not responding within 5000 ms. Url: http://localhost:9090/process-non-blocking?minMs=100&maxMs=100&qry=timeout";

    /**
     * @deprecated use expectedResult
     */
    private static final String expectedResultOLD =
        "{\"status\":\"Ok\",\"processingTimeMs\":100}\n" +
        "{\"status\":\"Ok\",\"processingTimeMs\":200}\n" +
        "{\"status\":\"Ok\",\"processingTimeMs\":300}\n" +
        "{\"status\":\"Ok\",\"processingTimeMs\":400}\n" +
        "{\"status\":\"Ok\",\"processingTimeMs\":500}\n";

    /*
     * Callback tests
     */

    @Test
    public void testRoutingSlipNonBlockingCallback() throws Exception {
        String url = "/routing-slip-non-blocking-callback";
        testNonBlocking(url, SC_OK, expectedResult);
    }

    @Test
    public void testRoutingSlipNonBlockingCallbackInvalidInput() throws Exception {
        String url = "/routing-slip-non-blocking-callback?qry=error";
        testNonBlocking(url, SC_INTERNAL_SERVER_ERROR, expectedInvalidInputError);
    }

    @Test
    public void testRoutingSlipNonBlockingCallbackTimeout() throws Exception {
        String url = "/routing-slip-non-blocking-callback?qry=timeout";
        testNonBlocking(url, SC_GATEWAY_TIMEOUT, expectedTImeoutError);
    }


    /*
     * RX tests
     */

    @Test
    public void testRoutingSlipNonBlockingRx() throws Exception {
        String url = "/routing-slip-non-blocking-rx";
        testNonBlocking(url, SC_OK, expectedResult);
    }

    @Test
    public void testRoutingSlipNonBlockingRxInvalidInput() throws Exception {
        String url = "/routing-slip-non-blocking-rx?qry=error";
        testNonBlocking(url, SC_INTERNAL_SERVER_ERROR, expectedInvalidInputError);
    }

    @Test
    public void testRoutingSlipNonBlockingRxTimeout() throws Exception {
        String url = "/routing-slip-non-blocking-rx?qry=timeout";
        testNonBlocking(url, SC_GATEWAY_TIMEOUT, expectedTImeoutError);
    }


    /*
     * Java8 tests
     */

    @Test
    public void testRoutingSlipNonBlockingJava8() throws Exception {
        String url = "/routing-slip-non-blocking-java8";
        testNonBlocking(url, SC_OK, expectedResult.trim());
    }

    @Test
    public void testRoutingSlipNonBlockingJava8InvalidInput() throws Exception {
        String url = "/routing-slip-non-blocking-java8?qry=error";
        testNonBlocking(url, SC_INTERNAL_SERVER_ERROR, expectedInvalidInputError);
    }

    @Test
    public void testRoutingSlipNonBlockingJava8Timeout() throws Exception {
        String url = "/routing-slip-non-blocking-java8?qry=timeout";
        testNonBlocking(url, SC_GATEWAY_TIMEOUT, expectedTImeoutError);
    }


    /*
     * Akka Futures tests
     */

    @Test
    public void testRoutingSlipNonBlockingAkkaFutures() throws Exception {
        String url = "/routing-slip-non-blocking-akkafutures";
        testNonBlocking(url, SC_OK, expectedResultOLD);
    }

    @Ignore
    @Test
    public void testRoutingSlipNonBlockingAkkaFuturesInvalidInput() throws Exception {
        String url = "/routing-slip-non-blocking-akkafutures?qry=error";
        testNonBlocking(url, SC_INTERNAL_SERVER_ERROR, expectedInvalidInputError);
    }

    @Ignore
    @Test
    public void testRoutingSlipNonBlockingAkkaFuturesTimeout() throws Exception {
        String url = "/routing-slip-non-blocking-akkafutures?qry=timeout";
        testNonBlocking(url, SC_GATEWAY_TIMEOUT, expectedTImeoutError);
    }
}