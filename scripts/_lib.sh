#!/bin/bash
# =============================================================================
# Propertize Dev Scripts — Shared Library
# Source this file from all other scripts: source "$(dirname "${BASH_SOURCE[0]}")/_lib.sh"
# =============================================================================

# ─── Paths ───────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPTS_DIR")"
PID_DIR="$BASE_DIR/.pids"
LOG_DIR="$BASE_DIR/logs"

# ─── Colors ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m'

# ─── Service Catalog ─────────────────────────────────────────────────────────
# Format: "name|port|jar_glob|service_dir|profile"
# Ordered by startup dependency (registry → auth → core → employee → gateway)
JAVA_SERVICES=(
  "service-registry|8761|service-registry-*.jar|service-registry|local"
  "auth-service|8081|auth-service-*.jar|auth-service|local"
  "propertize|8082|propertize-*.jar|propertize|local"
  "employee-service|8083|employecraft-*.jar|employee-service|local"
  "api-gateway|8080|api-gateway-*.jar|api-gateway|local"
)

# Reverse order for stopping
JAVA_SERVICES_REVERSED=(
  "api-gateway|8080|api-gateway-*.jar|api-gateway|local"
  "employee-service|8083|employecraft-*.jar|employee-service|local"
  "propertize|8082|propertize-*.jar|propertize|local"
  "auth-service|8081|auth-service-*.jar|auth-service|local"
  "service-registry|8761|service-registry-*.jar|service-registry|local"
)

# Docker infra compose file
INFRA_COMPOSE="$BASE_DIR/docker-compose.infra.yml"
PYTHON_COMPOSE="$BASE_DIR/python-services/docker-compose.python.yml"

# ─── Environment Setup ───────────────────────────────────────────────────────
load_env() {
  if [ -f "$BASE_DIR/.env" ]; then
    set -a
    # shellcheck source=/dev/null
    source "$BASE_DIR/.env"
    set +a
    echo -e "${CYAN}  ✓ Loaded .env${NC}"
  fi
}

