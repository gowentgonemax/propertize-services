# Docker Management Script

## Overview

The `docker-manage.sh` script provides a comprehensive command-line interface for managing the Propertize microservices Docker stack. It handles infrastructure services, application services, and the frontend separately for efficient development and deployment workflows.

## Quick Start

```bash
# Start everything
./docker-manage.sh start

# Check status
./docker-manage.sh status

# Check health
./docker-manage.sh health

# View logs
./docker-manage.sh logs employee-service

# Rebuild after code changes
./docker-manage.sh rebuild propertize
```

## Commands

### Starting Services

- **`start`** - Start all services (infrastructure + applications + frontend + UI tools)

  ```bash
  ./docker-manage.sh start
  ```

- **`start-infra`** - Start only infrastructure services (databases, Kafka, Redis)

  ```bash
  ./docker-manage.sh start-infra
  ```

  Use this when developing locally and want to run application services outside Docker.

- **`start-apps`** - Start only application services (Eureka, auth, main, employee, gateway)

  ```bash
  ./docker-manage.sh start-apps
  ```

- **`start-frontend`** - Start only the frontend service

  ```bash
  ./docker-manage.sh start-frontend
  ```

- **`start-ui`** - Start UI management tools (Adminer, Mongo Express, Kafka UI)
  ```bash
  ./docker-manage.sh start-ui
  ```

### Stopping Services

- **`stop`** - Stop all services

  ```bash
  ./docker-manage.sh stop
  ```

- **`stop-apps`** - Stop only application services (keeps infrastructure running)

  ```bash
  ./docker-manage.sh stop-apps
  ```

  Useful when you need to rebuild applications but keep databases running.

- **`stop-infra`** - Stop infrastructure services
  ```bash
  ./docker-manage.sh stop-infra
  ```
  ⚠️ **Warning:** This stops databases and message queues.

### Managing Individual Services

- **`restart <service>`** - Restart a specific service

  ```bash
  ./docker-manage.sh restart auth-service
  ```

- **`rebuild <service>`** - Rebuild and restart a service (builds JAR if needed)

  ```bash
  ./docker-manage.sh rebuild propertize
  ```

  This command:
  1. Builds the Maven JAR (for Java services)
  2. Rebuilds the Docker image
  3. Restarts the container

- **`build-all`** - Build all Java services and frontend (without Docker)

  ```bash
  ./docker-manage.sh build-all
  ```

  Builds all services:
  - service-registry
  - auth-service
  - propertize
  - employee-service
  - api-gateway
  - propertize-front-end

  Run this before `start` or `rebuild-all` when you've made code changes.

- **`rebuild-all`** - Build all services and restart Docker containers

  ```bash
  ./docker-manage.sh rebuild-all
  ```

  This command:
  1. Builds all Java services (mvn clean package)
  2. Builds frontend (npm run build)
  3. Rebuilds all Docker images
  4. Restarts all containers

### Monitoring

- **`status`** - Show comprehensive status of all services

  ```bash
  ./docker-manage.sh status
  ```

  Displays:
  - Container status
  - Services registered in Eureka
  - Service URLs

- **`health`** - Check health endpoints of all services

  ```bash
  ./docker-manage.sh health
  ```

- **`logs <service> [lines]`** - Show logs for a specific service (follow mode)

  ```bash
  # Default (last 100 lines)
  ./docker-manage.sh logs employee-service

  # Specify number of lines
  ./docker-manage.sh logs auth-service 50
  ```

### Maintenance

- **`cleanup`** - Remove stopped containers and unused images

  ```bash
  ./docker-manage.sh cleanup
  ```

- **`help`** - Show help message
  ```bash
  ./docker-manage.sh help
  ```

## Service Groups

### Infrastructure Services

These run only once and are rarely modified:

- **postgres** - PostgreSQL 16 database
- **mongodb** - MongoDB 7 document store
- **redis** - Redis 7 cache
- **zookeeper** - Zookeeper for Kafka
- **kafka** - Kafka 7.6.0 message broker

### Application Services

These are frequently modified during development:

- **service-registry** - Eureka service discovery (port 8761)
- **auth-service** - Authentication service (port 8081)
- **propertize** - Main business logic service (port 8082)
- **employee-service** - Employee management (port 8083)
- **api-gateway** - API Gateway (port 8080)

### Frontend Service

- **frontend** - Next.js frontend (port 3000)

### UI Tools (Optional)

- **adminer** - PostgreSQL web admin (port 8088)
- **mongo-express** - MongoDB web admin (port 8089)
- **kafka-ui** - Kafka web UI (port 8090)

## Service URLs

