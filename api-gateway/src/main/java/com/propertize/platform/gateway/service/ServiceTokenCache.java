package com.propertize.platform.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Local in-memory cache for service-to-service tokens
 *
 * <p>Reduces calls to auth-service by caching validated service tokens.
 * Tokens are cached for 4 minutes (expires before token actual 5-minute expiry).</p>
 *
 * <p>Thread-safe and optimized for high-throughput scenarios.</p>
 *
 * @author Platform Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ServiceTokenCache {

    private final Cache<String, ServiceTokenInfo> tokenCache;
    private final ConcurrentMap<String, Long> failureCount = new ConcurrentHashMap<>();

    public ServiceTokenCache() {
        this.tokenCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(4)) // Cache for 4 min (token valid for 5 min)
                .recordStats()
                .build();

        log.info("🔧 ServiceTokenCache initialized with 4-minute TTL");
    }

    /**
     * Caches a validated service token
     *
     * @param token Service JWT token
     * @param serviceName Name of the calling service
     * @param targetService Name of the target service
     */
    public void cacheToken(String token, String serviceName, String targetService) {
        ServiceTokenInfo info = new ServiceTokenInfo(serviceName, targetService, System.currentTimeMillis());
        tokenCache.put(token, info);
        log.debug("✅ Cached service token for {} -> {}", serviceName, targetService);
    }

    /**
     * Retrieves cached service token info
     *
     * @param token Service JWT token
     * @return Optional containing token info if cached and valid
     */
    public Optional<ServiceTokenInfo> get(String token) {
        ServiceTokenInfo info = tokenCache.getIfPresent(token);
        if (info != null) {
            log.debug("🎯 Cache HIT for service token: {} -> {}", info.serviceName(), info.targetService());
            return Optional.of(info);
        }

        log.debug("❌ Cache MISS for service token");
        return Optional.empty();
    }

    /**
     * Invalidates a specific token
     *
     * @param token Token to invalidate
     */
    public void invalidate(String token) {
        tokenCache.invalidate(token);
        log.debug("🗑️ Invalidated service token from cache");
    }

    /**
     * Clears all cached tokens
     */
    public void clearAll() {
        tokenCache.invalidateAll();
        log.info("🗑️ Cleared all cached service tokens");
    }

    /**
     * Increments failure count for a service
     *
     * @param serviceName Name of the failing service
     * @return Updated failure count
     */
    public long incrementFailureCount(String serviceName) {
        long count = failureCount.compute(serviceName, (k, v) -> (v == null ? 0 : v) + 1);
        log.warn("⚠️ Service {} failure count: {}", serviceName, count);
        return count;
    }

    /**
     * Resets failure count for a service (called on successful call)
     *
     * @param serviceName Name of the service
     */
    public void resetFailureCount(String serviceName) {
        failureCount.remove(serviceName);
        log.debug("✅ Reset failure count for service: {}", serviceName);
    }

    /**
     * Gets current failure count for a service
     *
     * @param serviceName Name of the service
     * @return Current failure count
     */
    public long getFailureCount(String serviceName) {
        return failureCount.getOrDefault(serviceName, 0L);
    }

    /**
     * Gets cache statistics
     *
     * @return Cache stats summary
     */
    public String getStats() {
        var stats = tokenCache.stats();
        return String.format(
                "Cache Stats - Hits: %d, Misses: %d, Hit Rate: %.2f%%, Evictions: %d, Size: %d",
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate() * 100,
                stats.evictionCount(),
                tokenCache.estimatedSize()
        );
    }

    /**
     * Service token information stored in cache
     */
    public record ServiceTokenInfo(
            String serviceName,
            String targetService,
            long cachedAt
    ) {}
}
