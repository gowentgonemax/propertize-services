package com.propertize.payroll.client;

import com.propertize.payroll.client.dto.OrganizationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign Client for Propertize Service Integration
 *
 * Handles:
 * - Organization validation
 * - User authentication validation
 */
@FeignClient(
    name = "propertize-service",
    url = "${propertize.api.url}",
    configuration = FeignClientConfig.class
)
public interface PropertizeFeignClient {

    /**
     * Validate organization exists
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
     * Get organization settings
     */
    @GetMapping("/api/v1/organizations/{id}/settings")
    ResponseEntity<OrganizationDto.SettingsDto> getOrganizationSettings(
        @PathVariable("id") UUID organizationId,
        @RequestHeader("Authorization") String authorization
    );
}
