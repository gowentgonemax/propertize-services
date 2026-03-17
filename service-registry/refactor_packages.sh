#!/bin/bash
set -e

BASE_DIR="/Users/ravishah/MySpace/MyWorkSpace/service-registry"
SRC_DIR="$BASE_DIR/src/main/java"
NEW_BASE="com/propertize/platform/registry"

echo "Starting service-registry package refactoring..."

# Create new package structure
mkdir -p "$SRC_DIR/$NEW_BASE"

# Move files from old location if they exist
if [ -d "$SRC_DIR/com/propertize/registry/config" ]; then
  mkdir -p "$SRC_DIR/$NEW_BASE/config"
  if [ -f "$SRC_DIR/com/propertize/registry/config/SecurityConfig.java" ]; then
    mv "$SRC_DIR/com/propertize/registry/config/SecurityConfig.java" "$SRC_DIR/$NEW_BASE/config/" 2>/dev/null || true
  fi
  # Clean up old directory
  rm -rf "$SRC_DIR/com/propertize/registry"
fi

echo "✅ service-registry package structure refactored"
