package com.propertize.platform.employecraft.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller with Dependency Validation
 * <p>
 * Provides comprehensive health check endpoints that validate:
 * - Database connectivity
 * - Redis connectivity
 * - Disk space availability
 * <p>
 * Returns HTTP 503 Service Unavailable if any critical dependency is down.
 * Kubernetes-ready with readiness and liveness probes.
 *
 * @author Employecraft Team
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Health", description = "Application health check endpoints with dependency validation")
public class HealthCheckController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.application.name:employecraft}")
    private String applicationName;

    /**
     * Comprehensive health check with dependency validation
     * 
     * @return Health status with dependency checks (HTTP 503 if unhealthy)
     */
    @GetMapping("/actuator/health")
    @Operation(summary = "Comprehensive health check", description = "Returns health status with all dependency checks")
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Health check requested at /actuator/health");

        Map<String, Object> health = new HashMap<>();
        health.put("application", applicationName);
        health.put("profile", activeProfile);
        health.put("timestamp", Instant.now().toString());

        // Check all dependencies
        Map<String, Object> checks = new HashMap<>();
        boolean allHealthy = true;

        // Database check
        Map<String, Object> dbCheck = checkDatabase();
        checks.put("database", dbCheck);
        if (!"UP".equals(dbCheck.get("status"))) {
            allHealthy = false;
        }

        // Redis check
        Map<String, Object> redisCheck = checkRedis();
        checks.put("redis", redisCheck);
        if (!"UP".equals(redisCheck.get("status"))) {
            allHealthy = false;
        }

        // Disk space check
        Map<String, Object> diskCheck = checkDiskSpace();
        checks.put("diskSpace", diskCheck);
        if (!"UP".equals(diskCheck.get("status"))) {
            allHealthy = false;
        }

        health.put("checks", checks);
        health.put("status", allHealthy ? "UP" : "DOWN");

        // Return 503 if any dependency is down
        return allHealthy
                ? ResponseEntity.ok(health)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
    }

    /**
     * Legacy health endpoint for backward compatibility
     */
    @GetMapping("/health")
    @Hidden
    public ResponseEntity<Map<String, Object>> legacyHealth() {
        return health();
    }

    /**
     * API versioned health check endpoint
     */
    @GetMapping("/api/v1/health")
    @Hidden
    public ResponseEntity<Map<String, Object>> apiHealth() {
        return health();
    }

    /**
     * Kubernetes readiness probe
     * Checks if service is ready to accept traffic
     */
    @GetMapping("/actuator/health/readiness")
    @Hidden
    public ResponseEntity<Map<String, Object>> readiness() {
        return health();
    }

    /**
     * Kubernetes liveness probe
     * Checks if service should be restarted
     */
    @GetMapping("/actuator/health/liveness")
    @Hidden
    public ResponseEntity<Map<String, Object>> liveness() {
        // Liveness only checks if application is running
        // (database/redis failures shouldn't trigger pod restart)
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", applicationName,
                "timestamp", Instant.now().toString()));
    }

    private Map<String, Object> checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Map.of("status", "UP", "type", "PostgreSQL");
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, Object> checkRedis() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return Map.of("status", "UP", "type", "Redis");
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, Object> checkDiskSpace() {
        try {
            File root = new File("/");
            long freeSpace = root.getFreeSpace();
            long totalSpace = root.getTotalSpace();
            long usableSpace = root.getUsableSpace();

            // Consider unhealthy if less than 1GB free
            boolean healthy = usableSpace > 1_073_741_824L; // 1GB in bytes

            return Map.of(
                    "status", healthy ? "UP" : "DOWN",
                    "free", formatBytes(freeSpace),
                    "total", formatBytes(totalSpace),
                    "usable", formatBytes(usableSpace));
        } catch (Exception e) {
            log.error("Disk space health check failed", e);
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
