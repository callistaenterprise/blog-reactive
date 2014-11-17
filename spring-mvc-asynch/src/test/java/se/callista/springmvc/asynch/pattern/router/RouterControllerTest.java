package se.callista.springmvc.asynch.pattern.router;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.callista.springmvc.asynch.Application;
import se.callista.springmvc.asynch.common.AsynchTestBase;

import static javax.servlet.http.HttpServletResponse.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by magnus on 29/05/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class RouterControllerTest extends AsynchTestBase {

    private MockMvc mockMvc;

    @Autowired
    WebApplicationContext wac;

    private final String expectedResult = "{\"status\":\"Ok\",\"processingTimeMs\":2000}";

    @Before
    public void setup(){

        // Process mock annotations
        MockitoAnnotations.initMocks(this);

        // Setup Spring test in webapp-mode (same config as spring-boot)
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testRouterNonBlockingCallback() throws Exception {

        String url = "/router-non-blocking-callback?minMs=2000&maxMs=2000";
        testNonBlocking(url, SC_OK, expectedResult);
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

    private void testNonBlocking(String url, int expectedHttpStatus, String expectedContent) throws Exception {
        testNonBlocking(url, "*/*", expectedHttpStatus, expectedContent);
    }

    private void testNonBlocking(String url, String accept, int expectedHttpStatus, String expectedContent) throws Exception {

        MvcResult mvcResult = this.mockMvc.perform(get(url).header("Accept", accept))
            .andExpect(request().asyncStarted())
            .andReturn();

        mvcResult.getAsyncResult();

        String expectedContentType = (accept.equals("*/*")) ? "text/plain;charset=ISO-8859-1" : accept;
        this.mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().is(expectedHttpStatus))
            .andExpect(content().contentType(expectedContentType))
            .andExpect(content().string(expectedContent));
    }
}