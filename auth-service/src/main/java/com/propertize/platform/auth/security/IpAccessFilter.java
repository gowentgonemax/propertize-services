package com.propertize.platform.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.platform.auth.service.IpAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servlet filter that enforces IP-based access control.
 *
 * Executes after authentication and checks whether the client IP address
 * is allowed based on the configured IP access rules. Supports extraction
 * of the real client IP from proxy headers (X-Forwarded-For, X-Real-IP).
 *
 * Can be disabled via the {@code app.ip-access.enabled} property
 * (defaults to false, so IP filtering is opt-in).
 *
 * @version 1.0 - Phase 4c: IP/Geo-Location Based Access Control
 */
@Component
@Order(110) // After JwtAuthenticationFilter
@RequiredArgsConstructor
@Slf4j
public class IpAccessFilter extends OncePerRequestFilter {

    private final IpAccessService ipAccessService;
    private final ObjectMapper objectMapper;

    @Value("${app.ip-access.enabled:false}")
    private boolean ipAccessEnabled;

    /**
     * Paths that should never be subject to IP filtering.
     */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/health",
            "/actuator/health",
            "/actuator/info");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Skip if IP access control is disabled
        if (!ipAccessEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip public endpoints
        String requestPath = request.getRequestURI();
        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only check authenticated requests
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        Long userId = extractUserId(request);
        Set<String> roles = extractRoles(authentication);
        Long orgId = extractOrgId(request);

        boolean allowed = ipAccessService.isIpAllowed(clientIp, userId, roles, orgId);

        if (!allowed) {
            log.warn("IP access denied: ip={}, userId={}, roles={}, path={}, method={}",
                    clientIp, userId, roles, requestPath, request.getMethod());
            sendForbiddenResponse(response, clientIp);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract the real client IP address, considering reverse proxy headers.
     */
    private String extractClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For first (may contain comma-separated list)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first (original client) IP
            String ip = xForwardedFor.split(",")[0].trim();
            if (!ip.isBlank())
                return ip;
        }

        // Check X-Real-IP
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Extract user ID from request headers.
     */
    private Long extractUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                log.debug("Invalid X-User-Id header: {}", userIdHeader);
            }
        }
        return null;
    }

    /**
     * Extract roles from the Spring Security Authentication object.
     */
    private Set<String> extractRoles(Authentication authentication) {
        if (authentication.getAuthorities() == null)
            return Collections.emptySet();

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .collect(Collectors.toSet());
    }

    /**
     * Extract organization ID from request headers.
     */
    private Long extractOrgId(HttpServletRequest request) {
        String orgIdHeader = request.getHeader("X-Organization-Id");
        if (orgIdHeader != null && !orgIdHeader.isBlank()) {
            try {
                return Long.parseLong(orgIdHeader);
            } catch (NumberFormatException e) {
                log.debug("Invalid X-Organization-Id header: {}", orgIdHeader);
            }
        }
        return null;
    }

    /**
     * Check if the request path is a public path that should skip IP filtering.
     */
    private boolean isPublicPath(String path) {
        if (path == null)
            return false;
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath))
                return true;
        }
        // Also allow all actuator endpoints
        if (path.startsWith("/actuator"))
            return true;
        return false;
    }

    /**
     * Send a 403 Forbidden JSON response.
     */
    private void sendForbiddenResponse(HttpServletResponse response, String clientIp) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("status", 403);
        errorBody.put("error", "Forbidden");
        errorBody.put("message", "Access denied: your IP address is not permitted to access this resource");
        errorBody.put("ip", clientIp);
        errorBody.put("timestamp", LocalDateTime.now().toString());

        objectMapper.writeValue(response.getOutputStream(), errorBody);
    }
}
