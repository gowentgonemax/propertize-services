package com.propertize.platform.auth.entity;

/**
 * Enum representing the outcome of an audited permission action.
 *
 * <p>
 * Used by {@link PermissionAuditLog} to record whether an access
 * decision was allowed, denied, errored, or not applicable.
 * </p>
 *
 * @version 1.0 - Phase 4b: Permission Audit Trail
 */
public enum AuditResult {

    /** The action was allowed / access was granted. */
    ALLOWED,

    /** The action was denied / access was refused. */
    DENIED,

    /** An error occurred while evaluating the action. */
    ERROR,

    /** The result concept does not apply to this action type (e.g., logout). */
    NOT_APPLICABLE
}
