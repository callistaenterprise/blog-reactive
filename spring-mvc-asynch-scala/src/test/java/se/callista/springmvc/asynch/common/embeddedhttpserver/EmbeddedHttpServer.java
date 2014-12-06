package se.callista.springmvc.asynch.common.embeddedhttpserver;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by magnus on 22/07/14.
 */
public class EmbeddedHttpServer {

    private int port;
    private Server server;
    private RequestHandler requestHandler;

    public EmbeddedHttpServer(int port, RequestHandler requestHandler) {
        this.port = port;
        this.requestHandler = requestHandler;
    }

    public void start() throws Exception {
        configureServer();
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    protected void configureServer() {
        server = new Server(port);

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                try {
                    requestHandler.handle(target, baseRequest, request, response);
                } catch (ServletException e) {
                    throw e;
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    protected Server getServer() {
        return server;
    }
}