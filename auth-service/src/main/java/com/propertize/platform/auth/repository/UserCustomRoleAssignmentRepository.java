package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.UserCustomRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCustomRoleAssignmentRepository extends JpaRepository<UserCustomRoleAssignment, Long> {

    /**
     * Returns all active role assignments for a user, eagerly loading the
     * {@link com.propertize.platform.auth.entity.RbacRole} so that callers
     * can read permissions without an extra query.
     */
    @Query("SELECT a FROM UserCustomRoleAssignment a JOIN FETCH a.rbacRole WHERE a.userId = :userId AND a.isActive = true")
    List<UserCustomRoleAssignment> findByUserIdAndIsActiveTrueWithRole(@Param("userId") Long userId);

    List<UserCustomRoleAssignment> findByOrganizationIdAndIsActiveTrue(Long organizationId);

    Optional<UserCustomRoleAssignment> findByUserIdAndRbacRoleIdAndIsActiveTrue(Long userId, Long rbacRoleId);

    boolean existsByUserIdAndRbacRoleIdAndIsActiveTrue(Long userId, Long rbacRoleId);

    /**
     * Bulk-deactivates all custom role assignments whose TTL has lapsed.
     * Called by the nightly expiry sweep in CustomRoleService.
     */
    @Modifying
    @Query("UPDATE UserCustomRoleAssignment a SET a.isActive = false " +
            "WHERE a.isActive = true AND a.expiresAt IS NOT NULL AND a.expiresAt < :now")
    int deactivateExpiredAssignments(@Param("now") LocalDateTime now);
}
