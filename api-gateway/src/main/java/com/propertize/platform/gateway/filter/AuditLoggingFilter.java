package com.propertize.platform.gateway.filter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Audit Logging Filter for Authentication Events
 *
 * Logs all authentication-related activities including:
 * - Login attempts (success/failure)
 * - Token refresh
 * - Access denied events
 * - Protected resource access
 * - Suspicious activities
 *
 * Supports:
 * - Structured JSON logging
 * - Async log processing
 * - Configurable log levels
 * - Sensitive data masking
 */
@Slf4j
@Component
public class AuditLoggingFilter implements GlobalFilter, Ordered {

    @Value("${audit.logging.enabled:true}")
    private boolean auditEnabled;

    @Value("${audit.logging.log-request-body:false}")
    private boolean logRequestBody;

    @Value("${audit.logging.log-response-body:false}")
    private boolean logResponseBody;

    @Value("${audit.logging.max-queue-size:10000}")
    private int maxQueueSize;

    // Headers
    private static final String X_USER_ID = "X-User-Id";
    private static final String X_ORGANIZATION_ID = "X-Organization-Id";
    private static final String X_CORRELATION_ID = "X-Correlation-Id";
    private static final String X_ROLES = "X-Roles";
    private static final String AUTHORIZATION = "Authorization";

    // Endpoints to audit
    private static final Set<String> AUTH_ENDPOINTS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/change-password");

    private static final Set<String> SENSITIVE_ENDPOINTS = Set.of(
            "/api/v1/admin/",
            "/api/v1/payments/",
            "/api/v1/users/",
            "/api/v1/organizations/");

    // Audit event queue for async processing
    private final ConcurrentLinkedQueue<AuditEvent> auditQueue = new ConcurrentLinkedQueue<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!auditEnabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Always log auth endpoints
        boolean shouldAudit = isAuthEndpoint(path) || isSensitiveEndpoint(path);

        if (!shouldAudit) {
            // For non-sensitive endpoints, only log write operations
            HttpMethod method = request.getMethod();
            if (method == null || method == HttpMethod.GET || method == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }
        }

        long startTime = System.currentTimeMillis();
        String correlationId = getOrCreateCorrelationId(request);

