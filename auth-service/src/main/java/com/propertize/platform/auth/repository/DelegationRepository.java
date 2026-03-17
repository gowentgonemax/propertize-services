package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.Delegation;
import com.propertize.platform.auth.entity.DelegationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing {@link Delegation} entities.
 *
 * @version 1.0 - Phase 3: Permission Delegation
 */
@Repository
public interface DelegationRepository extends JpaRepository<Delegation, Long> {

    /**
     * Find all delegations granted TO a specific user with a given status.
     */
    List<Delegation> findByDelegateUserIdAndStatus(Long delegateUserId, DelegationStatus status);

    /**
     * Find all delegations created BY a specific user with a given status.
     */
    List<Delegation> findByDelegatorUserIdAndStatus(Long delegatorUserId, DelegationStatus status);

    /**
     * Find delegations that have a specific status and have expired before the
     * given time.
     * Used by the scheduled expiry task to find ACTIVE delegations past their
     * expiry.
     */
    List<Delegation> findByStatusAndExpiresAtBefore(DelegationStatus status, LocalDateTime now);

    /**
     * Count how many delegations a user has for a specific permission with a given
     * status.
     * Useful for preventing duplicate active delegations for the same permission.
     */
    long countByDelegateUserIdAndPermissionAndStatus(Long delegateUserId, String permission, DelegationStatus status);

    /**
     * Find all delegations for a specific delegator (all statuses).
     */
    List<Delegation> findByDelegatorUserId(Long delegatorUserId);

    /**
     * Find all delegations for a specific delegate (all statuses).
     */
    List<Delegation> findByDelegateUserId(Long delegateUserId);
}
