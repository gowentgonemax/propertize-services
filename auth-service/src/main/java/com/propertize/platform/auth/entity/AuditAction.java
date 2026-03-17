package com.propertize.platform.auth.entity;

/**
 * Enum representing the types of auditable actions in the permission system.
 *
 * <p>
 * Used by {@link PermissionAuditLog} to classify each audit trail entry
 * so that compliance queries can filter by action category.
 * </p>
 *
 * @version 1.0 - Phase 4b: Permission Audit Trail
 */
public enum AuditAction {

    /** A permission was checked (e.g., during an authorization decision). */
    PERMISSION_CHECK,

    /** A permission was granted to a user. */
    PERMISSION_GRANT,

    /** A permission check resulted in denial. */
    PERMISSION_DENY,

    /** A previously granted permission was revoked. */
    PERMISSION_REVOKE,

    /** A role was assigned to a user. */
    ROLE_ASSIGN,

    /** A role was removed from a user. */
    ROLE_REVOKE,

    /** A delegation was created. */
    DELEGATION_CREATE,

    /** A delegation was revoked. */
    DELEGATION_REVOKE,

    /** A user logged in. */
    LOGIN,

    /** A user logged out. */
    LOGOUT,

    /** A token was refreshed. */
    TOKEN_REFRESH,

    /** A user changed their password. */
    PASSWORD_CHANGE,

    /** A generic access attempt (e.g., hitting a protected endpoint). */
    ACCESS_ATTEMPT
}
