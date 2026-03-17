package com.propertize.platform.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback controller for circuit breaker fallback routes.
 * Returns user-friendly error messages when downstream services are unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping(value = "/propertize", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> propertizeFallback() {
        return Mono.just(Map.of(
            "success", false,
            "message", "Property Management Service is temporarily unavailable. Please try again later.",
            "error", Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "service", "propertize-service",
                "details", "The property management service is experiencing issues. Our team has been notified."
            ),
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        ));
    }

    @GetMapping(value = "/auth-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> authServiceFallback() {
        return Mono.just(Map.of(
            "success", false,
            "message", "Authentication Service is temporarily unavailable. Please try again later.",
            "error", Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "service", "auth-service",
                "details", "The authentication service is experiencing issues. Our team has been notified."
            ),
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        ));
    }

    @GetMapping(value = "/employecraft", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> employecraftFallback() {
        return Mono.just(Map.of(
            "success", false,
            "message", "Employee Management Service is temporarily unavailable. Please try again later.",
            "error", Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "service", "employecraft-service",
                "details", "The employee management service is experiencing issues. Our team has been notified."
            ),
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        ));
    }

    @GetMapping(value = "/wagecraft", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> wagecraftFallback() {
        return Mono.just(Map.of(
            "success", false,
            "message", "Payroll Management Service is temporarily unavailable. Please try again later.",
            "error", Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "service", "wagecraft-service",
                "details", "The payroll management service is experiencing issues. Our team has been notified."
            ),
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        ));
    }

    @GetMapping(value = "/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> defaultFallback() {
        return Mono.just(Map.of(
            "success", false,
            "message", "Service is temporarily unavailable. Please try again later.",
            "error", Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "details", "One or more services are experiencing issues. Our team has been notified."
            ),
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        ));
    }
}
