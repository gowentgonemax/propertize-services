package com.propertize.platform.employecraft.config;

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
 * Two-level cache for employee-service:
 * - L1: Caffeine (in-process, hot path reads)
 * - L2: Redis (distributed, shared across instances)
 *
 * Cache regions and TTLs:
 * - employees: Employee lookups (L1: 5 min | L2: 10 min)
 * - departments: Department data (L1: 15 min | L2: 30 min)
 * - attendance: Attendance records (L1: 2 min | L2: 5 min)
 * - organizationStaff: Staff list per org (L1: 5 min | L2: 10 min)
 * - payroll: Payroll summary per employee(L1: 5 min | L2: 10 min)
 * - employeesByOrg: Employee page per org (L1: 5 min | L2: 10 min)
 * - orgMetadata: Organisation metadata (L1: 5 min | L2: 5 min)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .recordStats());
        manager.setCacheNames(java.util.List.of(
                "employees", "departments", "attendance", "organizationStaff",
                "payroll", "employeesByOrg", "orgMetadata"));
        return manager;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("employees", defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put("departments", defaults.entryTtl(Duration.ofMinutes(30)));
        perCache.put("attendance", defaults.entryTtl(Duration.ofMinutes(5)));
        perCache.put("organizationStaff", defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put("payroll", defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put("employeesByOrg", defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put("orgMetadata", defaults.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults.entryTtl(Duration.ofMinutes(10)))
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
