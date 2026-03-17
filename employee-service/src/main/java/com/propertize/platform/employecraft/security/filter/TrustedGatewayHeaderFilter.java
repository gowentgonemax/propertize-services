package com.propertize.platform.employecraft.security.filter;

import com.propertize.platform.employecraft.context.OrganizationContext;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Trusted Gateway Header Filter for Employecraft
 *
 * Production-Ready Authentication Design v2.0
 * 
 * Authenticates users based on headers propagated by the API Gateway.
 * Trusts headers only when X-Gateway-Source matches expected value.
 * 
 * Headers processed (per Production-Ready Design):
 * - X-Gateway-Source: Must match expected value to trust other headers
 * - X-User-Id: User's unique identifier
 * - X-Username: User's username
 * - X-Email: User's email address
 * - X-Organization-Id: User's organization ID
 * - X-Organization-Code: User's organization code
 * - X-Tenant-Id: Tenant ID for multi-tenancy
 * - X-Roles: Comma-separated list of roles
 * - X-Primary-Role: User's primary role
 * - X-Correlation-Id: Request correlation ID for tracing
 * - X-Session-Id: Session ID for session tracking
 * - X-Token-Jti: JWT ID for token tracking
 *
 * @author Platform Security Team
 * @version 2.0 - Production Ready
 */
@Slf4j
@Component
public class TrustedGatewayHeaderFilter extends OncePerRequestFilter {

    public static final String X_GATEWAY_SOURCE = "X-Gateway-Source";
    public static final String X_USER_ID = "X-User-Id";
    public static final String X_USERNAME = "X-Username";
    public static final String X_EMAIL = "X-Email";
    public static final String X_ORGANIZATION_ID = "X-Organization-Id";
    public static final String X_ORGANIZATION_CODE = "X-Organization-Code";
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_ROLES = "X-Roles";
    public static final String X_PRIMARY_ROLE = "X-Primary-Role";
    public static final String X_CORRELATION_ID = "X-Correlation-Id";
    public static final String X_SESSION_ID = "X-Session-Id";
    public static final String X_TOKEN_JTI = "X-Token-Jti";
    public static final String X_TOKEN_TYPE = "X-Token-Type";

    @Value("${security.gateway.expected-value:api-gateway}")
    private String expectedGatewaySource;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String gatewaySource = request.getHeader(X_GATEWAY_SOURCE);

        // Only trust headers from API Gateway
        if (gatewaySource == null || !gatewaySource.equals(expectedGatewaySource)) {
            log.debug("Request not from gateway, skipping header auth: {} {}",
                    request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user information from headers (per Production-Ready Design)
        String userId = request.getHeader(X_USER_ID);
        String username = request.getHeader(X_USERNAME);
        String email = request.getHeader(X_EMAIL);
        String organizationId = request.getHeader(X_ORGANIZATION_ID);
        String organizationCode = request.getHeader(X_ORGANIZATION_CODE);
        String tenantId = request.getHeader(X_TENANT_ID);
        String rolesHeader = request.getHeader(X_ROLES);
        String primaryRole = request.getHeader(X_PRIMARY_ROLE);
        String correlationId = request.getHeader(X_CORRELATION_ID);
        String sessionId = request.getHeader(X_SESSION_ID);
        String tokenJti = request.getHeader(X_TOKEN_JTI);

        if (userId == null || userId.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Parse roles
        Set<String> roles = parseRoles(rolesHeader);

        log.debug("Authenticating from gateway: {} with roles: {} [correlationId={}]",
                username, roles, correlationId);

        // Create authorities
        Collection<SimpleGrantedAuthority> authorities = createAuthorities(roles);

        // Create authentication with enhanced principal
        GatewayAuthenticatedUser principal = new GatewayAuthenticatedUser(
                userId, username, email, organizationId, organizationCode,
                tenantId, roles, primaryRole, sessionId, tokenJti);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null,
                authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Set organization context
        if (organizationId != null && !organizationId.isEmpty()) {
            try {
                OrganizationContext.setOrganizationId(UUID.fromString(organizationId));
            } catch (Exception e) {
                log.warn("Invalid organization ID: {}", organizationId);
            }
        }

        log.info("✅ Gateway auth: {} [org={}, roles={}, correlationId={}]",
                username, organizationCode, roles.size(), correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            OrganizationContext.clear();
        }
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

    private Collection<SimpleGrantedAuthority> createAuthorities(Set<String> roles) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            authorities.add(new SimpleGrantedAuthority(role));
        });
        return authorities;
    }

    /**
     * Enhanced authenticated user principal
     * Per Production-Ready Design: Contains all gateway-provided context
     */
    public static class GatewayAuthenticatedUser {
        private final String userId;
        private final String username;
        private final String email;
        private final String organizationId;
        private final String organizationCode;
        private final String tenantId;
        private final Set<String> roles;
        private final String primaryRole;
        private final String sessionId;
        private final String tokenJti;

        public GatewayAuthenticatedUser(String userId, String username, String email,
                String organizationId, String organizationCode,
                String tenantId, Set<String> roles, String primaryRole,
                String sessionId, String tokenJti) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.organizationId = organizationId;
            this.organizationCode = organizationCode;
            this.tenantId = tenantId;
            this.roles = roles;
            this.primaryRole = primaryRole;
            this.sessionId = sessionId;
            this.tokenJti = tokenJti;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public String getOrganizationCode() {
            return organizationCode;
        }

        public String getTenantId() {
            return tenantId;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public String getPrimaryRole() {
            return primaryRole;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getTokenJti() {
            return tokenJti;
        }

        public boolean hasRole(String role) {
            return roles.contains(role);
        }

        @Override
        public String toString() {
            return "GatewayAuthenticatedUser{" +
                    "userId='" + userId + '\'' +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", organizationId='" + organizationId + '\'' +
                    ", roles=" + roles +
                    '}';
        }
    }
}
