# 🚀 Quick Reference - Propertize Docker Setup

## Most Common Commands

```bash
# First Time Setup
make setup          # Create .env file
make build          # Build all images
make up             # Start everything

# Daily Use
make up             # Start all services
make down           # Stop all services
make restart        # Restart all services
make logs           # View all logs
make ps             # Check status

# Development
make dev            # Start only databases/infrastructure
make logs-propertize  # View specific service logs
make shell-propertize # Access container shell

# Cleanup
make clean          # Remove stopped containers
make clean-all      # ⚠️ Remove everything (including data)
```

## Service Ports

| Service       | Port | URL                   |
| ------------- | ---- | --------------------- |
| Frontend      | 3000 | http://localhost:3000 |
| API Gateway   | 8082 | http://localhost:8082 |
| Propertize    | 8080 | http://localhost:8080 |
| Employee      | 8081 | http://localhost:8081 |
| Auth          | 9090 | http://localhost:9090 |
| Eureka        | 8761 | http://localhost:8761 |
| Kafka UI      | 8090 | http://localhost:8090 |
| Adminer       | 8088 | http://localhost:8088 |
| Mongo Express | 8089 | http://localhost:8089 |

## Troubleshooting

```bash
# Service won't start
make logs-[service-name]
docker-compose restart [service-name]

# Database issues
make db-shell       # PostgreSQL
make mongo-shell    # MongoDB
make redis-cli      # Redis

# Check health
make health
make test-health

# Clean restart
make clean
make build
make up
```

## File Structure

```
propertize-Services/
├── Dockerfile              # ✅ Consolidated Java services
├── Dockerfile.frontend     # ✅ React frontend
├── docker-compose.yml      # ✅ Complete orchestration
├── .dockerignore           # ✅ Build optimization
├── .env.example            # ✅ Environment template
├── Makefile                # ✅ Convenient commands
└── DOCKER_GUIDE.md         # ✅ Full documentation
```

## Need Help?

See [DOCKER_GUIDE.md](DOCKER_GUIDE.md) for complete documentation.
