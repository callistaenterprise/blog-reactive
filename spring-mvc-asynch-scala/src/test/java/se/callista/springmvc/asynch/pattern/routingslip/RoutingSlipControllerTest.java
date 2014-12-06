package se.callista.springmvc.asynch.pattern.routingslip;

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
                    "{\"status\":\"Ok\",\"processingTimeMs\":500}";


    private static final String expectedInvalidInputError = "Error: Invalid query parameter, qry=error";

    private static final String expectedTImeoutError = "Request failed due to service provider not responding within 5000 ms. Url: http://localhost:9090/process-non-blocking?minMs=100&maxMs=100&qry=timeout";

    /*
     * Scala tests
     */

    @Test
    public void testRoutingSlipNonBlockingScala() throws Exception {
        String url = "/routing-slip-non-blocking-scala";
        testNonBlocking(url, SC_OK, expectedResult);
    }

    @Test
    public void testRoutingSlipNonBlockingScalaInvalidInput() throws Exception {
        String url = "/routing-slip-non-blocking-scala?qry=error";
        testNonBlocking(url, SC_INTERNAL_SERVER_ERROR, expectedInvalidInputError);
    }

    @Test
    public void testRoutingSlipNonBlockingScalaTimeout() throws Exception {
        String url = "/routing-slip-non-blocking-scala?qry=timeout";
        testNonBlocking(url, SC_GATEWAY_TIMEOUT, expectedTImeoutError);
    }

}