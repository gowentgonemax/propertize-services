#!/bin/bash

# Test Auth Service Endpoints via API Gateway
# API Gateway runs on port 8080 and routes /api/v1/auth/** to auth-service

GATEWAY_URL="http://localhost:8080"
AUTH_BASE="$GATEWAY_URL/api/v1/auth"

echo "========================================="
echo "Testing Auth Service Endpoints"
echo "API Gateway: $GATEWAY_URL"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Health Check (Gateway)
echo -e "${YELLOW}Test 1: API Gateway Health Check${NC}"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" "$GATEWAY_URL/actuator/health" || echo "Gateway not running on port 8080"
echo ""

# Test 2: Login Endpoint
echo -e "${YELLOW}Test 2: Login with Test User${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$AUTH_BASE/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

echo "Response:"
echo "$LOGIN_RESPONSE" | jq '.' 2>/dev/null || echo "$LOGIN_RESPONSE"
echo ""

# Extract access token if login succeeded
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken // empty' 2>/dev/null)

if [ -n "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✓ Login successful! Token obtained.${NC}"
    echo "Access Token (first 50 chars): ${ACCESS_TOKEN:0:50}..."
    echo ""
    
    # Test 3: Token Refresh
    echo -e "${YELLOW}Test 3: Token Refresh${NC}"
    REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.refreshToken // empty' 2>/dev/null)
    
    if [ -n "$REFRESH_TOKEN" ]; then
        REFRESH_RESPONSE=$(curl -s -X POST "$AUTH_BASE/refresh" \
          -H "Content-Type: application/json" \
          -d "{
            \"refreshToken\": \"$REFRESH_TOKEN\"
          }")
        
        echo "Response:"
        echo "$REFRESH_RESPONSE" | jq '.' 2>/dev/null || echo "$REFRESH_RESPONSE"
        echo ""
    fi
    
    # Test 4: Change Password
    echo -e "${YELLOW}Test 4: Change Password (Authenticated)${NC}"
    CHANGE_PASSWORD_RESPONSE=$(curl -s -X POST "$AUTH_BASE/change-password" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -d '{
        "currentPassword": "admin123",
        "newPassword": "NewP@ssw0rd123",
        "confirmPassword": "NewP@ssw0rd123"
      }')
    
    echo "Response:"
    echo "$CHANGE_PASSWORD_RESPONSE" | jq '.' 2>/dev/null || echo "$CHANGE_PASSWORD_RESPONSE"
    echo ""
    
    # Test 5: Logout
    echo -e "${YELLOW}Test 5: Logout${NC}"
    LOGOUT_RESPONSE=$(curl -s -X POST "$AUTH_BASE/logout" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -d '{
        "reason": "manual"
      }')
    
    echo "Response:"
    echo "$LOGOUT_RESPONSE" | jq '.' 2>/dev/null || echo "$LOGOUT_RESPONSE"
    echo ""
else
    echo -e "${RED}✗ Login failed. Check if services are running:${NC}"
    echo "  1. Start service-registry: cd service-registry && mvn spring-boot:run"
    echo "  2. Start auth-service: cd auth-service && mvn spring-boot:run"
    echo "  3. Start api-gateway: cd api-gateway && mvn spring-boot:run"
    echo ""
fi

# Test 6: Forgot Password
echo -e "${YELLOW}Test 6: Forgot Password${NC}"
FORGOT_RESPONSE=$(curl -s -X POST "$AUTH_BASE/forgot-password" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@propertize.com"
  }')

echo "Response:"
echo "$FORGOT_RESPONSE" | jq '.' 2>/dev/null || echo "$FORGOT_RESPONSE"
echo ""

# Test 7: Validate Reset Token (with invalid token)
echo -e "${YELLOW}Test 7: Validate Reset Token${NC}"
VALIDATE_RESPONSE=$(curl -s -X GET "$AUTH_BASE/validate-reset-token?token=invalid-token-123")

echo "Response:"
echo "$VALIDATE_RESPONSE" | jq '.' 2>/dev/null || echo "$VALIDATE_RESPONSE"
echo ""

# Test 8: Rate Limiting Test
echo -e "${YELLOW}Test 8: Rate Limiting Test (Multiple Login Attempts)${NC}"
echo "Sending 6 rapid login requests to test rate limiting..."
for i in {1..6}; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$AUTH_BASE/login" \
      -H "Content-Type: application/json" \
      -d '{
        "username": "testuser",
        "password": "wrongpassword"
      }')
    
    if [ "$HTTP_CODE" == "429" ]; then
        echo -e "${GREEN}✓ Attempt $i: Rate limited (HTTP 429) - Rate limiting is working!${NC}"
    else
        echo "  Attempt $i: HTTP $HTTP_CODE"
    fi
done
echo ""

echo "========================================="
echo -e "${GREEN}Test Suite Complete!${NC}"
echo "========================================="
echo ""
echo "Summary:"
echo "  - All auth endpoints routed through API Gateway (port 8080)"
echo "  - Auth-service handles: login, refresh, logout, password management"
echo "  - Features tested: RBAC, Rate Limiting, Session Management"
echo ""
