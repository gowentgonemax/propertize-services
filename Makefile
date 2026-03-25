# ============================================
# Propertize Platform - Makefile
# One-click Docker orchestration for all services
# ============================================
# QUICK START:
#   make build   — build every image (first time, ~10–15 min)
#   make up      — start the entire platform
#   make down    — stop the entire platform
#   make logs    — tail all logs
#   make status  — see health of every container
# ============================================

.PHONY: help build build-nc up up-infra up-java up-python up-all \
        down down-v stop start restart rebuild \
        logs logs-auth logs-propertize logs-gateway logs-registry \
        logs-frontend logs-python ps health stats \
        db-shell mongo-shell redis-cli \
        dev dev-down profiles shell-auth shell-propertize shell-gateway \
        clean clean-all clean-build setup install run status tail open-ui

COMPOSE = docker compose

# Default target
.DEFAULT_GOAL := help

help: ## Show this help message
	@echo ""
	@echo "  Propertize Platform — Docker Commands"
	@echo "  ======================================"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'
	@echo ""

# ============================================
# Build Commands
# ============================================

build: ## Build ALL service images (infra pulled, app images built)
	$(COMPOSE) build --parallel
	@echo "✅ All images built successfully"

build-nc: ## Build all images with no cache
	$(COMPOSE) build --no-cache --parallel
	@echo "✅ All images rebuilt (no cache)"

build-java: ## Build only Java service images
	$(COMPOSE) build service-registry auth-service propertize employee-service api-gateway

build-python: ## Build only Python service images
	$(COMPOSE) build report-service vendor-matching document-service search-reranker analytics-worker payment-worker screening-worker

build-frontend: ## Build only frontend image
	$(COMPOSE) build frontend

rebuild: down build up ## Full rebuild: stop → build → start

# ============================================
# Start / Stop (all services run in Docker)
# ============================================

local-start: ## ▶️  Build all images + start entire platform in Docker
	@bash scripts/start-all.sh

local-start-fast: ## ⚡ Start platform in Docker (skip image rebuild)
	@bash scripts/start-all.sh --skip-build

local-stop: ## ■  Stop all Docker services (volumes preserved)
	@bash scripts/stop-all.sh

local-stop-purge: ## ■  Stop all Docker services AND remove volumes
	@bash scripts/stop-all.sh --purge

local-restart: ## 🔄 Stop → rebuild images → start everything in Docker
	@bash scripts/stop-all.sh && bash scripts/start-all.sh



up: ## ▶️  Start ALL services (full platform)
	$(COMPOSE) up -d
	@echo ""
	@echo "⏳ Starting platform... (Java services take ~60-90s to become healthy)"
	@echo ""
	@echo "  Access points:"
	@echo "    Frontend     →  http://localhost:3000"
	@echo "    API Gateway  →  http://localhost:8080"
	@echo "    Eureka       →  http://localhost:8761  (admin/admin)"
	@echo "    Kafka UI     →  http://localhost:8086"
	@echo "    Adminer      →  http://localhost:8088"
	@echo "    Mongo Expr.  →  http://localhost:8089"
	@echo "    MinIO        →  http://localhost:9001  (propertize/propertize123)"
	@echo ""

down: ## ⏹️  Stop ALL services (keep volumes/data)
	$(COMPOSE) down
	@echo "✅ All services stopped"

down-v: ## 🗑️  Stop ALL services and DELETE all data volumes
	$(COMPOSE) down -v
	@echo "⚠️  All services stopped and data volumes removed"

stop: ## Pause all running containers (preserves state)
	$(COMPOSE) stop

start: ## Resume paused containers
	$(COMPOSE) start

restart: ## Restart all containers
	$(COMPOSE) restart
	@echo "✅ All services restarted"

# ============================================
# Selective Start Commands
# ============================================

up-infra: ## Start infrastructure only (DB, Kafka, Redis, MinIO)
	$(COMPOSE) up -d postgres mongodb redis zookeeper kafka minio
	@echo "✅ Infrastructure started"

