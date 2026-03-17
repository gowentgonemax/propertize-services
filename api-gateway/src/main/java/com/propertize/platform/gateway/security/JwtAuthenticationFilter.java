package com.propertize.platform.gateway.security;

import com.propertize.platform.gateway.service.TokenBlacklistService;
import com.propertize.platform.gateway.metrics.AuthenticationMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Global JWT Authentication Filter for API Gateway
 * 
 * Production-Ready Authentication Design v2.0
 * 
 * This filter implements the production-ready authentication flow:
 * 1. Extract JWT from Authorization header (~0.1ms)
 * 2. Validate JWT signature using cached public key (RS256) (~1ms)
 * 3. Validate claims (exp, iss, aud) (~0.5ms)
 * 4. Check token blacklist via Redis (~2ms cache hit)
 * 5. Extract claims and add to headers (~0.2ms)
 * 6. Forward request to downstream services (~0.1ms)
 * 
 * Total Authentication Overhead: ~3-4ms (target: <5ms)
 *
 * Features:
 * - RS256 (asymmetric) key verification - MANDATORY for production
 * - Local JWT validation - NO Auth Service calls for validation
 * - Multi-layer caching - Memory → Redis → Auth Service
 * - Token blacklist with TTL - Automatic cleanup
 * - Defense in depth - Gateway + Service-level validation
 *
 * @author Platform Security Team
 * @version 2.0 - Production Ready
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired(required = false)
    private AuthenticationMetrics metrics;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // Headers to propagate to downstream services (as per Production-Ready Design)
    public static final String X_USER_ID = "X-User-Id";
    public static final String X_USERNAME = "X-Username";
    public static final String X_EMAIL = "X-Email";
    public static final String X_ORGANIZATION_ID = "X-Organization-Id";
    public static final String X_ORGANIZATION_CODE = "X-Organization-Code";
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_ROLES = "X-Roles";
    public static final String X_PRIMARY_ROLE = "X-Primary-Role";
    public static final String X_GATEWAY_SOURCE = "X-Gateway-Source";
    public static final String X_CORRELATION_ID = "X-Correlation-Id";
    public static final String X_TOKEN_TYPE = "X-Token-Type";
    public static final String X_SERVICE_NAME = "X-Service-Name";
    public static final String X_SESSION_ID = "X-Session-Id";
    public static final String X_TOKEN_JTI = "X-Token-Jti";

    @Value("${security.gateway.source-value:api-gateway}")
    private String gatewaySourceValue;

    // Public endpoints that don't require authentication
    // Per Production-Ready Design: /api/v1/auth/validate should be admin-only
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/public-key",
            "/api/v1/auth/.well-known/jwks.json",
            "/.well-known/jwks.json",
            "/api/v1/public/**",
            "/api/v1/properties/public/**",
            "/api/v1/organizations/onboarding/**",
            "/api/v1/rental-applications/submit",
            "/api/v1/rental-applications/track/**",
            "/api/v1/gateway/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/graphql",
            "/graphiql",
            "/fallback/**",
            "/public/**");

    private final EnhancedJwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService blacklistService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(
            EnhancedJwtTokenProvider jwtTokenProvider,
            TokenBlacklistService blacklistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.blacklistService = blacklistService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        // Generate correlation ID for request tracking
        String correlationId = UUID.randomUUID().toString();

        log.debug("Processing request: {} {} [correlationId={}]", method, path, correlationId);

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            log.debug("Skipping authentication for public path: {} {}", method, path);
            return chain.filter(addCorrelationHeader(exchange, correlationId));
        }

        // Extract JWT token
        String token = extractToken(request);

        if (token == null) {
            log.warn("Missing JWT token for protected endpoint: {} {} [correlationId={}]",
                    method, path, correlationId);
            if (metrics != null) {
                metrics.recordAuthFailure("missing_token");
            }
            return onUnauthorized(exchange, "Missing authentication token");
        }

        // Validate token
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid JWT token for: {} {} [correlationId={}]", method, path, correlationId);
            if (metrics != null) {
                metrics.recordAuthFailure("invalid_token");
            }
            return onUnauthorized(exchange, "Invalid or expired token");
        }

        // Check if token is blacklisted using JTI (JWT ID) - per Production-Ready
        // Design
        Optional<String> jtiOpt = jwtTokenProvider.getTokenId(token);
        if (jtiOpt.isPresent() && blacklistService.isBlacklistedByJti(jtiOpt.get())) {
            log.warn("Blacklisted token (jti={}) used for: {} {} [correlationId={}]",
                    jtiOpt.get(), method, path, correlationId);
            if (metrics != null) {
                metrics.recordAuthFailure("blacklisted_token");
            }
            return onUnauthorized(exchange, "Token has been revoked");
        }

        // Fallback: Check by token hash for backward compatibility
        if (blacklistService.isBlacklisted(token)) {
            log.warn("Blacklisted token used for: {} {} [correlationId={}]", method, path, correlationId);
            if (metrics != null) {
                metrics.recordAuthFailure("blacklisted_token");
            }
            return onUnauthorized(exchange, "Token has been revoked");
        }

        // Reject refresh tokens used for API access
        if (jwtTokenProvider.isRefreshToken(token)) {
            log.warn("Refresh token used for API access: {} {} [correlationId={}]", method, path, correlationId);
            if (metrics != null) {
                metrics.recordAuthFailure("refresh_token_misuse");
            }
            return onUnauthorized(exchange, "Refresh tokens cannot be used for API access");
        }

        // Check if this is a service token
        if (jwtTokenProvider.isServiceToken(token)) {
            return handleServiceToken(token, exchange, chain, correlationId, method, path);
        }

        // Token is valid user access token - extract user information
        // Per Production-Ready Design: Extract all claims for downstream services
        String username = jwtTokenProvider.getUsername(token).orElse("unknown");
        String email = jwtTokenProvider.getEmail(token).orElse("");
        String organizationId = jwtTokenProvider.getOrganizationId(token).orElse("");
        String organizationCode = jwtTokenProvider.getOrganizationCode(token).orElse("");
        String tenantId = jwtTokenProvider.getTenantId(token).orElse("");
        String sessionId = jwtTokenProvider.getSessionId(token).orElse("");
        String jti = jwtTokenProvider.getTokenId(token).orElse("");
        Set<String> roles = jwtTokenProvider.getRoles(token);
        String primaryRole = jwtTokenProvider.getPrimaryRole(token).orElse("");
        String tokenType = jwtTokenProvider.isAccessToken(token) ? "access" : "unknown";

        log.info("✅ Authenticated user: {} with roles: {} for {} {} [correlationId={}]",
                username, roles, method, path, correlationId);

        // Record successful authentication
        if (metrics != null) {
            metrics.recordAuthSuccess();
        }

        // Build modified request with user context headers
        // Per Production-Ready Design: Gateway Headers (Added by JWT Filter)
        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .header(X_USER_ID, username)
                .header(X_USERNAME, username)
                .header(X_ORGANIZATION_ID, organizationId)
                .header(X_ORGANIZATION_CODE, organizationCode)
                .header(X_ROLES, String.join(",", roles))
                .header(X_PRIMARY_ROLE, primaryRole)
                .header(X_GATEWAY_SOURCE, gatewaySourceValue)
                .header(X_CORRELATION_ID, correlationId)
                .header(X_TOKEN_TYPE, tokenType);

        // Add optional headers if present
        if (!email.isEmpty()) {
            requestBuilder.header(X_EMAIL, email);
        }
        if (!tenantId.isEmpty()) {
            requestBuilder.header(X_TENANT_ID, tenantId);
        }
        if (!sessionId.isEmpty()) {
            requestBuilder.header(X_SESSION_ID, sessionId);
        }
        if (!jti.isEmpty()) {
            requestBuilder.header(X_TOKEN_JTI, jti);
        }

        ServerHttpRequest modifiedRequest = requestBuilder.build();

        // Continue with modified request
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private ServerWebExchange addCorrelationHeader(ServerWebExchange exchange, String correlationId) {
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header(X_CORRELATION_ID, correlationId)
                .header(X_GATEWAY_SOURCE, gatewaySourceValue)
                .build();
        return exchange.mutate().request(modifiedRequest).build();
    }

    /**
     * Handle service-to-service authentication
     * Service tokens have different claims than user tokens
     */
    private Mono<Void> handleServiceToken(String token, ServerWebExchange exchange,
            GatewayFilterChain chain, String correlationId,
            String method, String path) {
        // Extract service information
        String sourceService = jwtTokenProvider.getServiceName(token).orElse("unknown");
        String targetService = jwtTokenProvider.getTargetService(token).orElse("");
        Set<String> scopes = jwtTokenProvider.getServiceScopes(token);

        log.info("✅ Service-to-service request: {} -> target={} scopes={} for {} {} [correlationId={}]",
                sourceService, targetService, scopes, method, path, correlationId);

        // Record service authentication
        if (metrics != null) {
            metrics.recordAuthSuccess();
        }

        // Build modified request with service context headers
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header(X_SERVICE_NAME, sourceService)
                .header(X_TOKEN_TYPE, "service")
                .header(X_GATEWAY_SOURCE, gatewaySourceValue)
                .header(X_CORRELATION_ID, correlationId)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = String.format(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                message,
                exchange.getRequest().getPath().value());

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // Run early in the filter chain, but after CORS
        return -100;
    }
}
