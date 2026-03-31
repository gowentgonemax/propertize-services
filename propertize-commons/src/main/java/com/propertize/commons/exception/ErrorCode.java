package com.propertize.commons.exception;

/**
 * Canonical error codes used across all Propertize services.
 *
 * Naming convention: DOMAIN_REASON
 * HTTP status guidance is documented per constant.
 */
public enum ErrorCode {

    // ── Generic ─────────────────────────────────────────────────────────────
    /** 500 — unexpected server error */
    INTERNAL_ERROR,
    /** 400 — caller sent a syntactically invalid request */
    BAD_REQUEST,
    /** 400 — one or more fields failed bean-validation */
    VALIDATION_FAILED,
    /** 404 — requested resource does not exist */
    RESOURCE_NOT_FOUND,
    /** 409 — state-machine transition not allowed from current state */
    INVALID_STATE_TRANSITION,
    /** 409 — unique constraint violated */
    CONFLICT,
    /** 403 — caller lacks required permission */
    ACCESS_DENIED,
    /** 401 — token absent, expired, or invalid */
    UNAUTHENTICATED,
    /** 429 — rate limit exceeded */
    RATE_LIMIT_EXCEEDED,

    // ── Employee ─────────────────────────────────────────────────────────────
    EMPLOYEE_NOT_FOUND,
    EMPLOYEE_ALREADY_EXISTS,
    EMPLOYEE_INVALID_DEPARTMENT,

    // ── Payroll ──────────────────────────────────────────────────────────────
    PAYROLL_RUN_NOT_FOUND,
    PAYROLL_RUN_ALREADY_PROCESSED,
    PAYROLL_TIMESHEET_NOT_SUBMITTED,
    PAYROLL_CALCULATION_ERROR,

    // ── User / Auth ───────────────────────────────────────────────────────────
    USER_NOT_FOUND,
    USER_EMAIL_TAKEN,
    USER_ROLE_INSUFFICIENT,

    // ── Integration ───────────────────────────────────────────────────────────
    UPSTREAM_SERVICE_ERROR,
    UPSTREAM_TIMEOUT,
}