up-java: ## Start Java microservices (needs infra running)
	$(COMPOSE) up -d service-registry
	@sleep 5
	$(COMPOSE) up -d auth-service propertize employee-service
	@sleep 5
	$(COMPOSE) up -d api-gateway
	@echo "✅ Java services started"

up-python: ## Start Python microservices (needs infra running)
	$(COMPOSE) up -d report-service vendor-matching document-service search-reranker analytics-worker payment-worker screening-worker
	@echo "✅ Python services started"

up-frontend: ## Start frontend only (needs gateway running)
	$(COMPOSE) up -d frontend

up-registry: ## Start service registry
	$(COMPOSE) up -d service-registry

up-auth: ## Start auth service
	$(COMPOSE) up -d auth-service

up-propertize: ## Start propertize main service
	$(COMPOSE) up -d propertize

up-employee: ## Start employee service
	$(COMPOSE) up -d employee-service

up-gateway: ## Start API gateway
	$(COMPOSE) up -d api-gateway

# ============================================
# Monitoring Commands
# ============================================

logs: ## Tail logs for all services
	$(COMPOSE) logs -f

logs-auth: ## Show auth service logs
	$(COMPOSE) logs -f auth-service

logs-propertize: ## Show propertize main service logs
	$(COMPOSE) logs -f propertize

logs-gateway: ## Show API gateway logs
	$(COMPOSE) logs -f api-gateway

logs-registry: ## Show service registry logs
	$(COMPOSE) logs -f service-registry

logs-frontend: ## Show frontend logs
	$(COMPOSE) logs -f frontend

logs-python: ## Show all Python service logs
	$(COMPOSE) logs -f report-service vendor-matching document-service search-reranker analytics-worker payment-worker screening-worker

logs-infra: ## Show infrastructure logs (DB, Kafka, Redis)
	$(COMPOSE) logs -f postgres mongodb redis kafka

ps: ## Show status of all containers
	$(COMPOSE) ps

health: ## Check health of all application endpoints
	@echo ""
	@echo "  Service Health Status:"
	@echo "  ──────────────────────────────────────────"
	@curl -sf http://localhost:8761/actuator/health > /dev/null && echo "  ✅  Service Registry  :8761" || echo "  ❌  Service Registry  :8761"
	@curl -sf http://localhost:8081/actuator/health > /dev/null && echo "  ✅  Auth Service      :8081" || echo "  ❌  Auth Service      :8081"
	@curl -sf http://localhost:8082/actuator/health > /dev/null && echo "  ✅  Propertize Svc    :8082" || echo "  ❌  Propertize Svc    :8082"
	@curl -sf http://localhost:8083/actuator/health > /dev/null && echo "  ✅  Employee Service  :8083" || echo "  ❌  Employee Service  :8083"
	@curl -sf http://localhost:8080/actuator/health > /dev/null && echo "  ✅  API Gateway       :8080" || echo "  ❌  API Gateway       :8080"
	@curl -sf http://localhost:8090/health          > /dev/null && echo "  ✅  Report Service    :8090" || echo "  ❌  Report Service    :8090"
	@curl -sf http://localhost:8091/health          > /dev/null && echo "  ✅  Vendor Matching   :8091" || echo "  ❌  Vendor Matching   :8091"
	@curl -sf http://localhost:8092/health          > /dev/null && echo "  ✅  Document Service  :8092" || echo "  ❌  Document Service  :8092"
	@curl -sf http://localhost:8093/health          > /dev/null && echo "  ✅  Search Reranker   :8093" || echo "  ❌  Search Reranker   :8093"
	@curl -sf http://localhost:3000                 > /dev/null && echo "  ✅  Frontend          :3000" || echo "  ❌  Frontend          :3000"
	@echo ""

stats: ## Show real-time resource usage per container
	docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"

# ============================================
# Database Commands
# ============================================

db-shell: ## Connect to PostgreSQL shell
	$(COMPOSE) exec postgres psql -U dbuser -d propertize_db

