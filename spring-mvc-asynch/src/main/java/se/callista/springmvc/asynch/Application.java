package se.callista.springmvc.asynch;

import akka.actor.ActorSystem;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import se.callista.springmvc.asynch.common.AsyncHttpClientAkka;
import se.callista.springmvc.asynch.common.AsyncHttpClientJava8;
import se.callista.springmvc.asynch.common.AsyncHttpClientLambdaAware;
import se.callista.springmvc.asynch.common.AsyncHttpClientRx;
import se.callista.springmvc.asynch.config.MyEmbeddedServletContainerCustomizer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@ComponentScan()
@EnableAutoConfiguration
public class Application {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Bean
    public EmbeddedServletContainerCustomizer embeddedServletCustomizer(){
        return new MyEmbeddedServletContainerCustomizer();
    }

    @Value("${threadPool.db.init_size}")
    private int THREAD_POOL_DB_INIT_SIZE;

    @Value("${threadPool.db.max_size}")
    private int THREAD_POOL_DB_MAX_SIZE;

    @Value("${threadPool.db.queue_size}")
    private int THREAD_POOL_DB_QUEUE_SIZE;

    @Bean(name="dbThreadPoolExecutor")
    public TaskExecutor getTaskExecutor() {
        ThreadPoolTaskExecutor tpte = new ThreadPoolTaskExecutor();
        tpte.setCorePoolSize(THREAD_POOL_DB_INIT_SIZE);
        tpte.setMaxPoolSize(THREAD_POOL_DB_MAX_SIZE);
        tpte.setQueueCapacity(THREAD_POOL_DB_QUEUE_SIZE);
        tpte.initialize();
        return tpte;
    }

    @Value("${sp.connectionTimeoutMs}")
    private int spConnectionTimeoutMs;

    @Value("${sp.requestTimeoutMs}")
    private int spRequestTimeoutMs;

    @Value("${sp.maxRequestRetry}")
    private int spMaxRequestRetry;

    @Bean
    public AsyncHttpClient getAsyncHttpClient() {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().
                 setConnectionTimeoutInMs(spConnectionTimeoutMs).
                 setRequestTimeoutInMs(spRequestTimeoutMs).
                 setMaxRequestRetry(spMaxRequestRetry).
                 build();

        return new AsyncHttpClient(config);
    }

    @Bean
    public AsyncHttpClientLambdaAware getAsyncHttpClientCallback() {
        LOG.debug("Creates a new AsyncHttpClientLambdaAware-object: with: connection-timeout: {} ms, read-timeout: {} ms\", serviceProviderConnectionTimeoutMs, serviceProviderReadTimeoutMs);");
        return new AsyncHttpClientLambdaAware();
    }

    @Bean
    public AsyncHttpClientRx getHttpClientRx() {
        LOG.info("### Creates a new AsyncHttpClientRx-object");
        return new AsyncHttpClientRx();
    }

    @Bean
    public AsyncHttpClientJava8 getHttpClientJava8() {
        LOG.info("### Creates a new AsyncHttpClientJava8-object");
        return new AsyncHttpClientJava8();
    }

    @Bean
    public AsyncHttpClientAkka getHttpClientAkka() {
        LOG.info("### Creates a new AsyncHttpClientAkka-object");
        return new AsyncHttpClientAkka();
    }

    @Bean
    public ActorSystem getActorSystem() {
        LOG.info("### Creates a new Akka actor system");
        return ActorSystem.create("actorSystem");
    }

    @Bean(name="timerService")
    public ScheduledExecutorService getTimerService() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}