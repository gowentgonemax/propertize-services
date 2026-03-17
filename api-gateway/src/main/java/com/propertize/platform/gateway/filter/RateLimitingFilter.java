package com.propertize.platform.gateway.filter;

import com.propertize.platform.gateway.metrics.AuthenticationMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limiting Filter for API Gateway
 *
 * Implements token bucket algorithm with configurable limits per:
 * - IP address (for public endpoints)
 * - User ID (for authenticated endpoints)
 * - Organization ID (for multi-tenant scenarios)
 *
 * Features:
 * - Sliding window rate limiting
 * - Per-endpoint customization
 * - Graceful degradation headers
 * - Automatic cleanup of expired entries
 */
@Slf4j
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    @Autowired(required = false)
    private AuthenticationMetrics metrics;

    // Configuration
    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate-limit.default-requests-per-minute:60}")
    private int defaultRequestsPerMinute;

    @Value("${rate-limit.authenticated-requests-per-minute:120}")
    private int authenticatedRequestsPerMinute;

    @Value("${rate-limit.auth-endpoint-requests-per-minute:10}")
    private int authEndpointRequestsPerMinute;

    @Value("${rate-limit.burst-multiplier:1.5}")
    private double burstMultiplier;

    // Rate limit buckets: key -> RateLimitBucket
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    // Cleanup interval
    private static final long CLEANUP_INTERVAL_MS = 60_000; // 1 minute
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    // Headers
    private static final String X_USER_ID = "X-User-Id";
    private static final String X_ORGANIZATION_ID = "X-Organization-Id";
    private static final String RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_RESET = "X-RateLimit-Reset";
    private static final String RETRY_AFTER = "Retry-After";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitEnabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Determine rate limit key and limit
        String rateLimitKey = determineRateLimitKey(request);
        int limit = determineLimit(path, request);

        // Periodic cleanup
        cleanupExpiredBuckets();

        // Get or create bucket
        RateLimitBucket bucket = buckets.computeIfAbsent(rateLimitKey,
            k -> {
                if (metrics != null) {
                    metrics.recordRateLimitBucketCreated();
                }
                return new RateLimitBucket(limit, burstMultiplier);
            });

        // Try to consume a token
        if (!bucket.tryConsume()) {
            log.warn("⚠️ Rate limit exceeded for key: {} on path: {}", rateLimitKey, path);
            if (metrics != null) {
                metrics.recordRateLimitExceeded(rateLimitKey.startsWith("user:") ? "user" :
                                                rateLimitKey.startsWith("org:") ? "organization" : "ip");
            }
            return onRateLimitExceeded(exchange, bucket);
        }

        // Add rate limit headers to response
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(RATE_LIMIT_REMAINING, String.valueOf(bucket.getRemainingTokens()));
        response.getHeaders().add(RATE_LIMIT_LIMIT, String.valueOf(bucket.getLimit()));
        response.getHeaders().add(RATE_LIMIT_RESET, String.valueOf(bucket.getResetTimeSeconds()));

        return chain.filter(exchange);
    }

    private String determineRateLimitKey(ServerHttpRequest request) {
        // Try user ID first (authenticated requests)
        String userId = request.getHeaders().getFirst(X_USER_ID);
        if (userId != null && !userId.isEmpty()) {
            String orgId = request.getHeaders().getFirst(X_ORGANIZATION_ID);
            if (orgId != null && !orgId.isEmpty()) {
                return "user:" + userId + ":org:" + orgId;
            }
            return "user:" + userId;
        }

        // Fall back to IP address (anonymous requests)
        String clientIp = getClientIp(request);
        return "ip:" + clientIp;
    }

    private String getClientIp(ServerHttpRequest request) {
        // Check forwarded headers
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        // Direct connection
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private int determineLimit(String path, ServerHttpRequest request) {
        // Auth endpoints have stricter limits
        if (path.startsWith("/api/v1/auth/")) {
            return authEndpointRequestsPerMinute;
        }

        // Authenticated users get higher limits
        String userId = request.getHeaders().getFirst(X_USER_ID);
        if (userId != null && !userId.isEmpty()) {
            return authenticatedRequestsPerMinute;
        }

        return defaultRequestsPerMinute;
    }

    private void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup.get() > CLEANUP_INTERVAL_MS) {
            if (lastCleanup.compareAndSet(lastCleanup.get(), now)) {
                buckets.entrySet().removeIf(entry -> entry.getValue().isExpired());
                log.debug("Rate limit bucket cleanup completed. Active buckets: {}", buckets.size());
            }
        }
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange, RateLimitBucket bucket) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        response.getHeaders().add(RATE_LIMIT_REMAINING, "0");
        response.getHeaders().add(RATE_LIMIT_LIMIT, String.valueOf(bucket.getLimit()));
        response.getHeaders().add(RATE_LIMIT_RESET, String.valueOf(bucket.getResetTimeSeconds()));
        response.getHeaders().add(RETRY_AFTER, String.valueOf(bucket.getRetryAfterSeconds()));

        String body = String.format(
            "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please retry after %d seconds.\",\"path\":\"%s\",\"retryAfter\":%d}",
            bucket.getRetryAfterSeconds(),
            exchange.getRequest().getPath().value(),
            bucket.getRetryAfterSeconds()
        );

        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(body.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        // Run very early, before authentication
        return -200;
    }

    /**
     * Token Bucket implementation for rate limiting
     */
    private static class RateLimitBucket {
        private final int limit;
        private final int burstLimit;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;
        private final AtomicLong windowStart;
        private static final long WINDOW_MS = 60_000; // 1 minute window
        private static final long EXPIRY_MS = 300_000; // 5 minutes expiry

        RateLimitBucket(int limit, double burstMultiplier) {
            this.limit = limit;
            this.burstLimit = (int) (limit * burstMultiplier);
            this.tokens = new AtomicInteger(burstLimit);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
            this.windowStart = new AtomicLong(System.currentTimeMillis());
        }

        boolean tryConsume() {
            refillTokens();

            int currentTokens = tokens.get();
            while (currentTokens > 0) {
                if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                    return true;
                }
                currentTokens = tokens.get();
            }
            return false;
        }

        private void refillTokens() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            long elapsed = now - lastRefill;

            if (elapsed >= WINDOW_MS) {
                // Reset window
                if (lastRefillTime.compareAndSet(lastRefill, now)) {
                    tokens.set(burstLimit);
                    windowStart.set(now);
                }
            } else {
                // Gradual refill based on elapsed time
                double refillFraction = (double) elapsed / WINDOW_MS;
                int tokensToAdd = (int) (limit * refillFraction);
                if (tokensToAdd > 0) {
                    if (lastRefillTime.compareAndSet(lastRefill, now)) {
                        int newTokens = Math.min(burstLimit, tokens.get() + tokensToAdd);
                        tokens.set(newTokens);
                    }
                }
            }
        }

        int getRemainingTokens() {
            return Math.max(0, tokens.get());
        }

        int getLimit() {
            return limit;
        }

        long getResetTimeSeconds() {
            long now = System.currentTimeMillis();
            long windowEnd = windowStart.get() + WINDOW_MS;
            return Math.max(0, (windowEnd - now) / 1000);
        }

        int getRetryAfterSeconds() {
            return (int) Math.max(1, getResetTimeSeconds());
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastRefillTime.get() > EXPIRY_MS;
        }
    }
}
