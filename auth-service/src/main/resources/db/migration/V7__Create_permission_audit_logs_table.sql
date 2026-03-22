-- ============================================================================
-- V7: Create permission_audit_logs table
-- Phase 4b: Permission Audit Trail
--
-- Records every permission check, grant, denial, and access decision for
-- compliance auditing.  Indexes are designed for efficient range scans on
-- created_at combined with common filter columns.  The table is
-- partition-ready on created_at if future partitioning is needed.
-- ============================================================================

CREATE TABLE IF NOT EXISTS permission_audit_logs (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    username        VARCHAR(255)    NOT NULL,
    action          VARCHAR(50)     NOT NULL,
    permission      VARCHAR(255)    NOT NULL,
    resource_type   VARCHAR(100),
    resource_id     VARCHAR(255),
    result          VARCHAR(30)     NOT NULL,
    reason          VARCHAR(500),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    request_path    VARCHAR(500),
    request_method  VARCHAR(10),
    organization_id BIGINT,
    session_id      VARCHAR(255),
    context_data    TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Composite indexes for common audit queries
CREATE INDEX IF NOT EXISTS idx_audit_user_created       ON permission_audit_logs (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_permission_created ON permission_audit_logs (permission, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_result_created     ON permission_audit_logs (result, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_org_created        ON permission_audit_logs (organization_id, created_at);
