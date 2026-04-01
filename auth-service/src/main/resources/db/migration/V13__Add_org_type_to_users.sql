-- V13: Add organization_type to users table
-- Ties each user to the type of organization they belong to (IPO/PMC/REI/CORP/HA)
-- This is denormalized from organizations table for fast JWT claim generation.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS organization_type VARCHAR(50);

-- Populate from organizations table for existing users
-- Column in organizations table is: organization_type (not organization_type_enum)
UPDATE users u
SET    organization_type = o.organization_type
FROM   organizations o
WHERE  u.organization_id = o.id
   OR  u.organization_id = o.organization_code;

COMMENT ON COLUMN users.organization_type IS
    'Denormalized org type (INDIVIDUAL_PROPERTY_OWNER/PROPERTY_MANAGEMENT_COMPANY/etc.) sourced from organizations.organization_type';
