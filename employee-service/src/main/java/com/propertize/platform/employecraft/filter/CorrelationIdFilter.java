package com.propertize.platform.employecraft.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter for Employecraft Service
 * 
 * Production-Ready Design v2.0
 * 
 * Propagates correlation IDs for distributed tracing across microservices.
 * If a correlation ID is present from the API Gateway, it's used; otherwise,
 * a new one is generated (for direct service calls).
 * 
 * MDC Keys populated:
 * - correlationId: Full correlation ID for request tracing
 * - shortCorrelationId: First 8 characters for compact logging
 * - requestMethod: HTTP method (GET, POST, etc.)
 * - requestUri: Request URI path
 * - clientIp: Client IP address
 * - userId: User ID if authenticated
 * 
 * @author Platform Team
 * @version 2.0
 */
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String USER_ID_HEADER = "X-User-Id";

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String SHORT_CORRELATION_ID_MDC_KEY = "shortCorrelationId";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String REQUEST_METHOD_MDC_KEY = "requestMethod";
    private static final String REQUEST_URI_MDC_KEY = "requestUri";
    private static final String CLIENT_IP_MDC_KEY = "clientIp";
    private static final String USER_ID_MDC_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Get or generate correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Get or generate request ID
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Get user ID from header (propagated from Gateway)
        String userId = request.getHeader(USER_ID_HEADER);

        try {
            // Populate MDC for logging context
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            MDC.put(SHORT_CORRELATION_ID_MDC_KEY, correlationId.length() >= 8
                    ? correlationId.substring(0, 8)
                    : correlationId);
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());
            MDC.put(REQUEST_URI_MDC_KEY, request.getRequestURI());
            MDC.put(CLIENT_IP_MDC_KEY, getClientIp(request));

            if (userId != null && !userId.isBlank()) {
                MDC.put(USER_ID_MDC_KEY, userId);
            }

            log.debug("Request started: {} {} [correlationId={}, requestId={}]",
                    request.getMethod(), request.getRequestURI(), correlationId, requestId);

            // Add correlation ID to response headers
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            response.setHeader(REQUEST_ID_HEADER, requestId);

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            log.info("Request completed: {} {} - status={} duration={}ms [correlationId={}]",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration, correlationId);

            // Clear MDC to prevent context leaks
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove(SHORT_CORRELATION_ID_MDC_KEY);
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(REQUEST_METHOD_MDC_KEY);
            MDC.remove(REQUEST_URI_MDC_KEY);
            MDC.remove(CLIENT_IP_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
        }
    }

    /**
     * Extract client IP address, considering proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP if multiple are present
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip health check endpoints to reduce log noise
        return path.equals("/actuator/health")
                || path.equals("/actuator/health/liveness")
                || path.equals("/actuator/health/readiness");
    }
}
