#!/bin/bash

# Service-to-Service Auth Test Script
# Tests the auth-service → propertize-service internal authentication flow

set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 Testing Service-to-Service Authentication"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Login via API Gateway (Full Flow)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 1: Login via API Gateway"
echo "This tests: Client → Gateway → Auth-Service → Propertize"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "ravishah",
    "password": "password",
    "rememberMe": false
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Test 1 PASSED${NC}"
    echo "HTTP Status: $HTTP_CODE"
    echo "Response Body:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

    # Extract tokens for further tests
    ACCESS_TOKEN=$(echo "$BODY" | jq -r '.accessToken' 2>/dev/null)
    REFRESH_TOKEN=$(echo "$BODY" | jq -r '.refreshToken' 2>/dev/null)
else
    echo -e "${RED}❌ Test 1 FAILED${NC}"
    echo "HTTP Status: $HTTP_CODE"
    echo "Response Body: $BODY"
    exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 2: Direct Internal Endpoint Call (With Service Headers)"
echo "This tests propertize internal endpoint with service-to-service headers"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8082/internal/auth/validate \
  -H "Content-Type: application/json" \
  -H "X-Service-Source: auth-service" \
  -H "X-Service-Target: propertize-service" \
  -d '{
    "identifier": "ravishah",
    "password": "password"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Test 2 PASSED${NC}"
    echo "HTTP Status: $HTTP_CODE"
    echo "Response Body:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    echo -e "${RED}❌ Test 2 FAILED${NC}"
    echo "HTTP Status: $HTTP_CODE"
    echo "Response Body: $BODY"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 3: Direct Internal Endpoint Call (Without Service Headers)"
echo "This should still work but log a warning"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8082/internal/auth/validate \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "ravishah",
    "password": "password"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${YELLOW}⚠️  Test 3 PASSED (with expected warning)${NC}"
    echo "HTTP Status: $HTTP_CODE"
    echo "Response Body:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    echo ""
    echo -e "${YELLOW}Note: Check logs for: '⚠️ Internal auth endpoint called without proper authentication headers'${NC}"
else
    echo -e "${RED}❌ Test 3 FAILED${NC}"
    echo "HTTP Status: $HTTP_CODE"
    echo "Response Body: $BODY"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 4: Token Refresh (If Test 1 succeeded)"
echo "This tests: Client → Gateway → Auth-Service → Propertize"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ -n "$REFRESH_TOKEN" ] && [ "$REFRESH_TOKEN" != "null" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/v1/auth/refresh \
      -H "Content-Type: application/json" \
      -d "{
        \"refreshToken\": \"$REFRESH_TOKEN\"
      }")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✅ Test 4 PASSED${NC}"
        echo "HTTP Status: $HTTP_CODE"
        echo "Response Body:"
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    else
        echo -e "${RED}❌ Test 4 FAILED${NC}"
        echo "HTTP Status: $HTTP_CODE"
        echo "Response Body: $BODY"
    fi
else
    echo -e "${YELLOW}⚠️  Test 4 SKIPPED (No refresh token from Test 1)${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 5: Get User Info (If Test 1 succeeded)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET http://localhost:8080/api/v1/auth/me \
      -H "Authorization: Bearer $ACCESS_TOKEN")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✅ Test 5 PASSED${NC}"
        echo "HTTP Status: $HTTP_CODE"
        echo "Response Body:"
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    else
        echo -e "${RED}❌ Test 5 FAILED${NC}"
        echo "HTTP Status: $HTTP_CODE"
        echo "Response Body: $BODY"
    fi
else
    echo -e "${YELLOW}⚠️  Test 5 SKIPPED (No access token from Test 1)${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🏁 Test Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Check the following log files for detailed flow:"
echo "1. Auth Service logs: /Users/ravishah/MySpace/MyWorkSpace/auth-service/logs/*.log"
echo "2. Propertize logs: /Users/ravishah/MySpace/MyWorkSpace/propertize/logs/*.log"
echo ""
echo "Expected log messages in Propertize:"
echo "  - '✅ Internal auth request from auth-service (service-to-service)'"
echo "  - '✅ Credentials validated for user: ravishah'"
echo ""
echo "Expected log messages in Auth Service:"
echo "  - 'Login attempt for user: ravishah from IP: 127.0.0.1'"
echo "  - '✅ User authenticated: ravishah with N roles and N permissions'"
echo ""

