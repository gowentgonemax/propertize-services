#!/bin/bash
# =============================================================================
# Propertize — Resume paused/crashed services WITHOUT rebuilding
#
# Use this when:
#   • Docker containers were suspended with `docker pause`
#   • Java services crashed and need restarting from existing JARs
#   • You wake from laptop sleep and services died
#
# Usage:
#   ./scripts/resume-all.sh               # resume everything
#   ./scripts/resume-all.sh --java-only   # only restart crashed Java services
#   ./scripts/resume-all.sh --with-python # also unpause Python services
# =============================================================================

# shellcheck source=_lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/_lib.sh"

# ─── Parse flags ─────────────────────────────────────────────────────────────
JAVA_ONLY=false
WITH_PYTHON=false
for arg in "$@"; do
  case $arg in
    --java-only)   JAVA_ONLY=true ;;
    --with-python) WITH_PYTHON=true ;;
    --help|-h)
      echo "Usage: $0 [--java-only] [--with-python]"
      echo "  --java-only    Only check and restart Java services"
      echo "  --with-python  Also unpause Python services Docker stack"
      exit 0 ;;
  esac
done

print_banner "Propertize  ⏯  Resume All  ($(date '+%H:%M:%S'))"

# ─── Step 1: Unpause Docker containers ───────────────────────────────────────
if [ "$JAVA_ONLY" = false ]; then
  echo -e "${BOLD}[1/3] Docker containers${NC}"
  if docker info > /dev/null 2>&1; then

    # Find and unpause any paused infra containers
    PAUSED=$(docker ps --filter "status=paused" --format "{{.Names}}" 2>/dev/null)
    if [ -n "$PAUSED" ]; then
      echo -e "${CYAN}  ↳ Unpausing containers:${NC}"
      while IFS= read -r container; do
        docker unpause "$container" > /dev/null 2>&1 && \
          echo -e "${GREEN}    ✓ Unpaused: $container${NC}"
      done <<< "$PAUSED"
    else
      echo -e "${GREEN}  ✓ No paused containers${NC}"
    fi

    # Restart any stopped (exited) infra containers
    STOPPED=$(docker ps -a --filter "status=exited" \
      --filter "name=propertize" --format "{{.Names}}" 2>/dev/null)
    if [ -n "$STOPPED" ]; then
      echo -e "${CYAN}  ↳ Restarting stopped containers:${NC}"
      while IFS= read -r container; do
        docker start "$container" > /dev/null 2>&1 && \
          echo -e "${GREEN}    ✓ Restarted: $container${NC}"
      done <<< "$STOPPED"
      echo -e "${CYAN}  ⏳ Waiting 5s for containers to stabilize...${NC}"
      sleep 5
    else
      echo -e "${GREEN}  ✓ All infra containers already running${NC}"
    fi

    # Python services
    if [ "$WITH_PYTHON" = true ] && [ -f "$PYTHON_COMPOSE" ]; then
      echo -e "${CYAN}  ↳ Resuming Python services...${NC}"
      docker compose -f "$PYTHON_COMPOSE" start 2>/dev/null || \
        docker compose -f "$PYTHON_COMPOSE" up -d
      echo -e "${GREEN}  ✓ Python services resumed${NC}"
    fi

  else
    echo -e "${YELLOW}  ⚠  Docker not running — skipping container resume${NC}"
  fi
  echo ""
fi

# ─── Step 2: Check Java setup ────────────────────────────────────────────────
echo -e "${BOLD}[2/3] Environment check${NC}"
load_env
setup_java
check_postgres || echo -e "${YELLOW}  ⚠  PostgreSQL not ready yet (containers may still be starting)${NC}"
echo ""

# ─── Step 3: Restart dead Java services (no rebuild) ─────────────────────────
echo -e "${BOLD}[3/3] Java microservices${NC}"

RESTARTED=0
ALREADY_UP=0
FAILED=0

mkdir -p "$PID_DIR" "$LOG_DIR"

for svc in "${JAVA_SERVICES[@]}"; do
  IFS='|' read -r name port jar_glob dir profile <<< "$svc"

  existing_pid=$(get_pid_for_service "$name")

  if pid_alive "$existing_pid"; then
    echo -e "${GREEN}  ✓ $name running (PID $existing_pid)${NC}"
    ALREADY_UP=$((ALREADY_UP + 1))
    continue
  fi

  if port_in_use "$port"; then
    p=$(lsof -ti ":$port" 2>/dev/null | head -1)
    echo -e "${GREEN}  ✓ $name on :$port (PID ${p:-?}) already in use${NC}"
    [ -n "$p" ] && echo "$p" > "$PID_DIR/$name.pid"
    ALREADY_UP=$((ALREADY_UP + 1))
    continue
  fi

  # Service is dead — restart from existing JAR (no rebuild)
  jar=$(ls "$BASE_DIR/$dir/target/$jar_glob" 2>/dev/null | head -1)
  if [ -z "$jar" ]; then
    echo -e "${RED}  ✗ $name: no JAR found at $dir/target/$jar_glob — run start-all.sh to build${NC}"
    FAILED=$((FAILED + 1))
    continue
  fi

  echo -e "${BLUE}  ▶ Resuming $name ($(basename "$jar"))...${NC}"
  nohup "$JAVA_HOME/bin/java" -jar "$jar" \
    --spring.profiles.active="$profile" \
    >> "$LOG_DIR/$name.log" 2>&1 &
  pid=$!
  echo "$pid" > "$PID_DIR/$name.pid"
  echo -e "${CYAN}    ↳ PID $pid${NC}"

  wait_for_service "$name" "$port" 40
  RESTARTED=$((RESTARTED + 1))
done

# ─── Frontend check ───────────────────────────────────────────────────────────
fe_pid=$(get_pid_for_service "frontend")
if ! pid_alive "$fe_pid" && ! port_in_use 3000 && ! port_in_use 3001; then
  echo -e "${BLUE}  ▶ Resuming Frontend...${NC}"
  start_frontend
  wait_for_frontend
  RESTARTED=$((RESTARTED + 1))
else
  echo -e "${GREEN}  ✓ Frontend running${NC}"
  ALREADY_UP=$((ALREADY_UP + 1))
fi
echo ""

# ─── Summary ──────────────────────────────────────────────────────────────────
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║         ✅  Resume Complete                  ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${GREEN}Already running : $ALREADY_UP${NC}"
echo -e "  ${BLUE}Restarted       : $RESTARTED${NC}"
[ "$FAILED" -gt 0 ] && echo -e "  ${RED}Failed (no JAR) : $FAILED  →  run ./scripts/start-all.sh${NC}"
echo ""
print_service_urls

