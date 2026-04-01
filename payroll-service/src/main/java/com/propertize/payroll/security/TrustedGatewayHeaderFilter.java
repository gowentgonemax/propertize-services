package com.propertize.payroll.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Trusted Gateway Header Filter for Wagecraft
 *
 * This filter authenticates users based on headers propagated by the API
 * Gateway.
 * It replaces the local JwtRequestFilter for gateway-authenticated requests.
 *
 * When X-Gateway-Source header is "api-gateway", this filter:
 * 1. Extracts user info from gateway headers
 * 2. Sets up Spring Security authentication
 * 3. Does NOT require local user database lookup
 *
 * For direct API access (e.g., internal service calls), the legacy
 * JwtRequestFilter
 * can still be used as fallback.
 */
@Slf4j
@Component
public class TrustedGatewayHeaderFilter extends OncePerRequestFilter {

    public static final String X_GATEWAY_SOURCE = "X-Gateway-Source";
    public static final String X_USER_ID = "X-User-Id";
    public static final String X_USERNAME = "X-Username";
    public static final String X_ORGANIZATION_ID = "X-Organization-Id";
    public static final String X_ORGANIZATION_CODE = "X-Organization-Code";
    public static final String X_ROLES = "X-Roles";
    public static final String X_PRIMARY_ROLE = "X-Primary-Role";
    public static final String X_PERMISSIONS = "X-Permissions";
    public static final String X_CORRELATION_ID = "X-Correlation-Id";
    public static final String X_ORG_TYPE = "X-Org-Type";

    @Value("${security.gateway.expected-value:api-gateway}")
    private String expectedGatewaySource;

    /**
     * Shared secret for HMAC gateway header verification. Empty = verification
     * skipped.
     */
    @Value("${security.gateway.hmac-secret:}")
    private String hmacSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String gatewaySource = request.getHeader(X_GATEWAY_SOURCE);

        // Only process if request comes from API Gateway
        if (gatewaySource == null || !gatewaySource.equals(expectedGatewaySource)) {
            log.debug("Request not from gateway: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Verify HMAC signature when configured to prevent header-spoofing attacks
        if (!hmacSecret.isBlank()) {
            String userId = request.getHeader(X_USER_ID);
            String rolesHeader = request.getHeader(X_ROLES);
            String signature = request.getHeader("X-Gateway-Signature");
            if (!isValidGatewaySignature(signature, userId, rolesHeader)) {
                log.warn("Invalid or missing gateway HMAC signature for {} {}",
                        request.getMethod(), request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getOutputStream()
                        .print("{\"error\":\"Unauthorized\",\"message\":\"Invalid gateway signature\"}");
                return;
            }
        }

        // Skip if already authenticated (e.g., by legacy filter)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user information from gateway headers
        String userId = request.getHeader(X_USER_ID);
        String username = request.getHeader(X_USERNAME);
        String organizationId = request.getHeader(X_ORGANIZATION_ID);
        String organizationCode = request.getHeader(X_ORGANIZATION_CODE);
        String rolesHeader = request.getHeader(X_ROLES);
        String primaryRole = request.getHeader(X_PRIMARY_ROLE);
        String correlationId = request.getHeader(X_CORRELATION_ID);
        String permissionsHeader = request.getHeader(X_PERMISSIONS);
        String orgType = request.getHeader(X_ORG_TYPE);

        if (userId == null || userId.isEmpty()) {
            log.debug("No user ID in gateway headers for: {} {}",
                    request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Parse roles and permissions
        Set<String> roles = parseRoles(rolesHeader);
        Set<String> permissions = parseRoles(permissionsHeader);

        log.debug("Gateway auth - user: {}, org: {}, roles: {}, permissions: {} [correlationId={}]",
                username, organizationCode, roles, permissions.size(), correlationId);

        // Create authorities from roles AND permissions
        Collection<SimpleGrantedAuthority> authorities = createAuthorities(roles, permissions);

        // Create authentication principal
        GatewayAuthenticatedUser principal = new GatewayAuthenticatedUser(
                userId, username, organizationId, organizationCode, orgType, roles, primaryRole);

        // Create authentication token
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null,
                authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("✅ Gateway authenticated: {} [org={}, roles={}, correlationId={}]",
                username, organizationCode, roles.size(), correlationId);

        filterChain.doFilter(request, response);
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private Collection<SimpleGrantedAuthority> createAuthorities(Set<String> roles, Set<String> permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> {
            // Add both ROLE_ prefixed and non-prefixed authorities
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            authorities.add(new SimpleGrantedAuthority(role));
        });
        // Add permission-based authorities (e.g., "compensation:create")
        permissions.forEach(permission -> {
            authorities.add(new SimpleGrantedAuthority(permission));
        });
        return authorities;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") ||
                path.startsWith("/actuator/info") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/auth/");
    }

    /**
     * Verifies the X-Gateway-Signature header using HMAC-SHA256.
     * Accepts both the current minute and the previous minute to tolerate
     * minor clock skew between gateway and service.
     */
    private boolean isValidGatewaySignature(String signature, String userId, String roles) {
        if (signature == null || userId == null || roles == null) {
            return false;
        }
        long epochMinutes = Instant.now().getEpochSecond() / 60;
        return signature.equals(computeHmac(userId + ":" + roles + ":" + epochMinutes))
                || signature.equals(computeHmac(userId + ":" + roles + ":" + (epochMinutes - 1)));
    }

    private String computeHmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.error("HMAC computation failed", e);
            return "";
        }
    }

    /**
     * Authenticated user principal for gateway-authenticated requests
     */
    public static class GatewayAuthenticatedUser {
        private final String userId;
        private final String username;
        private final String organizationId;
        private final String organizationCode;
        private final String orgType;
        private final Set<String> roles;
        private final String primaryRole;

        public GatewayAuthenticatedUser(String userId, String username, String organizationId,
                String organizationCode, String orgType, Set<String> roles, String primaryRole) {
            this.userId = userId;
            this.username = username;
            this.organizationId = organizationId;
            this.organizationCode = organizationCode;
            this.orgType = orgType != null ? orgType : "";
            this.roles = roles != null ? roles : Collections.emptySet();
            this.primaryRole = primaryRole;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public String getOrganizationCode() {
            return organizationCode;
        }

        public String getOrgType() {
            return orgType;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public String getPrimaryRole() {
            return primaryRole;
        }

        public boolean hasRole(String role) {
            return roles.contains(role);
        }

        public boolean hasAnyRole(String... rolesToCheck) {
            for (String role : rolesToCheck) {
                if (roles.contains(role))
                    return true;
            }
            return false;
        }

        public boolean isPlatformAdmin() {
            return hasRole("PLATFORM_OVERSIGHT") || hasRole("PLATFORM_ADMIN");
        }

        public boolean isOrganizationOwner() {
            return hasRole("ORGANIZATION_OWNER");
        }

        @Override
        public String toString() {
            return "GatewayAuthenticatedUser{userId='" + userId + "', username='" + username +
                    "', org='" + organizationCode + "', roles=" + roles + "}";
        }
    }
}
