#!/bin/bash

# ============================================
# Propertize Local Development Stop Script
# ============================================

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$BASE_DIR"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PID_DIR="$BASE_DIR/.pids"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Stopping Propertize Local Services${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Function to stop service
stop_service() {
    local service_name=$1
    local pid_file="$PID_DIR/$service_name.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${YELLOW}Stopping $service_name (PID: $pid)...${NC}"
            kill $pid
            sleep 2
            if ps -p $pid > /dev/null 2>&1; then
                echo -e "${YELLOW}Force killing $service_name...${NC}"
                kill -9 $pid
            fi
            echo -e "${GREEN}✓${NC} $service_name stopped"
        else
            echo -e "${YELLOW}$service_name is not running${NC}"
        fi
        rm "$pid_file"
    else
        echo -e "${YELLOW}No PID file for $service_name${NC}"
    fi
}

# Stop services in reverse order
echo -e "${BLUE}Stopping application services...${NC}"
stop_service "frontend"
stop_service "api-gateway"
stop_service "employee-service"
stop_service "propertize"
stop_service "auth-service"
stop_service "service-registry"

echo ""
echo -e "${BLUE}Stopping infrastructure services (Docker)...${NC}"
docker-compose -f docker-compose.infra.yml down

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}All Services Stopped${NC}"
echo -e "${GREEN}============================================${NC}"

# Cleanup
if [ -d "$PID_DIR" ]; then
    rm -rf "$PID_DIR"
fi
