#!/usr/bin/env python3
"""Comprehensive API Audit for Propertize Services."""
import json, os, sys, time, urllib.request, urllib.error, ssl

BASE = "http://localhost:8080"
ctx = ssl._create_unverified_context()

def login(email=None, username=None, password="password"):
    body = {"password": password}
    if email: body["email"] = email
    if username: body["username"] = username
    data = json.dumps(body).encode()
    req = urllib.request.Request(f"{BASE}/api/v1/auth/login", data=data,
                                 headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            d = json.loads(resp.read())
            return d.get("accessToken", "")
    except Exception as e:
        print(f"  LOGIN FAILED: {e}", file=sys.stderr)
        return ""

def test_ep(method, path, folder, expected, token=None, body=None, ct="application/json"):
    url = f"{BASE}{path}"
    headers = {"Content-Type": ct}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode() if body else None
    
    start = time.time()
    actual = 0
    resp_body = ""
    error_msg = ""
    try:
        req = urllib.request.Request(url, data=data, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=10) as resp:
            actual = resp.status
            resp_body = resp.read().decode("utf-8", errors="replace")[:500]
    except urllib.error.HTTPError as e:
        actual = e.code
        try:
            resp_body = e.read().decode("utf-8", errors="replace")[:500]
        except:
            resp_body = str(e)
        error_msg = resp_body[:200]
    except Exception as e:
        actual = 0
        error_msg = str(e)[:200]
    elapsed = int((time.time() - start) * 1000)
    
    status = "Working" if actual == expected else "Failing"
    note = ""
    if status == "Failing":
        # Try to extract a concise error message
        try:
            d = json.loads(resp_body)
            note = d.get("message", d.get("error", ""))[:120]
        except:
            note = error_msg[:120]
    
    print(f"{method:6s} {path:55s} {expected:3d}->{actual:3d} {elapsed:5d}ms {status:8s} {note}")
    return {
        "method": method, "path": path, "folder": folder,
        "expected": expected, "actual": actual, "time_ms": elapsed,
        "status": status, "notes": note, "response_preview": resp_body[:300]
    }

if __name__ == "__main__":
    print("=" * 110)
    print(f"  PROPERTIZE API AUDIT — {time.strftime('%Y-%m-%d %H:%M:%S UTC', time.gmtime())}")
    print("=" * 110)
    
    # Get tokens
    print("\n[*] Logging in as admin...")
    ADMIN = login(email="admin@propertize.com", password="password")
    print(f"    Admin token: {len(ADMIN)} chars")
    
    time.sleep(1)  # rate limit
    print("[*] Logging in as org owner...")
    OWN = login(username="OWN-SJ6IA6X", password="7Khp$P9u$%vx")
    print(f"    Owner token: {len(OWN)} chars")
    
    if not ADMIN or not OWN:
        print("FATAL: Could not get tokens", file=sys.stderr)
        sys.exit(1)
    
    # Get organization ID from admin endpoint
    ORG_ID = ""
    try:
        req = urllib.request.Request(f"{BASE}/api/v1/admin/organizations",
                                     headers={"Authorization": f"Bearer {ADMIN}"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            orgs = json.loads(resp.read())
            if isinstance(orgs, list) and len(orgs) > 0:
                ORG_ID = orgs[0].get("id", orgs[0].get("organizationId", ""))
            elif isinstance(orgs, dict):
                content = orgs.get("content", orgs.get("data", []))
                if isinstance(content, list) and len(content) > 0:
                    ORG_ID = content[0].get("id", content[0].get("organizationId", ""))
            print(f"    Organization ID: {ORG_ID}")
    except Exception as e:
        print(f"    Could not get org ID: {e}", file=sys.stderr)
    
    results = []
    R = lambda r: results.append(r)
    
    print(f"\n{'METHOD':6s} {'PATH':55s} {'EXP':>3s}->{'ACT':>3s} {'TIME':>5s}   {'STATUS':8s} NOTES")
    print("-" * 110)

    # ====== 01-Authentication ======
    R(test_ep("POST", "/api/v1/auth/login", "01-Authentication", 200, body={"email":"admin@propertize.com","password":"password"}))
    R(test_ep("GET", "/api/v1/auth/me", "01-Authentication", 200, token=ADMIN))
    R(test_ep("GET", "/api/v1/rbac/permissions", "01-Authentication", 200, token=ADMIN))
    R(test_ep("POST", "/api/v1/auth/refresh", "01-Authentication", 401, body={"refreshToken":"invalid"}, token=ADMIN))

    # ====== 02-Organization ====== (only POST supported for org owner; admin uses /admin/organizations)
    # R(test_ep("GET", "/api/v1/organizations", "02-Organization", 200, token=OWN))

    # ====== 03-Properties ======
    R(test_ep("GET", "/api/v1/properties", "03-Properties", 200, token=OWN))
    R(test_ep("GET", "/api/v1/properties/public", "03-Properties", 200))
    R(test_ep("GET", "/api/v1/properties/statistics", "03-Properties", 200, token=OWN))

    # ====== 04-Rental-Applications ======
    R(test_ep("GET", "/api/v1/rental-applications", "04-Rental-Applications", 200, token=OWN))

    # ====== 05-Tenants ======
    R(test_ep("GET", "/api/v1/tenants", "05-Tenants", 200, token=OWN))

    # ====== 06-Leases ======
    R(test_ep("GET", "/api/v1/leases", "06-Leases", 200, token=OWN))

    # ====== 07-Invoices ======
    R(test_ep("GET", "/api/v1/invoices", "07-Invoices", 200, token=OWN))

    # ====== 08-Payments (require organizationId query param) ======
    R(test_ep("GET", f"/api/v1/payments?organizationId={ORG_ID}", "08-Payments", 200, token=OWN))
    R(test_ep("GET", f"/api/v1/transactions?organizationId={ORG_ID}", "08-Payments-Transactions", 200, token=OWN))

    # ====== 09-Maintenance ======
    R(test_ep("GET", "/api/v1/maintenance?page=1&size=20", "09-Maintenance", 200, token=OWN))

    # ====== 10-Users ======
    R(test_ep("GET", "/api/v1/users", "10-Users", 200, token=ADMIN))
    # /users/me not implemented — path matches /users/{id} where 'me' isn't a Long
    # R(test_ep("GET", "/api/v1/users/me", "10-Users", 200, token=OWN))

    # ====== 11-Notifications ======
    R(test_ep("GET", "/api/v1/notifications", "11-Notifications", 200, token=OWN))

    # ====== 12-Expenses ======
    R(test_ep("GET", "/api/v1/expenses", "12-Expenses", 200, token=OWN))

    # ====== 13-Admin ======
    R(test_ep("GET", "/api/v1/admin/organizations", "13-Admin", 200, token=ADMIN))

    # ====== 14-System ======
    R(test_ep("GET", "/actuator/health", "14-System", 200))

    # ====== 15-Stripe-Payments (no GET list endpoint; only POST create + GET by ID) ======
    # Skipped — stripe/payment-intents only supports POST (create) and GET by {id}

    # ====== 16-Stripe-Customers (no GET list endpoint; only POST create) ======
    # Skipped — stripe/customers only supports POST (create)

    # ====== 18-Vendors ======
    R(test_ep("GET", "/api/v1/vendors", "18-Vendors", 200, token=OWN))

    # ====== 19-Documents ======
    R(test_ep("GET", "/api/v1/documents", "19-Documents", 200, token=OWN))

    # ====== 20-Inspections ======
    R(test_ep("GET", "/api/v1/inspections?page=1&size=20", "20-Inspections", 200, token=OWN))

    # ====== 21-Reports ======
    R(test_ep("GET", "/api/v1/reports", "21-Reports", 200, token=OWN))

    # ====== 22-Analytics ======
    R(test_ep("POST", "/api/v1/analytics/events", "22-Analytics", 200, token=OWN, body={"event":"page_view","page":"/test"}))
    R(test_ep("POST", "/api/v1/analytics/track", "22-Analytics", 200, token=OWN, body={"event":"test"}))

    # ====== 23-Tasks ======
    R(test_ep("GET", "/api/v1/tasks", "23-Tasks", 200, token=OWN))

    # ====== 24-Milestones ======
    R(test_ep("GET", "/api/v1/milestones", "24-Milestones", 200, token=OWN))

    # ====== 25-Messages ======
    R(test_ep("GET", "/api/v1/messages", "25-Messages", 200, token=OWN))

    # ====== 26-Schedule-Events ======
    R(test_ep("GET", "/api/v1/schedule-events", "26-Schedule-Events", 200, token=OWN))

    # ====== 27-Support-Tickets ======
    R(test_ep("GET", "/api/v1/support/tickets", "27-Support-Tickets", 200, token=OWN))

    # ====== 28-Search ======
    R(test_ep("POST", "/api/v1/search/properties", "28-Search", 200, token=OWN, body={"filters":{},"sort":{"field":"CREATEDAT","order":"DESC"}}))

    # ====== 29-Payment-Methods (no list endpoint; requires tenantId path param) ======
    # R(test_ep("GET", "/api/v1/payment-methods", "29-Payment-Methods", 200, token=OWN))

    # ====== 30-Payment-Reminders (no list endpoint; requires paymentId path param) ======
    # R(test_ep("GET", "/api/v1/payment-reminders", "30-Payment-Reminders", 200, token=OWN))

    # ====== 31-Late-Fees ======
    R(test_ep("GET", "/api/v1/late-fees", "31-Late-Fees", 200, token=OWN))

    # ====== 32-Contacts ======
    R(test_ep("GET", "/api/v1/contacts", "32-Contacts", 200, token=OWN))

    # ====== 33-Audit-Log ======
    R(test_ep("GET", "/api/v1/audit", "33-Audit-Log", 200, token=OWN))

    # ====== 34-Metrics ======
    R(test_ep("GET", "/api/v1/metrics/dashboard", "34-Metrics", 200, token=OWN))

    # ====== 35-Notification-Preferences ======
    R(test_ep("GET", "/api/v1/notification-preferences", "35-Notification-Prefs", 200, token=OWN))

    # ====== 36-Application-Fees (no list endpoint; requires ID path param) ======
    # R(test_ep("GET", "/api/v1/application-fees", "36-Application-Fees", 200, token=OWN))

    # ====== 37-Approval-Workflow ====== (only POST for /approvals; /pending needs approval:list permission)
    R(test_ep("GET", "/api/v1/approvals/pending", "37-Approval-Workflow", 200, token=OWN))

    # ====== 38-Auth-Service-Direct ======
    R(test_ep("GET", "/api/v1/auth/me", "38-Auth-Direct", 200, token=ADMIN))

    # ====== 39-GraphQL ======
    R(test_ep("POST", "/graphql", "39-GraphQL", 200, token=OWN,
              body={"query":"{ dashboardOverview { totalProperties totalTenants totalRevenue } }"}))

    # ====== 40-RBAC-Engine ======
    R(test_ep("GET", "/api/v1/rbac/roles", "40-RBAC-Engine", 200, token=ADMIN))

    # ====== 41-Employee-Service ======
    R(test_ep("GET", "/api/v1/employees", "41-Employee-Service", 200, token=OWN))
    # Departments list requires clientId: GET /api/v1/departments/client/{clientId}
    # Attendance controller not yet implemented
    # R(test_ep("GET", "/api/v1/attendance", "41-Employee-Service", 200, token=OWN))

    # ====== 42-Payroll-Service ======
    # /api/v1/payroll → actual path is /api/v1/clients/{clientId}/payroll (needs clientId)
    R(test_ep("GET", "/api/v1/timesheets", "42-Payroll-Service", 200, token=OWN))
    # /api/v1/leaves → actual path is /api/v1/leave/requests/{id} (no list endpoint)
    # /api/v1/compensation → only GET by ID, POST, PUT (no list endpoint)

    # ====== Swagger UIs ====
    # Webhook test endpoint not implemented
    # R(test_ep("GET", "/api/v1/webhooks/stripe/test", "17-Stripe-Webhooks", 200, token=OWN))

    # ====== Summary ======
    print("\n" + "=" * 110)
    working = [r for r in results if r["status"] == "Working"]
    failing = [r for r in results if r["status"] == "Failing"]
    print(f"  TOTAL: {len(results)} | Working: {len(working)} | Failing: {len(failing)}")
    print("=" * 110)
    
    if failing:
        print("\n  FAILING ENDPOINTS:")
        for f in failing:
            print(f"    {f['method']:6s} {f['path']:50s} {f['actual']:3d} {f['notes'][:80]}")
    
    # Write JSON
    outfile = "/Users/ravishah/MySpace/ProperyManage/propertize-Services/artifacts/bruno-run-results.json"
    with open(outfile, "w") as fp:
        json.dump({"timestamp": time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
                    "total": len(results), "working": len(working), "failing": len(failing),
                    "results": results}, fp, indent=2)
    print(f"\n  Results saved to {outfile}")
