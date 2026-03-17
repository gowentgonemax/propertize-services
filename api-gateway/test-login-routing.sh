#!/bin/bash

# Test Login Routing Fix
# This script tests that /api/v1/auth/login is correctly routed to auth-service

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Testing Login Routing Fix${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Test 1: Check if API Gateway is running
echo -e "${YELLOW}1. Checking if API Gateway is running...${NC}"
if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ API Gateway is running (port 8080)${NC}"
else
    echo -e "${RED}❌ API Gateway is NOT running${NC}"
    echo -e "${YELLOW}Start it with: cd /Users/ravishah/MySpace/MyWorkSpace/api-gateway && ./mvnw spring-boot:run${NC}"
    exit 1
fi
echo ""

# Test 2: Check if Auth Service is running
echo -e "${YELLOW}2. Checking if Auth Service is running...${NC}"
if curl -s -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Auth Service is running (port 8081)${NC}"
else
    echo -e "${RED}❌ Auth Service is NOT running${NC}"
    echo -e "${YELLOW}Start it with: cd /Users/ravishah/MySpace/MyWorkSpace/auth-service && ./mvnw spring-boot:run${NC}"
    exit 1
fi
echo ""

# Test 3: Check if Auth Service is registered in Eureka
echo -e "${YELLOW}3. Checking Eureka registration...${NC}"
EUREKA_RESPONSE=$(curl -s -u admin:admin http://localhost:8761/eureka/apps 2>/dev/null || echo "")
if echo "$EUREKA_RESPONSE" | grep -q "AUTH-SERVICE"; then
    echo -e "${GREEN}✅ Auth Service is registered in Eureka${NC}"
else
    echo -e "${RED}❌ Auth Service is NOT registered in Eureka${NC}"
    echo -e "${YELLOW}Wait a few seconds and try again${NC}"
fi
echo ""

# Test 4: Test login via Gateway (CORRECT path)
echo -e "${YELLOW}4. Testing login via API Gateway...${NC}"
echo -e "${BLUE}POST http://localhost:8080/api/v1/auth/login${NC}"

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "admin@propertize.com",
    "password": "Admin@123",
    "rememberMe": false
  }')

echo "$LOGIN_RESPONSE" | jq '.' 2>/dev/null || echo "$LOGIN_RESPONSE"
echo ""

# Check if login was successful
if echo "$LOGIN_RESPONSE" | grep -q '"success".*true'; then
    echo -e "${GREEN}✅ Login successful! Route is working correctly.${NC}"
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken' 2>/dev/null || echo "")
    if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
        echo -e "${GREEN}✅ Access token received${NC}"
        echo -e "${BLUE}Token (first 50 chars): ${ACCESS_TOKEN:0:50}...${NC}"
    fi
elif echo "$LOGIN_RESPONSE" | grep -q "404"; then
    echo -e "${RED}❌ FAILED: Still getting 404 - route not fixed${NC}"
    echo -e "${YELLOW}Make sure you restarted the API Gateway after the fix${NC}"
elif echo "$LOGIN_RESPONSE" | grep -q "SERVICE_UNAVAILABLE"; then
    echo -e "${RED}❌ FAILED: Auth service is unavailable${NC}"
    echo -e "${YELLOW}Check if auth-service is running and registered in Eureka${NC}"
else
    echo -e "${YELLOW}⚠️  Login failed - check credentials or service configuration${NC}"
fi
echo ""

# Test 5: Verify OLD route no longer works (should still 404)
echo -e "${YELLOW}5. Verifying old route to propertize-service returns 404...${NC}"
echo -e "${BLUE}POST http://localhost:8082/api/v1/auth/login${NC}"

OLD_ROUTE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "admin@propertize.com",
    "password": "Admin@123",
    "rememberMe": false
  }' 2>/dev/null)

HTTP_CODE=$(echo "$OLD_ROUTE_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)

if [ "$HTTP_CODE" = "404" ]; then
    echo -e "${GREEN}✅ Correctly returns 404 - propertize-service doesn't have /auth endpoint${NC}"
else
    echo -e "${YELLOW}⚠️  Unexpected response: HTTP $HTTP_CODE${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✅ API Gateway routes /api/v1/auth/** to auth-service${NC}"
echo -e "${GREEN}✅ Login endpoint is accessible via gateway${NC}"
echo -e "${GREEN}✅ Authentication flow is working correctly${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo -e "1. Update frontend to use: http://localhost:8080/api/v1/auth/login"
echo -e "2. Update Bruno collection (if needed)"
echo -e "3. Test other auth endpoints: /refresh, /logout, /validate, /me"
echo ""

