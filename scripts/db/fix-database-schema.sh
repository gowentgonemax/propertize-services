#!/bin/bash

# Run Flyway migrations manually for auth-service
# This script creates missing tables in the database

echo "=========================================="
echo "🗄️  Running Database Migrations"
echo "=========================================="
echo ""

# Database connection details
DB_CONTAINER="propertize-postgres"
DB_USER="dbuser"
DB_NAME="propertize_db"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0;' # No Color

echo "Step 1: Creating temporal_permissions table..."
echo "----------------------------------------------"

docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME <<EOF
-- V3__Create_temporal_permissions_table.sql
CREATE TABLE IF NOT EXISTS temporal_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role VARCHAR(255),
    permission VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    granted_by UUID,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    revoked_by UUID,
    is_active BOOLEAN DEFAULT TRUE,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_temporal_permissions_user_id ON temporal_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_temporal_permissions_active_expires ON temporal_permissions(is_active, expires_at);
CREATE INDEX IF NOT EXISTS idx_temporal_permissions_user_active ON temporal_permissions(user_id, is_active);
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ temporal_permissions table created${NC}"
else
    echo -e "${RED}❌ Failed to create temporal_permissions table${NC}"
    exit 1
fi

echo ""
echo "Step 2: Creating delegations table..."
echo "----------------------------------------------"

docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME <<EOF
-- V5__Create_delegation_tables.sql
CREATE TABLE IF NOT EXISTS delegations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delegator_user_id UUID NOT NULL,
    delegate_user_id UUID NOT NULL,
    permission VARCHAR(255) NOT NULL,
    organization_id UUID,
    parent_delegation_id UUID,
    temporal_permission_id UUID,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    approved_by UUID,
    revoked_at TIMESTAMP,
    revoked_by UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    FOREIGN KEY (parent_delegation_id) REFERENCES delegations(id),
    FOREIGN KEY (temporal_permission_id) REFERENCES temporal_permissions(id)
);

CREATE INDEX IF NOT EXISTS idx_delegations_delegator_user_id ON delegations(delegator_user_id);
CREATE INDEX IF NOT EXISTS idx_delegations_delegate_user_id ON delegations(delegate_user_id);
CREATE INDEX IF NOT EXISTS idx_delegations_status_expires ON delegations(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_delegations_organization ON delegations(organization_id);
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ delegations table created${NC}"
else
    echo -e "${RED}❌ Failed to create delegations table${NC}"
    exit 1
fi

echo ""
echo "Step 3: Verifying tables exist..."
echo "----------------------------------------------"

TABLES=$(docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -t -c "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename IN ('temporal_permissions', 'delegations');" | tr -d ' ')

if echo "$TABLES" | grep -q "temporal_permissions"; then
    echo -e "${GREEN}✅ temporal_permissions table exists${NC}"
else
    echo -e "${RED}❌ temporal_permissions table NOT found${NC}"
fi

if echo "$TABLES" | grep -q "delegations"; then
    echo -e "${GREEN}✅ delegations table exists${NC}"
else
    echo -e "${RED}❌ delegations table NOT found${NC}"
fi

echo ""
echo "Step 4: Restarting auth-service..."
echo "----------------------------------------------"

docker restart propertize-auth-service > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Auth service restarted${NC}"
    echo "   Waiting for startup..."
    sleep 10
else
    echo -e "${RED}❌ Failed to restart auth service${NC}"
    exit 1
fi

echo ""
echo "Step 5: Testing authentication..."
echo "----------------------------------------------"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Authentication SUCCESS (HTTP 200)${NC}"
    
    # Check if we have a token
    if echo "$BODY" | grep -q "accessToken"; then
        echo -e "${GREEN}✅ JWT token received${NC}"
        echo ""
        echo "=========================================="
        echo "✅ DATABASE FIX COMPLETE"
        echo "=========================================="
        echo ""
        echo "You can now test login in the browser:"
        echo "  1. Open: http://localhost:3000"
        echo "  2. Navigate to /login"
        echo "  3. Username: admin"
        echo "  4. Password: admin123"
        echo ""
        echo "Expected: No CredentialsSignin error"
        exit 0
    else
        echo -e "${YELLOW}⚠️  No JWT token in response${NC}"
    fi
else
    echo -e "${RED}❌ Authentication FAILED (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
fi

echo ""
echo "=========================================="
echo "Checking auth service logs for errors..."
echo "=========================================="
docker logs propertize-auth-service 2>&1 | grep -i "error\|exception" | tail -n 10

exit 0
