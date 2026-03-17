# Running Propertize Services Locally

This guide explains how to run all Propertize services locally while using Docker only for infrastructure components.

## Architecture

**Infrastructure (Docker):**

- MongoDB (port 27017)
- Redis (port 6379)
- Kafka + Zookeeper (port 9092)
- Kafka UI (port 8090)
- Mongo Express (port 8089)

**Services (Local):**

- Service Registry (Eureka) - port 8761
- Auth Service - port 8081
- Propertize Main Service - port 8082
- Employee Service - port 8083
- API Gateway - port 8080
- Frontend (Next.js) - port 3000

**Database (Local):**

- PostgreSQL - port 5432

---

## Prerequisites

### 1. Install PostgreSQL Locally

**macOS (via Homebrew):**

```bash
brew install postgresql@16
brew services start postgresql@16
```

**Ubuntu/Debian:**

```bash
sudo apt update
sudo apt install postgresql-16
sudo systemctl start postgresql
```

**Windows:**
Download and install from https://www.postgresql.org/download/windows/

### 2. Create Database and User

```bash
# Connect to PostgreSQL
psql postgres

# Create user
CREATE USER dbuser WITH PASSWORD 'dbpassword';

# Create database
CREATE DATABASE propertize_db OWNER dbuser;

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE propertize_db TO dbuser;

# Connect to the database
\c propertize_db

# Grant schema privileges
GRANT ALL ON SCHEMA public TO dbuser;

# Exit
\q
```

### 3. Verify PostgreSQL Connection

```bash
psql -h localhost -U dbuser -d propertize_db
```

### 4. Install Java and Maven

```bash
# Verify Java 17+
java -version

# Verify Maven
mvn -version
```

### 5. Install Node.js for Frontend

```bash
# Verify Node.js 18+
node -version
npm -version
```

---

## Step 1: Start Infrastructure Services

```bash
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services

# Start only infrastructure services
docker-compose -f docker-compose.infra.yml up -d

# Verify services are running
docker-compose -f docker-compose.infra.yml ps

# Check logs
docker-compose -f docker-compose.infra.yml logs -f
```

**Expected Output:**

```
propertize-mongodb         Up (healthy)
propertize-redis           Up (healthy)
propertize-zookeeper       Up
propertize-kafka           Up (healthy)
propertize-kafka-ui        Up
propertize-mongo-express   Up
```

**Access UIs:**

- Kafka UI: http://localhost:8090
- Mongo Express: http://localhost:8089 (admin/admin)

---

## Step 2: Start Service Registry (Eureka)

```bash
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/service-registry

# Build
mvn clean package -DskipTests

# Run with local profile
java -jar target/service-registry-*.jar --spring.profiles.active=local
```

**Verify:**

- Open http://localhost:8761
- Login with admin/admin
- Should see Eureka dashboard

---

## Step 3: Start Auth Service

```bash
# Open new terminal
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/auth-service

# Build
mvn clean package -DskipTests

# Run with local profile
java -jar target/auth-service-*.jar --spring.profiles.active=local
```

**Verify:**

- Check http://localhost:8081/actuator/health
- Should return `{"status":"UP"}`
- Check Eureka: http://localhost:8761 - auth-service should be registered

---

## Step 4: Start Propertize Main Service

```bash
# Open new terminal
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/propertize

# Build
mvn clean package -DskipTests

# Run with local profile
java -jar target/propertize-*.jar --spring.profiles.active=local
```

**Verify:**

- Check http://localhost:8082/actuator/health
- Should return `{"status":"UP"}`
- Check Eureka: propertize-service should be registered

---

## Step 5: Start Employee Service

```bash
# Open new terminal
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/employee-service

# Build
mvn clean package -DskipTests

# Run with local profile
java -jar target/employee-service-*.jar --spring.profiles.active=local
```

**Verify:**

- Check http://localhost:8083/actuator/health
- Check Eureka: employee-service should be registered

---

## Step 6: Start API Gateway

```bash
# Open new terminal
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/api-gateway

# Build
mvn clean package -DskipTests

# Run with local profile
java -jar target/api-gateway-*.jar --spring.profiles.active=local
```

**Verify:**

- Check http://localhost:8080/actuator/health
- Check Eureka: api-gateway should be registered

---

## Step 7: Start Frontend

```bash
# Open new terminal
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services/propertize-front-end

# Install dependencies (first time only)
npm install

# Run development server
npm run dev

# OR build and run production
npm run build
npm start
```

**Verify:**

- Open http://localhost:3000
- Should see login page

---

