package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CustomRole} entities.
 *
 * Provides CRUD operations and custom queries for managing organization-scoped
 * custom roles, including lookup by name, organization, and inheritance chain.
 *
 * @version 1.0 - Phase 4a: Custom Role Builder
 */
@Repository
public interface CustomRoleRepository extends JpaRepository<CustomRole, Long> {

    /**
     * Find all active custom roles belonging to a specific organization.
     *
     * @param orgId the organization ID to filter by
     * @return list of active custom roles for the organization
     */
    List<CustomRole> findByOrganizationIdAndIsActiveTrue(Long orgId);

    /**
     * Find an active custom role by its name within a specific organization.
     *
     * @param roleName the custom role name
     * @param orgId    the organization ID
     * @return the custom role if found and active
     */
    Optional<CustomRole> findByRoleNameAndOrganizationIdAndIsActiveTrue(String roleName, Long orgId);

    /**
     * Check whether an active custom role with the given name already exists
     * within the specified organization.
     *
     * @param roleName the custom role name
     * @param orgId    the organization ID
     * @return true if an active custom role with this name exists in the org
     */
    boolean existsByRoleNameAndOrganizationIdAndIsActiveTrue(String roleName, Long orgId);

    /**
     * Find all active custom roles that inherit from a specific base role.
     * Useful for impact analysis when a base role's permissions change.
     *
     * @param baseRole the base role name
     * @return list of active custom roles inheriting from the base role
     */
    List<CustomRole> findByInheritsFromAndIsActiveTrue(String baseRole);
}
