#!/bin/bash

# ==============================================================================
# Auth Service - Login Test Script
# ==============================================================================
# This script tests the login flow through the API Gateway
#
# Requirements:
#   - API Gateway running on port 8080
#   - Auth Service running on port 8081
#   - Eureka Server running on port 8761
# ==============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Auth Service Login Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# ==============================================================================
# 1. Check if services are running
# ==============================================================================
echo -e "${YELLOW}1. Checking if services are running...${NC}"

check_service() {
    local name=$1
    local url=$2

    if curl -s -f "$url" > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ $name is running${NC}"
        return 0
    else
        echo -e "   ${RED}❌ $name is NOT running${NC}"
        echo -e "   ${RED}   URL: $url${NC}"
        return 1
    fi
}

# Check API Gateway
if ! check_service "API Gateway" "http://localhost:8080/actuator/health"; then
    echo -e "${RED}Please start API Gateway: cd /Users/ravishah/MySpace/MyWorkSpace/api-gateway && mvn spring-boot:run${NC}"
    exit 1
fi

# Check Auth Service
if ! check_service "Auth Service" "http://localhost:8081/actuator/health"; then
    echo -e "${RED}Please start Auth Service: cd /Users/ravishah/MySpace/MyWorkSpace/auth-service && mvn spring-boot:run${NC}"
    exit 1
fi

# Check Eureka Server
if ! check_service "Eureka Server" "http://localhost:8761/actuator/health"; then
    echo -e "${YELLOW}⚠️  Eureka Server is not running (optional)${NC}"
fi

echo ""

# ==============================================================================
# 2. Test login through API Gateway (CORRECT WAY)
# ==============================================================================
echo -e "${YELLOW}2. Testing login through API Gateway (port 8080)...${NC}"

LOGIN_PAYLOAD='{
  "identifier": "admin@propertize.com",
  "password": "Admin@123",
  "rememberMe": false
}'

echo -e "${BLUE}   Request: POST http://localhost:8080/api/v1/auth/login${NC}"
echo -e "${BLUE}   Payload: $LOGIN_PAYLOAD${NC}"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "$LOGIN_PAYLOAD" 2>&1)

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo -e "   HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "   ${GREEN}✅ Login successful!${NC}"

    # Extract and display tokens
    if command -v jq &> /dev/null; then
        ACCESS_TOKEN=$(echo "$BODY" | jq -r '.accessToken' 2>/dev/null)
        REFRESH_TOKEN=$(echo "$BODY" | jq -r '.refreshToken' 2>/dev/null)
        USERNAME=$(echo "$BODY" | jq -r '.user.username' 2>/dev/null)

        if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
            echo -e "   ${GREEN}   Username: $USERNAME${NC}"
            echo -e "   ${GREEN}   Access Token: ${ACCESS_TOKEN:0:50}...${NC}"
            echo -e "   ${GREEN}   Refresh Token: ${REFRESH_TOKEN:0:50}...${NC}"

            # Save tokens for other tests
            echo "$ACCESS_TOKEN" > /tmp/access_token.txt
            echo "$REFRESH_TOKEN" > /tmp/refresh_token.txt
            echo -e "   ${GREEN}   Tokens saved to /tmp/access_token.txt and /tmp/refresh_token.txt${NC}"
        else
            echo -e "   ${YELLOW}   Response: $BODY${NC}"
        fi
    else
        echo -e "   ${YELLOW}   Install 'jq' to see parsed response${NC}"
        echo -e "   Response: $BODY"
    fi
elif [ "$HTTP_CODE" = "401" ]; then
    echo -e "   ${RED}❌ Login failed: Invalid credentials${NC}"
    echo -e "   Response: $BODY"
elif [ "$HTTP_CODE" = "403" ]; then
    echo -e "   ${RED}❌ Login failed: Forbidden (403)${NC}"
    echo -e "   ${RED}   This means the security configuration is blocking the request${NC}"
    echo -e "   Response: $BODY"
