package com.propertize.platform.auth.filter;

import com.propertize.platform.auth.config.ServiceAuthenticationConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;

/**
 * Filter to validate service-to-service API keys for internal endpoints
 * Applied to /api/v1/users/* endpoints
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    private final ServiceAuthenticationConfig authConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply to user management endpoints
        if (!path.startsWith("/api/v1/users")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip authentication if disabled
        if (!authConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get API key from header
        String apiKey = request.getHeader(authConfig.getHeaderName());
        String serviceName = request.getHeader(authConfig.getServiceIdentifierHeader());

        // Validate API key
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ Service authentication failed: Missing API key from {} for path {}",
                    request.getRemoteAddr(), path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing service API key");
            return;
        }

        if (serviceName == null || serviceName.isBlank()) {
            log.warn("⚠️ Service authentication failed: Missing service name from {} for path {}",
                    request.getRemoteAddr(), path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing service identifier");
            return;
        }

        // Verify API key matches trusted service
        String expectedApiKey = authConfig.getTrustedServices().get(serviceName);
        if (expectedApiKey == null || !MessageDigest.isEqual(
                expectedApiKey.getBytes(), apiKey.getBytes())) {
            log.warn("⚠️ Service authentication failed: Invalid API key for service '{}' from {} for path {}",
                    serviceName, request.getRemoteAddr(), path);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid service API key");
            return;
        }

        log.debug("✅ Service authentication successful for: {} accessing {}", serviceName, path);
        request.setAttribute("authenticatedService", serviceName);
        filterChain.doFilter(request, response);
    }
}
