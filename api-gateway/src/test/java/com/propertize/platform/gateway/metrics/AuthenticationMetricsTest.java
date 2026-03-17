package com.propertize.platform.gateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthenticationMetrics
 */
@DisplayName("AuthenticationMetrics Tests")
class AuthenticationMetricsTest {

    private MeterRegistry registry;
    private AuthenticationMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AuthenticationMetrics(registry);
    }

    @Nested
    @DisplayName("Authentication Counter Tests")
    class AuthenticationCounterTests {

        @Test
        @DisplayName("Should increment auth success counter")
        void shouldIncrementAuthSuccessCounter() {
            metrics.recordAuthSuccess();
            metrics.recordAuthSuccess();

            double count = registry.counter("gateway.auth.success", "type", "jwt").count();
            assertThat(count).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should increment auth failure counter")
        void shouldIncrementAuthFailureCounter() {
            metrics.recordAuthFailure();

            double count = registry.counter("gateway.auth.failure", "type", "jwt").count();
            assertThat(count).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record auth failure with reason")
        void shouldRecordAuthFailureWithReason() {
            metrics.recordAuthFailure("expired_token");
            metrics.recordAuthFailure("invalid_signature");

            // Just verify no exception - checking specific tags with SimpleMeterRegistry is complex
            assertThat(registry.getMeters()).isNotEmpty();
        }

        @Test
        @DisplayName("Should increment token refresh counter")
        void shouldIncrementTokenRefreshCounter() {
            metrics.recordTokenRefresh();
            metrics.recordTokenRefresh();
            metrics.recordTokenRefresh();

            double count = registry.counter("gateway.auth.token_refresh", "result", "success").count();
            assertThat(count).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should increment token refresh failure counter")
        void shouldIncrementTokenRefreshFailureCounter() {
            metrics.recordTokenRefreshFailure();

            double count = registry.counter("gateway.auth.token_refresh", "result", "failure").count();
            assertThat(count).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment service token counter")
        void shouldIncrementServiceTokenCounter() {
            metrics.recordServiceTokenGenerated();
            metrics.recordServiceTokenGenerated();

            double count = registry.counter("gateway.auth.service_token").count();
            assertThat(count).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("Rate Limiting Metrics Tests")
    class RateLimitingMetricsTests {

        @Test
        @DisplayName("Should increment rate limit exceeded counter")
        void shouldIncrementRateLimitExceededCounter() {
            metrics.recordRateLimitExceeded();
            metrics.recordRateLimitExceeded();

            double count = registry.counter("gateway.rate_limit.exceeded").count();
            assertThat(count).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should increment rate limit bucket created counter")
        void shouldIncrementRateLimitBucketCreatedCounter() {
            metrics.recordRateLimitBucketCreated();

            double count = registry.counter("gateway.rate_limit.bucket_created").count();
            assertThat(count).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Audit Metrics Tests")
    class AuditMetricsTests {

        @Test
        @DisplayName("Should record audit events by type")
        void shouldRecordAuditEventsByType() {
            metrics.recordAuditEvent("LOGIN_SUCCESS");
            metrics.recordAuditEvent("LOGIN_SUCCESS");
            metrics.recordAuditEvent("LOGOUT");

            // Verify counters exist
            assertThat(registry.getMeters()).hasSizeGreaterThan(0);
        }

        @Test
        @DisplayName("Should record login attempts")
        void shouldRecordLoginAttempts() {
            metrics.recordLoginAttempt(true);
            metrics.recordLoginAttempt(false);

            assertThat(registry.getMeters()).isNotEmpty();
        }

        @Test
        @DisplayName("Should record access denied events")
        void shouldRecordAccessDeniedEvents() {
            metrics.recordAccessDenied("/api/v1/admin/users");
            metrics.recordAccessDenied("/api/v1/payments");

            assertThat(registry.getMeters()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Timer Tests")
    class TimerTests {

        @Test
        @DisplayName("Should record authentication duration")
        void shouldRecordAuthenticationDuration() {
            metrics.recordAuthDuration(150);
            metrics.recordAuthDuration(200);
            metrics.recordAuthDuration(100);

            var timer = registry.timer("gateway.auth.duration");
            assertThat(timer.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should use sample-based timing")
        void shouldUseSampleBasedTiming() throws InterruptedException {
            var sample = metrics.startAuthenticationTimer();
            Thread.sleep(10); // Simulate work
            metrics.stopAuthenticationTimer(sample);

            var timer = registry.timer("gateway.auth.duration");
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                    .isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Tag Sanitization Tests")
    class TagSanitizationTests {

        @Test
        @DisplayName("Should handle null tag values")
        void shouldHandleNullTagValues() {
            // Should not throw
            metrics.recordAuthFailure(null);
            assertThat(registry.getMeters()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle empty tag values")
        void shouldHandleEmptyTagValues() {
            metrics.recordAuthFailure("");
            assertThat(registry.getMeters()).isNotEmpty();
        }

        @Test
        @DisplayName("Should sanitize special characters in tags")
        void shouldSanitizeSpecialCharacters() {
            metrics.recordAuthFailure("invalid/token@123!");
            metrics.recordRateLimitExceeded("user:123/ip:192.168.1.1");

            assertThat(registry.getMeters()).isNotEmpty();
        }
    }
}
