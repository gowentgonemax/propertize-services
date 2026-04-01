package com.propertize.platform.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gateway Permission Cache Service
 *
 * Fetches the resolved permission set for a JWT from Redis, using the token's
 * {@code jti} claim as the cache key. This is the gateway-side complement to
 * the auth-service's {@code PermissionCacheService}.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>JWT is validated by {@link com.propertize.platform.gateway.security.JwtAuthenticationFilter}</li>
 *   <li>The {@code jti} claim is extracted from the validated token</li>
 *   <li>This service fetches {@code perms:jti:{jti}} from Redis</li>
 *   <li>The result is injected as the {@code X-Permissions} header for downstream services</li>
 * </ol>
 *
 * <p>On cache miss (Redis down or TTL expired), the filter will inject an empty
 * {@code X-Permissions} header. Downstream services should treat this as
 * "no permissions available" and deny access to protected resources.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private static final String PERM_KEY_PREFIX = "perms:jti:";
    private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(1);

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Synchronously fetch the permissions for a given JWT JTI.
     * Blocks for at most 1 second; returns empty set on timeout or error.
     *
     * @param jti the JWT ID (jti claim)
     * @return set of permission strings, or empty set on cache miss / error
     */
    public Set<String> getPermissions(String jti) {
        if (jti == null || jti.isBlank()) {
            return Collections.emptySet();
        }

        try {
            String value = redisTemplate.opsForValue()
                    .get(PERM_KEY_PREFIX + jti)
                    .block(LOOKUP_TIMEOUT);

            if (value == null || value.isBlank()) {
                log.debug("Permission cache miss for jti={}", jti);
                return Collections.emptySet();
            }

            Set<String> permissions = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            log.debug("Permission cache hit: {} permissions for jti={}", permissions.size(), jti);
            return permissions;

        } catch (Exception e) {
            log.warn("⚠️ Permission cache lookup failed for jti={}: {} — proceeding with empty permissions",
                    jti, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Evict the permission cache entry for a given JTI (e.g., when a token is
     * explicitly revoked from the gateway side).
     *
     * @param jti the JWT ID (jti claim)
     */
    public void evictPermissions(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(PERM_KEY_PREFIX + jti)
                    .subscribe(
                            deleted -> log.debug("Evicted permission cache for jti={} (deleted={})", jti, deleted),
                            error -> log.warn("Failed to evict permission cache for jti={}: {}", jti, error.getMessage()));
        } catch (Exception e) {
            log.warn("⚠️ Permission cache eviction failed for jti={}: {}", jti, e.getMessage());
        }
    }
}

