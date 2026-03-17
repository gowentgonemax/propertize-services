package com.propertize.platform.auth.service;

import com.propertize.platform.auth.entity.TemporalPermission;
import com.propertize.platform.auth.repository.TemporalPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing temporal (time-bound) permissions.
 *
 * Provides functionality to grant, revoke, query, and auto-expire
 * temporary permissions. Temporal permissions allow administrators to
 * grant users time-limited access to specific resources or actions,
 * supporting scenarios like emergency access, temporary project roles,
 * or time-boxed administrative privileges.
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Grant permissions with configurable duration</li>
 * <li>Automatic expiration via scheduled cleanup</li>
 * <li>Manual revocation with audit trail</li>
 * <li>Query active permissions for authorization decisions</li>
 * </ul>
 *
 * @version 1.0 - Phase 1: Time-Based Access Control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemporalPermissionService {

    private static final Duration MAX_PERMISSION_DURATION = Duration.ofHours(72);
    private static final Duration MIN_PERMISSION_DURATION = Duration.ofMinutes(5);

    private final TemporalPermissionRepository temporalPermissionRepository;

    /**
     * Grant a temporary permission to a user.
     *
     * @param userId     the ID of the user receiving the permission
     * @param permission the permission string to grant (e.g., "PROPERTY_WRITE")
     * @param duration   how long the permission should last
     * @param reason     the reason for granting the permission (audit trail)
     * @param grantedBy  the ID of the user granting the permission
     * @return the created {@link TemporalPermission} entity
     * @throws IllegalArgumentException if any input validation fails
     */
    @Transactional
    public TemporalPermission grantTemporaryPermission(Long userId, String permission,
            Duration duration, String reason,
            Long grantedBy) {
        log.info("Granting temporal permission '{}' to user {} for {} minutes, granted by user {}",
                permission, userId, duration.toMinutes(), grantedBy);

        // Validate inputs
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("Permission must not be null or blank");
        }
        if (duration == null) {
            throw new IllegalArgumentException("Duration must not be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason must not be null or blank");
        }
        if (grantedBy == null) {
            throw new IllegalArgumentException("GrantedBy user ID must not be null");
        }

        // Validate duration bounds
        if (duration.compareTo(MIN_PERMISSION_DURATION) < 0) {
            throw new IllegalArgumentException(
                    "Duration must be at least " + MIN_PERMISSION_DURATION.toMinutes() + " minutes");
        }
        if (duration.compareTo(MAX_PERMISSION_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "Duration must not exceed " + MAX_PERMISSION_DURATION.toHours() + " hours");
        }

        // Prevent self-granting
        if (userId.equals(grantedBy)) {
            throw new IllegalArgumentException("Users cannot grant temporal permissions to themselves");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(duration);

        TemporalPermission temporalPermission = TemporalPermission.builder()
                .userId(userId)
                .permission(permission)
                .grantedBy(grantedBy)
                .grantedAt(now)
                .expiresAt(expiresAt)
                .reason(reason)
                .isActive(true)
                .build();

        TemporalPermission saved = temporalPermissionRepository.save(temporalPermission);
        log.info("Temporal permission granted: id={}, user={}, permission={}, expires={}",
                saved.getId(), userId, permission, expiresAt);

        return saved;
    }

    /**
     * Get the set of active (non-expired) permission strings for a user.
     *
     * @param userId the user ID
     * @return set of active permission strings
     */
    @Transactional(readOnly = true)
    public Set<String> getActiveTemporalPermissions(Long userId) {
        log.debug("Fetching active temporal permissions for user {}", userId);

        List<TemporalPermission> activePermissions = temporalPermissionRepository
                .findByUserIdAndIsActiveTrueAndExpiresAtAfter(
                        userId, LocalDateTime.now());

        Set<String> permissions = activePermissions.stream()
                .map(TemporalPermission::getPermission)
                .collect(Collectors.toSet());

        log.debug("Found {} active temporal permissions for user {}", permissions.size(), userId);
        return permissions;
    }

    /**
     * Revoke a temporal permission by marking it as inactive.
     *
     * @param permissionId the ID of the temporal permission to revoke
     * @param revokedBy    the ID of the user revoking the permission
     * @throws IllegalArgumentException if the permission is not found or already
     *                                  inactive
     */
    @Transactional
    public void revokePermission(Long permissionId, Long revokedBy) {
        log.info("Revoking temporal permission id={} by user {}", permissionId, revokedBy);

        TemporalPermission permission = temporalPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Temporal permission not found with id: " + permissionId));

        if (!permission.isActive()) {
            throw new IllegalArgumentException(
                    "Temporal permission is already inactive: " + permissionId);
        }

        permission.setActive(false);
        permission.setRevokedAt(LocalDateTime.now());
        permission.setRevokedBy(revokedBy);

        temporalPermissionRepository.save(permission);
        log.info("Temporal permission revoked: id={}, user={}, permission={}, revokedBy={}",
                permissionId, permission.getUserId(), permission.getPermission(), revokedBy);
    }

    /**
     * Scheduled task to automatically expire old temporal permissions.
     * Runs every 60 seconds to find and deactivate permissions that have
     * passed their expiration time.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOldPermissions() {
        LocalDateTime now = LocalDateTime.now();
        List<TemporalPermission> expired = temporalPermissionRepository.findByIsActiveTrueAndExpiresAtBefore(now);

        if (!expired.isEmpty()) {
            log.info("Expiring {} temporal permissions", expired.size());
            for (TemporalPermission permission : expired) {
                permission.setActive(false);
                log.debug("Expired temporal permission: id={}, user={}, permission={}",
                        permission.getId(), permission.getUserId(), permission.getPermission());
            }
            temporalPermissionRepository.saveAll(expired);
            log.info("Successfully expired {} temporal permissions", expired.size());
        }
    }

    /**
     * Check if a user currently has a specific active temporal permission.
     *
     * @param userId     the user ID
     * @param permission the permission string to check
     * @return true if the user has an active, non-expired temporal permission
     */
    @Transactional(readOnly = true)
    public boolean isPermissionActive(Long userId, String permission) {
        log.debug("Checking if temporal permission '{}' is active for user {}", permission, userId);

        List<TemporalPermission> activePermissions = temporalPermissionRepository
                .findByUserIdAndIsActiveTrueAndExpiresAtAfter(
                        userId, LocalDateTime.now());

        boolean isActive = activePermissions.stream()
                .anyMatch(p -> p.getPermission().equals(permission));

        log.debug("Temporal permission '{}' for user {} is {}", permission, userId,
                isActive ? "ACTIVE" : "INACTIVE");
        return isActive;
    }

    /**
     * Get the full list of active temporal permission entities for a user.
     *
     * @param userId the user ID
     * @return list of active {@link TemporalPermission} entities
     */
    @Transactional(readOnly = true)
    public List<TemporalPermission> getActivePermissionsForUser(Long userId) {
        log.debug("Fetching all active temporal permission entities for user {}", userId);
        return temporalPermissionRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Get all active temporal permissions granted by a specific user.
     *
     * @param grantedBy the ID of the granting user
     * @return list of active {@link TemporalPermission} entities granted by the
     *         user
     */
    @Transactional(readOnly = true)
    public List<TemporalPermission> getPermissionsGrantedBy(Long grantedBy) {
        log.debug("Fetching active temporal permissions granted by user {}", grantedBy);
        return temporalPermissionRepository.findByGrantedByAndIsActiveTrue(grantedBy);
    }
}
