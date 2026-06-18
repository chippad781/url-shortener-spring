package com.linksnip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Click recording runs off the redirect thread so it never adds latency to a
 * redirect. A bounded queue with CallerRuns means that under extreme load we
 * degrade to synchronous recording rather than dropping clicks or OOM-ing.
 * (In a higher-scale system this would be a real queue — Kafka/SQS/Celery.)
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "clickExecutor")
    public Executor clickExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("click-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
