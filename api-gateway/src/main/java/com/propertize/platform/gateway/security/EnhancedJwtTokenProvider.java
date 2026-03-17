package com.propertize.platform.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EnhancedJwtTokenProvider {

    private static final int MIN_HMAC_KEY_BYTES = 32;

    private final RsaKeyProvider rsaKeyProvider;

    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    @Getter
    @Value("${security.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Getter
    @Value("${security.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${security.jwt.service-token-expiration-ms:300000}")
    private long serviceTokenExpirationMs;

    // Split into typed fields so verifyWith() receives the correct type
    private PublicKey rsaVerificationKey;
    private SecretKey hmacVerificationKey;
    private Key signingKey;

    public EnhancedJwtTokenProvider(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @PostConstruct
    public void init() {
        if (rsaKeyProvider.isRsaEnabled()) {
            initRsaKeys();
        } else {
            initHmacFallback();
        }
    }

    private void initRsaKeys() {
        this.rsaVerificationKey = rsaKeyProvider.getPublicKey();

        if (rsaKeyProvider.hasPrivateKey()) {
            this.signingKey = rsaKeyProvider.getPrivateKey();
            log.info("✅ JWT configured with RSA (RS256) — signing + verification");
        } else {
            this.signingKey = null;
            log.info("⚠️ JWT configured with RSA (RS256) — verification only (no private key)");
        }
    }

    private void initHmacFallback() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("❌ No JWT secret configured for HMAC fallback");
        }

        byte[] keyBytes = decodeSecret(jwtSecret);

        if (keyBytes.length < MIN_HMAC_KEY_BYTES) {
            throw new IllegalStateException("❌ JWT secret too short — must be at least 32 bytes");
        }

        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        this.hmacVerificationKey = key;
        this.signingKey = key;

        log.info("✅ JWT configured with HMAC (HS256)");
    }

    private byte[] decodeSecret(String raw) {
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            return raw.getBytes(StandardCharsets.UTF_8);
        }
    }

    // ============================================================
    // TOKEN VALIDATION
    // ============================================================

    public boolean validateToken(String token) {
        try {
            buildParser()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return buildParser()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Builds a JwtParser using the correctly-typed verification key.
     * verifyWith() is overloaded: one variant accepts SecretKey, the other PublicKey.
     * Passing the raw Key interface causes "Cannot resolve method 'verifyWith(Key)'".
     */
    private JwtParser buildParser() {
        if (rsaVerificationKey != null) {
            return Jwts.parser()
                    .verifyWith(rsaVerificationKey)
                    .build();
        }
        if (hmacVerificationKey != null) {
            return Jwts.parser()
                    .verifyWith(hmacVerificationKey)
                    .build();
        }
        throw new IllegalStateException("No verification key available");
    }

    // ============================================================
    // CLAIM EXTRACTION HELPERS
    // ============================================================

    public Optional<String> getUsername(String token) {
        return Optional.ofNullable(getClaims(token).getSubject());
    }

    public Optional<String> getOrganizationId(String token) {
        Claims c = getClaims(token);
        return Optional.ofNullable(
                Optional.ofNullable(c.get("organizationId", String.class))
                        .orElse(c.get("org", String.class))
        );
    }

    public Optional<String> getOrganizationCode(String token) {
        return Optional.ofNullable(getClaims(token).get("organizationCode", String.class));
    }

    public Set<String> getRoles(String token) {
        Object rolesObj = getClaims(token).get("roles");

        if (rolesObj instanceof List<?> list) {
            return new HashSet<>((List<String>) list);
        }

        if (rolesObj instanceof String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    public Optional<String> getPrimaryRole(String token) {
        return Optional.ofNullable(getClaims(token).get("role", String.class));
    }

    public Optional<String> getEmail(String token) {
        return Optional.ofNullable(getClaims(token).get("email", String.class));
    }

    public Optional<String> getTenantId(String token) {
        Claims c = getClaims(token);
        return Optional.ofNullable(
                Optional.ofNullable(c.get("tenant_id", String.class))
                        .orElse(c.get("tenantId", String.class))
        );
    }

    public Optional<String> getSessionId(String token) {
        Claims c = getClaims(token);
        return Optional.ofNullable(
                Optional.ofNullable(c.get("session_id", String.class))
                        .orElse(c.get("sessionId", String.class))
        );
    }

    public Optional<String> getTokenId(String token) {
        return Optional.ofNullable(getClaims(token).getId());
    }

    // ============================================================
    // TOKEN GENERATION
    // ============================================================

    public String generateAccessToken(String username, Map<String, Object> claims) {
        ensureSigningKey();

        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .claim("type", "access")   // Bug fix: access tokens must carry explicit type
                .claim("iss", "api-gateway");

        if (claims != null) {
            claims.forEach(builder::claim);
        }

        return builder.signWith(signingKey).compact();
    }

    public String generateRefreshToken(String username, Map<String, Object> claims) {
        ensureSigningKey();

        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .claim("type", "refresh")
                .claim("iss", "api-gateway");

        if (claims != null) {
            claims.forEach(builder::claim);
        }

        return builder.signWith(signingKey).compact();
    }

    public Optional<String> generateServiceToken(String source, String target) {
        if (signingKey == null) {
            log.warn("Cannot generate service token — no signing key available");
            return Optional.empty();
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + serviceTokenExpirationMs);

        try {
            String token = Jwts.builder()
                    .subject(source)
                    .issuedAt(now)
                    .expiration(expiration)
                    .claim("type", "service")
                    .claim("source", source)
                    .claim("target", target)
                    .claim("iss", "api-gateway")
                    .signWith(signingKey)
                    .compact();

            return Optional.of(token);
        } catch (Exception e) {
            log.error("Failed to generate service token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void ensureSigningKey() {
        if (signingKey == null) {
            throw new IllegalStateException("No signing key available");
        }
    }

    // ============================================================
    // TOKEN TYPE — single-parse helpers
    // ============================================================

    /**
     * Returns the token's "type" claim.
     * Defaults to "access" for backward compatibility with tokens issued before
     * the explicit type claim was added.
     * <p>
     * Use the Claims-accepting overload inside the same call-site where claims
     * have already been parsed to avoid a second token parse.
     */
    public String getTokenType(Claims claims) {
        String type = claims.get("type", String.class);
        return type != null ? type : "access";
    }

    /** Convenience overload — parses the token to extract its type. */
    public String getTokenType(String token) {
        return getTokenType(getClaims(token));
    }

    /**
     * Returns {@code true} if the token is a user access token.
     * Accepts both an explicit {@code type=access} claim and the absence of a
     * type claim (legacy tokens issued before the claim was introduced).
     */
    public boolean isAccessToken(String token) {
        try {
            return "access".equals(getTokenType(getClaims(token)));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} only when {@code type=refresh}.
     */
    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(getTokenType(getClaims(token)));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} only when {@code type=service}.
     */
    public boolean isServiceToken(String token) {
        try {
            return "service".equals(getTokenType(getClaims(token)));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates a refresh token: signature/expiry check AND type check.
     * A valid-but-wrong-type token (e.g. an access token) returns {@code false}.
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token) && isRefreshToken(token);
    }

    /**
     * Validates a service token, optionally checking the {@code target} claim.
     *
     * @param token          the JWT to validate
     * @param expectedTarget the service name that must match the {@code target}
     *                       claim, or {@code null} to skip target verification
     */
    public boolean validateServiceToken(String token, String expectedTarget) {
        if (!validateToken(token) || !isServiceToken(token)) {
            return false;
        }
        if (expectedTarget == null) {
            return true;
        }
        try {
            String target = getClaims(token).get("target", String.class);
            return expectedTarget.equals(target);
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // SERVICE TOKEN CLAIM EXTRACTION
    // ============================================================

    /** The {@code source} claim — name of the calling service. */
    public Optional<String> getServiceName(String token) {
        return Optional.ofNullable(getClaims(token).get("source", String.class));
    }

    /** The {@code target} claim — name of the intended downstream service. */
    public Optional<String> getTargetService(String token) {
        return Optional.ofNullable(getClaims(token).get("target", String.class));
    }

    /**
     * The {@code scopes} claim — service operation scopes.
     * Returns an empty set when the claim is absent (gateway-generated tokens
     * do not include scopes by default).
     */
    public Set<String> getServiceScopes(String token) {
        try {
            Object scopesObj = getClaims(token).get("scopes");
            if (scopesObj instanceof List<?> list) {
                return new HashSet<>((List<String>) list);
            }
            if (scopesObj instanceof String s) {
                return Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.debug("Could not extract service scopes: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    // ============================================================
    // SINGLE-PARSE HELPERS (avoid re-parsing inside the same request)
    // ============================================================

    /**
     * Extracts roles from already-parsed {@link Claims}.
     * Call this instead of {@link #getRoles(String)} when you have already
     * obtained the Claims object in the same request to avoid a second parse.
     */
    public Set<String> extractRolesFromClaims(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> list) {
            return new HashSet<>((List<String>) list);
        }
        if (rolesObj instanceof String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    // ============================================================
    // METADATA
    // ============================================================

    public Optional<Date> getExpirationDate(String token) {
        return Optional.ofNullable(getClaims(token).getExpiration());
    }

    public Optional<Date> getIssuedAt(String token) {
        return Optional.ofNullable(getClaims(token).getIssuedAt());
    }

}