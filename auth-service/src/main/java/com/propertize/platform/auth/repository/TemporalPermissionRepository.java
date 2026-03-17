package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.TemporalPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing {@link TemporalPermission} entities.
 *
 * Provides query methods for finding active, expired, and user-specific
 * temporal permissions. Used by
 * {@link com.propertize.platform.auth.service.TemporalPermissionService}
 * for time-based access control operations.
 *
 * @version 1.0 - Phase 1: Time-Based Access Control
 */
@Repository
public interface TemporalPermissionRepository extends JpaRepository<TemporalPermission, Long> {

    /**
     * Find all active temporal permissions for a user that have not yet expired.
     *
     * @param userId the user ID
     * @param now    the current timestamp to compare against expiration
     * @return list of active, non-expired temporal permissions
     */
    List<TemporalPermission> findByUserIdAndIsActiveTrueAndExpiresAtAfter(Long userId, LocalDateTime now);

    /**
     * Find all active temporal permissions that have expired (for cleanup).
     *
     * @param now the current timestamp to compare against expiration
     * @return list of active permissions that have passed their expiration time
     */
    List<TemporalPermission> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime now);

    /**
     * Find all active temporal permissions granted by a specific user.
     *
     * @param grantedBy the ID of the user who granted the permissions
     * @return list of active permissions granted by the specified user
     */
    List<TemporalPermission> findByGrantedByAndIsActiveTrue(Long grantedBy);

    /**
     * Find all active temporal permissions for a user (regardless of expiration).
     *
     * @param userId the user ID
     * @return list of all active temporal permissions for the user
     */
    List<TemporalPermission> findByUserIdAndIsActiveTrue(Long userId);
}