elif [ "$HTTP_CODE" = "404" ]; then
    echo -e "   ${RED}❌ Login failed: Not Found (404)${NC}"
    echo -e "   ${RED}   The gateway is not routing /api/v1/auth/** to auth-service${NC}"
    echo -e "   Response: $BODY"
else
    echo -e "   ${RED}❌ Login failed with HTTP $HTTP_CODE${NC}"
    echo -e "   Response: $BODY"
fi

echo ""

# ==============================================================================
# 3. Test direct login to auth-service (DEBUGGING ONLY)
# ==============================================================================
echo -e "${YELLOW}3. Testing direct login to Auth Service (port 8081)...${NC}"
echo -e "${BLUE}   Request: POST http://localhost:8081/api/v1/auth/login${NC}"
echo ""

RESPONSE_DIRECT=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "$LOGIN_PAYLOAD" 2>&1)

HTTP_CODE_DIRECT=$(echo "$RESPONSE_DIRECT" | tail -n 1)
BODY_DIRECT=$(echo "$RESPONSE_DIRECT" | sed '$d')

echo -e "   HTTP Status: $HTTP_CODE_DIRECT"

if [ "$HTTP_CODE_DIRECT" = "200" ]; then
    echo -e "   ${GREEN}✅ Direct auth service login successful!${NC}"
elif [ "$HTTP_CODE_DIRECT" = "000" ]; then
    echo -e "   ${RED}❌ Cannot connect to auth service on port 8081${NC}"
    echo -e "   ${RED}   Is the auth service running?${NC}"
else
    echo -e "   ${RED}❌ Direct login failed with HTTP $HTTP_CODE_DIRECT${NC}"
    echo -e "   Response: $BODY_DIRECT"
fi

echo ""

# ==============================================================================
# 4. Test wrong port (Propertize) to verify fix
# ==============================================================================
echo -e "${YELLOW}4. Testing wrong port (Propertize - port 8082) to verify it's now permitted...${NC}"
echo -e "${BLUE}   Request: POST http://localhost:8082/api/v1/auth/login${NC}"
echo ""

RESPONSE_WRONG=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "$LOGIN_PAYLOAD" 2>&1)

HTTP_CODE_WRONG=$(echo "$RESPONSE_WRONG" | tail -n 1)

echo -e "   HTTP Status: $HTTP_CODE_WRONG"

if [ "$HTTP_CODE_WRONG" = "404" ]; then
    echo -e "   ${GREEN}✅ Good! Propertize returns 404 (no handler) instead of 403 (forbidden)${NC}"
elif [ "$HTTP_CODE_WRONG" = "403" ]; then
    echo -e "   ${RED}❌ Still getting 403. Propertize needs to be restarted to apply security config changes.${NC}"
elif [ "$HTTP_CODE_WRONG" = "000" ]; then
    echo -e "   ${YELLOW}⚠️  Cannot connect to Propertize on port 8082${NC}"
else
    echo -e "   ${YELLOW}⚠️  Unexpected status: $HTTP_CODE_WRONG${NC}"
fi

echo ""

# ==============================================================================
# Summary
# ==============================================================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ API Gateway Login: SUCCESS${NC}"
else
    echo -e "${RED}❌ API Gateway Login: FAILED${NC}"
fi

if [ "$HTTP_CODE_DIRECT" = "200" ]; then
    echo -e "${GREEN}✅ Direct Auth Service Login: SUCCESS${NC}"
else
    echo -e "${RED}❌ Direct Auth Service Login: FAILED${NC}"
fi

echo ""
echo -e "${YELLOW}💡 Remember:${NC}"
echo -e "   • Always use API Gateway (port 8080) for client requests"
echo -e "   • Direct service access (ports 8081-8083) is for debugging only"
echo -e "   • JWT tokens are validated at the gateway, not in individual services"
echo ""

# Exit with appropriate code
if [ "$HTTP_CODE" = "200" ]; then
    exit 0
else
    exit 1
fi

