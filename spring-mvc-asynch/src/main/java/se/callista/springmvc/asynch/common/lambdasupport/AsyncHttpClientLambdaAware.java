package se.callista.springmvc.asynch.common.lambdasupport;

import com.ning.http.client.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

/**
 * Created by magnus on 18/07/14.
 */
public class AsyncHttpClientLambdaAware {

    private final AsyncHttpClient asyncHttpClient;

    public static ResponseEntity<String> createResponseEntity(Response response) {
        try {
            return new ResponseEntity<String>(response.getResponseBody(), HttpStatus.valueOf(response.getStatusCode()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AsyncHttpClientLambdaAware(int connectionTimeoutMs, int requestTimeoutMs, int maxRequestRetry) {

        AsyncHttpClientConfig config=new AsyncHttpClientConfig.Builder().
                setConnectionTimeoutInMs(connectionTimeoutMs).
                setRequestTimeoutInMs(requestTimeoutMs).
                setMaxRequestRetry(maxRequestRetry).
                build();

        asyncHttpClient = new AsyncHttpClient(config);
    }

    public ListenableFuture<Response> execute(String url, final Completed c, final Error e) {
        return execute(url, "*/*", c, e);
    }

    public ListenableFuture<Response> execute(String url, String acceptHeader, final Completed c, final Error e) {

        try {
            return asyncHttpClient.prepareGet(url).addHeader("Accept", acceptHeader).execute(new AsyncCompletionHandler<Response>() {

                @Override
                public Response onCompleted(Response response) throws Exception {
                    c.onCompleted(response);
                    return response;
                }

                @Override
                public void onThrowable(Throwable t) {
                    e.onThrowable(t);
                }

            });

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public ListenableFuture<Response> execute(String url, final Completed c) {

        try {
            return asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler<Response>() {

                @Override
                public Response onCompleted(Response response) throws Exception {
                    c.onCompleted(response);
                    return response;
                }
            });

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}