#!/bin/bash
# Test Auth Service Routing
# This script tests that the API Gateway correctly routes auth requests to auth-service
set -e
# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}API Gateway - Auth Routing Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
# Test login credentials
LOGIN_PAYLOAD='{
  "identifier": "admin@propertize.com",
  "password": "Admin@123",
  "rememberMe": false
}'
# ==============================================================================
# 1. Check Service Registry (Eureka)
# ==============================================================================
echo -e "${YELLOW}1. Checking Eureka Service Registry...${NC}"
echo -e "${BLUE}   Request: GET http://localhost:8761/eureka/apps${NC}"
echo ""
EUREKA_RESPONSE=$(curl -s http://admin:admin@localhost:8761/eureka/apps \
  -H "Accept: application/json" 2>&1)
if echo "$EUREKA_RESPONSE" | grep -q "auth-service"; then
    echo -e "   ${GREEN}✅ auth-service is registered with Eureka${NC}"
else
    echo -e "   ${RED}❌ auth-service is NOT registered with Eureka${NC}"
    echo -e "   ${YELLOW}   Please start auth-service first${NC}"
fi
if echo "$EUREKA_RESPONSE" | grep -q "api-gateway"; then
    echo -e "   ${GREEN}✅ api-gateway is registered with Eureka${NC}"
else
    echo -e "   ${RED}❌ api-gateway is NOT registered with Eureka${NC}"
    echo -e "   ${YELLOW}   Please start api-gateway first${NC}"
fi
echo ""
# ==============================================================================
# 2. Test direct access to auth-service (debugging)
# ==============================================================================
echo -e "${YELLOW}2. Testing direct access to Auth Service (port 8081)...${NC}"
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
    if echo "$BODY_DIRECT" | jq -e '.accessToken' > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Access token received${NC}"
    fi
elif [ "$HTTP_CODE_DIRECT" = "000" ]; then
    echo -e "   ${RED}❌ Cannot connect to auth service on port 8081${NC}"
    echo -e "   ${YELLOW}   Is auth-service running?${NC}"
else
    echo -e "   ${RED}❌ Direct login failed with status $HTTP_CODE_DIRECT${NC}"
    echo -e "   Response: $BODY_DIRECT"
fi
echo ""
# ==============================================================================
# 3. Test login through API Gateway
# ==============================================================================
echo -e "${YELLOW}3. Testing login through API Gateway (port 8080)...${NC}"
echo -e "${BLUE}   Request: POST http://localhost:8080/api/v1/auth/login${NC}"
echo ""
RESPONSE_GATEWAY=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "$LOGIN_PAYLOAD" 2>&1)
HTTP_CODE_GATEWAY=$(echo "$RESPONSE_GATEWAY" | tail -n 1)
BODY_GATEWAY=$(echo "$RESPONSE_GATEWAY" | sed '$d')
echo -e "   HTTP Status: $HTTP_CODE_GATEWAY"
if [ "$HTTP_CODE_GATEWAY" = "200" ]; then
    echo -e "   ${GREEN}✅ Gateway login successful!${NC}"
    if echo "$BODY_GATEWAY" | jq -e '.accessToken' > /dev/null 2>&1; then
        echo -e "   ${GREEN}✅ Access token received${NC}"
        TOKEN=$(echo "$BODY_GATEWAY" | jq -r '.accessToken')
        echo -e "   ${BLUE}   Token preview: ${TOKEN:0:50}...${NC}"
    fi
elif [ "$HTTP_CODE_GATEWAY" = "404" ]; then
    echo -e "   ${RED}❌ Gateway routing failed - 404 Not Found${NC}"
    echo -e "   ${YELLOW}   This means the gateway is routing to the wrong service${NC}"
    echo -e "   ${YELLOW}   Check GatewayRouteConfig.java${NC}"
    echo -e "   Response: $BODY_GATEWAY"
elif [ "$HTTP_CODE_GATEWAY" = "503" ]; then
    echo -e "   ${RED}❌ Service unavailable - 503${NC}"
    echo -e "   ${YELLOW}   Auth service may not be registered with Eureka${NC}"
    echo -e "   Response: $BODY_GATEWAY"
elif [ "$HTTP_CODE_GATEWAY" = "000" ]; then
    echo -e "   ${RED}❌ Cannot connect to API Gateway on port 8080${NC}"
    echo -e "   ${YELLOW}   Is api-gateway running?${NC}"
else
    echo -e "   ${RED}❌ Gateway login failed with status $HTTP_CODE_GATEWAY${NC}"
    echo -e "   Response: $BODY_GATEWAY"
fi
echo ""
# ==============================================================================
# 4. Summary
# ==============================================================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
if [ "$HTTP_CODE_GATEWAY" = "200" ] && [ "$HTTP_CODE_DIRECT" = "200" ]; then
    echo -e "${GREEN}✅ All tests passed!${NC}"
    echo -e "${GREEN}   Auth routing is working correctly${NC}"
elif [ "$HTTP_CODE_DIRECT" = "200" ] && [ "$HTTP_CODE_GATEWAY" != "200" ]; then
    echo -e "${RED}❌ Auth service works but gateway routing fails${NC}"
    echo -e "${YELLOW}   Action: Check GatewayRouteConfig.java${NC}"
    echo -e "${YELLOW}   Ensure route is: .uri(\"lb://auth-service\")${NC}"
elif [ "$HTTP_CODE_DIRECT" != "200" ] && [ "$HTTP_CODE_GATEWAY" != "200" ]; then
    echo -e "${RED}❌ Auth service is not working${NC}"
    echo -e "${YELLOW}   Action: Check auth-service logs and database connection${NC}"
else
    echo -e "${YELLOW}⚠️  Partial success - check above for details${NC}"
fi
echo ""
echo -e "${BLUE}Expected flow:${NC}"
echo -e "${BLUE}  Client → Gateway (8080) → Eureka lookup → Auth Service (8081)${NC}"
echo ""
