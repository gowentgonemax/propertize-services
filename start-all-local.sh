#!/bin/bash

# ============================================
# Propertize Local Development Startup Script
# ============================================

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$BASE_DIR"

# Load root .env so REDIS_PASSWORD, MONGO_PASSWORD etc. are available to Java services
# BUG FIX: without this, auth-service would connect to Redis without a password
if [ -f "$BASE_DIR/.env" ]; then
    set -a
    source "$BASE_DIR/.env"
    set +a
fi

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# PID files directory
PID_DIR="$BASE_DIR/.pids"
mkdir -p "$PID_DIR"

# Set Java 21 as required by the project
if [ -x "/usr/libexec/java_home" ]; then
    # macOS
    export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 21.0 2>/dev/null || /usr/libexec/java_home)
elif [ -d "/usr/lib/jvm/java-21-openjdk" ]; then
    # Linux - OpenJDK
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
elif [ -d "/usr/lib/jvm/java-21" ]; then
    # Linux - Alternative path
    export JAVA_HOME=/usr/lib/jvm/java-21
fi

# Verify Java version
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}ERROR: Java 21 or higher is required. Current version: $JAVA_VERSION${NC}"
    echo -e "${YELLOW}Please install Java 21 and try again.${NC}"
    exit 1
fi

echo -e "${GREEN}Using Java: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)${NC}"
echo ""

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Propertize Local Development Startup${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Function to check if service is running
check_service() {
    local service_name=$1
    local port=$2
    if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $service_name is running on port $port"
        return 0
    else
        echo -e "${RED}✗${NC} $service_name is not responding on port $port"
        return 1
    fi
}

# Function to wait for service
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo -e "${YELLOW}Waiting for $service_name on port $port...${NC}"
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC} $service_name is ready!"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    echo -e "${RED}✗${NC} $service_name failed to start"
    return 1
}

# Step 1: Start Infrastructure
echo -e "${BLUE}[1/7]${NC} Starting infrastructure services (Docker)..."
docker-compose -f docker-compose.infra.yml up -d

echo -e "${YELLOW}Waiting for infrastructure to be healthy...${NC}"
sleep 10

# Check PostgreSQL
echo -e "${YELLOW}Checking local PostgreSQL...${NC}"
if psql -h localhost -U dbuser -d propertize_db -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} PostgreSQL is ready"
else
    echo -e "${RED}✗${NC} PostgreSQL is not accessible. Please check LOCAL_DEVELOPMENT_GUIDE.md"
    exit 1
fi

# Step 2: Start Service Registry
echo ""
echo -e "${BLUE}[2/7]${NC} Starting Service Registry (Eureka)..."
cd "$BASE_DIR/service-registry"
if [ ! -f "target/service-registry"*".jar" ]; then
    echo -e "${YELLOW}Building service-registry...${NC}"
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests -q
    else
        mvn clean package -DskipTests -q
    fi
fi
nohup "$JAVA_HOME/bin/java" -jar target/service-registry-*.jar --spring.profiles.active=local > "$BASE_DIR/logs/service-registry.log" 2>&1 &
echo $! > "$PID_DIR/service-registry.pid"
wait_for_service "Service Registry" 8761

# Step 3: Start Auth Service
echo ""
echo -e "${BLUE}[3/7]${NC} Starting Auth Service..."
cd "$BASE_DIR/auth-service"
if [ ! -f "target/auth-service"*".jar" ]; then
    echo -e "${YELLOW}Building auth-service...${NC}"
    if [ -f "pom.xml" ]; then
        mvn clean package -DskipTests -q
    fi
fi
nohup "$JAVA_HOME/bin/java" -jar target/auth-service-*.jar --spring.profiles.active=local > "$BASE_DIR/logs/auth-service.log" 2>&1 &
echo $! > "$PID_DIR/auth-service.pid"
wait_for_service "Auth Service" 8081

