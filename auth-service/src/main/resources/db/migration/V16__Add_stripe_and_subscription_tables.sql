-- ──────────────────────────────────────────────────────────────────────
-- V16: Add Stripe fields to organizations + subscription_history +
--      organization_addons tables
-- ──────────────────────────────────────────────────────────────────────

-- 1. Stripe / billing columns on organizations
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS stripe_customer_id      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS stripe_subscription_id  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS trial_ends_at           TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_stripe_customer
    ON organizations (stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_stripe_subscription
    ON organizations (stripe_subscription_id)
    WHERE stripe_subscription_id IS NOT NULL;

-- 2. Subscription history — audit trail every time a plan changes
CREATE TABLE IF NOT EXISTS subscription_history (
    id                  BIGSERIAL PRIMARY KEY,
    organization_id     VARCHAR(36)  NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    previous_tier       VARCHAR(50),
    new_tier            VARCHAR(50)  NOT NULL,
    changed_by          VARCHAR(36),                    -- user_id who triggered the change
    change_reason       VARCHAR(255),                   -- 'UPGRADE', 'DOWNGRADE', 'TRIAL_CONVERSION', 'CANCELLATION'
    stripe_event_id     VARCHAR(100),                   -- idempotency key from Stripe webhook
    effective_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscription_history_org
    ON subscription_history (organization_id, effective_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_history_stripe_event
    ON subscription_history (stripe_event_id)
    WHERE stripe_event_id IS NOT NULL;

COMMENT ON TABLE subscription_history IS 'Immutable audit log of every subscription tier change per organization';

-- 3. Organization add-ons — feature entitlements purchased à-la-carte
CREATE TABLE IF NOT EXISTS organization_addons (
    id              BIGSERIAL PRIMARY KEY,
    organization_id VARCHAR(36)  NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    addon_key       VARCHAR(100) NOT NULL,   -- e.g. 'EXTRA_PROPERTIES_50', 'SMS_BUNDLE_500'
    quantity        INT          NOT NULL DEFAULT 1,
    activated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP,               -- NULL means no expiry
    stripe_item_id  VARCHAR(100),            -- Stripe subscription item id for the add-on line
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_org_addon UNIQUE (organization_id, addon_key)
);

CREATE INDEX IF NOT EXISTS idx_org_addons_org
    ON organization_addons (organization_id);

COMMENT ON TABLE organization_addons IS 'Per-organization add-on purchases (extra properties, SMS credits, etc.)';
