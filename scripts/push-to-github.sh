#!/bin/bash
# ============================================================
# push-to-github.sh
# Creates missing GitHub repos and pushes all services
# Usage: ./push-to-github.sh <GITHUB_PAT>
# ============================================================

set -euo pipefail
GITHUB_USER="gowentgonemax"
PAT="${1}"
BASE_DIR="/Users/ravishah/MySpace/ProperyManage/propertize-Services"

if [ -z "$PAT" ]; then
  echo ""
  echo "❌  Usage: ./push-to-github.sh <GITHUB_PERSONAL_ACCESS_TOKEN>"
  echo ""
  echo "  Get a PAT from: https://github.com/settings/tokens/new"
  echo "  Required scopes: repo (full control)"
  echo ""
  exit 1
fi

create_github_repo() {
  local REPO_NAME="$1"
  local DESCRIPTION="$2"
  echo "→ Creating GitHub repo: $GITHUB_USER/$REPO_NAME ..."
  RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST \
    -H "Authorization: token $PAT" \
    -H "Accept: application/vnd.github+json" \
    https://api.github.com/user/repos \
    -d "{\"name\":\"$REPO_NAME\",\"description\":\"$DESCRIPTION\",\"private\":false,\"auto_init\":false}")
  if [ "$RESPONSE" = "201" ]; then
    echo "  ✅  Created: https://github.com/$GITHUB_USER/$REPO_NAME"
  elif [ "$RESPONSE" = "422" ]; then
    echo "  ℹ️   Repo already exists: $REPO_NAME (skipping creation)"
  else
    echo "  ⚠️   Unexpected response $RESPONSE for $REPO_NAME"
  fi
}

push_service() {
  local SVC_DIR="$1"
  local REPO_NAME="$2"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "📦  Pushing: $REPO_NAME"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  cd "$SVC_DIR"

  # Set remote if not already pointing to the right place
  CURRENT_REMOTE=$(git remote get-url origin 2>/dev/null || echo "")
  EXPECTED_REMOTE="git@github.com:$GITHUB_USER/$REPO_NAME.git"
  if [ "$CURRENT_REMOTE" != "$EXPECTED_REMOTE" ]; then
    git remote set-url origin "$EXPECTED_REMOTE" 2>/dev/null || git remote add origin "$EXPECTED_REMOTE"
    echo "  ✓ Remote set to: $EXPECTED_REMOTE"
  fi

  # Push all branches
  CURRENT_BRANCH=$(git --no-pager branch --show-current)
  echo "  Current branch: $CURRENT_BRANCH"

  # Push develop if it exists
  if git --no-pager show-ref --verify --quiet refs/heads/develop; then
    git push origin develop 2>&1 && echo "  ✅  Pushed develop" || echo "  ⚠️   develop push failed"
  fi

  # Push master if it exists
  if git --no-pager show-ref --verify --quiet refs/heads/master; then
    git push origin master 2>&1 && echo "  ✅  Pushed master" || echo "  ⚠️   master push failed"
  fi

  cd "$BASE_DIR"
}

echo ""
echo "============================================================"
echo "  🚀  Propertize GitHub Push Script"
echo "  Account: $GITHUB_USER"
echo "============================================================"

# ── Step 1: Create missing repos ──────────────────────────────
echo ""
echo "Step 1: Creating missing GitHub repositories..."
create_github_repo "auth-service"           "Propertize auth-service: JWT authentication, RBAC, RSA key management"
create_github_repo "api-gateway"            "Propertize api-gateway: Spring Cloud Gateway, rate limiting, circuit breaker"
create_github_repo "service-registry"       "Propertize service-registry: Eureka service discovery"
create_github_repo "employee-service"       "Propertize employee-service: employee management microservice"
create_github_repo "propertize-services"    "Propertize monorepo umbrella: all microservices orchestration"

# ── Step 2: Push individual services ──────────────────────────
echo ""
echo "Step 2: Pushing services to GitHub..."

push_service "$BASE_DIR/auth-service"      "auth-service"
push_service "$BASE_DIR/api-gateway"       "api-gateway"
push_service "$BASE_DIR/service-registry"  "service-registry"
push_service "$BASE_DIR/employee-service"  "employee-service"

# ── Step 3: Push main propertize-Services repo ─────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦  Pushing: propertize-services (umbrella repo)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd "$BASE_DIR"

CURRENT_REMOTE=$(git remote get-url origin 2>/dev/null || echo "")
EXPECTED_REMOTE="git@github.com:$GITHUB_USER/propertize-services.git"
if [ "$CURRENT_REMOTE" != "$EXPECTED_REMOTE" ]; then
  git remote add origin "$EXPECTED_REMOTE" 2>/dev/null || git remote set-url origin "$EXPECTED_REMOTE"
  echo "  ✓ Remote set to: $EXPECTED_REMOTE"
fi

git push -u origin master 2>&1 && echo "  ✅  Pushed master" || echo "  ⚠️   master push failed"

# ── Summary ───────────────────────────────────────────────────
echo ""
echo "============================================================"
echo "  ✅  Done! All repositories pushed to GitHub."
echo ""
echo "  Repos:"
echo "  • https://github.com/$GITHUB_USER/propertize            (core service)"
echo "  • https://github.com/$GITHUB_USER/propertize-react-front-end (frontend)"
echo "  • https://github.com/$GITHUB_USER/auth-service           (auth)"
echo "  • https://github.com/$GITHUB_USER/api-gateway            (gateway)"
echo "  • https://github.com/$GITHUB_USER/service-registry       (eureka)"
echo "  • https://github.com/$GITHUB_USER/employee-service       (employees)"
echo "  • https://github.com/$GITHUB_USER/propertize-services    (umbrella)"
echo "============================================================"

