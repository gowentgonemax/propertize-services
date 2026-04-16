package com.propertize.platform.auth.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PermissionCacheService Tests")
class PermissionCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private PermissionCacheService permissionCacheService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── cachePermissions ────────────────────────────────────────────────────

    @Nested
    @DisplayName("cachePermissions()")
    class CachePermissions {

        @Test
        @DisplayName("Writes comma-separated permissions to Redis with TTL")
        void writesPermissionsToRedis() {
            Set<String> perms = Set.of("property:read", "tenant:view");

            permissionCacheService.cachePermissions("jti-001", perms);

            verify(valueOps).set(
                    eq("perms:jti:jti-001"),
                    argThat(v -> v.contains("property:read") && v.contains("tenant:view")),
                    eq(Duration.ofSeconds(PermissionCacheService.ACCESS_TOKEN_TTL_SECONDS)));
        }

        @Test
        @DisplayName("Skips write when jti is null")
        void skipsOnNullJti() {
            permissionCacheService.cachePermissions(null, Set.of("property:read"));
            verify(valueOps, never()).set(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("Skips write when jti is blank")
        void skipsOnBlankJti() {
            permissionCacheService.cachePermissions("  ", Set.of("property:read"));
            verify(valueOps, never()).set(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("Skips write when permissions set is null")
        void skipsOnNullPermissions() {
            permissionCacheService.cachePermissions("jti-x", null);
            verify(valueOps, never()).set(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("Does not throw when Redis write fails")
        void doesNotThrowOnRedisError() {
            doThrow(new RuntimeException("Redis down"))
                    .when(valueOps).set(any(), any(), any(Duration.class));

            assertThatCode(() -> permissionCacheService.cachePermissions("jti-err", Set.of("perm:x")))
                    .doesNotThrowAnyException();
        }
    }

    // ── getPermissions ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPermissions()")
    class GetPermissions {

        @Test
        @DisplayName("Returns parsed permission set on cache hit")
        void returnsParsedPermissions() {
            when(valueOps.get("perms:jti:jti-002")).thenReturn("property:read,tenant:view");

            Set<String> result = permissionCacheService.getPermissions("jti-002");

            assertThat(result).containsExactlyInAnyOrder("property:read", "tenant:view");
        }

        @Test
        @DisplayName("Returns empty set on cache miss (null value)")
        void returnsEmptyOnCacheMiss() {
            when(valueOps.get("perms:jti:jti-003")).thenReturn(null);

            assertThat(permissionCacheService.getPermissions("jti-003")).isEmpty();
        }

        @Test
        @DisplayName("Returns empty set when jti is null")
        void returnsEmptyForNullJti() {
            assertThat(permissionCacheService.getPermissions(null)).isEmpty();
        }

        @Test
        @DisplayName("Returns empty set when jti is blank")
        void returnsEmptyForBlankJti() {
            assertThat(permissionCacheService.getPermissions("   ")).isEmpty();
        }

        @Test
        @DisplayName("Returns empty set when Redis read throws")
        void returnsEmptyOnRedisError() {
            when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis unavailable"));

            assertThat(permissionCacheService.getPermissions("jti-err")).isEmpty();
        }

        @Test
        @DisplayName("Trims whitespace around individual permissions")
        void trimsPermissions() {
            when(valueOps.get("perms:jti:jti-trim")).thenReturn(" perm:a , perm:b ");

            Set<String> result = permissionCacheService.getPermissions("jti-trim");

            assertThat(result).containsExactlyInAnyOrder("perm:a", "perm:b");
        }
    }

    // ── evictPermissions ────────────────────────────────────────────────────

    @Nested
    @DisplayName("evictPermissions()")
    class EvictPermissions {

        @Test
        @DisplayName("Deletes Redis key for given jti")
        void deletesKey() {
            when(stringRedisTemplate.delete("perms:jti:jti-del")).thenReturn(true);

            permissionCacheService.evictPermissions("jti-del");

            verify(stringRedisTemplate).delete("perms:jti:jti-del");
        }

        @Test
        @DisplayName("Does nothing when jti is null")
        void doesNothingForNullJti() {
            permissionCacheService.evictPermissions(null);
            verify(stringRedisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Does nothing when jti is blank")
        void doesNothingForBlankJti() {
            permissionCacheService.evictPermissions("");
            verify(stringRedisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Does not throw when Redis delete fails")
        void doesNotThrowOnRedisError() {
            when(stringRedisTemplate.delete(anyString())).thenThrow(new RuntimeException("down"));

            assertThatCode(() -> permissionCacheService.evictPermissions("jti-err"))
                    .doesNotThrowAnyException();
        }
    }
}
