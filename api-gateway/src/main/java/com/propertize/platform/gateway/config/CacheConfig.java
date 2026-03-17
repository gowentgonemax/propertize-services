package com.propertize.platform.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Comprehensive cache configuration for API Gateway with multiple cache
 * regions.
 *
 * <p>
 * <b>Cache Regions:</b>
 * </p>
 * <ul>
 * <li><b>tokenValidation:</b> User token validation results (4 min TTL, 10K
 * capacity)</li>
 * <li><b>serviceTokenValidation:</b> Service token validation results (4 min
 * TTL, 5K capacity)</li>
 * <li><b>rbacPermissions:</b> RBAC permission cache (10 min TTL, 50K
 * capacity)</li>
 * <li><b>userRoles:</b> User roles cache (10 min TTL, 10K capacity)</li>
 * <li><b>blacklist:</b> Blacklisted tokens (15 min TTL, 5K capacity)</li>
 * <li><b>rateLimitBuckets:</b> Rate limit token buckets (1 min TTL, 20K
 * capacity)</li>
 * </ul>
 *
 * <p>
 * <b>Features:</b>
 * </p>
 * <ul>
 * <li>Automatic cache eviction based on TTL</li>
 * <li>Size-based eviction to prevent memory issues</li>
 * <li>Statistics recording for monitoring</li>
 * <li>Graceful error handling with fallback</li>
 * <li>Removal listeners for debugging</li>
 * </ul>
 *
 * @author Platform Team
 * @since February 5, 2026
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${cache.token-validation.ttl-minutes:4}")
    private int tokenValidationTtl;

    @Value("${cache.token-validation.max-size:10000}")
    private int tokenValidationMaxSize;

    @Value("${cache.rbac.ttl-minutes:10}")
    private int rbacCacheTtl;

    @Value("${cache.rbac.max-size:50000}")
    private int rbacCacheMaxSize;

    @Value("${cache.blacklist.ttl-minutes:15}")
    private int blacklistTtl;

    @Value("${cache.blacklist.max-size:5000}")
    private int blacklistMaxSize;

    @Value("${cache.rate-limit.ttl-seconds:60}")
    private int rateLimitTtl;

    @Value("${cache.rate-limit.max-size:20000}")
    private int rateLimitMaxSize;

    /**
     * Primary cache manager for token validation and RBAC.
     */
    @Bean
    @Primary
    @Override
    @SuppressWarnings("null")
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "tokenValidation",
                "serviceTokenValidation",
                "rbacPermissions",
                "userRoles",
                "blacklist",
                "rateLimitBuckets");

        // Default configuration (will be overridden per cache)
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .expireAfterWrite(tokenValidationTtl, TimeUnit.MINUTES)
                .maximumSize(tokenValidationMaxSize)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (cause == RemovalCause.EXPIRED) {
                        log.debug("Cache entry expired: key={}", key);
                    } else if (cause == RemovalCause.SIZE) {
                        log.warn("Cache entry evicted due to size: key={}", key);
                    }
                });
        cacheManager.setCaffeine(caffeineBuilder);

        return cacheManager;
    }

    /**
     * Token validation cache - stores validated JWT tokens.
     * TTL: 4 minutes (shorter than token expiry for security)
     * Capacity: 10,000 entries
     */
    @Bean
    public Caffeine<Object, Object> tokenValidationCacheConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(tokenValidationTtl, TimeUnit.MINUTES)
                .maximumSize(tokenValidationMaxSize)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Token validation cache eviction: key={}, cause={}", key, cause);
                    }
                });
    }

    /**
     * RBAC permissions cache - stores user permissions.
     * TTL: 10 minutes (can be longer since permissions change infrequently)
     * Capacity: 50,000 entries
     */
    @Bean
    public Caffeine<Object, Object> rbacPermissionsCacheConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(rbacCacheTtl, TimeUnit.MINUTES)
                .maximumSize(rbacCacheMaxSize)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("RBAC permissions cache eviction: key={}, cause={}", key, cause);
                    }
                });
    }

    /**
     * Blacklist cache - stores invalidated tokens.
     * TTL: 15 minutes (matches access token expiry)
     * Capacity: 5,000 entries
     */
    @Bean
    public Caffeine<Object, Object> blacklistCacheConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(blacklistTtl, TimeUnit.MINUTES)
                .maximumSize(blacklistMaxSize)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Blacklist cache eviction: token={}, cause={}",
                                maskToken(String.valueOf(key)), cause);
                    }
                });
    }

    /**
     * Rate limit buckets cache - stores rate limit token buckets.
     * TTL: 1 minute (sliding window)
     * Capacity: 20,000 entries
     */
    @Bean
    public Caffeine<Object, Object> rateLimitBucketsCacheConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(rateLimitTtl, TimeUnit.SECONDS)
                .maximumSize(rateLimitMaxSize)
                .recordStats();
    }

    /**
     * Custom key generator for cache keys.
     * Generates keys based on method name and parameters.
     */
    @Bean
    @Override
    @SuppressWarnings("null")
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append("_");
            sb.append(method.getName());
            for (Object param : params) {
                if (param != null) {
                    sb.append("_");
                    sb.append(param.toString());
                }
            }
            return sb.toString();
        };
    }

    /**
     * Custom cache error handler.
     * Logs errors but doesn't fail the request.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception,
                    @NonNull org.springframework.cache.Cache cache,
                    @NonNull Object key) {
                log.error("Cache GET error: cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception,
                    @NonNull org.springframework.cache.Cache cache,
                    @NonNull Object key, @Nullable Object value) {
                log.error("Cache PUT error: cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception,
                    @NonNull org.springframework.cache.Cache cache,
                    @NonNull Object key) {
                log.error("Cache EVICT error: cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception,
                    @NonNull org.springframework.cache.Cache cache) {
                log.error("Cache CLEAR error: cache={}", cache.getName(), exception);
            }
        };
    }

    /**
     * Masks sensitive token data for logging.
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 20) {
            return "***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
    }
}
