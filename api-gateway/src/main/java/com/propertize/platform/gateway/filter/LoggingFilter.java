package com.propertize.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Global filter for logging all HTTP exchanges through the API Gateway.
 *
 * <p>Logs every request at INFO level with:
 * method, path, query, client-IP, correlation-ID, user-ID, and org-ID.
 * Logs every response at INFO/WARN/ERROR depending on HTTP status.
 * Slow requests (>3 s) are additionally flagged at WARN regardless of status.</p>
 *
 * <p>Sensitive header values ({@code Authorization}, {@code Cookie}) are never
 * written to logs — only their presence is noted.</p>
 */
@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final long   SLOW_REQUEST_MS         = 3_000L;
    private static final String X_CORRELATION_ID        = "X-Correlation-Id";
    private static final String X_USER_ID               = "X-User-Id";
    private static final String X_ORGANIZATION_ID       = "X-Organization-Id";

    /** Paths that are intentionally excluded from INFO-level traffic logging. */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator/health",
            "/actuator/info"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        long   startTime     = System.currentTimeMillis();
        String clientIp      = extractClientIp(request);
        String correlationId = extractHeader(request, X_CORRELATION_ID, "n/a");
        String userId        = extractHeader(request, X_USER_ID, "anonymous");
        String orgId         = extractHeader(request, X_ORGANIZATION_ID, null);
        String method        = request.getMethod().name();
        String query         = request.getURI().getRawQuery();

        StringBuilder reqSb = new StringBuilder(128)
                .append("▶ GW-REQUEST")
                .append(" | ").append(method)
                .append(" ").append(path);
        if (query != null && !query.isBlank()) {
            reqSb.append("?").append(query);
        }
        reqSb.append(" | ip=").append(clientIp)
             .append(" | correlationId=").append(correlationId)
             .append(" | userId=").append(userId);
        if (orgId != null) {
            reqSb.append(" | orgId=").append(orgId);
        }
        log.info(reqSb.toString());

        return chain.filter(exchange)
                .doFinally(signal -> {
                    long          duration   = System.currentTimeMillis() - startTime;
                    ServerHttpResponse resp  = exchange.getResponse();
                    HttpStatusCode    status = resp.getStatusCode();
                    int               code   = status != null ? status.value() : 0;

                    StringBuilder resSb = new StringBuilder(128)
                            .append("◀ GW-RESPONSE")
                            .append(" | ").append(method)
                            .append(" ").append(path)
                            .append(" | status=").append(code)
                            .append(" | duration=").append(duration).append("ms")
                            .append(" | correlationId=").append(correlationId);

                    if (code >= 500) {
                        log.error(resSb.toString());
                    } else if (code >= 400) {
                        log.warn(resSb.toString());
                    } else {
                        log.info(resSb.toString());
                    }

                    if (duration > SLOW_REQUEST_MS) {
                        log.warn("⚠ GW-SLOW | {} {} took {}ms [correlationId={}]",
                                method, path, duration, correlationId);
                    }
                });
    }

    private String extractClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private String extractHeader(ServerHttpRequest request, String headerName, String defaultValue) {
        String value = request.getHeaders().getFirst(headerName);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
