package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Response DTO for custom role queries.
 *
 * <p>
 * Provides a clean API response representation of a custom role entity,
 * including both the directly assigned permissions and the resolved
 * effective permission set (which includes inherited permissions when
 * the role inherits from a base role).
 * </p>
 *
 * @version 1.0 - Phase 4a: Custom Role Builder
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomRoleResponse {

    /**
     * The unique identifier of the custom role.
     */
    private Long id;

    /**
     * Unique role name within the organization.
     */
    private String roleName;

    /**
     * Human-readable display name.
     */
    private String displayName;

    /**
     * Description of the custom role's purpose.
     */
    private String description;

    /**
     * The organization this custom role belongs to.
     */
    private Long organizationId;

    /**
     * The directly assigned permissions for this custom role.
     */
    private List<String> permissions;

    /**
     * The resolved effective permission set (inherited + custom).
     * Populated on detail queries; may be null for list responses.
     */
    private Set<String> effectivePermissions;

    /**
     * The base role name this custom role inherits from, if any.
     */
    private String inheritsFrom;

    /**
     * Maximum privilege level for this custom role.
     */
    private int maxLevel;

    /**
     * The ID of the user who created this custom role.
     */
    private Long createdBy;

    /**
     * Timestamp when this custom role was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this custom role was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Whether this custom role is currently active.
     */
    private boolean isActive;
}
