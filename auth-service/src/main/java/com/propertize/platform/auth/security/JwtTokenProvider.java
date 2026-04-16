package com.propertize.platform.auth.security;

import com.propertize.platform.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final RsaKeyProvider rsaKeyProvider;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(15, ChronoUnit.MINUTES);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("organizationId", user.getOrganizationId());
        claims.put("orgType", user.getOrganizationType() != null ? user.getOrganizationType() : "");
        claims.put("roles", user.getRoles());
        claims.put("tokenType", "access");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(rsaKeyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Generate an access token WITHOUT embedding the permissions list.
     * Permissions are stored separately in Redis (keyed by JTI) via
     * {@link com.propertize.platform.auth.service.PermissionCacheService}.
     *
     * <p>
     * This keeps the JWT small (no 50+ permission strings) and resolves
     * HTTP 431 "Request Header Fields Too Large" errors caused by large tokens.
     * </p>
     *
     * @return the compact JWT string (permissions NOT included in payload)
     */
    public String generateAccessTokenWithPermissions(String username, Set<String> roles,
            String organizationId, String organizationCode,
            Set<String> permissions, String organizationType,
            String firstName, String lastName) {
        Instant now = Instant.now();
        Instant expiration = now.plus(15, ChronoUnit.MINUTES);

        // Derive the primary role as the first alphabetically-sorted entry
        // (deterministic)
        String primaryRole = roles != null && !roles.isEmpty()
                ? roles.stream().sorted().findFirst().orElse("")
                : "";

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("role", primaryRole);
        claims.put("organizationId", organizationId);
        claims.put("organizationCode", organizationCode);
        claims.put("orgType", organizationType != null ? organizationType : "");
        claims.put("tokenType", "access");
        if (firstName != null && !firstName.isBlank())
            claims.put("firstName", firstName);
        if (lastName != null && !lastName.isBlank())
            claims.put("lastName", lastName);
        // NOTE: "permissions" intentionally NOT included in JWT payload.
        // Permissions are cached in Redis under key perms:jti:{jti} by
        // PermissionCacheService.
        // The API Gateway fetches them from Redis using the jti claim on every request.

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(rsaKeyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(7, ChronoUnit.DAYS);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .setId(UUID.randomUUID().toString())
                .claim("tokenType", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(rsaKeyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(rsaKeyProvider.getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Extract the JWT ID (jti claim) from a token.
     * Used by PermissionCacheService to look up cached permissions in Redis.
     *
     * @param token the compact JWT string
     * @return the jti claim value, or null if absent or parse fails
     */
    public String getJtiFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getId();
        } catch (Exception e) {
            log.debug("Could not extract jti from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
