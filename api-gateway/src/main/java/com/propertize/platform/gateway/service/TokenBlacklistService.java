package com.propertize.platform.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Blacklist Service for API Gateway
 *
 * Handles:
 * - Token blacklisting (for logout)
 * - JTI-based blacklisting (recommended)
 * - Refresh token tracking (for rotation)
 * - Replay attack prevention
 *
 * Supports:
 * - Redis (distributed, recommended for production)
 * - In-memory fallback (for development/testing)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String JTI_BLACKLIST_PREFIX = "token:blacklist:jti:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final String USED_TOKEN_PREFIX = "token:used:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    // In-memory fallback for when Redis is not available
    private final ConcurrentHashMap<String, Long> memoryBlacklist = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> memoryJtiBlacklist = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> memoryRefreshTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> memoryUsedTokens = new ConcurrentHashMap<>();

    private boolean useRedis = true;

    /**
     * Blacklist an access token (logout)
     */
    public void blacklistToken(String token, long expirationSeconds) {
        String key = BLACKLIST_PREFIX + hashToken(token);

        if (useRedis) {
            try {
                redisTemplate.opsForValue()
                    .set(key, "blacklisted", Duration.ofSeconds(expirationSeconds))
                    .subscribe(
                        success -> log.debug("Token blacklisted in Redis"),
                        error -> {
                            log.warn("Redis blacklist failed, using memory: {}", error.getMessage());
                            memoryBlacklist.put(key, System.currentTimeMillis() + (expirationSeconds * 1000));
                        }
                    );
            } catch (Exception e) {
                log.warn("Redis unavailable, using memory blacklist");
                useRedis = false;
                memoryBlacklist.put(key, System.currentTimeMillis() + (expirationSeconds * 1000));
            }
        } else {
            memoryBlacklist.put(key, System.currentTimeMillis() + (expirationSeconds * 1000));
        }
    }

    /**
     * Blacklist a token by JTI (JWT ID) - Recommended approach
     * More efficient than full token hashing
     */
    public void blacklistTokenByJti(String jti, long expirationSeconds) {
        if (jti == null || jti.isEmpty()) {
            log.warn("Attempted to blacklist null or empty JTI");
            return;
        }

        String key = JTI_BLACKLIST_PREFIX + jti;

        if (useRedis) {
            try {
                redisTemplate.opsForValue()
                    .set(key, "blacklisted", Duration.ofSeconds(expirationSeconds))
                    .subscribe(
                        success -> log.debug("Token blacklisted by JTI in Redis: {}", jti),
                        error -> {
                            log.warn("Redis blacklist failed, using memory: {}", error.getMessage());
                            memoryJtiBlacklist.put(key, System.currentTimeMillis() + (expirationSeconds * 1000));
                        }
                    );
            } catch (Exception e) {
                log.warn("Redis unavailable, using memory blacklist");
                useRedis = false;
                memoryJtiBlacklist.put(key, System.currentTimeMillis() + (expirationSeconds * 1000));
            }
        } else {
            memoryJtiBlacklist.put(key, System.currentTimeMillis() + (expirationSeconds * 1000));
        }
    }

    /**
     * Check if token is blacklisted by JTI (JWT ID)
     * Per Production-Ready Design: Use JTI for efficient blacklist lookup
     *
     * @param jti the JWT ID (jti claim) to check
     * @return true if blacklisted, false otherwise
     */
    public boolean isBlacklistedByJti(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }

        String key = JTI_BLACKLIST_PREFIX + jti;

        if (useRedis) {
            try {
                Boolean exists = redisTemplate.hasKey(key).block(Duration.ofSeconds(1));
                return Boolean.TRUE.equals(exists);
            } catch (Exception e) {
                log.warn("Redis check failed, using memory: {}", e.getMessage());
                useRedis = false;
            }
        }

        // Memory fallback
        Long expiry = memoryJtiBlacklist.get(key);
        if (expiry != null) {
            if (System.currentTimeMillis() < expiry) {
                return true;
            } else {
                memoryJtiBlacklist.remove(key);
            }
        }
        return false;
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + hashToken(token);

        if (useRedis) {
            try {
                Boolean exists = redisTemplate.hasKey(key).block(Duration.ofSeconds(1));
                return Boolean.TRUE.equals(exists);
            } catch (Exception e) {
                log.warn("Redis check failed, using memory: {}", e.getMessage());
                useRedis = false;
            }
        }

        // Memory fallback
        Long expiry = memoryBlacklist.get(key);
        if (expiry != null) {
            if (System.currentTimeMillis() < expiry) {
                return true;
            } else {
                memoryBlacklist.remove(key);
            }
        }
        return false;
    }

    /**
     * Store refresh token for tracking
     */
    public void storeRefreshToken(String refreshToken, String username, long expirationSeconds) {
        String key = REFRESH_TOKEN_PREFIX + hashToken(refreshToken);

        if (useRedis) {
            try {
                redisTemplate.opsForValue()
                    .set(key, username, Duration.ofSeconds(expirationSeconds))
                    .subscribe(
                        success -> log.debug("Refresh token stored in Redis for user: {}", username),
                        error -> {
                            log.warn("Redis store failed, using memory: {}", error.getMessage());
                            memoryRefreshTokens.put(key, username);
                        }
                    );
            } catch (Exception e) {
                log.warn("Redis unavailable, using memory");
                useRedis = false;
                memoryRefreshTokens.put(key, username);
            }
        } else {
            memoryRefreshTokens.put(key, username);
        }
    }

    /**
     * Mark refresh token as used (for rotation)
     */
    public void markRefreshTokenAsUsed(String refreshToken) {
        String key = USED_TOKEN_PREFIX + hashToken(refreshToken);

        if (useRedis) {
            try {
                redisTemplate.opsForValue()
                    .set(key, "used", Duration.ofDays(7)) // Keep for 7 days for audit
                    .subscribe(
                        success -> log.debug("Refresh token marked as used"),
                        error -> {
                            log.warn("Redis mark failed, using memory: {}", error.getMessage());
                            memoryUsedTokens.put(key, true);
                        }
                    );
            } catch (Exception e) {
                log.warn("Redis unavailable, using memory");
                useRedis = false;
                memoryUsedTokens.put(key, true);
            }
        } else {
            memoryUsedTokens.put(key, true);
        }
    }

    /**
     * Check if refresh token has been used (replay attack detection)
     */
    public boolean isRefreshTokenUsed(String refreshToken) {
        String key = USED_TOKEN_PREFIX + hashToken(refreshToken);

        if (useRedis) {
            try {
                Boolean exists = redisTemplate.hasKey(key).block(Duration.ofSeconds(1));
                return Boolean.TRUE.equals(exists);
            } catch (Exception e) {
                log.warn("Redis check failed, using memory: {}", e.getMessage());
                useRedis = false;
            }
        }

        return memoryUsedTokens.containsKey(key);
    }

    /**
     * Revoke refresh token (logout or security event)
     */
    public void revokeRefreshToken(String refreshToken) {
        String refreshKey = REFRESH_TOKEN_PREFIX + hashToken(refreshToken);
        String usedKey = USED_TOKEN_PREFIX + hashToken(refreshToken);

        if (useRedis) {
            try {
                redisTemplate.delete(refreshKey).subscribe();
                redisTemplate.opsForValue()
                    .set(usedKey, "revoked", Duration.ofDays(7))
                    .subscribe();
            } catch (Exception e) {
                log.warn("Redis revoke failed, using memory: {}", e.getMessage());
                useRedis = false;
            }
        }

        memoryRefreshTokens.remove(refreshKey);
        memoryUsedTokens.put(usedKey, true);
    }

    /**
     * Clean up expired entries from memory (called periodically)
     */
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        memoryBlacklist.entrySet().removeIf(entry -> entry.getValue() < now);
        log.debug("Cleaned up expired blacklist entries");
    }

    /**
     * Hash token for storage (to reduce key size and add security)
     */
    private String hashToken(String token) {
        // Use last 32 chars as a pseudo-hash for simplicity
        // In production, use actual hashing
        if (token.length() > 32) {
            return token.substring(token.length() - 32);
        }
        return token;
    }
}