mongo-shell: ## Connect to MongoDB shell
	$(COMPOSE) exec mongodb mongosh -u admin -p mongo_secure_pass

redis-cli: ## Connect to Redis CLI
	$(COMPOSE) exec redis redis-cli -a redis_secure_pass

# ============================================
# Development Commands
# ============================================

dev: ## Start infrastructure only — for local Java/npm dev (profile=local)
	$(COMPOSE) -f docker-compose.infra.yml up -d
	@echo ""
	@echo "  ✅  Infrastructure ready — use profile=local for your JVM services"
	@echo "    PostgreSQL  → localhost:5432  (dbuser/dbpassword)"
	@echo "    MongoDB     → localhost:27017 (admin/mongo_secure_pass)"
	@echo "    Redis       → localhost:6379  (redis_secure_pass)"
	@echo "    Kafka       → localhost:9092"
	@echo "    Eureka      → localhost:8761"
	@echo "    MinIO       → localhost:9000  (propertize/propertize123)"
	@echo ""
	@echo "  Then in each service directory:"
	@echo "    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local"
	@echo ""

dev-down: ## Stop infrastructure services
	$(COMPOSE) -f docker-compose.infra.yml down

profiles: ## Show the 3-profile rule for this project
	@echo ""
	@echo "  Propertize — Spring Profile Strategy"
	@echo "  ======================================="
	@echo "  local   → YOU run the JVM, Docker infra on localhost:*"
	@echo "            How: make dev  then  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local"
	@echo ""
	@echo "  docker  → docker-compose.yml, services talk via container DNS"
	@echo "            How: docker compose up --build"
	@echo ""
	@echo "  prod    → CI/Kubernetes, everything from env vars, no defaults"
	@echo "            How: set SPRING_PROFILES_ACTIVE=prod in deploy pipeline"
	@echo ""
	@echo "  test    → Maven test runner, H2 in-memory DB"
	@echo "            How: ./mvnw test  (automatic)"
	@echo ""
	@echo "  See docs/PROFILE_GUIDE.md for the full guide"
	@echo ""

shell-auth: ## Shell into auth service container
	$(COMPOSE) exec auth-service sh

shell-propertize: ## Shell into propertize main service container
	$(COMPOSE) exec propertize sh

shell-gateway: ## Shell into API gateway container
	$(COMPOSE) exec api-gateway sh

# ============================================
# Cleanup Commands
# ============================================

clean: ## Remove stopped containers and dangling images
	$(COMPOSE) down
	docker system prune -f
	@echo "✅ Cleaned stopped containers and dangling images"

clean-all: ## Remove EVERYTHING including volumes — ⚠️ destroys all data
	$(COMPOSE) down -v --rmi all
	docker system prune -af --volumes
	@echo "⚠️  Full clean complete — all containers, images, and volumes removed"

clean-build: ## clean-all then rebuild from scratch
	@make clean-all
	@make build
	@make up

# ============================================
# Environment Setup
# ============================================

setup: ## Initial setup — copy .env.example to .env
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "✅ Created .env file from template"; \
		echo "⚠️  Review .env and update secrets before running in production"; \
	else \
		echo "ℹ️  .env already exists — skipping"; \
	fi

# ============================================
# Shortcuts
# ============================================

install: setup build ## setup + build all images (first-time install)
run: up ## Alias for 'up'
status: ps ## Alias for 'ps'
tail: logs ## Alias for 'logs'
test-health: health ## Alias for 'health'

open-ui: ## Open all management UIs in browser
	@echo "Opening management UIs..."
	@open http://localhost:8761 || true  # Eureka Service Registry
	@open http://localhost:8086 || true  # Kafka UI
	@open http://localhost:8088 || true  # Adminer (PostgreSQL)
	@open http://localhost:8089 || true  # Mongo Express
	@open http://localhost:9001 || true  # MinIO Console
	@open http://localhost:3000 || true  # Frontend

