#!/bin/bash

# Service Registry Quick Test Script
# This script verifies the Eureka service registry is working correctly

echo "=========================================="
echo "Eureka Service Registry - Quick Test"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
EUREKA_URL="http://localhost:8761"
EUREKA_USERNAME="admin"
EUREKA_PASSWORD="admin123"

echo "Testing Eureka Server..."
echo "URL: $EUREKA_URL"
echo ""

# Test 1: Health Check (No Auth)
echo -n "1. Testing Health Endpoint (no auth)... "
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$EUREKA_URL/actuator/health")
if [ "$HEALTH_RESPONSE" == "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP $HEALTH_RESPONSE)"
else
    echo -e "${RED}✗ FAIL${NC} (HTTP $HEALTH_RESPONSE)"
fi

# Test 2: Dashboard Access (With Auth)
echo -n "2. Testing Dashboard Access (with auth)... "
DASHBOARD_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -u "$EUREKA_USERNAME:$EUREKA_PASSWORD" "$EUREKA_URL/")
if [ "$DASHBOARD_RESPONSE" == "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP $DASHBOARD_RESPONSE)"
else
    echo -e "${RED}✗ FAIL${NC} (HTTP $DASHBOARD_RESPONSE)"
fi

# Test 3: Apps Endpoint (With Auth)
echo -n "3. Testing Apps Endpoint (with auth)... "
APPS_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -u "$EUREKA_USERNAME:$EUREKA_PASSWORD" "$EUREKA_URL/eureka/apps")
if [ "$APPS_RESPONSE" == "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP $APPS_RESPONSE)"
else
    echo -e "${RED}✗ FAIL${NC} (HTTP $APPS_RESPONSE)"
fi

# Test 4: Dashboard Without Auth (Should Fail)
echo -n "4. Testing Dashboard Without Auth (should fail)... "
NO_AUTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$EUREKA_URL/")
if [ "$NO_AUTH_RESPONSE" == "401" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP $NO_AUTH_RESPONSE - Correctly requiring auth)"
else
    echo -e "${YELLOW}⚠ WARNING${NC} (HTTP $NO_AUTH_RESPONSE - Expected 401)"
fi

echo ""
echo "=========================================="
echo "Registered Services:"
echo "=========================================="

# Get registered services
SERVICES=$(curl -s -u "$EUREKA_USERNAME:$EUREKA_PASSWORD" -H "Accept: application/json" "$EUREKA_URL/eureka/apps" | grep -o '"app":"[^"]*"' | cut -d'"' -f4 | sort -u)

if [ -z "$SERVICES" ]; then
    echo -e "${YELLOW}No services registered yet${NC}"
else
    echo "$SERVICES"
fi

echo ""
echo "=========================================="
echo "Access Information:"
echo "=========================================="
echo "Dashboard URL: $EUREKA_URL"
echo "Username: $EUREKA_USERNAME"
echo "Password: $EUREKA_PASSWORD"
echo ""
echo -e "${GREEN}Open http://localhost:8761 in your browser${NC}"
echo "Enter credentials when prompted"
echo ""

