package com.propertize.platform.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Token Blacklist Service
 * 
 * Manages revoked JWT tokens using Redis for distributed blacklist storage.
 * Implements token revocation for logout and security incidents.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String JTI_BLACKLIST_PREFIX = "blacklist:jti:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final String USED_TOKEN_PREFIX = "token:used:";
    private static final String SESSION_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Blacklist a token by its JTI with reason
     */
    public void blacklistByJti(String jti, long expirationSeconds, String reason) {
        if (jti == null || jti.isEmpty()) {
            return;
        }
        String key = JTI_BLACKLIST_PREFIX + jti;
        String value = String.format("blacklisted at %s - reason: %s", LocalDateTime.now(), reason);
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(expirationSeconds));
        log.info("Token blacklisted by JTI: jti={}, reason={}, ttl={}s", jti, reason, expirationSeconds);
    }

    /**
     * Check if a token is blacklisted by JTI
     */
    public boolean isBlacklistedByJti(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        String key = JTI_BLACKLIST_PREFIX + jti;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Blacklist a token by its hash
     */
    public void blacklistToken(String token, long expirationSeconds) {
        String hash = hashToken(token);
        String key = TOKEN_BLACKLIST_PREFIX + hash;
        redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(expirationSeconds));
        log.info("Token blacklisted by hash: ttl={}s", expirationSeconds);
    }

    /**
     * Check if a token is blacklisted by hash
     */
    public boolean isBlacklisted(String token) {
        String hash = hashToken(token);
        String key = TOKEN_BLACKLIST_PREFIX + hash;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Store refresh token with username
     */
    public void storeRefreshToken(String refreshToken, String username, long expirationSeconds) {
        String hash = hashToken(refreshToken);
        String key = REFRESH_TOKEN_PREFIX + hash;
        redisTemplate.opsForValue().set(key, username, Duration.ofSeconds(expirationSeconds));
        log.info("Refresh token stored for user: {}", username);
    }

    /**
     * Mark refresh token as used
     */
    public void markRefreshTokenAsUsed(String refreshToken) {
        String hash = hashToken(refreshToken);
        String key = USED_TOKEN_PREFIX + hash;
        redisTemplate.opsForValue().set(key, "used", Duration.ofDays(7));
        log.info("Refresh token marked as used");
    }

    /**
     * Check if refresh token has been used
     */
    public boolean isRefreshTokenUsed(String refreshToken) {
        String hash = hashToken(refreshToken);
        String key = USED_TOKEN_PREFIX + hash;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Revoke refresh token
     */
    public void revokeRefreshToken(String refreshToken) {
        String hash = hashToken(refreshToken);
        String refreshKey = REFRESH_TOKEN_PREFIX + hash;
        String usedKey = USED_TOKEN_PREFIX + hash;

        redisTemplate.delete(refreshKey);
        redisTemplate.opsForValue().set(usedKey, "revoked", Duration.ofDays(7));
        log.info("Refresh token revoked");
    }

    /**
     * Terminate a session
     */
    public void terminateSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, "terminated", Duration.ofDays(7));
        log.info("Session terminated: {}", sessionId);
    }

    /**
     * Check if session is terminated
     */
    public boolean isSessionTerminated(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        String key = SESSION_PREFIX + sessionId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Check if Redis is available
     */
    public boolean isRedisAvailable() {
        try {
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory != null) {
                factory.getConnection().ping();
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Redis connection failed", e);
            return false;
        }
    }

    private String hashToken(String token) {
        return DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
    }
}
