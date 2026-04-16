package com.propertize.platform.auth.config;

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
 * Two-level cache for auth-service:
 * - L1: Caffeine (in-process, sub-ms for hot paths)
 * - L2: Redis (distributed, shared across instances)
 *
 * Cache regions and TTLs:
 * - permissions: RBAC permission set per JTI (15 min — matches token lifetime)
 * - userPermissions: Resolved user permission set (15 min)
 * - userMetadata: User profile/claims pre-computed (10 min)
 * - orgMetadata: Organisation-level metadata (5 min)
 * - rbacPolicy: RBAC policy rules (30 min — changes infrequently)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "permissions", "userPermissions", "userMetadata", "orgMetadata", "rbacPolicy");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        return manager;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("permissions", defaults.entryTtl(Duration.ofMinutes(15)));
        perCache.put("userPermissions", defaults.entryTtl(Duration.ofMinutes(15)));
        perCache.put("userMetadata", defaults.entryTtl(Duration.ofMinutes(10)));
        perCache.put("orgMetadata", defaults.entryTtl(Duration.ofMinutes(5)));
        perCache.put("rbacPolicy", defaults.entryTtl(Duration.ofMinutes(30)));

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
