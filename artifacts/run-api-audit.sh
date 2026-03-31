#!/usr/bin/env bash
# Comprehensive API Audit Script for Propertize Services
# Hits every Bruno collection endpoint and records results as JSON
set -euo pipefail

BASE="http://localhost:8080"
RESULTS_FILE="/Users/ravishah/MySpace/ProperyManage/propertize-Services/artifacts/bruno-run-results.json"

# Tokens must be set in environment
if [[ -z "${ADMIN_TOKEN:-}" || -z "${OWN_TOKEN:-}" ]]; then
  echo "ERROR: ADMIN_TOKEN and OWN_TOKEN must be set" >&2
  exit 1
fi

# Helper: test an endpoint
# Usage: test_ep METHOD PATH FOLDER EXPECTED_STATUS [TOKEN] [BODY] [CONTENT_TYPE]
results=()
test_ep() {
  local method="$1" path="$2" folder="$3" expected="$4"
  local token="${5:-$OWN_TOKEN}" body="${6:-}" ct="${7:-application/json}"
  
  local url="${BASE}${path}"
  local curl_args=(-s -w '\n{"_http_code":%{http_code},"_time_ms":%{time_total}}' --max-time 10)
  curl_args+=(-H "Authorization: Bearer ${token}")
  curl_args+=(-H "Content-Type: ${ct}")
  curl_args+=(-X "${method}")
  
  if [[ -n "$body" ]]; then
    curl_args+=(-d "$body")
  fi

  local raw
  raw=$(curl "${curl_args[@]}" "$url" 2>/dev/null || echo '{"_error":"curl_failed"}{"_http_code":0,"_time_ms":0}')
  
  # Extract the last JSON object (our metadata)
  local meta
  meta=$(echo "$raw" | grep -o '{"_http_code":[^}]*}' | tail -1)
  local resp_body
  resp_body=$(echo "$raw" | sed 's/{"_http_code":[^}]*}$//')
  
  local actual_code
  actual_code=$(echo "$meta" | python3 -c "import sys,json; print(json.load(sys.stdin).get('_http_code',0))" 2>/dev/null || echo "0")
  local time_ms
  time_ms=$(echo "$meta" | python3 -c "import sys,json; print(int(json.load(sys.stdin).get('_time_ms',0)*1000))" 2>/dev/null || echo "0")
  
  local status="Working"
  if [[ "$actual_code" != "$expected" ]]; then
    status="Failing"
  fi
  
  # Truncate body for storage
  local short_body
  short_body=$(echo "$resp_body" | head -c 500)
  
  printf '%-7s %-50s %3s -> %3s  %4sms  %-8s\n' "$method" "$path" "$expected" "$actual_code" "$time_ms" "$status"
  
  # Append to results array
  results+=("{\"method\":\"$method\",\"path\":\"$path\",\"folder\":\"$folder\",\"expected\":$expected,\"actual\":$actual_code,\"time_ms\":$time_ms,\"status\":\"$status\"}")
}

echo "============================================================"
echo "  PROPERTIZE API AUDIT - $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "============================================================"
echo ""
echo "METHOD  PATH                                               EXP -> ACT   TIME  STATUS"
echo "------- -------------------------------------------------- --- -- ----  ----  --------"

# ====== 01-Authentication ======
test_ep POST "/api/v1/auth/login" "01-Authentication" 200 "NONE" '{"email":"admin@propertize.com","password":"password"}' "application/json"
test_ep GET "/api/v1/auth/me" "01-Authentication" 200 "$ADMIN_TOKEN"
test_ep GET "/api/v1/auth/permissions" "01-Authentication" 200 "$ADMIN_TOKEN"

# ====== 02-Organization ======
test_ep GET "/api/v1/organizations" "02-Organization" 200 "$ADMIN_TOKEN"

# ====== 03-Properties ======
test_ep GET "/api/v1/properties" "03-Properties" 200 "$OWN_TOKEN"
test_ep GET "/api/v1/properties/public" "03-Properties" 200 "NONE"
test_ep GET "/api/v1/properties/statistics" "03-Properties" 200 "$OWN_TOKEN"

# ====== 04-Rental-Applications ======
test_ep GET "/api/v1/rental-applications" "04-Rental-Applications" 200 "$OWN_TOKEN"

# ====== 05-Tenants ======
test_ep GET "/api/v1/tenants" "05-Tenants" 200 "$OWN_TOKEN"

# ====== 06-Leases ======
test_ep GET "/api/v1/leases" "06-Leases" 200 "$OWN_TOKEN"

# ====== 07-Invoices ======
test_ep GET "/api/v1/invoices" "07-Invoices" 200 "$OWN_TOKEN"

# ====== 08-Payments ======
test_ep GET "/api/v1/payments" "08-Payments" 200 "$OWN_TOKEN"

# ====== 09-Maintenance ======
test_ep GET "/api/v1/maintenance" "09-Maintenance" 200 "$OWN_TOKEN"

# ====== 10-Users ======
test_ep GET "/api/v1/users" "10-Users" 200 "$ADMIN_TOKEN"
test_ep GET "/api/v1/users/me" "10-Users" 200 "$OWN_TOKEN"

# ====== 11-Notifications ======
test_ep GET "/api/v1/notifications" "11-Notifications" 200 "$OWN_TOKEN"

# ====== 12-Expenses ======
test_ep GET "/api/v1/expenses" "12-Expenses" 200 "$OWN_TOKEN"

# ====== 13-Admin ======
test_ep GET "/api/v1/admin/organizations" "13-Admin" 200 "$ADMIN_TOKEN"

