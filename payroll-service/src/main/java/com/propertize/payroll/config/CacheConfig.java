package com.propertize.payroll.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Two-level cache for payroll-service:
 * - L1: Caffeine (in-process, hot path reads)
 * - L2: Redis (distributed, shared across instances)
 *
 * Cache regions and TTLs:
 * - paystubs: Paystub lookups per employee (L1: 5 min | L2: 10 min)
 * - paystubsYtd: YTD aggregation per employee (L1: 1 hr | L2: 24 hr — only
 * recomputed nightly)
 * - payrollRuns: Payroll run lookups (L1: 5 min | L2: 10 min)
 * - timesheets: Timesheet reads (L1: 5 min | L2: 10 min)
 * - departments: Department data (L1: 15 min | L2: 30 min)
 * - benefits: Benefit plan data (L1: 15 min | L2: 30 min)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        manager.setCacheNames(java.util.List.of(
                "paystubs", "paystubsYtd", "payrollRuns",
                "timesheets", "departments", "benefits"));
        return manager;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("paystubs", defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put("paystubsYtd", defaults.entryTtl(Duration.ofHours(24)));
        perCache.put("payrollRuns", defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put("timesheets", defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put("departments", defaults.entryTtl(Duration.ofMinutes(30)));
        perCache.put("benefits", defaults.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Primary
    @Bean
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
            RedisCacheManager redisCacheManager) {
        CompositeCacheManager composite = new CompositeCacheManager();
        composite.setCacheManagers(java.util.List.of(caffeineCacheManager, redisCacheManager));
        composite.setFallbackToNoOpCache(false);
        return composite;
    }
}
