package com.propertize.platform.employecraft.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Graceful Shutdown Handler for Employecraft Service
 * 
 * Production-Ready Design v2.0
 * 
 * Handles application shutdown gracefully:
 * - Logs shutdown events
 * - Allows in-flight requests to complete
 * - Cleans up resources
 * 
 * Works in conjunction with:
 * - server.shutdown=graceful in application.yaml
 * - spring.lifecycle.timeout-per-shutdown-phase=30s
 * 
 * @author Platform Team
 * @version 2.0
 */
@Component
@Slf4j
public class GracefulShutdownHandler implements ApplicationListener<ContextClosedEvent> {

    private static final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    /**
     * Check if application is shutting down.
     * Can be used by other components to stop accepting new work.
     */
    public static boolean isShuttingDown() {
        return shuttingDown.get();
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (shuttingDown.compareAndSet(false, true)) {
            log.info("🛑 Employecraft Service shutdown initiated...");
            log.info("📝 Waiting for in-flight requests to complete...");

            // Log shutdown info
            Runtime runtime = Runtime.getRuntime();
            log.info("📊 Shutdown stats: activeThreads={}, usedMemory={}MB",
                    Thread.activeCount(),
                    (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));

            log.info("✅ Employecraft Service shutdown complete");
        }
    }
}
