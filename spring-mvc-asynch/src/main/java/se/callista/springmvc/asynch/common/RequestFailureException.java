package se.callista.springmvc.asynch.common;

import com.ning.http.client.Response;

/**
 *
 */
public class RequestFailureException extends RuntimeException {
    private Response response;

    public RequestFailureException(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}
