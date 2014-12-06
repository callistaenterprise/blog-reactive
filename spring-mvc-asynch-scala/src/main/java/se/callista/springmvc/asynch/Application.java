package se.callista.springmvc.asynch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import se.callista.springmvc.asynch.config.MyEmbeddedServletContainerCustomizer;

@ComponentScan()
@EnableAutoConfiguration
public class Application {

    @Bean
    public EmbeddedServletContainerCustomizer embeddedServletCustomizer(){
        return new MyEmbeddedServletContainerCustomizer();
    }

    @Value("${threadPool.db.max_size}")
    private int THREAD_POOL_DB_MAX_SIZE;

    @Value("${threadPool.db.init_size}")
    private int THREAD_POOL_DB_INIT_SIZE;

    @Value("${threadPool.db.queue_size}")
    private int THREAD_POOL_DB_QUEUE_SIZE;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}