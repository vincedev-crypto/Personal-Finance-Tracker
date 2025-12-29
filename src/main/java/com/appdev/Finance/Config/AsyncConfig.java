package com.appdev.Finance.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // This can also be on your main application class
public class AsyncConfig {

    @Value("${async.executor.core-pool-size:2}") // Default to 2 if not set in properties
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:5}")  // Default to 5
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:10}") // Default to 10
    private int queueCapacity;

    @Value("${async.executor.thread-name-prefix:AsyncMail-}") // Default prefix
    private String threadNamePrefix;

    @Bean(name = "asyncMailExecutor") // You can name your executor
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}