## Quick Start Script

Create a script to start all services:

```bash
#!/bin/bash
# start-all-local.sh

echo "Starting infrastructure..."
docker-compose -f docker-compose.infra.yml up -d

echo "Waiting for infrastructure to be healthy..."
sleep 10

echo "Starting Service Registry..."
cd service-registry && mvn clean package -DskipTests && java -jar target/service-registry-*.jar --spring.profiles.active=local &
REGISTRY_PID=$!
sleep 15

echo "Starting Auth Service..."
cd ../auth-service && mvn clean package -DskipTests && java -jar target/auth-service-*.jar --spring.profiles.active=local &
AUTH_PID=$!
sleep 15

echo "Starting Propertize Main Service..."
cd ../propertize && mvn clean package -DskipTests && java -jar target/propertize-*.jar --spring.profiles.active=local &
PROPERTIZE_PID=$!
sleep 15

echo "Starting Employee Service..."
cd ../employee-service && mvn clean package -DskipTests && java -jar target/employee-service-*.jar --spring.profiles.active=local &
EMPLOYEE_PID=$!
sleep 10

echo "Starting API Gateway..."
cd ../api-gateway && mvn clean package -DskipTests && java -jar target/api-gateway-*.jar --spring.profiles.active=local &
GATEWAY_PID=$!
sleep 10

echo "Starting Frontend..."
cd ../propertize-front-end && npm run dev &
FRONTEND_PID=$!

echo "All services started!"
echo "Service Registry: http://localhost:8761"
echo "Auth Service: http://localhost:8081"
echo "Propertize: http://localhost:8082"
echo "Employee Service: http://localhost:8083"
echo "API Gateway: http://localhost:8080"
echo "Frontend: http://localhost:3000"
echo ""
echo "To stop all services, run: kill $REGISTRY_PID $AUTH_PID $PROPERTIZE_PID $EMPLOYEE_PID $GATEWAY_PID $FRONTEND_PID"
```

---

## Troubleshooting

### PostgreSQL Connection Issues

```bash
# Check if PostgreSQL is running
psql -h localhost -U dbuser -d propertize_db

# If connection refused, start PostgreSQL
brew services start postgresql@16  # macOS
sudo systemctl start postgresql    # Linux
```

### Port Already in Use

```bash
# Find process using port
lsof -i :8081  # Replace with your port

# Kill process
kill -9 <PID>
```

### Eureka Registration Issues

- Wait 30 seconds for services to register
- Check application-local.yml has correct eureka.client.service-url.defaultZone
- Check service logs for connection errors

### Redis/MongoDB Connection Issues

```bash
# Verify Docker containers are running
docker-compose -f docker-compose.infra.yml ps

# Check container logs
docker logs propertize-redis
docker logs propertize-mongodb

# Restart infrastructure
docker-compose -f docker-compose.infra.yml restart
```

---

## Environment Variables

Create `.env` file in root directory:

```bash
# Database
DB_USERNAME=dbuser
DB_PASSWORD=dbpassword
DB_HOST=localhost
DB_PORT=5432
DB_NAME=propertize_db

# MongoDB (Docker)
MONGO_USERNAME=admin
MONGO_PASSWORD=mongo_secure_pass

# Redis (Docker)
REDIS_PASSWORD=redis_secure_pass

# JWT
JWT_SECRET=your-secret-key-change-in-production

# Service Auth
SERVICE_API_KEY=dev-api-key-12345
```

---

## Development Workflow

### Hot Reload with Spring Boot DevTools

Add to pom.xml:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

Run with:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend Hot Reload

```bash
npm run dev  # Automatically reloads on file changes
```

---

## Stopping Services

### Stop All Java Services

```bash
# Find all Java processes
ps aux | grep java

# Kill specific service
kill <PID>

# Or kill all Java processes (be careful!)
pkill -f "spring.profiles.active=local"
```

### Stop Infrastructure

```bash
docker-compose -f docker-compose.infra.yml down

# To remove volumes as well
docker-compose -f docker-compose.infra.yml down -v
```

---

## Benefits of Local Development

1. **Faster Startup**: No Docker overhead for services
2. **Better Debugging**: Attach debugger directly to processes
3. **Hot Reload**: Spring DevTools and npm dev server for instant feedback
4. **Resource Efficiency**: Less memory/CPU usage
5. **Native Performance**: Services run at native speed
6. **Easier Profiling**: Use native profiling tools

---

## Production Docker Setup

For production, use the original `docker-compose.yml` file:

```bash
docker-compose up -d
```

This runs all services in Docker containers.
