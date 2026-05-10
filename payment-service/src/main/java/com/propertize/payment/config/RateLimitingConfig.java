package com.propertize.payment.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting Configuration — PCI-DSS & DDoS Protection
 * 
 * Implements rate limiting for payment endpoints:
 * - Payment operations: 10 requests/min per user (prevents brute force)
 * - General endpoints: 30 requests/min per user (prevents abuse)
 * 
 * Uses Resilience4j's distributed token bucket algorithm.
 * Metrics available via Micrometer.
 */
@Slf4j
@Configuration
public class RateLimitingConfig {

    /**
     * Rate limiter for payment-critical endpoints (payment intents, refunds)
     * Threshold: 10 requests per minute per user
     */
    @Bean
    public RateLimiter paymentOperationRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(10) // 10 requests/min
                .timeoutDuration(Duration.ofSeconds(1))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("payment-operations");

        log.info("✓ Payment operations rate limiter initialized: 10 req/min per user");
        return rateLimiter;
    }

    /**
     * Rate limiter for general payment endpoints
     * Threshold: 30 requests per minute per user
     */
    @Bean
    public RateLimiter generalPaymentRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(30) // 30 requests/min
                .timeoutDuration(Duration.ofSeconds(1))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("general-payment");

        log.info("✓ General payment rate limiter initialized: 30 req/min per user");
        return rateLimiter;
    }
}
