#!/bin/bash
# =============================================================================
# Propertize — Backward-compatible startup wrapper
# All scripts have been consolidated into scripts/ directory.
# This file is kept for convenience — it delegates to scripts/start-all.sh
#
# ┌─────────────────────────────────────────────────────────────────────────┐
# │  Prefer using the scripts in scripts/ directly:                         │
# │                                                                         │
# │  scripts/start-all.sh        ← Start infra + microservices + frontend  │
# │  scripts/stop-all.sh         ← Stop all services                       │
# │  scripts/restart-all.sh      ← Stop, rebuild, restart everything       │
# │  scripts/resume-all.sh       ← Resume paused/crashed services          │
# │  scripts/start-services.sh   ← Start only Java microservices (no infra)│
# └─────────────────────────────────────────────────────────────────────────┘
# =============================================================================

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$BASE_DIR/scripts/start-all.sh" "$@"
