-- ============================================================================
-- V8: Create IP Access Rules table
-- Phase 4c: IP/Geo-Location Based Access Control
-- ============================================================================

CREATE TABLE IF NOT EXISTS ip_access_rules (
    id              BIGSERIAL       PRIMARY KEY,
    rule_type       VARCHAR(20)     NOT NULL,           -- WHITELIST or BLACKLIST
    ip_pattern      VARCHAR(255)    NOT NULL,           -- IP, CIDR, or wildcard
    description     VARCHAR(500),
    scope           VARCHAR(20)     NOT NULL,           -- GLOBAL, ORGANIZATION, ROLE, USER
    scope_value     VARCHAR(255),                       -- depends on scope
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP                           -- null = never expires
);

-- Indexes for efficient lookups
CREATE INDEX IF NOT EXISTS idx_ip_rule_scope          ON ip_access_rules (scope);
CREATE INDEX IF NOT EXISTS idx_ip_rule_scope_value    ON ip_access_rules (scope, scope_value);
CREATE INDEX IF NOT EXISTS idx_ip_rule_is_active      ON ip_access_rules (is_active);
CREATE INDEX IF NOT EXISTS idx_ip_rule_scope_active   ON ip_access_rules (scope, is_active);
CREATE INDEX IF NOT EXISTS idx_ip_rule_scope_val_act  ON ip_access_rules (scope, scope_value, is_active);

-- ============================================================================
-- Seed data (uncomment / adjust for your environment)
-- ============================================================================

-- Example: Blacklist a known bad range
-- INSERT INTO ip_access_rules (rule_type, ip_pattern, description, scope, is_active)
-- VALUES ('BLACKLIST', '0.0.0.0/8', 'Block reserved range', 'GLOBAL', true);

-- Example: Whitelist office VPN
-- INSERT INTO ip_access_rules (rule_type, ip_pattern, description, scope, is_active)
-- VALUES ('WHITELIST', '10.0.0.0/8', 'Office VPN network', 'GLOBAL', true);

-- Example: Restrict PLATFORM_ENGINEERING role to office IPs
-- INSERT INTO ip_access_rules (rule_type, ip_pattern, description, scope, scope_value, is_active)
-- VALUES ('WHITELIST', '192.168.1.0/24', 'Engineering office subnet', 'ROLE', 'PLATFORM_ENGINEERING', true);
