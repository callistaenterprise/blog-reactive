package se.callista.springmvc.asynch.pattern.router;

import org.junit.Test;
import se.callista.springmvc.asynch.common.AsynchTestBase;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * Created by magnus on 29/05/14.
 */
public class RouterControllerTest extends AsynchTestBase {

    /*
     * Callback tests
     */

    @Test
    public void testRouterNonBlockingCallback() throws Exception {

        String url = "/router-non-blocking-callback?minMs=2000&maxMs=2000";
        testNonBlocking(url, SC_OK, "{\"status\":\"Ok\",\"processingTimeMs\":2000}");
    }

    @Test
    public void testRouterNonBlockingCallbackInvalidInput() throws Exception {

        String url = "/router-non-blocking-callback?minMs=2000&maxMs=200";
        testNonBlocking(url, SC_INTERNAL_SERVER_ERROR, "Error: maxMs < minMs  (200 < 2000)");
    }

    @Test
    public void testRouterNonBlockingCallbackTimeout() throws Exception {

        String url = "/router-non-blocking-callback?minMs=10000&maxMs=10000";
        testNonBlocking(url, SC_GATEWAY_TIMEOUT, "Request failed due to service provider not responding within 5000 ms. Url: http://localhost:9090/process-non-blocking?minMs=10000&maxMs=10000");
    }
}