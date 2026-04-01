package com.propertize.platform.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Permission Cache Service
 *
 * Stores and retrieves the resolved permission set for a JWT (keyed by JTI)
 * in Redis. This is the core of the JWT-size-reduction strategy:
 *
 * Instead of embedding the full permissions list in the JWT payload (which can
 * balloon to 50+ entries × ~20 chars = ~1 KB+), we store permissions in Redis
 * and let the API Gateway look them up at request time using the JWT's {@code jti}.
 *
 * Key format: {@code perms:jti:{jti}}
 * TTL: same as the access token lifetime (900 seconds = 15 minutes)
 *
 * On logout / token blacklist: call {@link #evictPermissions(String)} to
 * immediately invalidate the cache entry.
 * On token refresh: old JTI's entry expires naturally (or is evicted), and a
 * new entry is written for the new JTI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheService {

    /** Redis key prefix for permission cache entries. */
    private static final String PERM_KEY_PREFIX = "perms:jti:";

    /** TTL matches the access token lifetime: 15 minutes. */
    public static final long ACCESS_TOKEN_TTL_SECONDS = 900L;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Store the resolved permissions for a given JWT JTI.
     *
     * @param jti         the JWT ID (jti claim) of the access token
     * @param permissions the fully resolved, denial-applied permission set
     */
    public void cachePermissions(String jti, Set<String> permissions) {
        if (jti == null || jti.isBlank() || permissions == null) {
            log.warn("⚠️ Skipping permission cache: jti={}, permissions={}", jti, permissions);
            return;
        }

        String key = PERM_KEY_PREFIX + jti;
        String value = String.join(",", permissions);

        try {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ACCESS_TOKEN_TTL_SECONDS));
            log.debug("✅ Cached {} permissions for jti={}", permissions.size(), jti);
        } catch (Exception e) {
            log.warn("⚠️ Redis permission cache write failed for jti={}: {}", jti, e.getMessage());
            // Non-fatal — gateway will fall back to empty permission set
        }
    }

    /**
     * Retrieve the cached permissions for a JWT JTI.
     *
     * @param jti the JWT ID (jti claim)
     * @return the permission set, or empty set if cache miss or Redis unavailable
     */
    public Set<String> getPermissions(String jti) {
        if (jti == null || jti.isBlank()) {
            return Collections.emptySet();
        }

        try {
            String value = stringRedisTemplate.opsForValue().get(PERM_KEY_PREFIX + jti);
            if (value == null || value.isBlank()) {
                log.debug("Cache miss for jti={}", jti);
                return Collections.emptySet();
            }
            Set<String> permissions = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            log.debug("Cache hit: {} permissions for jti={}", permissions.size(), jti);
            return permissions;
        } catch (Exception e) {
            log.warn("⚠️ Redis permission cache read failed for jti={}: {}", jti, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Remove the cached permissions for a JWT JTI (e.g., on logout or revocation).
     *
     * @param jti the JWT ID (jti claim)
     */
    public void evictPermissions(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        try {
            Boolean deleted = stringRedisTemplate.delete(PERM_KEY_PREFIX + jti);
            log.debug("Evicted permission cache for jti={} (deleted={})", jti, deleted);
        } catch (Exception e) {
            log.warn("⚠️ Redis permission cache eviction failed for jti={}: {}", jti, e.getMessage());
        }
    }
}

