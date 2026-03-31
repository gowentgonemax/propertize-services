#!/usr/bin/env python3
"""Validate all API endpoints against the running backend."""
import urllib.request
import json
import sys

BASE = "http://localhost:8080"

def login():
    req = urllib.request.Request(
        f"{BASE}/api/v1/auth/login",
        data=json.dumps({"username": "admin", "password": "Admin@123"}).encode(),
        headers={"Content-Type": "application/json"},
    )
    resp = json.loads(urllib.request.urlopen(req).read())
    print(f"Login OK: {resp['username']} / {resp['roles']}")
    return resp["accessToken"]

def test_endpoint(name, path, token):
    try:
        req = urllib.request.Request(
            f"{BASE}{path}",
            headers={"Authorization": f"Bearer {token}"},
        )
        resp = urllib.request.urlopen(req, timeout=5)
        body = resp.read().decode()[:80]
        print(f"  ✅ {name}: HTTP {resp.status} — {body}")
    except urllib.error.HTTPError as e:
        print(f"  ❌ {name}: HTTP {e.code} — {e.reason}")
    except Exception as e:
        print(f"  ⚠️  {name}: {type(e).__name__} — {e}")

if __name__ == "__main__":
    token = login()
    
    endpoints = [
        ("Audit",             "/api/v1/audit?page=0&size=5"),
        ("Maintenance Stats", "/api/v1/maintenance/statistics"),
        ("Properties",        "/api/v1/properties?page=0&size=5"),
        ("Tenants",           "/api/v1/tenants?page=0&size=5"),
        ("Leases",            "/api/v1/leases?page=0&size=5"),
        ("Invoices",          "/api/v1/invoices?page=0&size=5"),
        ("Vendors",           "/api/v1/vendors?page=0&size=5"),
        ("Tasks",             "/api/v1/tasks?page=0&size=5"),
        ("Employees",         "/api/v1/employees"),
        ("Timesheets",        "/api/v1/timesheets"),
        ("Leave Pending",     "/api/v1/leave/requests/pending"),
        ("Compensation",      "/api/v1/compensation"),
        ("Departments",       "/api/v1/departments/client/1"),
        ("Payments",          "/api/v1/payments"),
        ("RBAC Roles",        "/api/v1/rbac/roles"),
        ("RBAC Permissions",  "/api/v1/rbac/permissions"),
        ("Users",             "/api/v1/users"),
        ("Notifications",     "/api/v1/notifications"),
        ("Assets",            "/api/v1/assets"),
    ]
    
    passed = 0
    failed = 0
    for name, path in endpoints:
        test_endpoint(name, path, token)
        # Simple pass/fail counting
    
    print(f"\nDone — tested {len(endpoints)} endpoints")