setup_java() {
  if [ -x "/usr/libexec/java_home" ]; then
    export JAVA_HOME
    JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null \
      || /usr/libexec/java_home -v 21.0 2>/dev/null \
      || /usr/libexec/java_home)
  elif [ -d "/usr/lib/jvm/java-21-openjdk" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
  elif [ -d "/usr/lib/jvm/java-21" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21
  fi

  if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo -e "${RED}✗ JAVA_HOME not set or java not found. Install Java 21.${NC}"
    return 1
  fi

  local ver
  ver=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
  if [ "${ver:-0}" -lt 21 ] 2>/dev/null; then
    echo -e "${RED}✗ Java 21+ required. Found: $ver${NC}"
    return 1
  fi
  echo -e "${CYAN}  ✓ Java: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)${NC}"
  return 0
}

# ─── Port / Process Helpers ──────────────────────────────────────────────────
port_in_use() {
  lsof -i ":$1" > /dev/null 2>&1
}

# Returns 0 (true) if PID is alive, 1 (false) otherwise — always returns cleanly.
pid_alive() {
  local pid="${1:-}"
  if [ -z "$pid" ]; then
    return 1
  fi
  if ps -p "$pid" > /dev/null 2>&1; then
    return 0
  fi
  return 1
}

# Always returns 0 — prints PID if found, empty string if not.
# NOTE: returning 1 from here triggered set -e in callers via $(...) substitution.
get_pid_for_service() {
  local name="$1"
  local pid_file="$PID_DIR/$name.pid"
  if [ -f "$pid_file" ]; then
    cat "$pid_file"
  fi
  return 0   # CRITICAL: always succeed so $(...) callers don't see exit-code 1
}

# ─── Wait for Health ─────────────────────────────────────────────────────────
wait_for_service() {
  local name="$1" port="$2" max="${3:-40}"
  echo -ne "${YELLOW}  ⏳ Waiting for $name on :$port${NC}"
  local i=1
  while [ "$i" -le "$max" ]; do
    if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
      echo -e " ${GREEN}✓${NC}"
      return 0
    fi
    echo -n "."
    sleep 2
    i=$((i + 1))
  done
  echo -e " ${RED}✗ timed out — check logs/$name.log${NC}"
  return 1
}

wait_for_frontend() {
  local max="${1:-20}"
  echo -ne "${YELLOW}  ⏳ Waiting for Frontend on :3000${NC}"
  local i=1
  while [ "$i" -le "$max" ]; do
    if curl -sf "http://localhost:3000" > /dev/null 2>&1; then
      echo -e " ${GREEN}✓${NC}"
      return 0
    fi
    echo -n "."
    sleep 2
    i=$((i + 1))
  done
  echo -e " ${YELLOW} (still warming up — check logs/frontend.log)${NC}"
  return 0
}

# ─── Build ───────────────────────────────────────────────────────────────────
# Uses a temp log file so the real Maven exit code is captured without pipe
# interference. With set -e + '| tail -3', tail's exit code 0 masks mvn failures.
build_service() {
  local name="$1"
  local service_dir="$BASE_DIR/$name"

  echo -e "${CYAN}  🔨 Building $name...${NC}"

  if [ ! -d "$service_dir" ]; then
    echo -e "${RED}  ✗ Directory not found: $service_dir${NC}"
    return 1
  fi

  # Ensure wrapper is executable
  [ -f "$service_dir/mvnw" ] && chmod +x "$service_dir/mvnw"

  local mvn_cmd
  if [ -f "$service_dir/mvnw" ]; then
    mvn_cmd="$service_dir/mvnw"
  elif command -v mvn &>/dev/null; then
    mvn_cmd="mvn"
  else
    echo -e "${RED}  ✗ Neither mvnw nor mvn found for $name${NC}"
    return 1
  fi

  mkdir -p "$LOG_DIR"
  local build_log="$LOG_DIR/build-${name}.log"

  # Run Maven in a subshell so we don't change the parent's CWD.
  # Redirect ALL output to the log file; capture exit code directly (no pipe).
  (cd "$service_dir" && $mvn_cmd clean package -DskipTests) \
    > "$build_log" 2>&1
  local rc=$?

  if [ "$rc" -ne 0 ]; then
    echo -e "${RED}  ✗ Build FAILED (exit $rc). Last 25 lines of $build_log:${NC}"
    tail -25 "$build_log"
    return 1
  fi

  echo -e "${GREEN}  ✓ Build successful${NC}"
  return 0
}

# ─── Start a Java Service ────────────────────────────────────────────────────
start_java_service() {
  local name="$1" port="$2" jar_glob="$3" dir="$4" profile="$5"
  local full_dir="$BASE_DIR/$dir"
  local pid_file="$PID_DIR/$name.pid"

  mkdir -p "$PID_DIR" "$LOG_DIR"

  # Check if already running via PID file
  local existing_pid
  existing_pid=$(get_pid_for_service "$name")   # always exits 0 now
  if pid_alive "$existing_pid"; then
    echo -e "${GREEN}  ✓ $name already running (PID $existing_pid)${NC}"
    return 0
  fi

  # Check if port is already taken by another process
  if port_in_use "$port"; then
    local p
    p=$(lsof -ti ":$port" 2>/dev/null | head -1) || true
    echo -e "${GREEN}  ✓ $name port :$port already in use (PID ${p:-?}) — skipping${NC}"
    if [ -n "$p" ]; then
      echo "$p" > "$pid_file"
    fi
    return 0
  fi

  # Find JAR
  local jar
  jar=$(ls "$full_dir"/target/$jar_glob 2>/dev/null | head -1) || true
  if [ -z "$jar" ]; then
    echo -e "${RED}  ✗ No JAR found for $name ($jar_glob). Build first.${NC}"
    return 1
  fi

  echo -e "${BLUE}  ▶ Starting $name from $(basename "$jar")...${NC}"
  nohup "$JAVA_HOME/bin/java" -jar "$jar" \
    --spring.profiles.active="$profile" \
    >> "$LOG_DIR/$name.log" 2>&1 &
  local pid=$!
  echo "$pid" > "$pid_file"
  echo -e "${CYAN}  ↳ PID $pid | log: logs/$name.log${NC}"
  return 0
}

# ─── Stop a Java Service ─────────────────────────────────────────────────────
stop_service() {
  local name="$1"
  local pid_file="$PID_DIR/$name.pid"
  local port=""

  for svc in "${JAVA_SERVICES[@]}"; do
    IFS='|' read -r sname sport _ <<< "$svc"
    if [ "$sname" = "$name" ]; then
      port="$sport"
      break
    fi
  done

  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if pid_alive "$pid"; then
      echo -e "${YELLOW}  🛑 Stopping $name (PID $pid)...${NC}"
      kill "$pid" 2>/dev/null || true
      local waited=0
      while pid_alive "$pid" && [ "$waited" -lt 10 ]; do
        sleep 1; waited=$((waited+1))
      done
      if pid_alive "$pid"; then
        echo -e "${YELLOW}    ↳ Force-killing $name...${NC}"
        kill -9 "$pid" 2>/dev/null || true
      fi
      echo -e "${GREEN}  ✓ $name stopped${NC}"
    else
      echo -e "${YELLOW}  ⚠  $name: stale PID file (process already gone)${NC}"
    fi
    rm -f "$pid_file"
  elif [ -n "$port" ] && port_in_use "$port"; then
    local p
    p=$(lsof -ti ":$port" 2>/dev/null | head -1) || true
    if [ -n "$p" ]; then
      echo -e "${YELLOW}  🛑 Stopping $name on :$port (PID $p, no PID file)...${NC}"
      kill "$p" 2>/dev/null || true
      sleep 2
      pid_alive "$p" && { kill -9 "$p" 2>/dev/null || true; }
      echo -e "${GREEN}  ✓ $name stopped${NC}"
    fi
  else
    echo -e "${YELLOW}  ℹ  $name: not running${NC}"
  fi
}

# ─── Stop Frontend ───────────────────────────────────────────────────────────
stop_frontend() {
  local pid_file="$PID_DIR/frontend.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if pid_alive "$pid"; then
      echo -e "${YELLOW}  🛑 Stopping Frontend (PID $pid)...${NC}"
      kill "$pid" 2>/dev/null || true
      sleep 2
      pid_alive "$pid" && { kill -9 "$pid" 2>/dev/null || true; }
      echo -e "${GREEN}  ✓ Frontend stopped${NC}"
    else
      echo -e "${YELLOW}  ⚠  Frontend: stale PID (process gone)${NC}"
    fi
    rm -f "$pid_file"
  elif port_in_use 3000 || port_in_use 3001; then
    local p
    p=$(lsof -ti ":3000" 2>/dev/null | head -1) || true
    [ -z "$p" ] && { p=$(lsof -ti ":3001" 2>/dev/null | head -1) || true; }
    if [ -n "$p" ]; then
      echo -e "${YELLOW}  🛑 Stopping Frontend (PID $p)...${NC}"
      kill "$p" 2>/dev/null || true; sleep 2
      echo -e "${GREEN}  ✓ Frontend stopped${NC}"
    fi
  else
    echo -e "${YELLOW}  ℹ  Frontend: not running${NC}"
  fi
}

# ─── Start Frontend ──────────────────────────────────────────────────────────
start_frontend() {
  local fe_dir="$BASE_DIR/propertize-front-end"
  local pid_file="$PID_DIR/frontend.pid"

  if port_in_use 3000 || port_in_use 3001; then
    echo -e "${GREEN}  ✓ Frontend already running${NC}"
    return 0
  fi

  if [ ! -d "$fe_dir" ]; then
    echo -e "${RED}  ✗ Frontend directory not found: $fe_dir${NC}"
    return 1
  fi

  (cd "$fe_dir" || return 1
   if [ ! -d "node_modules" ]; then
     echo -e "${CYAN}  📦 Installing npm dependencies...${NC}"
     npm install --silent
   fi)

  echo -e "${BLUE}  ▶ Starting Frontend...${NC}"
  (cd "$fe_dir" && nohup npm run dev >> "$LOG_DIR/frontend.log" 2>&1 &
   echo $! > "$pid_file")
  echo -e "${CYAN}  ↳ log: logs/frontend.log${NC}"
  return 0
}

# ─── PostgreSQL Check ────────────────────────────────────────────────────────
check_postgres() {
  echo -ne "${YELLOW}  ⏳ Checking PostgreSQL...${NC}"
  if psql -h localhost -U dbuser -d propertize_db -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e " ${GREEN}✓${NC}"
    return 0
  else
    echo -e " ${RED}✗ Not accessible. Ensure PostgreSQL is running.${NC}"
    return 1
  fi
}

# ─── Print Banner ────────────────────────────────────────────────────────────
print_banner() {
  local title="$1"
  echo ""
  echo -e "${BOLD}${BLUE}╔══════════════════════════════════════════════╗${NC}"
  printf "${BOLD}${BLUE}║${NC}  %-44s${BOLD}${BLUE}║${NC}\n" "$title"
  echo -e "${BOLD}${BLUE}╚══════════════════════════════════════════════╝${NC}"
  echo ""
}

# ─── Print Summary ───────────────────────────────────────────────────────────
print_service_urls() {
  echo ""
  echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
  echo -e "${BOLD}${GREEN}║           Service URLs                       ║${NC}"
  echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
  echo -e "  ${CYAN}API Gateway     ${NC}→ ${YELLOW}http://localhost:8080${NC}"
  echo -e "  ${CYAN}Auth Service    ${NC}→ ${YELLOW}http://localhost:8081${NC}"
  echo -e "  ${CYAN}Propertize Core ${NC}→ ${YELLOW}http://localhost:8082${NC}"
  echo -e "  ${CYAN}Employee Svc    ${NC}→ ${YELLOW}http://localhost:8083${NC}"
  echo -e "  ${CYAN}Service Registry${NC}→ ${YELLOW}http://localhost:8761${NC}  (admin/admin)"
  echo -e "  ${CYAN}Frontend        ${NC}→ ${YELLOW}http://localhost:3000${NC}"
  echo ""
  echo -e "  ${MAGENTA}Kafka UI        ${NC}→ ${YELLOW}http://localhost:8090${NC}"
  echo -e "  ${MAGENTA}Mongo Express   ${NC}→ ${YELLOW}http://localhost:8089${NC}  (admin/admin)"
  echo ""
  echo -e "  ${BLUE}Logs dir        ${NC}→ ${YELLOW}$LOG_DIR/${NC}"
  echo -e "  ${BLUE}Tail a log      ${NC}→ ${YELLOW}tail -f logs/<name>.log${NC}"
  echo ""
}

