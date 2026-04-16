package com.propertize.platform.gateway.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.Duration;
import java.util.*;

/**
 * RBAC Authorization Filter for API Gateway
 *
 * This filter checks if the authenticated user has permission to access
 * the requested endpoint based on their roles and the RBAC configuration.
 *
 * It runs AFTER the JwtAuthenticationFilter and uses the role information
 * from the X-Roles header to make authorization decisions.
 *
 * Key Features:
 * - Endpoint-to-permission mapping
 * - Role-based permission checking
 * - Permission caching for performance
 * - Detailed logging for debugging
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RbacAuthorizationFilter implements GlobalFilter, Ordered {

    private final RbacConfig rbacConfig;
    private final MeterRegistry meterRegistry;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${rbac.cache.ttl-minutes:60}")
    private int cacheTtlMinutes;

    private Counter deniedRequestsCounter;

    // TTL-bounded Caffeine cache — permissions expire to pick up rbac.yml reloads
    // (or future DB-backed permission changes) without requiring a service restart.
    // Max 100 roles covers all built-in + custom roles with safe headroom.
    private Cache<String, Set<String>> permissionCache;

    @PostConstruct
    void initCache() {
        permissionCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(cacheTtlMinutes))
                .recordStats() // exposes hit/miss metrics via Micrometer
                .build();
        deniedRequestsCounter = Counter.builder("rbac.requests.denied")
                .description("Number of requests denied by RBAC filter")
                .register(meterRegistry);
        log.info("RBAC permission cache initialised: ttl={}min, maxSize=100", cacheTtlMinutes);
    }

    // Endpoint to permission mapping (pattern -> method -> permission)
    private static final Map<String, Map<String, String>> ENDPOINT_PERMISSIONS = new LinkedHashMap<>();

    static {
        // ========================================
        // PROPERTY ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/properties", "GET", "property:list");
        addEndpoint("/api/v1/properties", "POST", "property:create");
        addEndpoint("/api/v1/properties/**", "GET", "property:read");
        addEndpoint("/api/v1/properties/**", "PUT", "property:update");
        addEndpoint("/api/v1/properties/**", "PATCH", "property:update");
        addEndpoint("/api/v1/properties/**", "DELETE", "property:delete");

        // ========================================
        // TENANT ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/tenants", "GET", "tenant:list");
        addEndpoint("/api/v1/tenants", "POST", "tenant:create");
        addEndpoint("/api/v1/tenants/**", "GET", "tenant:read");
        addEndpoint("/api/v1/tenants/**", "PUT", "tenant:update");
        addEndpoint("/api/v1/tenants/**", "PATCH", "tenant:update");
        addEndpoint("/api/v1/tenants/**", "DELETE", "tenant:delete");

        // ========================================
        // LEASE ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/leases", "GET", "lease:list");
        addEndpoint("/api/v1/leases", "POST", "lease:create");
        addEndpoint("/api/v1/leases/**", "GET", "lease:read");
        addEndpoint("/api/v1/leases/**", "PUT", "lease:update");
        addEndpoint("/api/v1/leases/**", "PATCH", "lease:update");
        addEndpoint("/api/v1/leases/**", "DELETE", "lease:delete");
        addEndpoint("/api/v1/leases/**/approve", "POST", "lease:approve");
        addEndpoint("/api/v1/leases/**/sign", "POST", "lease:sign");

        // ========================================
        // RENTAL APPLICATION ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/rental-applications", "GET", "rental_application:list");
        addEndpoint("/api/v1/rental-applications", "POST", "rental_application:create");
        addEndpoint("/api/v1/rental-applications/**", "GET", "rental_application:read");
        addEndpoint("/api/v1/rental-applications/**", "PUT", "rental_application:update");
        addEndpoint("/api/v1/rental-applications/**/approve", "POST", "rental_application:approve");
        addEndpoint("/api/v1/rental-applications/**/reject", "POST", "rental_application:reject");
        addEndpoint("/api/v1/rental-applications/**/review", "POST", "rental_application:review");

        // ========================================
        // PAYMENT ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/payments", "GET", "payment:list");
        addEndpoint("/api/v1/payments", "POST", "payment:create");
        addEndpoint("/api/v1/payments/**", "GET", "payment:read");
        addEndpoint("/api/v1/payments/**/process", "POST", "payment:process");
        addEndpoint("/api/v1/payments/**/refund", "POST", "payment:refund");

        // ========================================
        // INVOICE ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/invoices", "GET", "invoice:list");
        addEndpoint("/api/v1/invoices", "POST", "invoice:create");
        addEndpoint("/api/v1/invoices/**", "GET", "invoice:read");
        addEndpoint("/api/v1/invoices/**", "PUT", "invoice:update");
        addEndpoint("/api/v1/invoices/**/send", "POST", "invoice:send");

        // ========================================
        // MAINTENANCE ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/maintenance", "GET", "maintenance:list");
        addEndpoint("/api/v1/maintenance", "POST", "maintenance:create");
        addEndpoint("/api/v1/maintenance/**", "GET", "maintenance:read");
        addEndpoint("/api/v1/maintenance/**", "PUT", "maintenance:update");
        addEndpoint("/api/v1/maintenance/**", "PATCH", "maintenance:update");

        // ========================================
        // VENDOR ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/vendors", "GET", "vendor:list");
        addEndpoint("/api/v1/vendors", "POST", "vendor:create");
        addEndpoint("/api/v1/vendors/**", "GET", "vendor:read");
        addEndpoint("/api/v1/vendors/**", "PUT", "vendor:update");
        addEndpoint("/api/v1/vendors/**", "DELETE", "vendor:delete");

        // ========================================
        // USER ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/users", "GET", "user:list");
        addEndpoint("/api/v1/users", "POST", "user:create");
        addEndpoint("/api/v1/users/**", "GET", "user:read");
        addEndpoint("/api/v1/users/**", "PUT", "user:update");
        addEndpoint("/api/v1/users/**", "DELETE", "user:delete");

        // ========================================
        // ORGANIZATION ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/organizations", "GET", "organization:list");
        addEndpoint("/api/v1/organizations/**", "GET", "organization:read");
        addEndpoint("/api/v1/organizations/**", "PUT", "organization:update");
        addEndpoint("/api/v1/organizations/**/configure", "POST", "organization:configure");

        // ========================================
        // ADMIN ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/admin/**", "*", "admin:access");

        // ========================================
        // REPORT ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/reports", "GET", "report:list");
        addEndpoint("/api/v1/reports/**", "GET", "report:read");
        addEndpoint("/api/v1/reports/**/generate", "POST", "report:generate");

        // ========================================
        // NOTIFICATION ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/notifications", "GET", "notification:list");
        addEndpoint("/api/v1/notifications/**", "GET", "notification:read");
        addEndpoint("/api/v1/notifications/**/send", "POST", "notification:send");

        // ========================================
        // DASHBOARD ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/dashboard/**", "GET", "dashboard:read");

        // ========================================
        // EMPLOYEE ENDPOINTS (Employecraft)
        // ========================================
        addEndpoint("/api/v1/employees", "GET", "employee:list");
        addEndpoint("/api/v1/employees", "POST", "employee:create");
        addEndpoint("/api/v1/employees/**", "GET", "employee:read");
        addEndpoint("/api/v1/employees/**", "PUT", "employee:update");
        addEndpoint("/api/v1/employees/**", "DELETE", "employee:delete");

        // ========================================
        // PAYROLL ENDPOINTS (Wagecraft)
        // ========================================
        addEndpoint("/api/v1/payroll", "GET", "payroll:list");
        addEndpoint("/api/v1/payroll/**", "GET", "payroll:read");
        addEndpoint("/api/v1/payroll/**", "POST", "payroll:process");
        addEndpoint("/api/v1/payroll/**", "PUT", "payroll:update");
        addEndpoint("/api/v1/salaries/**", "*", "payroll:manage");
        addEndpoint("/api/v1/deductions/**", "*", "payroll:manage");
        addEndpoint("/api/v1/payslips/**", "GET", "payroll:read");
        addEndpoint("/api/v1/timesheets/**", "GET", "payroll:read");
        addEndpoint("/api/v1/timesheets/**", "POST", "payroll:create");
        addEndpoint("/api/v1/timesheets/**", "PUT", "payroll:update");

        // ========================================
        // STRIPE ENDPOINTS
        // ========================================
        addEndpoint("/api/v1/stripe/**", "*", "payment:manage");
    }

    private static void addEndpoint(String pattern, String method, String permission) {
        ENDPOINT_PERMISSIONS.computeIfAbsent(pattern, k -> new HashMap<>())
                .put(method.toUpperCase(), permission);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        String rolesHeader = request.getHeaders().getFirst(JwtAuthenticationFilter.X_ROLES);
        String permissionsHeader = request.getHeaders().getFirst(JwtAuthenticationFilter.X_PERMISSIONS);
        String correlationId = request.getHeaders().getFirst(JwtAuthenticationFilter.X_CORRELATION_ID);

        // Skip authorization for public endpoints (already handled by
        // JwtAuthenticationFilter)
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // If no roles header, user wasn't authenticated
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            log.debug("No roles header, skipping RBAC check for: {} {}", method, path);
            return chain.filter(exchange);
        }

        // Find required permission for this endpoint
        String requiredPermission = findRequiredPermission(path, method);

        if (requiredPermission == null) {
            // No specific permission mapped, allow access
            log.debug("No permission mapping for: {} {}, allowing access", method, path);
            return chain.filter(exchange);
        }

        // Get user's permissions based on roles (YAML-backed system roles)
        Set<String> roles = parseRoles(rolesHeader);

        // ── Read-only enforcement ───────────────────────────────────────────────
        // If any of the user's roles is marked readOnly in rbac.yml restrictions,
        // reject mutating HTTP methods (anything other than GET, HEAD, OPTIONS).
        if (rbacConfig.isAnyRoleReadOnly(roles)) {
            if (!"GET".equals(method) && !"HEAD".equals(method) && !"OPTIONS".equals(method)) {
                log.warn("❌ Read-only role rejected mutating request: {} {} [roles={}, correlationId={}]",
                        method, path, roles, correlationId);
                return onForbidden(exchange, "read-only role cannot perform " + method + " requests");
            }
        }

        Set<String> userPermissions = getPermissionsForRoles(roles);

        // Phase 2: if role not found in YAML (custom role), fall back to
        // permissions forwarded directly from the JWT via X-Permissions header.
        if (userPermissions.isEmpty() && permissionsHeader != null && !permissionsHeader.isBlank()) {
            log.debug("🔄 No YAML permissions for roles {}, using X-Permissions header fallback", roles);
            for (String p : permissionsHeader.split(",")) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty())
                    userPermissions.add(trimmed);
            }
        }

        // Check if user has required permission
        if (hasPermission(userPermissions, requiredPermission)) {
            log.debug("✅ RBAC authorized: {} {} (permission: {}) [correlationId={}]",
                    method, path, requiredPermission, correlationId);
            return chain.filter(exchange);
        } else {
            log.warn("❌ RBAC denied: {} {} - User lacks permission: {} [roles={}, correlationId={}]",
                    method, path, requiredPermission, roles, correlationId);
            return onForbidden(exchange, requiredPermission);
        }
    }

    private String findRequiredPermission(String path, String method) {
        // Check each pattern in order (more specific patterns should be first)
        for (Map.Entry<String, Map<String, String>> entry : ENDPOINT_PERMISSIONS.entrySet()) {
            String pattern = entry.getKey();
            if (pathMatcher.match(pattern, path)) {
                Map<String, String> methodPermissions = entry.getValue();
                // Check for exact method match
                if (methodPermissions.containsKey(method)) {
                    return methodPermissions.get(method);
                }
                // Check for wildcard method
                if (methodPermissions.containsKey("*")) {
                    return methodPermissions.get("*");
                }
            }
        }
        return null;
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> roles = new HashSet<>();
        for (String role : rolesHeader.split(",")) {
            roles.add(role.trim());
        }
        return roles;
    }

    private Set<String> getPermissionsForRoles(Set<String> roles) {
        Set<String> allPermissions = new HashSet<>();
        for (String role : roles) {
            // Caffeine.get() returns cached value or computes and caches it atomically
            Set<String> rolePermissions = permissionCache.get(role,
                    k -> rbacConfig.getPermissionsForRole(k));
            if (rolePermissions != null) {
                allPermissions.addAll(rolePermissions);
            }
        }
        return allPermissions;
    }

    private boolean hasPermission(Set<String> userPermissions, String requiredPermission) {
        // Direct permission check
        if (userPermissions.contains(requiredPermission)) {
            return true;
        }

        // Check for wildcard permission (e.g., "property:*" grants "property:read")
        String[] parts = requiredPermission.split(":");
        if (parts.length == 2) {
            String resource = parts[0];
            if (userPermissions.contains(resource + ":*")) {
                return true;
            }
            // Check for global wildcard
            if (userPermissions.contains("*:*") || userPermissions.contains("*")) {
                return true;
            }
        }

        // Check for uppercase version (some roles use uppercase)
        String upperPermission = requiredPermission.toUpperCase().replace(":", "_");
        if (userPermissions.contains(upperPermission)) {
            return true;
        }

        return false;
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/v1/auth/") ||
                path.startsWith("/api/v1/rbac/") ||
                path.startsWith("/api/v1/public/") ||
                path.startsWith("/api/v1/organizations/onboarding/") ||
                path.equals("/api/v1/organizations/apply") ||
                path.equals("/api/v1/rental-applications/submit") ||
                path.startsWith("/api/v1/rental-applications/track/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.equals("/graphql") ||
                path.startsWith("/fallback/");
    }

    private Mono<Void> onForbidden(ServerWebExchange exchange, String requiredPermission) {
        deniedRequestsCounter.increment();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        // Do NOT expose internal permission names to the caller — log them server-side
        // only.
        String body = String.format(
                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"You do not have permission to access this resource.\",\"path\":\"%s\"}",
                exchange.getRequest().getPath().value());

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // Run after JwtAuthenticationFilter (-100) but before routing
        return -50;
    }

    /**
     * Clear permission cache (call when RBAC config changes at runtime)
     */
    public void clearCache() {
        permissionCache.invalidateAll();
        log.info("RBAC permission cache cleared");
    }
}
