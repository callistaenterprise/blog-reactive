package se.callista.springmvc.asynch.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.callista.springmvc.asynch.Application;
import se.callista.springmvc.asynch.common.embeddedhttpserver.EmbeddedHttpServerTestBase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.io.IOUtils.write;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by magnus on 22/07/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
abstract public class AsynchTestBase extends EmbeddedHttpServerTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(AsynchTestBase.class);

    private MockMvc mockMvc;

    @Autowired
    WebApplicationContext wac;

    @Before
    public void baseSetup(){

        // Process mock annotations
        MockitoAnnotations.initMocks(this);

        // Setup Spring test in webapp-mode (same config as spring-boot)
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    protected void testNonBlocking(String url, int expectedHttpStatus, String expectedContent) throws Exception {
        testNonBlocking(url, "*/*", expectedHttpStatus, expectedContent);
    }

    protected void testNonBlocking(String url, String accept, int expectedHttpStatus, String expectedContent) throws Exception {

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

    protected void testNonBlockingTimeout(String url, int timeoutDbHits1, int timeoutMs1) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(url))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult();

        this.mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=ISO-8859-1"));

        String result = mvcResult.getAsyncResult().toString();

        LOG.debug("JSON: {}", result);
        String[] psArr = result.split("\n");

        // Verify that we got some timeouts
        assertTrue("Expected at least one timeout to occur", psArr.length < timeoutDbHits1);

        LOG.debug("assert that no response time was over the timeout: {}", timeoutMs1);
        ObjectMapper mapper = new ObjectMapper();
        for (String aPsArr : psArr) {
            ProcessingStatus ps = mapper.readValue(aPsArr, ProcessingStatus.class);
            LOG.debug("psArr: {} - {}", ps.getStatus(), ps.getProcessingTimeMs());
            assertTrue(ps.getProcessingTimeMs() < timeoutMs1);
        }
    }




    protected void createResponse(HttpServletRequest request, String requestBody, HttpServletResponse response) throws IOException, URISyntaxException {

        // Get query parameters, first initialize some decent default values
        Map<String, String> parMap = new HashMap<>();
        parMap.put("minMs", "0");
        parMap.put("maxMs", "0");
        parMap.put("qry",   "");
        getQueryParameters(request, parMap);

        int minMs = Integer.parseInt(parMap.get("minMs"));
        int maxMs = Integer.parseInt(parMap.get("maxMs"));
        String qry = parMap.get("qry");

        // Cause a if qry = timeout
        if (qry.equals("timeout")) {
            maxMs = minMs = 20000;
        }

        // Return a 500 error if qry = error
        if (qry.equals("error")) {
            respondWithInternalServerError(response, "Error: Invalid query parameter, qry=" + qry);
            return;
        }

        // Return a 500 error if max < min
        if (maxMs < minMs) {
            respondWithInternalServerError(response, "Error: maxMs < minMs  (" + maxMs + " < " + minMs + ")");
            return;
        }

        int processingTimeMs = calculateProcessingTime(minMs, maxMs);

        LOG.debug("Start blocking request, processing time: {} ms (" + minMs + " - " + maxMs + ").", processingTimeMs);
        try {
            Thread.sleep(processingTimeMs);
        }
        catch (InterruptedException e) {}

        String responseBody = "{\"status\":\"Ok\",\"processingTimeMs\":" + processingTimeMs + "}";
        response.setStatus(SC_OK);
        response.setContentType("text/plain;charset=ISO-8859-1");
        write(responseBody, response.getOutputStream());
    }

    private void respondWithInternalServerError(HttpServletResponse response, String msg) throws IOException {
        LOG.error("Processing failed due to invalid input: " + msg);
        response.setStatus(SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/plain;charset=ISO-8859-1");
        write(msg, response.getOutputStream());
    }

    private int calculateProcessingTime(int minMs, int maxMs) {
        int processingTimeMs = minMs + (int) (Math.random() * (maxMs - minMs));
        return processingTimeMs;
    }
}