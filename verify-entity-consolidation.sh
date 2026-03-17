#!/bin/bash
# Verification script for duplicate entity removal changes
# This script verifies that the User and Organization entities are properly configured

set -e

echo "======================================"
echo "Entity Consolidation Verification"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
    fi
}

# 1. Check if both services compile
echo "1. Checking compilation status..."
echo "   Compiling auth-service..."
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service
if mvn clean compile -q 2>&1 | grep -q "BUILD SUCCESS"; then
    print_status 0 "auth-service compiles successfully"
else
    print_status 1 "auth-service has compilation errors"
fi

echo "   Compiling propertize..."
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/propertize
if mvn clean compile -q 2>&1 | grep -q "BUILD SUCCESS"; then
    print_status 0 "propertize compiles successfully"
else
    print_status 1 "propertize has compilation errors"
fi

echo ""
echo "2. Checking User entity locations..."
# Check User entity in auth-service
if [ -f "/Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service/src/main/java/com/propertize/platform/auth/entity/User.java" ]; then
    print_status 0 "User entity exists in auth-service (MASTER)"
    # Check for @Immutable annotation absence
    if ! grep -q "@Immutable" "/Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service/src/main/java/com/propertize/platform/auth/entity/User.java"; then
        print_status 0 "  - auth-service User is NOT immutable (correct - this is the master)"
    else
        print_status 1 "  - auth-service User should NOT be @Immutable"
    fi
else
    print_status 1 "User entity missing in auth-service"
fi

# Check User entity in propertize
if [ -f "/Users/ravishah/MySpace/ProperyManage/propertize-Services/propertize/src/main/java/com/propertize/entity/User.java" ]; then
    print_status 0 "User entity exists in propertize (READ-ONLY MIRROR)"
    # Check for @Immutable annotation
    if grep -q "@Immutable" "/Users/ravishah/MySpace/ProperyManage/propertize-Services/propertize/src/main/java/com/propertize/entity/User.java"; then
        print_status 0 "  - propertize User is @Immutable (correct - read-only)"
    else
        print_status 1 "  - propertize User should be @Immutable"
    fi
else
    print_status 1 "User entity missing in propertize"
fi

echo ""
echo "3. Checking Organization entity locations..."
# Check Organization NOT in auth-service
if [ ! -f "/Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service/src/main/java/com/propertize/platform/auth/entity/Organization.java" ]; then
    print_status 0 "Organization entity correctly removed from auth-service"
else
    print_status 1 "Organization entity should NOT exist in auth-service"
fi

# Check Organization in propertize
if [ -f "/Users/ravishah/MySpace/ProperyManage/propertize-Services/propertize/src/main/java/com/propertize/entity/Organization.java" ]; then
    print_status 0 "Organization entity exists in propertize (MASTER)"
else
    print_status 1 "Organization entity missing in propertize"
fi

echo ""
echo "4. Checking OrganizationInfoService..."
# Check OrganizationInfoService NOT in auth-service
if [ ! -f "/Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service/src/main/java/com/propertize/platform/auth/service/OrganizationInfoService.java" ]; then
    print_status 0 "OrganizationInfoService correctly removed from auth-service"
else
    print_status 1 "OrganizationInfoService should NOT exist in auth-service"
fi

# Check OrganizationInfoService in propertize
if [ -f "/Users/ravishah/MySpace/ProperyManage/propertize-Services/propertize/src/main/java/com/propertize/services/OrganizationInfoService.java" ]; then
    print_status 0 "OrganizationInfoService exists in propertize (correct)"
else
    print_status 1 "OrganizationInfoService missing in propertize"
fi

echo ""
echo "5. Checking UserRoleEnum..."
# Check UserRoleEnum in auth-service
if [ -f "/Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service/src/main/java/com/propertize/enums/UserRoleEnum.java" ]; then
    print_status 0 "UserRoleEnum exists in auth-service"
else
    print_status 1 "UserRoleEnum missing in auth-service"
fi

# Check UserRoleEnum in propertize
if [ -f "/Users/ravishah/MySpace/ProperyManage/propertize-Services/propertize/src/main/java/com/propertize/enums/UserRoleEnum.java" ]; then
    print_status 0 "UserRoleEnum exists in propertize"
else
    print_status 1 "UserRoleEnum missing in propertize"
fi

echo ""
echo "6. Checking User entity features..."
# Check for organizationIds field in auth-service User
if grep -q "organizationIds" "/Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service/src/main/java/com/propertize/platform/auth/entity/User.java"; then
    print_status 0 "User entity has organizationIds field (JSONB)"
else
    print_status 1 "User entity missing organizationIds field"
fi

# Check for helper methods
if grep -q "public boolean isEnabled()" "/Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service/src/main/java/com/propertize/platform/auth/entity/User.java"; then
    print_status 0 "User entity has isEnabled() helper method"
else
    print_status 1 "User entity missing isEnabled() helper method"
fi

if grep -q "public boolean hasRole" "/Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service/src/main/java/com/propertize/platform/auth/entity/User.java"; then
    print_status 0 "User entity has hasRole() helper method"
else
    print_status 1 "User entity missing hasRole() helper method"
fi

echo ""
echo "7. Checking AuthController refactoring..."
# Check auth-service AuthController doesn't use OrganizationInfoService
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service
if ! grep -q "OrganizationInfoService" "src/main/java/com/propertize/platform/auth/controller/AuthController.java"; then
    print_status 0 "auth-service AuthController doesn't use OrganizationInfoService"
else
    print_status 1 "auth-service AuthController still references OrganizationInfoService"
fi

# Check if it uses user.getOrganizationIds()
if grep -q "getOrganizationIds()" "src/main/java/com/propertize/platform/auth/controller/AuthController.java"; then
    print_status 0 "auth-service AuthController uses user.getOrganizationIds()"
else
    print_status 1 "auth-service AuthController should use user.getOrganizationIds()"
fi

echo ""
echo "======================================"
echo "Verification Complete"
echo "======================================"
echo ""
echo "Summary:"
echo "- User entity: Exists in both services (auth-service MASTER, propertize READ-ONLY)"
echo "- Organization entity: Exists ONLY in propertize"
echo "- OrganizationInfoService: Removed from auth-service, kept in propertize"
echo "- UserRoleEnum: Copied to both services"
echo "- AuthController: Refactored to use user.organizationIds instead of OrganizationInfoService"
echo ""
echo "Next steps:"
echo "1. Rebuild both services: mvn clean package -DskipTests"
echo "2. Run ./reset-database.sh to drop and recreate database"
echo "3. Start services: docker-compose up -d"
echo "4. Run ./init-superadmin.sh to create superadmin user"
echo "5. Test login: curl -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{\"username\":\"superadmin\",\"password\":\"password\"}'"
