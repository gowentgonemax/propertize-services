package com.propertize.platform.employecraft.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Multi-layer Caching Configuration for Employecraft
 * 
 * L1 Cache: Caffeine (in-memory, sub-millisecond access)
 * 
 * Cache Regions:
 * - employees: Employee lookups (10 min TTL, max 5000 entries)
 * - departments: Department data (30 min TTL, max 500 entries)
 * - attendance: Attendance records (5 min TTL, max 10000 entries)
 * - organizationStaff: Staff by org (10 min TTL, max 1000 entries)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(defaultCaffeineBuilder());
        cacheManager.setCacheNames(java.util.List.of(
                "employees", "departments", "attendance", "organizationStaff",
                "payroll", "employeesByOrg"));
        return cacheManager;
    }

    private Caffeine<Object, Object> defaultCaffeineBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats();
    }
}
