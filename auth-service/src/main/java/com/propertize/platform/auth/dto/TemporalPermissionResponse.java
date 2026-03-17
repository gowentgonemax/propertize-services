package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for temporal permission queries.
 *
 * Provides a clean API response representation of a temporal permission
 * entity, including grant/expiration timestamps and active status.
 *
 * @version 1.0 - Phase 1: Time-Based Access Control
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemporalPermissionResponse {

    /**
     * The unique identifier of the temporal permission.
     */
    private Long id;

    /**
     * The ID of the user who holds the permission.
     */
    private Long userId;

    /**
     * The permission string (e.g., "PROPERTY_WRITE").
     */
    private String permission;

    /**
     * The ID of the user who granted this permission.
     */
    private Long grantedBy;

    /**
     * Timestamp when the permission was granted.
     */
    private LocalDateTime grantedAt;

    /**
     * Timestamp when the permission expires.
     */
    private LocalDateTime expiresAt;

    /**
     * Reason for granting this permission.
     */
    private String reason;

    /**
     * Whether this permission is currently active.
     */
    private boolean isActive;
}
