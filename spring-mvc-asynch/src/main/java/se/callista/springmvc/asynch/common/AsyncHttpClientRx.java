package se.callista.springmvc.asynch.common;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rx.Observable;

import java.io.IOException;

/**
 *
 */
public class AsyncHttpClientRx {

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

    public Observable<Response> observable(String url, String acceptHeader) {
        return Observable.create(observer -> {

            try {
                asyncHttpClient.prepareGet(url).addHeader("Accept", acceptHeader).execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        observer.onNext(response);
                        observer.onCompleted();
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        observer.onError(t);
                    }
                });
            } catch (Exception e) {
                observer.onError(e);
            }
        });
    }
}
