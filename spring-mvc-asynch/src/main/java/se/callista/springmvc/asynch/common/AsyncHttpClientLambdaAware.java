package se.callista.springmvc.asynch.common;

import com.ning.http.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

/**
 * Created by magnus on 18/07/14.
 */
public class AsyncHttpClientLambdaAware {

    @Autowired
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    private AsyncHttpClient asyncHttpClient;

    public static ResponseEntity<String> createResponseEntity(Response response) {
        try {
            return createResponseEntity(response.getResponseBody(), HttpStatus.valueOf(response.getStatusCode()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ResponseEntity<String> createResponseEntity(String responseBody, HttpStatus httpStatusCode) {
        return new ResponseEntity<>(responseBody, httpStatusCode);
    }

    public ListenableFuture<Response> execute(String url, final Error e, final Completed c) {
        return execute(url, "*/*", e, c);
    }

    public ListenableFuture<Response> execute(String url, String acceptHeader, final Error e, final Completed c) {

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
}