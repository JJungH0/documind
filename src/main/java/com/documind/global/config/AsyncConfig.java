package com.documind.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EMBEDDING_EXECUTOR = "embeddingExecutor";

    @Bean(name = EMBEDDING_EXECUTOR)
    public ThreadPoolTaskExecutor embeddingExecutor(EmbeddingProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.workerCount());
        executor.setMaxPoolSize(properties.workerCount());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix("embedding-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
