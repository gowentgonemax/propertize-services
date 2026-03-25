#!/bin/bash
# =============================================================================
# Propertize — Start ONLY Java microservices with build (no infra, no frontend)
#
# Use this when:
#   • Docker infrastructure is already running
#   • You want to rebuild and restart only the Java services
#   • Database / Redis / Kafka are up — you just need the apps
#
# Usage:
#   ./scripts/start-services.sh                    # build + start all Java services
#   ./scripts/start-services.sh --skip-build       # start from existing JARs
#   ./scripts/start-services.sh --service auth-service   # single service only
#   ./scripts/start-services.sh --with-frontend    # also start the frontend
# =============================================================================
set -e

# shellcheck source=_lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/_lib.sh"

# ─── Parse flags ─────────────────────────────────────────────────────────────
SKIP_BUILD=false
WITH_FRONTEND=false
TARGET_SERVICE=""
for arg in "$@"; do
  case $arg in
    --skip-build)    SKIP_BUILD=true ;;
    --with-frontend) WITH_FRONTEND=true ;;
    --service)       shift; TARGET_SERVICE=$1 ;;
    --service=*)     TARGET_SERVICE="${arg#--service=}" ;;
    --help|-h)
      echo "Usage: $0 [--skip-build] [--with-frontend] [--service <name>]"
      echo ""
      echo "  --skip-build         Use existing JARs, skip Maven build"
      echo "  --with-frontend      Also start the Next.js frontend"
      echo "  --service <name>     Start only one service (e.g. auth-service, propertize)"
      echo ""
      echo "  Available service names:"
      for svc in "${JAVA_SERVICES[@]}"; do
        IFS='|' read -r name port _ _ _ <<< "$svc"
        printf "    %-22s (port %s)\n" "$name" "$port"
      done
      exit 0 ;;
  esac
done

print_banner "Propertize  ⚙  Start Services  ($(date '+%H:%M:%S'))"

# ─── Environment & Java ──────────────────────────────────────────────────────
echo -e "${BOLD}[0] Environment setup${NC}"
load_env
setup_java
check_postgres || echo -e "${YELLOW}  ⚠  PostgreSQL check failed — ensure infra is running first${NC}"
echo ""

# ─── Build & Start ───────────────────────────────────────────────────────────
mkdir -p "$PID_DIR" "$LOG_DIR"

STEP=1
TOTAL=${#JAVA_SERVICES[@]}

for svc in "${JAVA_SERVICES[@]}"; do
  IFS='|' read -r name port jar_glob dir profile <<< "$svc"

  # If a specific service was requested, skip others
  if [ -n "$TARGET_SERVICE" ] && [ "$name" != "$TARGET_SERVICE" ]; then
    continue
  fi

  echo -e "${BOLD}[$STEP/$TOTAL] $name${NC}"

  # Stop existing instance if running (so we can replace it)
  existing_pid=$(get_pid_for_service "$name")
  if pid_alive "$existing_pid"; then
    echo -e "${YELLOW}  ↻ Service already running — stopping PID $existing_pid for restart${NC}"
    stop_service "$name"
    sleep 1
  elif port_in_use "$port"; then
    echo -e "${YELLOW}  ↻ Port :$port in use — stopping process for restart${NC}"
    stop_service "$name"
    sleep 1
  fi

  # Build
  if [ "$SKIP_BUILD" = false ]; then
    build_service "$dir"
  else
    jar=$(ls "$BASE_DIR/$dir/target/$jar_glob" 2>/dev/null | head -1)
    if [ -z "$jar" ]; then
      echo -e "${YELLOW}  ⚠  No JAR — building anyway...${NC}"
      build_service "$dir"
    else
      echo -e "${CYAN}  ↳ Skipping build, using: $(basename "$jar")${NC}"
    fi
  fi

  start_java_service "$name" "$port" "$jar_glob" "$dir" "$profile"
  wait_for_service "$name" "$port"
  STEP=$((STEP + 1))
  echo ""
done

# ─── Optional: Frontend ───────────────────────────────────────────────────────
if [ "$WITH_FRONTEND" = true ]; then
  echo -e "${BOLD}[+] Frontend${NC}"
  start_frontend
  wait_for_frontend
  echo ""
fi

# ─── Done ────────────────────────────────────────────────────────────────────
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║    ✅  Java Services Ready                   ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
print_service_urls

echo -e "  ${BLUE}Stop all       ${NC}→ ${YELLOW}./scripts/stop-all.sh${NC}"
echo -e "  ${BLUE}Restart all    ${NC}→ ${YELLOW}./scripts/restart-all.sh${NC}"
echo -e "  ${BLUE}Single service ${NC}→ ${YELLOW}./scripts/start-services.sh --service <name>${NC}"
echo ""

