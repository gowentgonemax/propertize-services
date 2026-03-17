package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.CompositeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CompositeRole} entities.
 *
 * Provides CRUD operations and custom queries for managing composite roles,
 * including lookup by name, organization, and active status.
 *
 * @version 1.0 - Phase 3: Dynamic Role Composition
 */
@Repository
public interface CompositeRoleRepository extends JpaRepository<CompositeRole, Long> {

    /**
     * Find an active composite role by its unique name.
     *
     * @param name the composite role name
     * @return the composite role if found and active
     */
    Optional<CompositeRole> findByNameAndIsActiveTrue(String name);

    /**
     * Find all active composite roles.
     *
     * @return list of active composite roles
     */
    List<CompositeRole> findByIsActiveTrue();

    /**
     * Find all active composite roles belonging to a specific organization.
     *
     * @param organizationId the organization ID to filter by
     * @return list of active composite roles for the organization
     */
    List<CompositeRole> findByOrganizationIdAndIsActiveTrue(Long organizationId);

    /**
     * Check whether an active composite role with the given name already exists.
     *
     * @param name the composite role name
     * @return true if an active composite role with this name exists
     */
    boolean existsByNameAndIsActiveTrue(String name);
}
