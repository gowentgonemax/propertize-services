package com.propertize.commons.constants;

/**
 * Canonical HTTP header names propagated by the API Gateway to all downstream services.
 *
 * <p>Every downstream service (propertize, employee-service, payment-service, payroll-service,
 * auth-service) MUST import these constants instead of redefining header name literals
 * in each filter/config.  This eliminates magic-string duplication and ensures a single
 * point of change if a header name ever needs updating.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * import com.propertize.commons.constants.GatewayHeaders;
 *
 * String userId = request.getHeader(GatewayHeaders.X_USER_ID);
 * }</pre>
 *
 * @see <a href="docs/JWT_ENDTOEND_GUIDE.md">JWT End-to-End Guide</a>
 */
public final class GatewayHeaders {

    private GatewayHeaders() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ── Identity headers (set by JwtAuthenticationFilter in api-gateway) ─────

    /** Authenticated user's numeric ID (from JWT {@code sub} claim). */
    public static final String X_USER_ID = "X-User-Id";

    /** Authenticated user's email address (from JWT {@code email} claim). */
    public static final String X_USER_EMAIL = "X-User-Email";

    /** Comma-separated list of the user's roles (from JWT {@code roles} claim). */
    public static final String X_USER_ROLES = "X-User-Roles";

    /** The primary/first role of the authenticated user. */
    public static final String X_PRIMARY_ROLE = "X-Primary-Role";

    // ── Organization headers ──────────────────────────────────────────────────

    /** Organization UUID extracted from JWT {@code organizationId} claim. */
    public static final String X_ORGANIZATION_ID = "X-Organization-Id";

    /**
     * Tenant/org numeric ID (legacy alias — prefer {@link #X_ORGANIZATION_ID}).
     * Some downstream filters still read this value; kept for backward compat.
     */
    public static final String X_TENANT_ID = "X-Tenant-Id";

    /** Organization type string (e.g. {@code PROPERTY_MANAGEMENT}, {@code REAL_ESTATE}). */
    public static final String X_ORG_TYPE = "X-Org-Type";

    // ── Permission / RBAC headers ─────────────────────────────────────────────

    /** Comma-separated list of permission codes (SCREAMING_SNAKE_CASE). */
    public static final String X_PERMISSIONS = "X-Permissions";

    // ── Auth / standard headers ───────────────────────────────────────────────

    /** Standard HTTP Authorization header. */
    public static final String AUTHORIZATION = "Authorization";

    /** Bearer token prefix used in the Authorization header value. */
    public static final String BEARER_PREFIX = "Bearer ";

    // ── Observability ─────────────────────────────────────────────────────────

    /**
     * Request correlation ID — set by the gateway, propagated downstream.
     * Services should read this value into MDC for structured logging.
     */
    public static final String X_CORRELATION_ID = "X-Correlation-ID";

    /** Internal service-to-service API key header (trusted network only). */
    public static final String X_API_KEY = "X-Api-Key";
}

