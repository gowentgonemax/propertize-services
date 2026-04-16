package com.propertize.platform.gateway.controller;

import com.propertize.platform.gateway.security.EnhancedJwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Token Controller for API Gateway
 *
 * Handles token validation requests at the gateway level.
 *
 * Flow:
 * 1. Client sends token to gateway for validation
 * 2. Gateway validates token using EnhancedJwtTokenProvider
 * 3. Gateway returns validation status and token details
 *
 * This provides:
 * - Centralized token validation
 * - Reduced latency (no downstream service call needed)
 * - Gateway-level token introspection
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class TokenController {

    private static final String BEARER_PREFIX = "Bearer ";
    private final EnhancedJwtTokenProvider jwtTokenProvider;

    /**
     * Validate a token and return its status.
     * <p>
     * The token is parsed ONCE via {@link EnhancedJwtTokenProvider#getClaims(String)};
     * all claim fields are extracted from the resulting {@link Claims} object without
     * re-parsing. This reduces cryptographic work from O(n) parses to exactly 1.
     */
    @PostMapping("/token/validate")
    public Mono<ResponseEntity<TokenValidationResponse>> validateToken(
            @RequestBody TokenValidationRequest request) {

        String token = request.getToken();

        // Bug fix: isBlank() catches whitespace-only strings that isEmpty() misses
        if (token == null || token.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(
                TokenValidationResponse.invalid("Token is required")
            ));
        }

        // Bug fix: strip "Bearer " prefix so clients that pass the full header value
        // do not get a silent JwtException from the parser
        token = stripBearer(token);

        try {
            // Bug fix: parse ONCE — getClaims() throws JwtException if invalid/expired,
            // eliminating the 7-parse fan-out that existed before
            Claims claims = jwtTokenProvider.getClaims(token);

            String username  = Optional.ofNullable(claims.getSubject()).orElse("unknown");
            Set<String> roles = jwtTokenProvider.extractRolesFromClaims(claims);
            String orgId = Optional.ofNullable(claims.get("organizationId", String.class))
                               .orElse(claims.get("org", String.class));
            // Bug fix: determine type from already-parsed Claims (no extra parse)
            String type = jwtTokenProvider.getTokenType(claims);
            long expiresAt = Optional.ofNullable(claims.getExpiration())
                                 .map(d -> d.toInstant().toEpochMilli())
                                 .orElse(0L);

            log.info("Token introspection succeeded: user='{}' type='{}' roles={}", username, type, roles);

            return Mono.just(ResponseEntity.ok(
                TokenValidationResponse.builder()
                    .valid(true)
                    .username(username)
                    .roles(roles)
                    .organizationId(orgId)
                    .tokenType(type)
                    .expiresAt(expiresAt)
                    .message("Token is valid")
                    .build()
            ));
        } catch (JwtException e) {
            // Token signature is bad or the token has expired — not a server error
            log.debug("Token introspection failed (invalid/expired): {}", e.getMessage());
            return Mono.just(ResponseEntity.ok(
                TokenValidationResponse.invalid("Token is invalid or expired")
            ));
        } catch (Exception e) {
            // Bug fix: server-side errors must not be swallowed as 200 OK
            log.error("Token introspection error: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                TokenValidationResponse.invalid("Token validation service error")
            ));
        }
    }

    /**
     * Validate a refresh token.
     * <p>
     * Distinguishes three failure modes:
     * <ul>
     *   <li>400 Bad Request — null/blank input</li>
     *   <li>400 Bad Request — token is valid but is NOT a refresh token
     *       (e.g. a caller accidentally sending an access token)</li>
     *   <li>200 OK with {@code valid=false} — expired or invalid signature</li>
     * </ul>
     */
    @PostMapping("/token/refresh/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validateRefreshToken(
            @RequestBody RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        // Bug fix: isBlank() catches whitespace-only input
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "error", "Refresh token is required"
            )));
        }

        // Bug fix: strip Bearer prefix for consistency with /token/validate
        refreshToken = stripBearer(refreshToken);

        try {
            // Bug fix: parse ONCE — avoids double parse from validateRefreshToken + getUsername
            Claims claims = jwtTokenProvider.getClaims(refreshToken);

            // Bug fix: explicitly distinguish wrong token type from an invalid token
            // so callers can surface a meaningful message ("you sent an access token")
            if (!"refresh".equals(jwtTokenProvider.getTokenType(claims))) {
                log.warn("Refresh token validation rejected: token has type='{}'",
                        jwtTokenProvider.getTokenType(claims));
                return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "Provided token is not a refresh token"
                )));
            }

            String username = Optional.ofNullable(claims.getSubject()).orElse("unknown");

            return Mono.just(ResponseEntity.ok(Map.of(
                "valid", true,
                "username", username,
                "message", "Refresh token is valid. Proceed to auth service for new access token."
            )));
        } catch (JwtException e) {
            // Expired or invalid signature — expected, not a server error
            log.debug("Refresh token validation failed (invalid/expired): {}", e.getMessage());
            return Mono.just(ResponseEntity.ok(Map.of(
                "valid", false,
                "error", "Invalid or expired refresh token"
            )));
        } catch (Exception e) {
            // Bug fix: server-side failures must not return 200 OK
            log.error("Refresh token validation error: {}", e.getMessage());
            Map<String, Object> body = new HashMap<>();
            body.put("valid", false);
            body.put("error", "Refresh token validation service error");
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body));
        }
    }

    /**
     * Gateway health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "api-gateway",
            "timestamp", Instant.now().toString(),
            "features", Map.of(
                "jwtValidation", true,
                "rateLimiting", true,
                "auditLogging", true,
                "circuitBreaker", true
            )
        )));
    }

    /**
     * Get gateway version and capabilities
     */
    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "name", "Propertize API Gateway",
            "version", "1.0.0",
            "capabilities", Map.of(
                "authentication", "JWT (RS256/HS256)",
                "authorization", "RBAC",
                "rateLimiting", "Token Bucket",
                "auditLogging", "Structured JSON",
                "loadBalancing", "Round Robin via Eureka",
                "circuitBreaker", "Resilience4j"
            ),
            "services", Map.of(
                "propertize", "lb://propertize-service",
                "employecraft", "lb://employecraft-service",
                "wagecraft", "lb://wagecraft-service"
            )
        )));
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    /**
     * Strips the "Bearer " prefix from a token string if present.
     * Allows callers that pass the full Authorization header value to work
     * correctly instead of receiving a silent JwtException.
     */
    private static String stripBearer(String token) {
        if (token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length()).trim();
        }
        return token;
    }

    // ============================================
    // REQUEST/RESPONSE MODELS
    // ============================================

    @Data
    public static class TokenValidationRequest {
        private String token;
    }

    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    // Bug fix: @lombok.Builder (fully-qualified, missing import) → @Builder with proper import
    @Data
    @Builder
    public static class TokenValidationResponse {
        private boolean valid;
        private String username;
        private Set<String> roles;
        private String organizationId;
        private String tokenType;
        private long expiresAt;
        private String message;

        public static TokenValidationResponse invalid(String message) {
            return TokenValidationResponse.builder()
                .valid(false)
                .message(message)
                .build();
        }
    }
}
