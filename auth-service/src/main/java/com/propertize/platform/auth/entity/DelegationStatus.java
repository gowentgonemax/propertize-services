package com.propertize.platform.auth.entity;

/**
 * Delegation Status Enum
 *
 * Represents the lifecycle states of a permission delegation.
 *
 * @version 1.0 - Phase 3: Permission Delegation
 */
public enum DelegationStatus {

    /** Delegation is currently active and the delegate has the permission. */
    ACTIVE,

    /** Delegation has expired (past its expiresAt timestamp). */
    EXPIRED,

    /** Delegation was manually revoked before expiry. */
    REVOKED,

    /** Delegation requires approval from an authorized approver. */
    PENDING_APPROVAL
}
