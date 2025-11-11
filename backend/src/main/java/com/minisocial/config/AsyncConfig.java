package com.minisocial.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for asynchronous task execution.
 * Configures a custom TaskExecutor for handling async operations like
 * image processing and feed rebuilding.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.corePoolSize:5}")
    private int corePoolSize;

    @Value("${async.maxPoolSize:10}")
    private int maxPoolSize;

    @Value("${async.queueCapacity:100}")
    private int queueCapacity;

    @Value("${async.threadNamePrefix:async-}")
    private String threadNamePrefix;

    /**
     * Creates a custom TaskExecutor bean for async processing.
     * 
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configure core pool size - minimum number of threads to keep alive
        executor.setCorePoolSize(corePoolSize);
        
        // Configure max pool size - maximum number of threads
        executor.setMaxPoolSize(maxPoolSize);
        
        // Configure queue capacity - number of tasks to queue before creating new threads
        executor.setQueueCapacity(queueCapacity);
        
        // Set thread name prefix for debugging and monitoring
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // Initialize the executor
        executor.initialize();
        
        return executor;
    }
}
