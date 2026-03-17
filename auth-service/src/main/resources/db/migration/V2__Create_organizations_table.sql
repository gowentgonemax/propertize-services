-- Create organizations table if it doesn't exist
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    organization_code VARCHAR(50) NOT NULL UNIQUE,
    organization_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add name column if it doesn't exist (for existing tables)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='organizations' AND column_name='organization_name') THEN
        ALTER TABLE organizations ADD COLUMN organization_name VARCHAR(255);
    END IF;
END $$;

-- Create index for organization_code
CREATE INDEX IF NOT EXISTS idx_organization_code ON organizations(organization_code);

-- Add comments
COMMENT ON TABLE organizations IS 'Stores organization/tenant information for multi-tenancy';
COMMENT ON COLUMN organizations.organization_code IS 'Unique code identifying the organization';
COMMENT ON COLUMN organizations.organization_name IS 'Display name of the organization';

-- Add updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_organizations_updated_at ON organizations;
CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
