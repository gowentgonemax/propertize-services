package com.propertize.platform.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating a permission delegation request.
 *
 * Used by API consumers to request delegation of a specific permission
 * from the caller (delegator) to a target user (delegate) for a
 * specified duration.
 *
 * @version 1.0 - Phase 3: Permission Delegation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelegationRequest {

    /**
     * The ID of the user to delegate the permission to.
     */
    @NotNull(message = "Delegate user ID is required")
    private Long delegateUserId;

    /**
     * The permission string to delegate (e.g., "maintenance:assign").
     */
    @NotBlank(message = "Permission is required")
    private String permission;

    /**
     * Duration in hours for the delegation. Must be at least 1 hour.
     */
    @NotNull(message = "Duration in hours is required")
    @Min(value = 1, message = "Duration must be at least 1 hour")
    private Integer durationHours;

    /**
     * Reason for the delegation (required for audit trail).
     */
    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * The delegator's role (used to look up delegation rules).
     * If not provided, it will be resolved from the user's assigned roles.
     */
    private String delegatorRole;

    /**
     * The delegate's role (used to validate allowed delegate roles).
     */
    private String delegateRole;

    /**
     * Optional parent delegation ID, if this is a re-delegation.
     */
    private Long parentDelegationId;

    /**
     * Organization context for the delegation.
     */
    private Long organizationId;
}