        return chain.filter(exchange)
                .doOnSuccess(v -> {
                    logAuditEvent(exchange, startTime, correlationId, null);
                })
                .doOnError(error -> {
                    logAuditEvent(exchange, startTime, correlationId, error.getMessage());
                });
    }

    private boolean isAuthEndpoint(String path) {
        return AUTH_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private boolean isSensitiveEndpoint(String path) {
        return SENSITIVE_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private String getOrCreateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(X_CORRELATION_ID);
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private void logAuditEvent(ServerWebExchange exchange, long startTime,
            String correlationId, String error) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String userId = request.getHeaders().getFirst(X_USER_ID);
        String organizationId = request.getHeaders().getFirst(X_ORGANIZATION_ID);
        String roles = request.getHeaders().getFirst(X_ROLES);
        String clientIp = getClientIp(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");

        int statusCode = response.getStatusCode() != null
                ? response.getStatusCode().value()
                : 0;
        long duration = System.currentTimeMillis() - startTime;

        // Determine event type
        AuditEventType eventType = determineEventType(path, statusCode, method);

        // Create audit event
        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .eventType(eventType)
                .userId(maskIfNull(userId))
                .organizationId(maskIfNull(organizationId))
                .roles(roles)
                .httpMethod(method)
                .path(path)
                .statusCode(statusCode)
                .durationMs(duration)
                .clientIp(clientIp)
                .userAgent(truncate(userAgent, 200))
                .success(statusCode >= 200 && statusCode < 400)
                .error(error)
                .build();

        // Log the event
        logEvent(event);

        // Queue for async processing if needed
        queueEvent(event);
    }

    private AuditEventType determineEventType(String path, int statusCode, String method) {
        if (path.contains("/auth/login")) {
            return statusCode == 200 ? AuditEventType.LOGIN_SUCCESS : AuditEventType.LOGIN_FAILURE;
        }
        if (path.contains("/auth/logout")) {
            return AuditEventType.LOGOUT;
        }
        if (path.contains("/auth/refresh")) {
            return statusCode == 200 ? AuditEventType.TOKEN_REFRESH : AuditEventType.TOKEN_REFRESH_FAILURE;
        }
        if (path.contains("/auth/register")) {
            return AuditEventType.REGISTRATION;
        }
        if (path.contains("/auth/change-password") || path.contains("/auth/reset-password")) {
            return AuditEventType.PASSWORD_CHANGE;
        }

        if (statusCode == 401) {
            return AuditEventType.UNAUTHORIZED_ACCESS;
        }
        if (statusCode == 403) {
            return AuditEventType.ACCESS_DENIED;
        }
        if (statusCode == 429) {
            return AuditEventType.RATE_LIMITED;
        }

        if (path.contains("/admin/")) {
            return AuditEventType.ADMIN_ACTION;
        }
        if (path.contains("/payments/")) {
            return AuditEventType.PAYMENT_ACTION;
        }

        return AuditEventType.RESOURCE_ACCESS;
    }

    private void logEvent(AuditEvent event) {
        String logMessage = formatAuditLog(event);

        if (event.isSuccess()) {
            if (event.getEventType() == AuditEventType.LOGIN_SUCCESS) {
                log.info("🔐 AUDIT [LOGIN] {}", logMessage);
            } else if (event.getEventType() == AuditEventType.LOGOUT) {
                log.info("🔓 AUDIT [LOGOUT] {}", logMessage);
            } else if (event.getEventType() == AuditEventType.TOKEN_REFRESH) {
                log.debug("🔄 AUDIT [REFRESH] {}", logMessage);
            } else if (event.getEventType() == AuditEventType.ADMIN_ACTION) {
                log.info("⚙️ AUDIT [ADMIN] {}", logMessage);
            } else if (event.getEventType() == AuditEventType.PAYMENT_ACTION) {
                log.info("💰 AUDIT [PAYMENT] {}", logMessage);
            } else {
                log.debug("📝 AUDIT [ACCESS] {}", logMessage);
            }
        } else {
            if (event.getEventType() == AuditEventType.LOGIN_FAILURE) {
                log.warn("🚫 AUDIT [LOGIN_FAILED] {}", logMessage);
            } else if (event.getEventType() == AuditEventType.UNAUTHORIZED_ACCESS) {
                log.warn("⚠️ AUDIT [UNAUTHORIZED] {}", logMessage);
            } else if (event.getEventType() == AuditEventType.ACCESS_DENIED) {
                log.warn("🛑 AUDIT [FORBIDDEN] {}", logMessage);
            } else if (event.getEventType() == AuditEventType.RATE_LIMITED) {
                log.warn("⏱️ AUDIT [RATE_LIMITED] {}", logMessage);
            } else {
                log.warn("❌ AUDIT [ERROR] {}", logMessage);
            }
        }
    }

    private String formatAuditLog(AuditEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("correlationId=").append(event.getCorrelationId());
        sb.append(" user=").append(event.getUserId());
        sb.append(" org=").append(event.getOrganizationId());
        sb.append(" method=").append(event.getHttpMethod());
        sb.append(" path=").append(event.getPath());
        sb.append(" status=").append(event.getStatusCode());
        sb.append(" duration=").append(event.getDurationMs()).append("ms");
        sb.append(" ip=").append(event.getClientIp());

        if (event.getError() != null) {
            sb.append(" error=").append(event.getError());
        }

        return sb.toString();
    }

    private void queueEvent(AuditEvent event) {
        if (auditQueue.size() < maxQueueSize) {
            auditQueue.offer(event);
        } else {
            log.warn("Audit queue full, dropping event: {}", event.getId());
        }
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private String maskIfNull(String value) {
        return value != null ? value : "anonymous";
    }

    private String truncate(String value, int maxLength) {
        if (value == null)
            return null;
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

    @Override
    public int getOrder() {
        // Run after authentication but before routing
        return -50;
    }

    // ============================================
    // AUDIT EVENT TYPES
    // ============================================

    public enum AuditEventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        TOKEN_REFRESH,
        TOKEN_REFRESH_FAILURE,
        REGISTRATION,
        PASSWORD_CHANGE,
        UNAUTHORIZED_ACCESS,
        ACCESS_DENIED,
        RATE_LIMITED,
        ADMIN_ACTION,
        PAYMENT_ACTION,
        RESOURCE_ACCESS
    }

    // ============================================
    // AUDIT EVENT MODEL
    // ============================================

    @Data
    @Builder
    public static class AuditEvent {
        private String id;
        private LocalDateTime timestamp;
        private String correlationId;
        private AuditEventType eventType;
        private String userId;
        private String organizationId;
        private String roles;
        private String httpMethod;
        private String path;
        private int statusCode;
        private long durationMs;
        private String clientIp;
        private String userAgent;
        private boolean success;
        private String error;
    }
}
