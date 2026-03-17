package com.propertize.platform.gateway.controller;

import com.propertize.platform.gateway.service.ServiceTokenCache;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Circuit Breaker monitoring and management endpoints
 *
 * <p>Provides real-time insights into circuit breaker states,
 * cache statistics, and service health.</p>
 *
 * @author Platform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gateway/monitoring")
@RequiredArgsConstructor
public class CircuitBreakerMonitoringController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ServiceTokenCache serviceTokenCache;

    /**
     * Get circuit breaker status for all registered circuit breakers
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getAllCircuitBreakers() {
        Map<String, Object> response = new HashMap<>();

        Map<String, Map<String, Object>> circuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .collect(Collectors.toMap(
                        CircuitBreaker::getName,
                        cb -> {
                            var metrics = cb.getMetrics();
                            Map<String, Object> cbInfo = new HashMap<>();
                            cbInfo.put("state", cb.getState().name());
                            cbInfo.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
                            cbInfo.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
                            cbInfo.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
                            cbInfo.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
                            cbInfo.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
                            cbInfo.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
                            return cbInfo;
                        }
                ));

        response.put("circuitBreakers", circuitBreakers);
        response.put("totalCircuitBreakers", circuitBreakers.size());

        log.debug("📊 Circuit breaker status requested");
        return ResponseEntity.ok(response);
    }

    /**
     * Get specific circuit breaker status
     */
    @GetMapping("/circuit-breakers/{name}")
    public ResponseEntity<Map<String, Object>> getCircuitBreaker(@PathVariable String name) {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            var metrics = cb.getMetrics();

            Map<String, Object> response = new HashMap<>();
            response.put("name", name);
            response.put("state", cb.getState().name());
            response.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
            response.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
            response.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
            response.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
            response.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
            response.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
            response.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Circuit breaker '{}' not found", name);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Transition circuit breaker to specific state (for testing/debugging)
     */
    @PostMapping("/circuit-breakers/{name}/state")
    public ResponseEntity<Map<String, String>> transitionCircuitBreaker(
            @PathVariable String name,
            @RequestParam String targetState) {

        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);

            switch (targetState.toUpperCase()) {
                case "CLOSED" -> cb.transitionToClosedState();
                case "OPEN" -> cb.transitionToOpenState();
                case "HALF_OPEN" -> cb.transitionToHalfOpenState();
                case "FORCE_OPEN" -> cb.transitionToForcedOpenState();
                case "DISABLED" -> cb.transitionToDisabledState();
                default -> {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid state: " + targetState));
                }
            }

            log.info("✅ Circuit breaker '{}' transitioned to {}", name, targetState);
            return ResponseEntity.ok(Map.of(
                    "circuitBreaker", name,
                    "newState", cb.getState().name()
            ));

        } catch (Exception e) {
            log.error("❌ Failed to transition circuit breaker '{}': {}", name, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get service token cache statistics
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, String>> getCacheStats() {
        Map<String, String> response = new HashMap<>();
        response.put("stats", serviceTokenCache.getStats());

        log.debug("📊 Cache stats requested");
        return ResponseEntity.ok(response);
    }

    /**
     * Clear service token cache
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        serviceTokenCache.clearAll();

        log.info("🗑️ Service token cache cleared manually");
        return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
    }

    /**
     * Get overall health summary
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();

        // Circuit breaker health
        Map<String, String> cbHealth = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .collect(Collectors.toMap(
                        CircuitBreaker::getName,
                        cb -> cb.getState().name()
                ));
        response.put("circuitBreakers", cbHealth);

        // Cache health
        response.put("cacheStats", serviceTokenCache.getStats());

        // Overall status
        boolean allHealthy = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .noneMatch(cb -> cb.getState() == CircuitBreaker.State.OPEN);

        response.put("status", allHealthy ? "HEALTHY" : "DEGRADED");

        return ResponseEntity.ok(response);
    }
}
