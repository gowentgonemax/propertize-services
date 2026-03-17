-- =============================================================================
-- V5: Create Permission Delegation Tables
-- Phase 3: Permission Delegation
--
-- Creates:
--   1. delegation_rules  - configurable rules for who can delegate what
--   2. delegations       - actual delegation records
--   3. Seed data         - default delegation rules for common roles
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Delegation Rules Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS delegation_rules (
    id                      BIGSERIAL       PRIMARY KEY,
    delegator_role          VARCHAR(100)    NOT NULL,
    delegatable_permissions VARCHAR(1000)   NOT NULL,
    allowed_delegate_roles  VARCHAR(500)    NOT NULL,
    max_duration_hours      INT             NOT NULL DEFAULT 168,
    requires_reason         BOOLEAN         NOT NULL DEFAULT TRUE,
    requires_approval       BOOLEAN         NOT NULL DEFAULT FALSE,
    max_chain_depth         INT             NOT NULL DEFAULT 1,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_delegation_rule_delegator_role ON delegation_rules(delegator_role);
CREATE INDEX IF NOT EXISTS idx_delegation_rule_active ON delegation_rules(is_active);

-- -----------------------------------------------------------------------------
-- 2. Delegations Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS delegations (
    id                      BIGSERIAL       PRIMARY KEY,
    delegator_user_id       BIGINT          NOT NULL,
    delegate_user_id        BIGINT          NOT NULL,
    permission              VARCHAR(255)    NOT NULL,
    granted_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at              TIMESTAMP       NOT NULL,
    reason                  VARCHAR(500),
    status                  VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    parent_delegation_id    BIGINT,
    approved_by             BIGINT,
    approved_at             TIMESTAMP,
    revoked_by              BIGINT,
    revoked_at              TIMESTAMP,
    organization_id         BIGINT,
    temporal_permission_id  BIGINT,

    CONSTRAINT fk_delegation_parent
        FOREIGN KEY (parent_delegation_id)
        REFERENCES delegations(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_delegation_status
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED', 'PENDING_APPROVAL')),

    CONSTRAINT chk_delegation_users_different
        CHECK (delegator_user_id <> delegate_user_id)
);

CREATE INDEX IF NOT EXISTS idx_delegation_delegator ON delegations(delegator_user_id);
CREATE INDEX IF NOT EXISTS idx_delegation_delegate ON delegations(delegate_user_id);
CREATE INDEX IF NOT EXISTS idx_delegation_status ON delegations(status);
CREATE INDEX IF NOT EXISTS idx_delegation_delegate_status ON delegations(delegate_user_id, status);
CREATE INDEX IF NOT EXISTS idx_delegation_delegator_status ON delegations(delegator_user_id, status);
CREATE INDEX IF NOT EXISTS idx_delegation_expires_at ON delegations(expires_at);
CREATE INDEX IF NOT EXISTS idx_delegation_org ON delegations(organization_id);

-- -----------------------------------------------------------------------------
-- 3. Seed Delegation Rules
-- -----------------------------------------------------------------------------

-- ORGANIZATION_OWNER: Can delegate broad permissions to Property Managers and Admins
INSERT INTO delegation_rules (delegator_role, delegatable_permissions, allowed_delegate_roles, max_duration_hours, requires_reason, requires_approval, max_chain_depth, is_active)
VALUES (
    'ORGANIZATION_OWNER',
    'payment:approve,payment:view,lease:approve,lease:terminate,property:manage,tenant:manage,report:financial,report:occupancy',
    'ORGANIZATION_ADMIN,PROPERTY_MANAGER,PORTFOLIO_OWNER,PROPERTY_ACCOUNTANT',
    720,
    TRUE,
    FALSE,
    2,
    TRUE
);

-- PROPERTY_MANAGER: Can delegate operational permissions to leasing and maintenance staff
INSERT INTO delegation_rules (delegator_role, delegatable_permissions, allowed_delegate_roles, max_duration_hours, requires_reason, requires_approval, max_chain_depth, is_active)
VALUES (
    'PROPERTY_MANAGER',
    'maintenance:assign,maintenance:view,maintenance:update,tenant:view,tenant:communicate,lease:view,lease:draft,inspection:schedule,inspection:view',
    'LEASING_AGENT,LEASING_COORDINATOR,MAINTENANCE_SUPERVISOR,MAINTENANCE_TECHNICIAN,ASSISTANT_PROPERTY_MANAGER,TENANT_COORDINATOR',
    168,
    TRUE,
    FALSE,
    1,
    TRUE
);

-- MAINTENANCE_SUPERVISOR: Can delegate maintenance tasks to technicians
INSERT INTO delegation_rules (delegator_role, delegatable_permissions, allowed_delegate_roles, max_duration_hours, requires_reason, requires_approval, max_chain_depth, is_active)
VALUES (
    'MAINTENANCE_SUPERVISOR',
    'maintenance:assign,maintenance:update,maintenance:close,inspection:perform,inspection:report',
    'MAINTENANCE_TECHNICIAN,MAINTENANCE_COORDINATOR',
    72,
    TRUE,
    FALSE,
    1,
    TRUE
);
