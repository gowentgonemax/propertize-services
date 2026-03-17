package com.propertize.platform.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating temporal permission requests.
 *
 * Used by API consumers to request the granting of a time-bound
 * permission to a specific user. The {@code durationMinutes} field
 * controls how long the permission will remain active.
 *
 * @version 1.0 - Phase 1: Time-Based Access Control
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemporalPermissionDTO {

    /**
     * The ID of the user to grant the permission to.
     */
    @NotNull(message = "User ID is required")
    private Long userId;

    /**
     * The permission string to grant (e.g., "PROPERTY_WRITE", "TENANT_DELETE").
     */
    @NotBlank(message = "Permission is required")
    private String permission;

    /**
     * Duration in minutes for which the permission should be active.
     * Must be at least 5 minutes.
     */
    @NotNull(message = "Duration in minutes is required")
    @Min(value = 5, message = "Duration must be at least 5 minutes")
    private Integer durationMinutes;

    /**
     * Reason for granting this temporary permission (required for audit trail).
     */
    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * Optional resource type that the permission is scoped to (e.g., "PROPERTY",
     * "LEASE").
     */
    private String resourceType;

    /**
     * Optional specific resource ID that the permission is scoped to.
     */
    private Long resourceId;
}
