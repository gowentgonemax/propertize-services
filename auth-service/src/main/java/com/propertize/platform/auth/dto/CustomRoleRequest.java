package com.propertize.platform.auth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for creating or updating a custom role.
 *
 * <p>
 * Used by API consumers to define organization-scoped custom roles with
 * a curated permission set. Permissions must be a subset of the creator's
 * effective permissions.
 * </p>
 *
 * @version 1.0 - Phase 4a: Custom Role Builder
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomRoleRequest {

    /**
     * Unique role name within the organization (e.g., "FRONT_DESK_AGENT").
     */
    @NotBlank(message = "Role name is required")
    @Size(max = 150, message = "Role name must not exceed 150 characters")
    private String roleName;

    /**
     * Human-readable display name (e.g., "Front Desk Agent").
     */
    @NotBlank(message = "Display name is required")
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;

    /**
     * Optional description of the custom role's purpose.
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * The organization this custom role belongs to.
     */
    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    /**
     * List of permissions to assign to this custom role.
     * Must be a subset of the creator's effective permissions.
     */
    @NotEmpty(message = "At least one permission is required")
    private List<String> permissions;

    /**
     * Optional base role name to inherit permissions from.
     * When set, the effective permission set is the union of
     * inherited base role permissions + the permissions listed above.
     */
    private String inheritsFrom;

    /**
     * Maximum privilege level for this custom role.
     * Cannot exceed the creator's own privilege level.
     */
    @Min(value = 0, message = "Max level must be at least 0")
    @Max(value = 100, message = "Max level must not exceed 100")
    private int maxLevel;
}