# Step 4: Start Propertize Main Service
echo ""
echo -e "${BLUE}[4/7]${NC} Starting Propertize Main Service..."
cd "$BASE_DIR/propertize"
if [ ! -f "target/propertize"*".jar" ]; then
    echo -e "${YELLOW}Building propertize...${NC}"
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests -q
    else
        mvn clean package -DskipTests -q
    fi
fi
nohup "$JAVA_HOME/bin/java" -jar target/propertize-*.jar --spring.profiles.active=local > "$BASE_DIR/logs/propertize.log" 2>&1 &
echo $! > "$PID_DIR/propertize.pid"
wait_for_service "Propertize Service" 8082

# Step 5: Start Employee Service
echo ""
echo -e "${BLUE}[5/7]${NC} Starting Employee Service..."
cd "$BASE_DIR/employee-service"
if [ ! -f "target/employecraft"*".jar" ]; then
    echo -e "${YELLOW}Building employee-service...${NC}"
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests -q
    else
        mvn clean package -DskipTests -q
    fi
fi
nohup "$JAVA_HOME/bin/java" -jar target/employecraft-*.jar --spring.profiles.active=local > "$BASE_DIR/logs/employee-service.log" 2>&1 &
echo $! > "$PID_DIR/employee-service.pid"
# BUG FIX: wait_for_service call was missing; 'waitif' orphan caused a bash syntax error
wait_for_service "Employee Service" 8083

# Step 6: Start API Gateway
echo ""
echo -e "${BLUE}[6/7]${NC} Starting API Gateway..."
cd "$BASE_DIR/api-gateway"
if [ ! -f "target/api-gateway"*".jar" ]; then
    echo -e "${YELLOW}Building api-gateway...${NC}"
    # BUG FIX: was "$JAVA_HOME/bin/mvn" — Maven is NOT inside JAVA_HOME
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests -q
    else
        mvn clean package -DskipTests -q
    fi
fi
nohup "$JAVA_HOME/bin/java" -jar target/api-gateway-*.jar --spring.profiles.active=local > "$BASE_DIR/logs/api-gateway.log" 2>&1 &
echo $! > "$PID_DIR/api-gateway.pid"
wait_for_service "API Gateway" 8080

# Step 7: Start Frontend
echo ""
echo -e "${BLUE}[7/7]${NC} Starting Frontend..."
cd "$BASE_DIR/propertize-front-end"
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing npm dependencies...${NC}"
    npm install
fi
nohup npm run dev > "$BASE_DIR/logs/frontend.log" 2>&1 &
echo $! > "$PID_DIR/frontend.pid"

echo -e "${YELLOW}Waiting for frontend to start...${NC}"
sleep 10

# Summary
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}All Services Started Successfully!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo -e "  Service Registry: ${YELLOW}http://localhost:8761${NC} (admin/admin)"
echo -e "  Auth Service:     ${YELLOW}http://localhost:8081${NC}"
echo -e "  Propertize:       ${YELLOW}http://localhost:8082${NC}"
echo -e "  Employee Service: ${YELLOW}http://localhost:8083${NC}"
echo -e "  API Gateway:      ${YELLOW}http://localhost:8080${NC}"
echo -e "  Frontend:         ${YELLOW}http://localhost:3000${NC}"
echo ""
echo -e "${BLUE}Infrastructure URLs:${NC}"
echo -e "  Kafka UI:         ${YELLOW}http://localhost:8090${NC}"
echo -e "  Mongo Express:    ${YELLOW}http://localhost:8089${NC} (admin/admin)"
echo ""
echo -e "${BLUE}Logs:${NC}"
echo -e "  All logs are in: ${YELLOW}$BASE_DIR/logs/${NC}"
echo -e "  View: ${YELLOW}tail -f logs/<service-name>.log${NC}"
echo ""
echo -e "${BLUE}To stop all services:${NC}"
echo -e "  ${YELLOW}./stop-all-local.sh${NC}"
echo ""