| Service          | URL                   | Description                    |
| ---------------- | --------------------- | ------------------------------ |
| Eureka           | http://localhost:8761 | Service registry dashboard     |
| API Gateway      | http://localhost:8080 | Main API endpoint              |
| Auth Service     | http://localhost:8081 | Authentication endpoints       |
| Main Service     | http://localhost:8082 | Core business logic            |
| Employee Service | http://localhost:8083 | Employee management            |
| Frontend         | http://localhost:3000 | Web application                |
| Adminer          | http://localhost:8088 | PostgreSQL admin (admin/admin) |
| Mongo Express    | http://localhost:8089 | MongoDB admin                  |
| Kafka UI         | http://localhost:8090 | Kafka management               |

## Common Workflows

### Full Development Setup

```bash
# Start everything
./docker-manage.sh start

# Check status
./docker-manage.sh status

# Check health
./docker-manage.sh health
```

### Local Development with Docker Infrastructure

```bash
# Start only infrastructure
./docker-manage.sh start-infra

# Run your services locally (IDE, mvn spring-boot:run, etc.)

# Stop infrastructure when done
./docker-manage.sh stop-infra
```

### After Code Changes

```bash
# Rebuild specific service
./docker-manage.sh rebuild auth-service

# Check logs
./docker-manage.sh logs auth-service

# Verify health
./docker-manage.sh health
```

### Debugging Issues

```bash
# Check status
./docker-manage.sh status

# View logs
./docker-manage.sh logs propertize 200

# Restart specific service
./docker-manage.sh restart employee-service
```

### End of Day Shutdown

```bash
# Stop applications but keep infrastructure
./docker-manage.sh stop-apps

# Or stop everything
./docker-manage.sh stop
```

## Health Checks

All application services expose health endpoints via Spring Boot Actuator:

- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/info` - Application information
- `/actuator/metrics` - Metrics
- `/actuator/prometheus` - Prometheus metrics

Use the `health` command to check all services at once:

```bash
./docker-manage.sh health
```

## Troubleshooting

### Service Won't Start

```bash
# Check logs
./docker-manage.sh logs <service>

# Rebuild from scratch
./docker-manage.sh rebuild <service>
```

### Port Already in Use

```bash
# Check what's using the port
lsof -i :8080

# Stop all services
./docker-manage.sh stop
```

### Services Not Registering with Eureka

```bash
# Check Eureka status
curl http://localhost:8761/eureka/apps

# Restart service registry
./docker-manage.sh restart service-registry

# Restart application services
./docker-manage.sh stop-apps
./docker-manage.sh start-apps
```

### Database Connection Issues

```bash
# Check infrastructure status
docker ps | grep -E "postgres|mongodb|redis"

# Restart infrastructure
./docker-manage.sh stop-infra
./docker-manage.sh start-infra
```

### Clean Slate

```bash
# Remove all containers and images
./docker-manage.sh cleanup

# Rebuild everything
./docker-manage.sh start
```

## Development Tips

1. **Keep Infrastructure Running**: Use `start-infra` and `stop-apps` to keep databases running between application restarts.

2. **Rebuild Efficiently**: Only rebuild the service you changed with `rebuild <service>` instead of restarting everything.

3. **Monitor Health**: Use `./docker-manage.sh health` regularly to catch issues early.

4. **Check Eureka**: Visit http://localhost:8761 to see service registration status.

5. **Use Logs**: The `logs` command follows logs in real-time - perfect for debugging.

## Script Location

The script is located at:

```
/Users/ravishah/MySpace/ProperyManage/propertize-Services/docker-manage.sh
```

Make it executable:

```bash
chmod +x docker-manage.sh
```

## Requirements

- Docker and Docker Compose installed
- Maven (for rebuilding Java services)
- curl (for health checks)
- bash 4.0 or higher

## Support

For issues or questions, check:

- Docker logs: `./docker-manage.sh logs <service>`
- Service status: `./docker-manage.sh status`
- Health checks: `./docker-manage.sh health`
- Eureka dashboard: http://localhost:8761

## Eureka Human-Readable Endpoints

The Eureka service registry provides additional JSON endpoints for easier service discovery:

- **Dashboard**: http://localhost:8761 (requires admin/admin login)
- **Registered Apps (JSON)**: http://localhost:8761/registry/apps - Returns all registered services in human-readable JSON format
- **Registry Health**: http://localhost:8761/registry/health - Returns health status in JSON format

**Note**: The standard `/eureka/apps` endpoint returns XML. Use `/registry/apps` for JSON output.

## Build Commands

### Build All Applications

Build all Java services and frontend without Docker:

```bash
./docker-manage.sh build-all
```

This builds:

- service-registry (Maven)
- auth-service (Maven)
- propertize (Maven)
- employee-service (Maven)
- api-gateway (Maven)
- propertize-front-end (npm)

### Rebuild All with Docker

Build all applications and restart Docker containers:

```bash
./docker-manage.sh rebuild-all
```

This performs:

1. Builds all Java services (mvn clean package -DskipTests)
2. Builds frontend (npm run build)
3. Rebuilds all Docker images
4. Restarts all containers

Use this after code changes to rebuild and restart everything in one command.
