package com.propertize.platform.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Public Key Cache for JWT Verification
 * 
 * Implements multi-level caching as per Production-Ready Authentication Design:
 * - Level 1: In-memory cache (fastest, ~0.001ms)
 * - Level 2: Redis cache (~1ms)
 * - Level 3: Auth Service fetch (~10-20ms, only on cache miss)
 * 
 * Performance targets:
 * - Cache hit rate: > 99%
 * - Public key fetch (cache hit): < 0.01ms
 * - Public key fetch (cache miss): < 20ms
 * 
 * @author Platform Security Team
 * @version 2.0
 */
@Slf4j
@Service
public class PublicKeyCache {

    private static final String PUBLIC_KEY_CACHE_KEY = "jwt:public:key";
    private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final RsaKeyProvider rsaKeyProvider;
    private final StringRedisTemplate redisTemplate;
    private final WebClient webClient;

    @Value("${auth-service.public-key-url:http://auth-service/api/v1/auth/public-key}")
    private String authServicePublicKeyUrl;

    @Value("${security.jwt.issuer:https://auth.propertize.com}")
    private String expectedIssuer;

    // Level 1: In-memory cache
    private RSAPublicKey cachedKey;
    private Instant lastRefresh;
    private String currentKeyId;

    // Level 1 cache statistics
    private long cacheHits = 0;
    private long cacheMisses = 0;

    public PublicKeyCache(RsaKeyProvider rsaKeyProvider,
            StringRedisTemplate redisTemplate) {
        this.rsaKeyProvider = rsaKeyProvider;
        this.redisTemplate = redisTemplate;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @PostConstruct
    public void init() {
        // Try to load from RSA key provider first (file-based)
        if (rsaKeyProvider.isRsaEnabled()) {
            cachedKey = (RSAPublicKey) rsaKeyProvider.getPublicKey();
            lastRefresh = Instant.now();
            currentKeyId = "file-based-key";
            log.info("✅ Public key loaded from file-based RSA key provider");
        } else {
            // Try to load from Redis or Auth Service
            try {
                refreshPublicKey();
            } catch (Exception e) {
                log.warn("Could not load public key on startup: {}", e.getMessage());
            }
        }
    }

    /**
     * Get public key - uses multi-level caching for performance
     * 
     * @return RSA public key for JWT verification
     */
    public RSAPublicKey getPublicKey() {
        // Level 1: In-memory cache (fastest, ~0.001ms)
        if (cachedKey != null && isStillValid()) {
            cacheHits++;
            return cachedKey;
        }

        cacheMisses++;

        // Level 2: Redis cache (~1ms)
        try {
            String redisKey = redisTemplate.opsForValue().get(PUBLIC_KEY_CACHE_KEY);
            if (redisKey != null && !redisKey.isEmpty()) {
                cachedKey = parsePublicKey(redisKey);
                lastRefresh = Instant.now();
                log.debug("Public key loaded from Redis cache");
                return cachedKey;
            }
        } catch (Exception e) {
            log.warn("Redis cache miss or error: {}", e.getMessage());
        }

        // Level 3: Fetch from Auth Service (~10-20ms, only on cache miss)
        return refreshPublicKey();
    }

    /**
     * Refresh public key from Auth Service
     * Called only on cache miss or expiry
     */
    @Scheduled(fixedRate = 3600000) // Refresh every hour as backup
    public RSAPublicKey refreshPublicKey() {
        // If RSA key provider has keys, use those
        if (rsaKeyProvider.isRsaEnabled() && rsaKeyProvider.getPublicKey() != null) {
            cachedKey = (RSAPublicKey) rsaKeyProvider.getPublicKey();
            lastRefresh = Instant.now();
            return cachedKey;
        }

        try {
            log.info("Fetching public key from Auth Service: {}", authServicePublicKeyUrl);

            // Fetch from Auth Service
            String response = webClient.get()
                    .uri(authServicePublicKeyUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            if (response != null) {
                // Extract public key from response (assuming JSON response)
                String publicKeyPem = extractPublicKeyFromResponse(response);
                cachedKey = parsePublicKey(publicKeyPem);
                lastRefresh = Instant.now();

                // Store in Redis for other gateway instances
                try {
                    redisTemplate.opsForValue().set(
                            PUBLIC_KEY_CACHE_KEY,
                            publicKeyPem,
                            CACHE_TTL);
                    log.info("✅ Public key cached in Redis");
                } catch (Exception e) {
                    log.warn("Failed to cache public key in Redis: {}", e.getMessage());
                }

                log.info("✅ Public key refreshed from Auth Service");
                return cachedKey;
            }

            throw new RuntimeException("Empty response from Auth Service");

        } catch (Exception e) {
            // If Auth Service is down but we have cached key, use it
            if (cachedKey != null) {
                log.warn("Auth Service unavailable, using cached public key: {}", e.getMessage());
                return cachedKey;
            }
            throw new RuntimeException("Cannot retrieve public key and no cache available", e);
        }
    }

    /**
     * Check if cached key is still valid
     */
    private boolean isStillValid() {
        return lastRefresh != null &&
                Duration.between(lastRefresh, Instant.now()).compareTo(CACHE_TTL) < 0;
    }

    /**
     * Parse public key from PEM format
     */
    private RSAPublicKey parsePublicKey(String publicKeyPem) {
        try {
            String publicKeyContent = publicKeyPem
                    .replace(PUBLIC_KEY_HEADER, "")
                    .replace(PUBLIC_KEY_FOOTER, "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse public key", e);
        }
    }

    /**
     * Extract public key PEM from Auth Service JSON response
     */
    private String extractPublicKeyFromResponse(String response) {
        // Simple extraction - assumes response contains publicKey field
        // In production, use proper JSON parsing
        if (response.contains("publicKey")) {
            int start = response.indexOf("\"publicKey\":\"") + 13;
            int end = response.indexOf("\"", start);
            if (start > 12 && end > start) {
                return response.substring(start, end)
                        .replace("\\n", "\n");
            }
        }
        // If response is already PEM format
        if (response.contains(PUBLIC_KEY_HEADER)) {
            return response;
        }
        throw new RuntimeException("Could not extract public key from response");
    }

    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(cacheHits, cacheMisses, lastRefresh, cachedKey != null);
    }

    /**
     * Invalidate cache (for key rotation)
     */
    public void invalidateCache() {
        cachedKey = null;
        lastRefresh = null;
        try {
            redisTemplate.delete(PUBLIC_KEY_CACHE_KEY);
        } catch (Exception e) {
            log.warn("Failed to delete Redis cache: {}", e.getMessage());
        }
        log.info("Public key cache invalidated");
    }

    /**
     * Get current key ID
     */
    public String getCurrentKeyId() {
        return currentKeyId;
    }

    public record CacheStats(long hits, long misses, Instant lastRefresh, boolean hasKey) {
        public double hitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }
    }
}
