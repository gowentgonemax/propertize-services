#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================="
echo "  Microservices Health Check"
echo "========================================="
echo ""

# Function to check if port is open
check_port() {
    local port=$1
    local service=$2

    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo -e "${GREEN}✅ $service (port $port) is running${NC}"
        return 0
    else
        echo -e "${RED}❌ $service (port $port) is NOT running${NC}"
        return 1
    fi
}

# Function to check HTTP endpoint
check_endpoint() {
    local url=$1
    local service=$2

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)

    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
        echo -e "${GREEN}✅ $service responding (HTTP $HTTP_CODE)${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  $service returned HTTP $HTTP_CODE${NC}"
        return 1
    fi
}

echo "1️⃣  Checking Service Ports..."
echo "-------------------------------------------"
check_port 8761 "Eureka Server"
check_port 8080 "API Gateway"
check_port 8081 "Auth Service"
check_port 8082 "Propertize Service"
check_port 8083 "Employee Service"
echo ""

echo "2️⃣  Checking Database Ports..."
echo "-------------------------------------------"
check_port 5432 "PostgreSQL"
check_port 3306 "MySQL"
check_port 6379 "Redis"
echo ""

echo "3️⃣  Checking Service Health Endpoints..."
echo "-------------------------------------------"
check_endpoint "http://localhost:8761/actuator/health" "Eureka Health"
check_endpoint "http://localhost:8080/actuator/health" "Gateway Health"
check_endpoint "http://localhost:8081/api/v1/auth/health" "Auth Health"
check_endpoint "http://localhost:8082/api/v1/health" "Propertize Health"
check_endpoint "http://localhost:8083/actuator/health" "Employee Health"
echo ""

echo "4️⃣  Checking Eureka Service Registration..."
echo "-------------------------------------------"

EUREKA_RESPONSE=$(curl -s "http://admin:admin@localhost:8761/eureka/apps" 2>/dev/null)

if echo "$EUREKA_RESPONSE" | grep -q "API-GATEWAY"; then
    echo -e "${GREEN}✅ API-GATEWAY registered${NC}"
else
    echo -e "${RED}❌ API-GATEWAY not registered${NC}"
fi

if echo "$EUREKA_RESPONSE" | grep -q "AUTH-SERVICE"; then
    echo -e "${GREEN}✅ AUTH-SERVICE registered${NC}"
else
    echo -e "${RED}❌ AUTH-SERVICE not registered${NC}"
fi

if echo "$EUREKA_RESPONSE" | grep -q "PROPERTIZE-SERVICE"; then
    echo -e "${GREEN}✅ PROPERTIZE-SERVICE registered${NC}"
else
    echo -e "${RED}❌ PROPERTIZE-SERVICE not registered${NC}"
fi

if echo "$EUREKA_RESPONSE" | grep -q "EMPLOYECRAFT"; then
    echo -e "${GREEN}✅ EMPLOYECRAFT-SERVICE registered${NC}"
else
    echo -e "${RED}❌ EMPLOYECRAFT-SERVICE not registered${NC}"
fi

if echo "$EUREKA_RESPONSE" | grep -q "WAGECRAFT-SERVICE"; then
    echo -e "${GREEN}✅ WAGECRAFT-SERVICE registered${NC}"
else
    echo -e "${RED}❌ WAGECRAFT-SERVICE not registered${NC}"
fi
echo ""

echo "5️⃣  Testing Gateway Routing..."
echo "-------------------------------------------"

# Test auth routing through gateway
echo -e "${BLUE}Testing: POST http://localhost:8080/api/v1/auth/login${NC}"
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"test@test.com","password":"wrongpassword"}' 2>&1)

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n 1)
if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    echo -e "${GREEN}✅ Gateway → Auth Service routing works (HTTP $HTTP_CODE)${NC}"
elif [ "$HTTP_CODE" = "503" ]; then
    echo -e "${RED}❌ Service unavailable - Auth service may not be registered${NC}"
elif [ "$HTTP_CODE" = "000" ]; then
    echo -e "${RED}❌ Cannot connect to gateway on port 8080${NC}"
else
    echo -e "${YELLOW}⚠️  Unexpected response: HTTP $HTTP_CODE${NC}"
fi
echo ""

echo "6️⃣  Configuration Check..."
echo "-------------------------------------------"

# Check if JWT secret is consistent
echo -e "${BLUE}Checking JWT secret configuration...${NC}"
if [ -z "$JWT_SECRET" ]; then
    echo -e "${YELLOW}⚠️  JWT_SECRET environment variable not set${NC}"
    echo -e "   Using default: dev-jwt-secret-key-change-in-production-min-32-chars"
else
    echo -e "${GREEN}✅ JWT_SECRET environment variable is set${NC}"
fi
echo ""

echo "========================================="
echo "  Summary"
echo "========================================="
echo ""
echo -e "${BLUE}Expected Port Usage:${NC}"
echo "  • 8761 - Eureka Dashboard"
echo "  • 8080 - API Gateway (CLIENT ENTRY POINT)"
echo "  • 8081 - Auth Service (internal)"
echo "  • 8082 - Propertize (internal)"
echo "  • 8083 - Employee (internal)"
echo ""
echo -e "${YELLOW}Remember:${NC}"
echo "  1. Clients should ONLY use port 8080 (Gateway)"
echo "  2. Start Eureka FIRST, then other services"
echo "  3. Wait 10-15 seconds for service registration"
echo "  4. Check Eureka dashboard to verify registration"
echo ""

