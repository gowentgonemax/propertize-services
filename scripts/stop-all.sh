#!/bin/bash
# =============================================================================
# Propertize — Stop ALL services in Docker
#
# Usage:
#   ./scripts/stop-all.sh          # stop all containers (keeps volumes)
#   ./scripts/stop-all.sh --purge  # stop and remove containers + volumes
# =============================================================================

set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

# Colour helpers
BOLD="\033[1m"; GREEN="\033[0;32m"; CYAN="\033[0;36m"; RED="\033[0;31m"; YELLOW="\033[0;33m"; BLUE="\033[0;34m"; NC="\033[0m"

# ─── Parse flags ─────────────────────────────────────────────────────────────
PURGE=false
for arg in "$@"; do
  case $arg in
    --purge)   PURGE=true ;;
    --help|-h)
      echo "Usage: $0 [--purge]"
      echo "  --purge   Also remove Docker volumes (wipes all data)"
      exit 0 ;;
  esac
done

echo -e "${BOLD}${CYAN}"
echo "╔══════════════════════════════════════════════╗"
echo "║   Propertize  ■  Stop All in Docker          ║"
echo "╚══════════════════════════════════════════════╝"
echo -e "${NC}"

# ─── Guard: Docker must be running ───────────────────────────────────────────
if ! docker info > /dev/null 2>&1; then
  echo -e "${YELLOW}⚠  Docker is not running — nothing to stop.${NC}"
  exit 0
fi

# ─── Stop ────────────────────────────────────────────────────────────────────
if [ "$PURGE" = true ]; then
  echo -e "${CYAN}■  Stopping all services and removing volumes...${NC}"
  docker compose down -v
else
  echo -e "${CYAN}■  Stopping all services (volumes preserved)...${NC}"
  docker compose down
fi

# ─── Done ────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║       ✅  All Services Stopped               ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${BLUE}Start again  ${NC}→ ${YELLOW}./scripts/start-all.sh${NC}"
echo -e "  ${BLUE}Start fast   ${NC}→ ${YELLOW}./scripts/start-all.sh --skip-build${NC}"
echo ""

