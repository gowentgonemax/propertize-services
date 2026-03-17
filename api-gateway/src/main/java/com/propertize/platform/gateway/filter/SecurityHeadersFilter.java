package com.propertize.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Security Headers Filter for API Gateway
 * 
 * Production-Ready Authentication Design v2.0
 * 
 * Adds security headers to all responses as per OWASP recommendations:
 * - X-Content-Type-Options: nosniff
 * - X-Frame-Options: DENY
 * - X-XSS-Protection: 1; mode=block
 * - Strict-Transport-Security: max-age=31536000; includeSubDomains
 * - Content-Security-Policy: default-src 'self'
 * - Referrer-Policy: strict-origin-when-cross-origin
 * - Permissions-Policy: camera=(), microphone=(), geolocation=()
 * - Cache-Control: no-store (for authenticated responses)
 *
 * @author Platform Security Team
 * @version 2.0 - Production Ready
 */
@Slf4j
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();

        // Add security headers BEFORE the response is committed
        response.beforeCommit(() -> {
            // Prevent MIME type sniffing
            response.getHeaders().set("X-Content-Type-Options", "nosniff");

            // Prevent clickjacking
            response.getHeaders().set("X-Frame-Options", "DENY");

            // XSS protection (legacy, but still useful)
            response.getHeaders().set("X-XSS-Protection", "1; mode=block");

            // HTTP Strict Transport Security (HSTS)
            response.getHeaders().set(
                    "Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");

            // Content Security Policy
            response.getHeaders().set(
                    "Content-Security-Policy",
                    "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self' data:; " +
                            "frame-ancestors 'none'; " +
                            "base-uri 'self'; " +
                            "form-action 'self'");

            // Referrer Policy
            response.getHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions Policy (formerly Feature-Policy)
            response.getHeaders().set(
                    "Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), " +
                            "payment=(), usb=(), magnetometer=(), " +
                            "accelerometer=(), gyroscope=()");

            // Cache-Control for authenticated responses
            String path = exchange.getRequest().getPath().value();
            if (!path.startsWith("/public/") &&
                    !path.startsWith("/actuator/") &&
                    !path.startsWith("/swagger-ui/")) {
                response.getHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
                response.getHeaders().set("Pragma", "no-cache");
            }

            // API Versioning headers
            response.getHeaders().set("X-API-Version", "1");
            response.getHeaders().set("X-API-Supported-Versions", "1");

            // Cross-Origin headers (defense-in-depth)
            response.getHeaders().set("Cross-Origin-Opener-Policy", "same-origin");
            response.getHeaders().set("Cross-Origin-Resource-Policy", "same-origin");

            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Execute last, after all other filters
        return Ordered.LOWEST_PRECEDENCE;
    }
}
