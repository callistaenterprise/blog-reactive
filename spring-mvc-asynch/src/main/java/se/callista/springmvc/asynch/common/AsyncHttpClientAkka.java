package se.callista.springmvc.asynch.common;

import akka.dispatch.Futures;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.springframework.beans.factory.annotation.Autowired;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * @author Pär Wenåker <par.wenaker@callistaenterprise.se>
 */
public class AsyncHttpClientAkka {

    @Autowired
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    private AsyncHttpClient asyncHttpClient;

    public Future<Response> execute(String url) {
        Promise<Response> promise = Futures.promise();

        try {
            asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(Response response) throws Exception {
                    promise.success(response);
                    return response;
                }

                @Override
                public void onThrowable(Throwable t) {
                    promise.failure(t);
                }
            });
        } catch (Throwable t) {
            promise.failure(t);
        }

        return promise.future();
    }
}
