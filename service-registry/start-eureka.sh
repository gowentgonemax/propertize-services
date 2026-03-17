#!/bin/bash

# Quick Start Script for Eureka Service Registry
# This script helps you quickly start and verify the service registry

set -e  # Exit on error

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_DIR="/Users/ravishah/MySpace/MyWorkSpace/service-registry"
PORT=8761

echo -e "${BLUE}=========================================="
echo "Eureka Service Registry - Quick Start"
echo -e "==========================================${NC}"
echo ""

# Check if already running
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Port $PORT is already in use.${NC}"
    echo -e "${YELLOW}Stopping existing service...${NC}"
    kill $(lsof -t -i:$PORT) 2>/dev/null || true
    sleep 2
fi

cd "$PROJECT_DIR"

echo -e "${BLUE}Step 1: Building application...${NC}"
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Step 2: Starting Service Registry...${NC}"
echo -e "${YELLOW}This may take 20-30 seconds...${NC}"

# Start in background
nohup mvn spring-boot:run > eureka-startup.log 2>&1 &
EUREKA_PID=$!

echo "Process ID: $EUREKA_PID"

# Wait for service to start
MAX_WAIT=60
COUNTER=0
echo -n "Waiting for service to start"

while [ $COUNTER -lt $MAX_WAIT ]; do
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}✓ Service started successfully!${NC}"
        break
    fi
    echo -n "."
    sleep 1
    COUNTER=$((COUNTER + 1))
done

if [ $COUNTER -ge $MAX_WAIT ]; then
    echo ""
    echo -e "${RED}✗ Service failed to start within $MAX_WAIT seconds${NC}"
    echo -e "${YELLOW}Check eureka-startup.log for details${NC}"
    tail -20 eureka-startup.log
    exit 1
fi

sleep 2

echo ""
echo -e "${BLUE}Step 3: Verifying endpoints...${NC}"

# Test health endpoint
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/actuator/health 2>/dev/null || echo "000")
if [ "$HEALTH_STATUS" == "200" ]; then
    echo -e "${GREEN}✓ Health endpoint: OK${NC}"
else
    echo -e "${RED}✗ Health endpoint: Failed (HTTP $HEALTH_STATUS)${NC}"
fi

# Test dashboard (should require auth)
DASHBOARD_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/ 2>/dev/null || echo "000")
if [ "$DASHBOARD_STATUS" == "401" ]; then
    echo -e "${GREEN}✓ Dashboard security: OK (Auth required)${NC}"
elif [ "$DASHBOARD_STATUS" == "200" ]; then
    echo -e "${YELLOW}⚠ Dashboard accessible without auth${NC}"
else
    echo -e "${RED}✗ Dashboard: Not accessible (HTTP $DASHBOARD_STATUS)${NC}"
fi

echo ""
echo -e "${BLUE}=========================================="
echo "Service Registry Started Successfully!"
echo -e "==========================================${NC}"
echo ""
echo -e "${GREEN}Dashboard URL:${NC} http://localhost:$PORT"
echo -e "${GREEN}Credentials:${NC}"
echo "  Username: admin"
echo "  Password: admin123"
echo ""
echo -e "${BLUE}Health Check:${NC} http://localhost:$PORT/actuator/health"
echo -e "${BLUE}Registered Apps:${NC} http://localhost:$PORT/eureka/apps"
echo ""
echo -e "${YELLOW}Logs:${NC} $PROJECT_DIR/eureka-startup.log"
echo -e "${YELLOW}Process ID:${NC} $EUREKA_PID"
echo ""
echo -e "${BLUE}To stop the service:${NC}"
echo "  kill $EUREKA_PID"
echo "  OR"
echo "  lsof -t -i:$PORT | xargs kill"
echo ""
echo -e "${GREEN}Open http://localhost:$PORT in your browser to access the dashboard${NC}"
echo ""

# Save PID for later
echo $EUREKA_PID > eureka.pid

