package se.callista.springmvc.asynch.common;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AsyncHttpClientJava8 {

    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpClientJava8.class);

    @Autowired
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    private AsyncHttpClient asyncHttpClient;

	public CompletableFuture<Response> execute(String url, int id) {
        final CompletableFuture<Response> result = new CompletableFuture<>();
        try {
            asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(Response response) throws Exception {
                    logger.debug("Request #{} completed successfully", id);
                    result.complete(response);
                    return response;
                }

                @Override
                public void onThrowable(Throwable t) {
                    logger.debug("Request #{} failed");
                    result.completeExceptionally(t);
                }
            });

        } catch (IOException e) {
            result.completeExceptionally(e);
        }
        return result;
    }

}
