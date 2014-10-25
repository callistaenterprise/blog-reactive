package se.callista.springmvc.asynch.common.lambdasupport;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AsyncHttpClientJava8LambdaAware {

    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpClientJava8LambdaAware.class);
    private static final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

	public CompletableFuture<Response> execute(String url) {
        final CompletableFuture<Response> result = new CompletableFuture<>();

        try {
            logger.debug("Executing async http request to {}: ", url);
            asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler<Response>() {
	            @Override
	            public Response onCompleted(Response response) throws Exception {
                    logger.debug("Async http request to {} is completed.", url);
		            result.complete(response);
                    return response;
                }

                @Override
                public void onThrowable(Throwable t) {
                    logger.error("Async http request to {} failed.", url);
                    result.completeExceptionally(t);
                }
            });

        } catch (IOException e) {
            result.completeExceptionally(e);
        }
        return result;
    }
}
