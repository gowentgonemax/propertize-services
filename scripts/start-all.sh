#!/bin/bash
# =============================================================================
# Propertize — Start ALL services in Docker (infra + microservices + frontend)
#
# Usage:
#   ./scripts/start-all.sh               # build images then start everything
#   ./scripts/start-all.sh --skip-build  # start using existing Docker images
# =============================================================================
set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

# Colour helpers (safe — no external dependencies)
BOLD="\033[1m"; GREEN="\033[0;32m"; CYAN="\033[0;36m"; RED="\033[0;31m"; YELLOW="\033[0;33m"; BLUE="\033[0;34m"; NC="\033[0m"

# ─── Parse flags ─────────────────────────────────────────────────────────────
SKIP_BUILD=false
for arg in "$@"; do
  case $arg in
    --skip-build)  SKIP_BUILD=true ;;
    --help|-h)
      echo "Usage: $0 [--skip-build]"
      echo "  --skip-build   Use existing Docker images, do not rebuild"
      exit 0 ;;
  esac
done

echo -e "${BOLD}${GREEN}"
echo "╔══════════════════════════════════════════════╗"
echo "║   Propertize  ▶  Start All in Docker         ║"
echo "╚══════════════════════════════════════════════╝"
echo -e "${NC}"

# ─── Guard: Docker must be running ───────────────────────────────────────────
if ! docker info > /dev/null 2>&1; then
  echo -e "${RED}✗ Docker is not running. Please start Docker Desktop first.${NC}"
  exit 1
fi

# ─── Build + Start ───────────────────────────────────────────────────────────
if [ "$SKIP_BUILD" = true ]; then
  echo -e "${CYAN}⏩ Skipping build — starting with existing images...${NC}"
  docker compose up -d
else
  echo -e "${CYAN}🔨 Building all service images...${NC}"
  docker compose build --parallel
  echo -e "${GREEN}✓ All images built${NC}"
  echo ""
  echo -e "${CYAN}▶  Bringing up all services...${NC}"
  docker compose up -d
fi

# ─── Done ────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║     ✅  All Services Started in Docker        ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${BLUE}Frontend     ${NC}→ ${CYAN}http://localhost:3000${NC}"
echo -e "  ${BLUE}API Gateway  ${NC}→ ${CYAN}http://localhost:8080${NC}"
echo -e "  ${BLUE}Auth Service ${NC}→ ${CYAN}http://localhost:8081${NC}"
echo -e "  ${BLUE}Core Service ${NC}→ ${CYAN}http://localhost:8082${NC}"
echo -e "  ${BLUE}Employee Svc ${NC}→ ${CYAN}http://localhost:8083${NC}"
echo -e "  ${BLUE}Eureka UI    ${NC}→ ${CYAN}http://localhost:8761${NC}"
echo -e "  ${BLUE}Kafka UI     ${NC}→ ${CYAN}http://localhost:8086${NC}"
echo -e "  ${BLUE}Adminer      ${NC}→ ${CYAN}http://localhost:8088${NC}"
echo -e "  ${BLUE}Mongo UI     ${NC}→ ${CYAN}http://localhost:8089${NC}"
echo ""
echo -e "  ${YELLOW}Note: Java services take ~60–90 s to become healthy.${NC}"
echo -e "  ${BLUE}Check status ${NC}→ ${YELLOW}docker ps${NC}"
echo -e "  ${BLUE}View logs    ${NC}→ ${YELLOW}docker compose logs -f <service>${NC}"
echo -e "  ${BLUE}Stop all     ${NC}→ ${YELLOW}./scripts/stop-all.sh${NC}"
echo ""

# ─── DEAD CODE REMOVED ───────────────────────────────────────────────────────
# The old hybrid section below started Java natively and tracked PIDs — that
# logic is no longer needed; everything now runs inside Docker containers.
# ─────────────────────────────────────────────────────────────────────────────
# (end of file)

