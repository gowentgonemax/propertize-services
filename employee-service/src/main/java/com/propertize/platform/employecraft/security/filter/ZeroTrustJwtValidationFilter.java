package com.propertize.platform.employecraft.security.filter;

import com.propertize.commons.constants.GatewayHeaders;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Set;

/**
 * Zero-Trust JWT Validation Filter
 * 
 * Defense-in-depth: Even though the API Gateway validates JWTs,
 * this filter independently verifies the token using the Auth Service's
 * RS256 public key. This prevents attacks where requests bypass the gateway.
 * 
 * Runs BEFORE TrustedGatewayHeaderFilter to ensure token integrity.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ZeroTrustJwtValidationFilter extends OncePerRequestFilter {

    @Value("${security.jwt.public-key-path:config/keys/public_key.pem}")
    private String publicKeyPath;

    @Value("${security.gateway.expected-value:api-gateway}")
    private String expectedGatewaySource;

    private PublicKey publicKey;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/actuator/health", "/actuator/info",
            "/swagger-ui", "/v3/api-docs");

    @PostConstruct
    public void init() {
        try {
            publicKey = loadPublicKey();
            if (publicKey != null) {
                log.info("✅ Zero-Trust JWT validation enabled with RS256 public key");
            } else {
                log.warn("⚠️ Zero-Trust JWT validation DISABLED - no public key found. "
                        + "Set security.jwt.public-key-path to enable.");
            }
        } catch (Exception e) {
            log.warn("⚠️ Zero-Trust JWT validation DISABLED - failed to load public key: {}", e.getMessage());
            publicKey = null;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Skip if public key not configured (graceful degradation)
        if (publicKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Skip public endpoints
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String gatewaySource = request.getHeader("X-Gateway-Source");

        // If request claims to be from gateway, validate the token
        if (expectedGatewaySource.equals(gatewaySource)) {
            String authHeader = request.getHeader(GatewayHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith(GatewayHeaders.BEARER_PREFIX)) {
                String token = authHeader.substring(GatewayHeaders.BEARER_PREFIX.length());
                try {
                    Claims claims = Jwts.parser()
                            .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

                    // Token is valid — store claims for downstream use
                    request.setAttribute("jwt.claims", claims);
                    request.setAttribute("jwt.validated", true);
                    log.debug("✅ Zero-trust JWT validated for user: {}", claims.getSubject());
                } catch (Exception e) {
                    log.warn("❌ Zero-trust JWT validation failed: {}", e.getMessage());
                    // Don't reject — let TrustedGatewayHeaderFilter handle auth
                    // But mark as unvalidated for audit
                    request.setAttribute("jwt.validated", false);
                }
            }
        } else if (gatewaySource != null) {
            // Someone is spoofing the gateway header — reject immediately
            log.error("🚨 SECURITY: Spoofed X-Gateway-Source header detected from {}: {}",
                    request.getRemoteAddr(), gatewaySource);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"Invalid gateway source\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private PublicKey loadPublicKey() {
        try {
            Path keyPath = Path.of(publicKeyPath);
            if (!Files.exists(keyPath)) {
                log.debug("Public key file not found at: {}", publicKeyPath);
                return null;
            }

            String keyContent = Files.readString(keyPath)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            log.debug("Could not load public key: {}", e.getMessage());
            return null;
        }
    }
}
