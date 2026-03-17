# Propertize - Local Development Mode

## Overview

This setup separates **infrastructure** (running in Docker) from **application services** (running locally) for optimal development experience.

## Architecture

### Infrastructure (Docker) - `docker-compose.infra.yml`

- **MongoDB** (port 27017) - Document database
- **Redis** (port 6379) - Cache and sessions
- **Kafka + Zookeeper** (port 9092) - Message broker
- **Kafka UI** (port 8090) - Kafka management
- **Mongo Express** (port 8089) - MongoDB UI

### Application Services (Local)

- **Service Registry** (Eureka) - Port 8761
- **Auth Service** - Port 8081
- **Propertize Main Service** - Port 8082
- **Employee Service** - Port 8083
- **API Gateway** - Port 8080
- **Frontend** (Next.js) - Port 3000

### Database (Local)

- **PostgreSQL** - Port 5432

---

## Quick Start

### 1. Setup PostgreSQL (One-time)

```bash
./setup-postgres-local.sh
```

This script will:

- Check if PostgreSQL is installed
- Start PostgreSQL service
- Create database `propertize_db`
- Create user `dbuser` with password `dbpassword`
- Grant necessary privileges

### 2. Start Infrastructure (Docker)

```bash
docker-compose -f docker-compose.infra.yml up -d
```

Verify:

```bash
docker-compose -f docker-compose.infra.yml ps
```

### 3. Start All Services

```bash
./start-all-local.sh
```

This script will:

1. Start infrastructure services
2. Build all Java services (if needed)
3. Start services in correct order
4. Wait for each service to be ready
5. Display all service URLs

### 4. Access Applications

- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Service Registry**: http://localhost:8761 (admin/admin)
- **Kafka UI**: http://localhost:8090
- **Mongo Express**: http://localhost:8089 (admin/admin)

### 5. Stop All Services

```bash
./stop-all-local.sh
```

---

## Manual Service Control

### Start Individual Services

```bash
# Service Registry
cd service-registry
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Auth Service
cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Propertize Main
cd propertize
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Employee Service
cd employee-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# API Gateway
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Frontend
cd propertize-front-end
npm run dev
```

### Check Service Health

```bash
# Service Registry
curl http://localhost:8761/actuator/health

# Auth Service
curl http://localhost:8081/actuator/health

# Propertize
curl http://localhost:8082/actuator/health

# Employee Service
curl http://localhost:8083/actuator/health

# API Gateway
curl http://localhost:8080/actuator/health
```

---

## Configuration Files

Each service has a `application-local.yml` configuration file:

- `service-registry/src/main/resources/application-local.yml`
- `auth-service/src/main/resources/application-local.yml`
- `propertize/src/main/resources/application-local.yml`
- `employee-service/src/main/resources/application-local.yml`
- `api-gateway/src/main/resources/application-local.yml`
- `propertize-front-end/.env.local`

All configurations point to:

- **PostgreSQL**: `localhost:5432`
- **MongoDB**: `localhost:27017` (Docker)
- **Redis**: `localhost:6379` (Docker)
- **Kafka**: `localhost:9092` (Docker)
- **Eureka**: `localhost:8761`

---

## Logs

All service logs are stored in `logs/` directory:

```bash
# View service logs
tail -f logs/service-registry.log
tail -f logs/auth-service.log
tail -f logs/propertize.log
tail -f logs/employee-service.log
tail -f logs/api-gateway.log
tail -f logs/frontend.log

# View all logs
tail -f logs/*.log
```

---

## Development Workflow

### Hot Reload

**Java Services** (with Spring Boot DevTools):

```bash
# Add to pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>

# Run
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend**:

```bash
npm run dev  # Automatically reloads
```

### Debugging

Attach your IDE debugger directly to the Java processes running locally.

**IntelliJ IDEA**:

1. Run/Debug Configurations
2. Add "Spring Boot" configuration
3. Set profile to "local"
4. Debug

**VS Code**:

```json
{
  "type": "java",
  "name": "Debug Auth Service",
  "request": "launch",
  "mainClass": "com.propertize.platform.auth.AuthServiceApplication",
  "args": "--spring.profiles.active=local"
}
```

---

## Troubleshooting

### PostgreSQL Connection Failed

```bash
# Check if running
psql -h localhost -U dbuser -d propertize_db

# Start PostgreSQL
brew services start postgresql@16  # macOS
sudo systemctl start postgresql    # Linux
```

### Port Already in Use

```bash
# Find process
lsof -i :8081

# Kill process
kill -9 <PID>
```

### Service Won't Start

```bash
# Check logs
tail -f logs/service-name.log

# Check if infrastructure is running
docker-compose -f docker-compose.infra.yml ps

# Restart infrastructure
docker-compose -f docker-compose.infra.yml restart
```

### Eureka Registration Issues

- Wait 30 seconds for registration
- Check `application-local.yml` for correct Eureka URL
- Verify Service Registry is running: http://localhost:8761

### Build Failures

```bash
# Clean and rebuild
cd service-name
mvn clean install -DskipTests

# Check Java version
java -version  # Should be 17+
```

---

## Benefits of Local Development

1. **Faster Startup**: No Docker overhead for Java services
2. **Better Debugging**: Direct debugger attachment
3. **Hot Reload**: Instant code changes with DevTools
4. **Resource Efficient**: Lower CPU/memory usage
5. **Native Performance**: Full speed execution
6. **Easy Profiling**: Use native profiling tools

---

## Production Deployment

For production with all services in Docker:

```bash
docker-compose -f docker-compose.fullstack.yml up -d
```

---

## File Structure

```
propertize-Services/
├── docker-compose.infra.yml          # Infrastructure only
├── docker-compose.fullstack.yml      # Full stack (all services)
├── start-all-local.sh                # Start all services locally
├── stop-all-local.sh                 # Stop all services
├── setup-postgres-local.sh           # PostgreSQL setup
├── .env.local.example                # Environment variables template
├── LOCAL_DEVELOPMENT_GUIDE.md        # Detailed guide
├── logs/                             # Service logs
│   ├── service-registry.log
│   ├── auth-service.log
│   ├── propertize.log
│   ├── employee-service.log
│   ├── api-gateway.log
│   └── frontend.log
├── service-registry/
│   └── src/main/resources/
│       └── application-local.yml
├── auth-service/
│   └── src/main/resources/
│       └── application-local.yml
├── propertize/
│   └── src/main/resources/
│       └── application-local.yml
├── employee-service/
│   └── src/main/resources/
│       └── application-local.yml
├── api-gateway/
│   └── src/main/resources/
│       └── application-local.yml
└── propertize-front-end/
    └── .env.local
```

---

## Support

For detailed instructions, see [LOCAL_DEVELOPMENT_GUIDE.md](LOCAL_DEVELOPMENT_GUIDE.md)

For issues:

1. Check logs in `logs/` directory
2. Verify infrastructure: `docker-compose -f docker-compose.infra.yml ps`
3. Check PostgreSQL: `psql -h localhost -U dbuser -d propertize_db`
4. Verify Eureka: http://localhost:8761
