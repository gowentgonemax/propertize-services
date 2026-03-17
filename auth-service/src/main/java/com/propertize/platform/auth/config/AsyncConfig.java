package com.propertize.platform.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for the auth-service.
 *
 * <p>
 * Enables {@code @Async} processing and configures a dedicated thread
 * pool for audit-log writes so they do not block the request processing
 * pipeline.
 * </p>
 *
 * @version 1.0 - Phase 4b: Permission Audit Trail
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Thread pool executor for asynchronous audit-log writes.
     *
     * <p>
     * Core pool size of 2 threads with a max of 5, and a queue capacity
     * of 500 entries to absorb bursts. Thread names are prefixed with
     * {@code audit-} for easy identification in logs and thread dumps.
     * </p>
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        executor.initialize();
        log.info("Audit thread pool executor initialized (core=2, max=5, queue=500)");
        return executor;
    }
}
