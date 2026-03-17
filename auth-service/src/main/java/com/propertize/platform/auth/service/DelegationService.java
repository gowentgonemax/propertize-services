package com.propertize.platform.auth.service;

import com.propertize.platform.auth.entity.Delegation;
import com.propertize.platform.auth.entity.DelegationRule;
import com.propertize.platform.auth.entity.DelegationStatus;
import com.propertize.platform.auth.entity.TemporalPermission;
import com.propertize.platform.auth.repository.DelegationRepository;
import com.propertize.platform.auth.repository.DelegationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing permission delegations.
 *
 * Handles the complete lifecycle of permission delegation:
 * <ul>
 * <li>Validating delegation rules (who can delegate what to whom)</li>
 * <li>Creating delegations with corresponding temporal permissions</li>
 * <li>Revoking delegations and their underlying permissions</li>
 * <li>Auto-expiring delegations via scheduled task</li>
 * <li>Enforcing delegation chain depth limits</li>
 * </ul>
 *
 * @version 1.0 - Phase 3: Permission Delegation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DelegationService {

    private final DelegationRepository delegationRepository;
    private final DelegationRuleRepository delegationRuleRepository;
    private final TemporalPermissionService temporalPermissionService;

    /**
     * Delegate a permission from one user to another.
     *
     * Performs comprehensive validation against delegation rules, then creates
     * both a Delegation record and a corresponding TemporalPermission to ensure
     * the permission is active for the delegate during authorization checks.
     *
     * @param delegatorUserId    the ID of the user delegating the permission
     * @param delegateUserId     the ID of the user receiving the permission
     * @param permission         the permission string to delegate
     * @param durationHours      how long the delegation should last (in hours)
     * @param reason             the reason for delegation (audit trail)
     * @param delegatorRole      the delegator's role (for rule lookup)
     * @param delegateRole       the delegate's role (for rule validation)
     * @param parentDelegationId optional parent delegation ID (for re-delegation
     *                           chains)
     * @param orgId              the organization context
     * @return the created {@link Delegation} entity
     * @throws IllegalArgumentException if any validation fails
     */
    @Transactional
    public Delegation delegatePermission(Long delegatorUserId, Long delegateUserId,
            String permission, int durationHours,
            String reason, String delegatorRole,
            String delegateRole, Long parentDelegationId,
            Long orgId) {
        log.info("Delegation request: delegator={}, delegate={}, permission={}, duration={}h, role={}",
                delegatorUserId, delegateUserId, permission, durationHours, delegatorRole);

        // --- Input validation ---
        if (delegatorUserId == null) {
            throw new IllegalArgumentException("Delegator user ID must not be null");
        }
        if (delegateUserId == null) {
            throw new IllegalArgumentException("Delegate user ID must not be null");
        }
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("Permission must not be null or blank");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason must not be null or blank");
        }
        if (delegatorRole == null || delegatorRole.isBlank()) {
            throw new IllegalArgumentException("Delegator role is required to look up delegation rules");
        }
        if (delegatorUserId.equals(delegateUserId)) {
            throw new IllegalArgumentException("Users cannot delegate permissions to themselves");
        }

        // --- Look up delegation rule for the delegator's role ---
        DelegationRule rule = delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue(delegatorRole)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active delegation rule found for role: " + delegatorRole +
                                ". This role is not authorized to delegate permissions."));

        // --- Validate the permission is delegatable ---
        if (!rule.isPermissionDelegatable(permission)) {
            throw new IllegalArgumentException(
                    "Permission '" + permission + "' is not delegatable by role '" + delegatorRole +
                            "'. Allowed permissions: " + rule.getDelegatablePermissions());
        }

        // --- Validate the delegate's role is allowed ---
        if (delegateRole != null && !delegateRole.isBlank() && !rule.isRoleAllowed(delegateRole)) {
            throw new IllegalArgumentException(
                    "Role '" + delegateRole + "' is not an allowed delegation target for '" + delegatorRole +
                            "'. Allowed roles: " + rule.getAllowedDelegateRoles());
        }

        // --- Validate duration ---
        if (durationHours < 1) {
            throw new IllegalArgumentException("Delegation duration must be at least 1 hour");
        }
        if (durationHours > rule.getMaxDurationHours()) {
            throw new IllegalArgumentException(
                    "Delegation duration " + durationHours + "h exceeds maximum allowed " +
                            rule.getMaxDurationHours() + "h for role '" + delegatorRole + "'");
        }

        // --- Validate reason if required ---
        if (rule.isRequiresReason() && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException(
                    "A reason is required for delegations from role '" + delegatorRole + "'");
        }

        // --- Validate delegation chain depth ---
        if (parentDelegationId != null) {
            validateDelegationChain(parentDelegationId, rule.getMaxChainDepth());
        }

        // --- Check for duplicate active delegations ---
        long existingCount = delegationRepository.countByDelegateUserIdAndPermissionAndStatus(
                delegateUserId, permission, DelegationStatus.ACTIVE);
        if (existingCount > 0) {
            throw new IllegalArgumentException(
                    "User " + delegateUserId + " already has an active delegation for permission '" + permission + "'");
        }

        // --- Determine initial status ---
        DelegationStatus initialStatus = rule.isRequiresApproval()
                ? DelegationStatus.PENDING_APPROVAL
                : DelegationStatus.ACTIVE;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(durationHours);

        // --- Create the Delegation entity ---
        Delegation delegation = Delegation.builder()
                .delegatorUserId(delegatorUserId)
                .delegateUserId(delegateUserId)
                .permission(permission)
                .grantedAt(now)
                .expiresAt(expiresAt)
                .reason(reason)
                .status(initialStatus)
                .parentDelegationId(parentDelegationId)
                .organizationId(orgId)
                .build();

        // --- If no approval required, create the temporal permission immediately ---
        if (initialStatus == DelegationStatus.ACTIVE) {
            TemporalPermission temporalPermission = temporalPermissionService.grantTemporaryPermission(
                    delegateUserId,
                    permission,
                    Duration.ofHours(durationHours),
                    "Delegated by user " + delegatorUserId + ": " + reason,
                    delegatorUserId);

            delegation.setTemporalPermissionId(temporalPermission.getId());
            log.info("Created temporal permission id={} for delegation", temporalPermission.getId());
        } else {
            log.info("Delegation requires approval — temporal permission will be created upon approval");
        }

        Delegation saved = delegationRepository.save(delegation);
        log.info("Delegation created: id={}, delegator={}, delegate={}, permission={}, status={}, expires={}",
                saved.getId(), delegatorUserId, delegateUserId, permission, initialStatus, expiresAt);

        return saved;
    }

    /**
     * Revoke an active delegation and its underlying temporal permission.
     *
     * @param delegationId the ID of the delegation to revoke
     * @param revokedBy    the ID of the user performing the revocation
     * @throws IllegalArgumentException if delegation is not found or not active
     */
    @Transactional
    public void revokeDelegation(Long delegationId, Long revokedBy) {
        log.info("Revoking delegation id={} by user {}", delegationId, revokedBy);

        Delegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Delegation not found with id: " + delegationId));

        if (delegation.getStatus() != DelegationStatus.ACTIVE &&
                delegation.getStatus() != DelegationStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException(
                    "Delegation " + delegationId + " is already " + delegation.getStatus() +
                            " and cannot be revoked");
        }

        delegation.setStatus(DelegationStatus.REVOKED);
        delegation.setRevokedBy(revokedBy);
        delegation.setRevokedAt(LocalDateTime.now());
        delegationRepository.save(delegation);

        // Revoke the underlying temporal permission if it exists
        if (delegation.getTemporalPermissionId() != null) {
            try {
                temporalPermissionService.revokePermission(delegation.getTemporalPermissionId(), revokedBy);
                log.info("Revoked underlying temporal permission id={}", delegation.getTemporalPermissionId());
            } catch (IllegalArgumentException e) {
                log.warn("Could not revoke temporal permission id={}: {}",
                        delegation.getTemporalPermissionId(), e.getMessage());
            }
        }

        log.info("Delegation {} revoked successfully by user {}", delegationId, revokedBy);
    }

    /**
     * Get all delegations created by a specific user (outgoing delegations).
     *
     * @param userId the delegator's user ID
     * @return list of delegations created by the user
     */
    @Transactional(readOnly = true)
    public List<Delegation> getMyDelegations(Long userId) {
        log.debug("Fetching outgoing delegations for user {}", userId);
        return delegationRepository.findByDelegatorUserId(userId);
    }

    /**
     * Get all delegations granted to a specific user (incoming delegations).
     *
     * @param userId the delegate's user ID
     * @return list of delegations granted to the user
     */
    @Transactional(readOnly = true)
    public List<Delegation> getDelegationsToMe(Long userId) {
        log.debug("Fetching incoming delegations for user {}", userId);
        return delegationRepository.findByDelegateUserId(userId);
    }

    /**
     * Get all active delegation rules.
     *
     * @return list of active delegation rules
     */
    @Transactional(readOnly = true)
    public List<DelegationRule> getActiveDelegationRules() {
        return delegationRuleRepository.findByIsActiveTrue();
    }

    /**
     * Get delegation rules applicable to a specific delegator role.
     *
     * @param delegatorRole the role to look up rules for
     * @return the delegation rule, if found
     */
    @Transactional(readOnly = true)
    public DelegationRule getDelegationRuleForRole(String delegatorRole) {
        return delegationRuleRepository.findByDelegatorRoleAndIsActiveTrue(delegatorRole)
                .orElse(null);
    }

    /**
     * Scheduled task to expire old delegations.
     *
     * Runs every 2 minutes to find ACTIVE delegations that have passed
     * their expiry time and marks them as EXPIRED. Also revokes the
     * underlying temporal permissions.
     */
    @Scheduled(fixedRate = 120_000)
    @Transactional
    public void expireOldDelegations() {
        LocalDateTime now = LocalDateTime.now();
        List<Delegation> expired = delegationRepository.findByStatusAndExpiresAtBefore(
                DelegationStatus.ACTIVE, now);

        if (!expired.isEmpty()) {
            log.info("Expiring {} delegations", expired.size());
            for (Delegation delegation : expired) {
                delegation.setStatus(DelegationStatus.EXPIRED);

                // Also revoke the temporal permission
                if (delegation.getTemporalPermissionId() != null) {
                    try {
                        temporalPermissionService.revokePermission(
                                delegation.getTemporalPermissionId(), null);
                    } catch (Exception e) {
                        log.warn("Could not revoke temporal permission for expired delegation {}: {}",
                                delegation.getId(), e.getMessage());
                    }
                }

                log.debug("Expired delegation: id={}, delegator={}, delegate={}, permission={}",
                        delegation.getId(), delegation.getDelegatorUserId(),
                        delegation.getDelegateUserId(), delegation.getPermission());
            }
            delegationRepository.saveAll(expired);
            log.info("Successfully expired {} delegations", expired.size());
        }
    }

    /**
     * Validate the delegation chain depth to prevent excessive re-delegation.
     *
     * Walks up the parent delegation chain and ensures the total depth
     * does not exceed the configured maximum.
     *
     * @param parentDelegationId the parent delegation ID
     * @param maxChainDepth      the maximum allowed chain depth
     * @throws IllegalArgumentException if chain depth would be exceeded
     */
    private void validateDelegationChain(Long parentDelegationId, int maxChainDepth) {
        log.debug("Validating delegation chain from parent id={}, maxDepth={}", parentDelegationId, maxChainDepth);

        int depth = 0;
        Long currentId = parentDelegationId;

        while (currentId != null) {
            depth++;
            if (depth >= maxChainDepth) {
                throw new IllegalArgumentException(
                        "Delegation chain depth would exceed maximum of " + maxChainDepth +
                                ". Re-delegation is not allowed beyond this depth.");
            }

            Delegation parent = delegationRepository.findById(currentId).orElse(null);
            if (parent == null) {
                throw new IllegalArgumentException(
                        "Parent delegation not found: " + currentId);
            }
            if (parent.getStatus() != DelegationStatus.ACTIVE) {
                throw new IllegalArgumentException(
                        "Parent delegation " + currentId + " is not active (status: " + parent.getStatus() + ")");
            }

            currentId = parent.getParentDelegationId();
        }

        log.debug("Delegation chain validation passed, depth={}", depth);
    }
}
