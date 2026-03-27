#!/bin/bash
# =============================================================================
# Propertize — Restart ALL services with full rebuild
#
# Usage:
#   ./scripts/restart-all.sh              # stop everything, rebuild, restart all
#   ./scripts/restart-all.sh --keep-infra # keep Docker running, just restart Java
#   ./scripts/restart-all.sh --with-python
# =============================================================================
set -euo pipefail
# shellcheck source=_lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/_lib.sh"

# ─── Parse flags and pass through ────────────────────────────────────────────
KEEP_INFRA_FLAG=""
WITH_PYTHON_FLAG=""
for arg in "$@"; do
  case $arg in
    --keep-infra)  KEEP_INFRA_FLAG="--keep-infra" ;;
    --with-python) WITH_PYTHON_FLAG="--with-python" ;;
    --help|-h)
      echo "Usage: $0 [--keep-infra] [--with-python]"
      echo "  --keep-infra   Keep Docker infrastructure containers between restart"
      echo "  --with-python  Include Python services Docker stack"
      exit 0 ;;
  esac
done

print_banner "Propertize  🔄  Restart All  ($(date '+%H:%M:%S'))"

# ─── Phase 1: Stop everything ────────────────────────────────────────────────
echo -e "${BOLD}━━━  Phase 1: Stopping all services  ━━━${NC}"
echo ""
# shellcheck source=stop-all.sh
bash "$SCRIPTS_DIR/stop-all.sh" $KEEP_INFRA_FLAG $WITH_PYTHON_FLAG

echo -e "${CYAN}  ⏳ Waiting 3s for ports to free up...${NC}"
sleep 3
echo ""

# ─── Phase 2: Rebuild & Start ────────────────────────────────────────────────
echo -e "${BOLD}━━━  Phase 2: Rebuilding & starting  ━━━${NC}"
echo ""
# shellcheck source=start-all.sh
bash "$SCRIPTS_DIR/start-all.sh" $WITH_PYTHON_FLAG

echo ""
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║   🎉  Restart Complete  (with fresh build)   ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""

