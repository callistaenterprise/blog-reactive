package se.callista.springmvc.asynch.common.lambdasupport;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

/**
 *
 */
public class AsyncHttpClientRx {

    @Autowired
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    private AsyncHttpClient asyncHttpClient;

    public Observable<Response> observable(String url) {
        return Observable.create(subscriber -> {

            try {
                asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        subscriber.onNext(response);
                        subscriber.onCompleted();
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        subscriber.onError(t);
                    }
                });
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }
}
