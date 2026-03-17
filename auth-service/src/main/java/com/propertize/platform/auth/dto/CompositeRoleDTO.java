package com.propertize.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for creating a composite role.
 *
 * <p>
 * Used by API consumers to request the creation of a new composite role,
 * which combines multiple base roles into a single assignable unit.
 * </p>
 *
 * @version 1.0 - Phase 3: Dynamic Role Composition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompositeRoleDTO {

    /**
     * Unique name for the composite role (e.g., "PM_ACCOUNTANT").
     */
    @NotBlank(message = "Composite role name is required")
    private String name;

    /**
     * Human-readable description of the composite role.
     */
    private String description;

    /**
     * List of base role names that compose this role.
     * Must contain at least one role.
     */
    @NotEmpty(message = "At least one component role is required")
    private List<String> componentRoles;

    /**
     * Optional organization ID to scope this composite role to a specific org.
     * If null, the composite role is available platform-wide.
     */
    private Long organizationId;
}
