package com.propertize.platform.employecraft.client;

import com.propertize.platform.employecraft.dto.propertize.OrganizationDto;
import com.propertize.platform.employecraft.dto.propertize.UserCreateRequest;
import com.propertize.platform.employecraft.dto.propertize.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign Client for Propertize Service Integration
 *
 * Handles:
 * - Organization validation
 * - User creation for employees needing system access
 * - User status updates
 */
@FeignClient(
    name = "propertize-service",
    url = "${propertize.api.url}",
    configuration = FeignClientConfig.class
)
public interface PropertizeFeignClient {

    /**
     * Validate organization exists and get details
     */
    @GetMapping("/api/v1/organizations/{id}")
    ResponseEntity<OrganizationDto> getOrganization(
        @PathVariable("id") UUID organizationId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Validate organization exists (internal API)
     */
    @GetMapping("/api/v1/internal/organizations/{id}/validate")
    ResponseEntity<Boolean> validateOrganization(
        @PathVariable("id") UUID organizationId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Create a new user in Propertize for an employee
     */
    @PostMapping("/api/v1/users")
    ResponseEntity<UserDto> createUser(
        @RequestBody UserCreateRequest request,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Get user by ID
     */
    @GetMapping("/api/v1/users/{id}")
    ResponseEntity<UserDto> getUser(
        @PathVariable("id") Long userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Deactivate a user
     */
    @PutMapping("/api/v1/users/{id}/deactivate")
    ResponseEntity<Void> deactivateUser(
        @PathVariable("id") Long userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Reactivate a user
     */
    @PutMapping("/api/v1/users/{id}/activate")
    ResponseEntity<Void> activateUser(
        @PathVariable("id") Long userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Health check
     */
    @GetMapping("/actuator/health")
    ResponseEntity<Object> health();
}
