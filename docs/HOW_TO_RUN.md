# 🚀 Quick Start - Running All Services

## Option 1: Automated Startup Script (Recommended)

The easiest way to start everything:

```bash
./start.sh
```

This script will:
1. Start infrastructure (PostgreSQL, MongoDB, Redis)
2. Start message queue (Kafka, Zookeeper)
3. Build and start all microservices in the correct order
4. Start the frontend application
5. Optionally start management UIs

**First run will take 5-10 minutes** to download images and build services.

---

## Option 2: Manual Step-by-Step

### Step 1: Start Infrastructure

```bash
# Start databases and cache
docker-compose up -d postgres mongodb redis

# Wait for them to be ready
sleep 10
```

### Step 2: Start Message Queue

```bash
# Start Kafka ecosystem
docker-compose up -d zookeeper kafka kafka-ui

# Wait for Kafka
sleep 15
```

### Step 3: Build and Start Microservices

```bash
# Service Registry (must be first)
docker-compose build service-registry
docker-compose up -d service-registry
sleep 20

# Auth Service (must be second)
docker-compose build auth-service
docker-compose up -d auth-service
sleep 15

# Remaining services (can be parallel)
docker-compose build propertize employee-service api-gateway
docker-compose up -d propertize employee-service api-gateway
sleep 20

# Frontend
docker-compose build wagecraft-frontend
docker-compose up -d wagecraft-frontend
```

### Step 4: (Optional) Start Management UIs

```bash
docker-compose up -d adminer mongo-express
```

---

## Option 3: Using Makefile

```bash
# If you have make installed
make install    # Setup
make build      # Build all
make up         # Start all
```

---

## Verify Services Are Running

```bash
# Check container status
docker-compose ps

# Should see all services as "Up (healthy)" or "Up"
```

### Check Individual Services

```bash
# View logs for a specific service
docker-compose logs -f propertize
docker-compose logs -f auth-service
docker-compose logs -f api-gateway

# Check if a service is responding
curl http://localhost:8761  # Service Registry
curl http://localhost:9090/actuator/health  # Auth Service
curl http://localhost:8080/actuator/health  # Propertize
curl http://localhost:8082/actuator/health  # API Gateway
```

---

## Access the Application

Once all services are running:

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:3000 | Main application UI |
| **API Gateway** | http://localhost:8082 | API endpoint |
| **Eureka Dashboard** | http://localhost:8761 | Service registry |
| **Kafka UI** | http://localhost:8090 | Monitor Kafka topics |
| **Adminer** | http://localhost:8088 | Database management |
| **Mongo Express** | http://localhost:8089 | MongoDB management |

---

## Common Issues & Solutions

### Services won't start

```bash
# Check logs
docker-compose logs service-name

# Restart a specific service
docker-compose restart service-name

# Rebuild and restart
docker-compose up -d --build service-name
```

### Port already in use

```bash
# Find what's using the port
lsof -i :8080

# Stop the process or change the port in docker-compose.yml
```

### Database connection errors

```bash
# Make sure databases are healthy
docker-compose ps postgres mongodb redis

# Restart dependent services
docker-compose restart auth-service propertize
```

### Build failures

```bash
# Clean build
docker-compose build --no-cache service-name

# Or rebuild everything
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Out of memory

Increase Docker Desktop memory allocation:
1. Open Docker Desktop
2. Settings → Resources
3. Set Memory to at least 8GB
4. Click "Apply & Restart"

---

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ deletes all data)
docker-compose down -v

# Stop but keep containers for faster restart
docker-compose stop
```

---

## Next Steps

1. **Configure Environment**: Edit `.env` file with your settings
2. **Test APIs**: Use the Bruno collection in `/bruno-collection`
3. **Monitor Logs**: `docker-compose logs -f`
4. **Scale Services**: Edit docker-compose.yml to add replicas

---

## Need Help?

- Check [DOCKER_GUIDE.md](DOCKER_GUIDE.md) for detailed documentation
- View [SERVICE_DEPENDENCIES.md](SERVICE_DEPENDENCIES.md) for architecture
- See [Makefile](Makefile) for all available commands

---

**Estimated Startup Time:**
- First build: 5-10 minutes
- Subsequent starts: 1-2 minutes
