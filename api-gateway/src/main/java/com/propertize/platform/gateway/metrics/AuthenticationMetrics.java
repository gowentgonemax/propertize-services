package com.propertize.platform.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Authentication Metrics for API Gateway
 *
 * Provides Prometheus-compatible metrics for monitoring:
 * - Authentication success/failure rates
 * - Token refresh operations
 * - Rate limiting events
 * - Service-to-service authentication
 * - Audit events
 *
 * Metrics are exposed at /actuator/prometheus
 */
@Component
@Slf4j
public class AuthenticationMetrics {

    private final MeterRegistry registry;

    // Authentication counters
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Counter tokenRefreshCounter;
    private final Counter tokenRefreshFailureCounter;
    private final Counter serviceTokenCounter;

    // Rate limiting counters
    private final Counter rateLimitExceededCounter;
    private final Counter rateLimitBucketCreatedCounter;

    // Audit counters
    private final Map<String, Counter> auditEventCounters = new ConcurrentHashMap<>();

    // Authentication timing
    private final Timer authenticationTimer;

    public AuthenticationMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Authentication counters
        this.authSuccessCounter = Counter.builder("gateway.auth.success")
                .description("Number of successful authentications")
                .tag("type", "jwt")
                .register(registry);

        this.authFailureCounter = Counter.builder("gateway.auth.failure")
                .description("Number of failed authentications")
                .tag("type", "jwt")
                .register(registry);

        this.tokenRefreshCounter = Counter.builder("gateway.auth.token_refresh")
                .description("Number of token refresh operations")
                .tag("result", "success")
                .register(registry);

        this.tokenRefreshFailureCounter = Counter.builder("gateway.auth.token_refresh")
                .description("Number of failed token refresh operations")
                .tag("result", "failure")
                .register(registry);

        this.serviceTokenCounter = Counter.builder("gateway.auth.service_token")
                .description("Number of service tokens generated")
                .register(registry);

        // Rate limiting counters
        this.rateLimitExceededCounter = Counter.builder("gateway.rate_limit.exceeded")
                .description("Number of rate limit exceeded events")
                .register(registry);

        this.rateLimitBucketCreatedCounter = Counter.builder("gateway.rate_limit.bucket_created")
                .description("Number of new rate limit buckets created")
                .register(registry);

        // Authentication timer
        this.authenticationTimer = Timer.builder("gateway.auth.duration")
                .description("Time taken for authentication")
                .register(registry);

        log.info("🔐 Authentication metrics initialized");
    }

    // ========== Authentication Metrics ==========

    /**
     * Record a successful authentication
     */
    public void recordAuthSuccess() {
        authSuccessCounter.increment();
    }

    /**
     * Record a failed authentication
     */
    public void recordAuthFailure() {
        authFailureCounter.increment();
    }

    /**
     * Record a failed authentication with reason
     */
    public void recordAuthFailure(String reason) {
        Counter.builder("gateway.auth.failure")
                .tag("type", "jwt")
                .tag("reason", sanitizeTag(reason))
                .register(registry)
                .increment();
    }

    /**
     * Record a token refresh
     */
    public void recordTokenRefresh() {
        tokenRefreshCounter.increment();
    }

    /**
     * Record a token refresh failure
     */
    public void recordTokenRefreshFailure() {
        tokenRefreshFailureCounter.increment();
    }

    /**
     * Record a service token generation
     */
    public void recordServiceTokenGenerated() {
        serviceTokenCounter.increment();
    }

    /**
     * Time an authentication operation
     */
    public Timer.Sample startAuthenticationTimer() {
        return Timer.start(registry);
    }

    /**
     * Stop the authentication timer and record
     */
    public void stopAuthenticationTimer(Timer.Sample sample) {
        sample.stop(authenticationTimer);
    }

    /**
     * Record authentication duration in milliseconds
     */
    public void recordAuthDuration(long durationMs) {
        authenticationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ========== Rate Limiting Metrics ==========

    /**
     * Record a rate limit exceeded event
     */
    public void recordRateLimitExceeded() {
        rateLimitExceededCounter.increment();
    }

    /**
     * Record a rate limit exceeded event with key type
     */
    public void recordRateLimitExceeded(String keyType) {
        Counter.builder("gateway.rate_limit.exceeded")
                .tag("key_type", sanitizeTag(keyType))
                .register(registry)
                .increment();
    }

    /**
     * Record a new rate limit bucket created
     */
    public void recordRateLimitBucketCreated() {
        rateLimitBucketCreatedCounter.increment();
    }

    // ========== Audit Metrics ==========

    /**
     * Record an audit event by type
     */
    public void recordAuditEvent(String eventType) {
        Counter counter = auditEventCounters.computeIfAbsent(eventType,
                type -> Counter.builder("gateway.audit.events")
                        .tag("type", sanitizeTag(type))
                        .description("Number of audit events by type")
                        .register(registry));
        counter.increment();
    }

    /**
     * Record a login attempt
     */
    public void recordLoginAttempt(boolean success) {
        Counter.builder("gateway.audit.login")
                .tag("result", success ? "success" : "failure")
                .description("Login attempts")
                .register(registry)
                .increment();
    }

    /**
     * Record access denied event
     */
    public void recordAccessDenied(String resource) {
        Counter.builder("gateway.audit.access_denied")
                .tag("resource", sanitizeTag(resource))
                .description("Access denied events")
                .register(registry)
                .increment();
    }

    // ========== Helper Methods ==========

    /**
     * Sanitize tag value to prevent metric explosion
     */
    private String sanitizeTag(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        // Limit length and replace problematic characters
        String sanitized = value.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .substring(0, Math.min(value.length(), 50));
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }
}
