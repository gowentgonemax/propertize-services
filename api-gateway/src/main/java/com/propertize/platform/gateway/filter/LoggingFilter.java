package com.propertize.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for logging all gateway requests and responses.
 * Provides visibility into traffic passing through the gateway.
 */
@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String clientIp = getClientIp(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String authHeader = request.getHeaders().getFirst("Authorization");
        boolean hasAuth = authHeader != null && !authHeader.isBlank();

        log.debug(">>> Gateway Request: {} {} | IP: {} | Auth: {} | User-Agent: {}",
                request.getMethod(),
                request.getURI().getPath(),
                clientIp,
                hasAuth ? "Bearer [REDACTED]" : "None",
                userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "Unknown");

        return chain.filter(exchange)
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - startTime;

                    log.debug("<<< Gateway Response: {} {} | Status: {} | Duration: {}ms",
                            request.getMethod(),
                            request.getURI().getPath(),
                            exchange.getResponse().getStatusCode(),
                            duration);

                    // Log slow requests
                    if (duration > 5000) {
                        log.warn("Slow request detected: {} {} took {}ms",
                                request.getMethod(),
                                request.getURI().getPath(),
                                duration);
                    }
                });
    }

    private String getClientIp(ServerHttpRequest request) {
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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
