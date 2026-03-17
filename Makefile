# ============================================
# Propertize Platform - Makefile
# Convenient commands for Docker operations
# ============================================

.PHONY: help build up down restart logs clean rebuild ps health

# Default target
.DEFAULT_GOAL := help

help: ## Show this help message
	@echo "Propertize Platform - Docker Commands"
	@echo "====================================="
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ============================================
# Build Commands
# ============================================

build: ## Build all services
	docker-compose build

build-nc: ## Build all services (no cache)
	docker-compose build --no-cache

rebuild: down build up ## Rebuild and restart all services

# ============================================
# Lifecycle Commands
# ============================================

up: ## Start all services
	docker-compose up -d
	@echo "⏳ Waiting for services to be healthy..."
	@sleep 10
	@make ps

down: ## Stop all services
	docker-compose down

down-v: ## Stop all services and remove volumes (⚠️  deletes data)
	docker-compose down -v

restart: ## Restart all services
	docker-compose restart

stop: ## Stop all services without removing
	docker-compose stop

start: ## Start previously stopped services
	docker-compose start

# ============================================
# Individual Service Commands
# ============================================

up-registry: ## Start service registry
	docker-compose up -d service-registry

up-auth: ## Start auth service
	docker-compose up -d auth-service

up-propertize: ## Start propertize service
	docker-compose up -d propertize

up-employee: ## Start employee service
	docker-compose up -d employee-service

up-gateway: ## Start API gateway
	docker-compose up -d api-gateway

up-frontend: ## Start frontend app
	docker-compose up -d wagecraft-frontend

# ============================================
# Monitoring Commands
# ============================================

logs: ## Show logs for all services
	docker-compose logs -f

logs-auth: ## Show auth service logs
	docker-compose logs -f auth-service

logs-propertize: ## Show propertize service logs
	docker-compose logs -f propertize

logs-gateway: ## Show API gateway logs
	docker-compose logs -f api-gateway

logs-registry: ## Show service registry logs
	docker-compose logs -f service-registry

ps: ## Show status of all services
	docker-compose ps

health: ## Check health of all services
	@echo "Service Health Status:"
	@docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

stats: ## Show resource usage statistics
	docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"

# ============================================
# Database Commands
# ============================================

db-shell: ## Connect to PostgreSQL shell
	docker exec -it propertize-postgres psql -U propertize_user -d propertize_db

mongo-shell: ## Connect to MongoDB shell
	docker exec -it propertize-mongodb mongosh -u admin -p mongo_secure_pass

redis-cli: ## Connect to Redis CLI
	docker exec -it propertize-redis redis-cli -a redis_secure_pass

# ============================================
# Development Commands
# ============================================

dev: ## Start infrastructure only (for local development)
	docker-compose up -d postgres mongodb redis kafka zookeeper

dev-down: ## Stop infrastructure
	docker-compose stop postgres mongodb redis kafka zookeeper

shell-auth: ## Access auth service container shell
	docker exec -it propertize-auth-service sh

shell-propertize: ## Access propertize service container shell
	docker exec -it propertize-main-service sh

shell-gateway: ## Access gateway container shell
	docker exec -it propertize-api-gateway sh

# ============================================
# Cleanup Commands
# ============================================

clean: ## Remove stopped containers and unused images
	docker-compose down
	docker system prune -f

clean-all: ## Remove everything including volumes (⚠️  deletes data)
	docker-compose down -v --rmi all
	docker system prune -af --volumes

clean-build: ## Clean and rebuild everything
	@make clean-all
	@make build
	@make up

# ============================================
# Testing Commands
# ============================================

test-health: ## Test all service health endpoints
	@echo "Testing Service Health Endpoints..."
	@curl -sf http://localhost:8761/actuator/health && echo "✅ Service Registry" || echo "❌ Service Registry"
	@curl -sf http://localhost:8081/actuator/health && echo "✅ Auth Service" || echo "❌ Auth Service"
	@curl -sf http://localhost:8082/actuator/health && echo "✅ Propertize Service" || echo "❌ Propertize Service"
	@curl -sf http://localhost:8083/actuator/health && echo "✅ Employee Service" || echo "❌ Employee Service"
	@curl -sf http://localhost:8080/actuator/health && echo "✅ API Gateway" || echo "❌ API Gateway"

open-ui: ## Open all management UIs in browser
	@echo "Opening management UIs..."
	@open http://localhost:8761 || true  # Eureka
	@open http://localhost:8090 || true  # Kafka UI
	@open http://localhost:8088 || true  # Adminer
	@open http://localhost:8089 || true  # Mongo Express
	@open http://localhost:3000 || true  # Frontend

# ============================================
# Environment Setup
# ============================================

setup: ## Initial setup - create .env file
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "✅ Created .env file from template"; \
		echo "⚠️  Please edit .env and update passwords/secrets"; \
	else \
		echo "ℹ️  .env file already exists"; \
	fi

# ============================================
# Quick Commands
# ============================================

install: setup build ## Complete installation (setup + build)

run: up ## Alias for 'up'

status: ps ## Alias for 'ps'

tail: logs ## Alias for 'logs'
