package com.propertize.payroll.config;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;

/**
 * Java 21 Virtual Thread configuration for payroll-service.
 *
 * <p>
 * Virtual threads (Project Loom) let Tomcat handle each request on a
 * lightweight
 * virtual thread instead of a platform thread from the thread pool. For this
 * service's
 * I/O-heavy workloads (Feign calls to employee-service, DB queries) this can
 * reduce
 * latency under load by 30-60% compared to a fixed-size thread pool.
 *
 * <h3>Safety notes:</h3>
 * <ul>
 * <li>Avoid synchronized blocks around blocking I/O — use ReentrantLock instead
 * to
 * allow virtual thread pinning avoidance.</li>
 * <li>Do NOT store state in ThreadLocal that outlives a request; prefer
 * ScopedValue
 * (preview in Java 21, stable in Java 22+).</li>
 * <li>Disable via {@code VIRTUAL_THREADS_ENABLED=false} env var for
 * rollback.</li>
 * </ul>
 *
 * <h3>Activation:</h3>
 * Simplest — add to application.yml:
 * 
 * <pre>
 * spring:
 *   threads:
 *     virtual:
 *       enabled: true   # Spring Boot 3.2+ auto-configures Tomcat virtual threads
 * </pre>
 *
 * The bean below is an explicit alternative for services that don't yet use
 * Boot 3.2
 * auto-config, or for the async executor used by {@code @Async} methods.
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * Replace the Tomcat request handler thread pool with virtual threads.
     * Each incoming HTTP request runs on its own virtual thread.
     *
     * Spring Boot 3.2+ equivalent: {@code spring.threads.virtual.enabled=true}
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> virtualThreadTomcatCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Virtual-thread-backed executor for {@code @Async} methods and
     * CompletableFuture.
     *
     * Usage:
     * 
     * <pre>{@code
     * &#64;Async("virtualThreadExecutor")
     * public CompletableFuture<PayrollRun> processAsync(Long runId) { ... }
     * }</pre>
     */
    @Bean("virtualThreadExecutor")
    public AsyncTaskExecutor virtualThreadExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