# ====== 14-System ======
test_ep GET "/actuator/health" "14-System" 200 "$ADMIN_TOKEN"

# ====== 15-Stripe-Payments ======
test_ep GET "/api/v1/stripe/payment-intents" "15-Stripe-Payments" 200 "$OWN_TOKEN"

# ====== 16-Stripe-Customers ======
test_ep GET "/api/v1/stripe/customers" "16-Stripe-Customers" 200 "$OWN_TOKEN"

# ====== 18-Vendors ======
test_ep GET "/api/v1/vendors" "18-Vendors" 200 "$OWN_TOKEN"

# ====== 19-Documents ======
test_ep GET "/api/v1/documents" "19-Documents" 200 "$OWN_TOKEN"

# ====== 20-Inspections ======
test_ep GET "/api/v1/inspections" "20-Inspections" 200 "$OWN_TOKEN"

# ====== 21-Reports ======
test_ep GET "/api/v1/reports" "21-Reports" 200 "$OWN_TOKEN"

# ====== 22-Analytics ======
test_ep POST "/api/v1/analytics/events" "22-Analytics" 200 "NONE" '{"event":"page_view","page":"/test"}' "application/json"

# ====== 23-Tasks ======
test_ep GET "/api/v1/tasks" "23-Tasks" 200 "$OWN_TOKEN"

# ====== 24-Milestones ======
test_ep GET "/api/v1/milestones" "24-Milestones" 200 "$OWN_TOKEN"

# ====== 25-Messages ======
test_ep GET "/api/v1/messages" "25-Messages" 200 "$OWN_TOKEN"

# ====== 26-Schedule-Events ======
test_ep GET "/api/v1/schedule-events" "26-Schedule-Events" 200 "$OWN_TOKEN"

# ====== 27-Support-Tickets ======
test_ep GET "/api/v1/support/tickets" "27-Support-Tickets" 200 "$OWN_TOKEN"

# ====== 28-Search ======
test_ep POST "/api/v1/search/properties" "28-Search" 200 "$OWN_TOKEN" '{"filters":{},"sort":"createdAt"}' "application/json"

# ====== 29-Payment-Methods ======
test_ep GET "/api/v1/payment-methods" "29-Payment-Methods" 200 "$OWN_TOKEN"

# ====== 30-Payment-Reminders ======
test_ep GET "/api/v1/payment-reminders" "30-Payment-Reminders" 200 "$OWN_TOKEN"

# ====== 31-Late-Fees ======
test_ep GET "/api/v1/late-fees" "31-Late-Fees" 200 "$OWN_TOKEN"

# ====== 32-Contacts ======
test_ep GET "/api/v1/contacts" "32-Contacts" 200 "$OWN_TOKEN"

# ====== 33-Audit-Log ======
test_ep GET "/api/v1/audit" "33-Audit-Log" 200 "$OWN_TOKEN"

# ====== 34-Metrics ======
test_ep GET "/api/v1/metrics/dashboard" "34-Metrics" 200 "$OWN_TOKEN"

# ====== 35-Notification-Preferences ======
test_ep GET "/api/v1/notification-preferences" "35-Notification-Preferences" 200 "$OWN_TOKEN"

# ====== 36-Application-Fees ======
test_ep GET "/api/v1/application-fees" "36-Application-Fees" 200 "$OWN_TOKEN"

# ====== 37-Approval-Workflow ======
test_ep GET "/api/v1/approvals" "37-Approval-Workflow" 200 "$OWN_TOKEN"
test_ep GET "/api/v1/approvals/pending" "37-Approval-Workflow" 200 "$OWN_TOKEN"

# ====== 38-Auth-Service-Direct ======
test_ep GET "/api/v1/auth/health" "38-Auth-Service-Direct" 200 "NONE"

# ====== 39-GraphQL ======
test_ep POST "/graphql" "39-GraphQL" 200 "$OWN_TOKEN" '{"query":"{ dashboardOverview { totalProperties totalTenants totalRevenue } }"}' "application/json"

# ====== 40-RBAC-Engine ======
test_ep GET "/api/v1/rbac/roles" "40-RBAC-Engine" 200 "$ADMIN_TOKEN"

# ====== 41-Employee-Service ======
test_ep GET "/api/v1/employees" "41-Employee-Service" 200 "$OWN_TOKEN"
test_ep GET "/api/v1/departments" "41-Employee-Service" 200 "$OWN_TOKEN"

# ====== 42-Payroll-Service ======
test_ep GET "/api/v1/payroll" "42-Payroll-Service" 200 "$OWN_TOKEN"
test_ep GET "/api/v1/timesheets" "42-Payroll-Service" 200 "$OWN_TOKEN"
test_ep GET "/api/v1/leaves" "42-Payroll-Service" 200 "$OWN_TOKEN"

# ====== Additional endpoints from user error reports ======
test_ep GET "/api/v1/transactions" "08-Payments" 200 "$OWN_TOKEN"

# ====== Swagger UI ======
test_ep GET "/swagger-ui/index.html" "14-System" 200 "NONE"

echo ""
echo "============================================================"
echo "  AUDIT COMPLETE"
echo "============================================================"

# Write JSON results
{
  echo "["
  local first=1
  for r in "${results[@]}"; do
    if [[ $first -eq 1 ]]; then
      echo "  $r"
      first=0
    else
      echo "  ,$r"
    fi
  done
  echo "]"
} > "$RESULTS_FILE"

echo "Results written to $RESULTS_FILE"
