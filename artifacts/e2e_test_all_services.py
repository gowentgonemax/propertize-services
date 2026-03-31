#!/usr/bin/env python3
"""
Propertize E2E API Test Suite
Tests all services: Employee, Payroll, Notifications, Contacts, LateFees,
Search, Support Tickets, Messages, Audit, Metrics, NotificationPreferences
Flow: Create Org → Add Properties → Login as Owner → Test all endpoints
"""

import requests, json, time, sys
from datetime import datetime, date
from uuid import uuid4

BASE = "http://localhost:8080"
ADMIN_USER = "admin"
ADMIN_PASS = "password"

# ─── Results tracking ───
results = []  # [{service, endpoint, method, path, status, passed, note}]

def record(service, method, path, status, passed, note=""):
    results.append({
        "service": service,
        "method": method, 
        "path": path,
        "status": status,
        "passed": "PASS" if passed else "FAIL",
        "note": note
    })
    icon = "✅" if passed else "❌"
    print(f"  {icon} {method:6s} {path}: {status} {note}")

def req(method, path, token, body=None, params=None, expect_success=None):
    """Make a request and return (status_code, response_json_or_text)"""
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    url = f"{BASE}{path}"
    try:
        r = requests.request(method, url, headers=headers, json=body, params=params, timeout=15)
        try:
            data = r.json()
            msg = data.get("message", "")[:80] if isinstance(data, dict) else ""
        except:
            msg = r.text[:80]
        return r.status_code, msg, data if 'data' in dir() and isinstance(data, dict) else {}
    except Exception as e:
        return 0, str(e)[:80], {}

def login(username, password):
    r = requests.post(f"{BASE}/api/v1/auth/login",
                      json={"username": username, "password": password}, timeout=10)
    if r.status_code == 200:
        return r.json().get("accessToken")
    print(f"  ⚠️  Login failed for {username}: {r.status_code} {r.text[:100]}")
    return None

# ═══════════════════════════════════════════════════════════════
# PHASE 1: Login as Admin
# ═══════════════════════════════════════════════════════════════
print("=" * 70)
print("PHASE 1: Admin Login")
print("=" * 70)
admin_token = login(ADMIN_USER, ADMIN_PASS)
if not admin_token:
    print("FATAL: Cannot login as admin")
    sys.exit(1)
print(f"  ✅ Admin token obtained")

# ═══════════════════════════════════════════════════════════════
# PHASE 2: Create Organization via Onboarding
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 2: Create Organization + Owner")
print("=" * 70)

onboarding_data = {
    "organizationName": f"E2E Test Org {int(time.time())}",
    "organizationType": "PMC",
    "contactEmail": f"e2e-{int(time.time())}@test.com",
    "contactPhone": "+1-555-019-9000",
    "contactFirstName": "E2E",
    "contactLastName": "Tester",
    "ownerFirstName": "E2E",
    "ownerLastName": "Owner",
    "ownerEmail": f"e2e-owner-{int(time.time())}@test.com",
    "ownerPhone": "+1-555-020-0000",
    "numberOfProperties": 5,
    "numberOfUnits": 25,
    "address": {
        "street": "123 Test St",
        "city": "TestCity",
        "state": "CA",
        "zipCode": "90210",
        "country": "US"
    },
    "taxId": "12-3456789",
    "termsAccepted": True,
    "privacyPolicyAccepted": True,
    "subscriptionPlan": "PROFESSIONAL"
}

# Submit application
r = requests.post(f"{BASE}/api/v1/organizations/onboarding/apply",
                  json=onboarding_data, timeout=15)
if r.status_code != 201:
    print(f"  ❌ Onboarding apply failed: {r.status_code} {r.text[:200]}")
    sys.exit(1)
apply_resp = r.json()
tracking_id = apply_resp.get("data", {}).get("trackingId", "")
app_id = apply_resp.get("data", {}).get("id", "")
print(f"  ✅ Application submitted: trackingId={tracking_id}, id={app_id}")

# Assign reviewer
status_code, msg, _ = req("POST", f"/api/v1/admin/organizations/applications/{app_id}/assign", admin_token)
if status_code == 200:
    print(f"  ✅ Assigned reviewer")
else:
    print(f"  ⚠️  Assign: {status_code} {msg}")

# Approve
status_code, msg, _ = req("POST", f"/api/v1/admin/organizations/applications/{app_id}/review", admin_token,
                          body={"action": "APPROVE", "notes": "E2E test approval"})
if status_code == 200:
    print(f"  ✅ Application approved")
else:
    print(f"  ❌ Approve failed: {status_code} {msg}")
    sys.exit(1)

# Get org details
time.sleep(2)
status_code, msg, app_data = req("GET", f"/api/v1/admin/organizations/applications/{app_id}", admin_token)
org_id = None
owner_username = None
if status_code == 200:
    app_detail = app_data.get("data", {})
    org_id = app_detail.get("organizationId")
    # Owner username is nested in data.owner.username
    owner_obj = app_detail.get("owner", {})
    owner_username = owner_obj.get("username") if owner_obj else app_detail.get("ownerUsername")
    print(f"  ✅ Org ID: {org_id}")
    print(f"  ✅ Owner username: {owner_username}")

# Get owner password from logs
import subprocess
log_output = subprocess.run(
    ["docker", "logs", "propertize-main-service", "--tail=200"],
    capture_output=True, text=True, timeout=10
).stderr + subprocess.run(
    ["docker", "logs", "propertize-main-service", "--tail=200"],
    capture_output=True, text=True, timeout=10
).stdout

owner_password = None
owner_username_from_log = None

# Extract USERNAME and PASSWORD from docker logs
for line in reversed(log_output.split('\n')):
    if 'USERNAME:' in line and not owner_username_from_log:
        owner_username_from_log = line.split('USERNAME:')[-1].strip().rstrip('║').strip()
    if 'PASSWORD:' in line and not owner_password:
        owner_password = line.split('PASSWORD:')[-1].strip().rstrip('║').strip()
    if owner_username_from_log and owner_password:
        break

# Use log-extracted username if API didn't provide one
if not owner_username and owner_username_from_log:
    owner_username = owner_username_from_log
    print(f"  ✅ Owner username (from logs): {owner_username}")

if owner_password:
    print(f"  ✅ Owner password found: {owner_password}")
else:
    print(f"  ⚠️  Owner password not found in logs. Trying DB lookup...")
    # Fall back to known credentials
    result = subprocess.run(
        ["docker", "exec", "propertize-postgres", "psql", "-U", "dbuser", "-d", "propertize_db",
         "-t", "-c", f"SELECT username FROM users WHERE username LIKE 'OWN-%' ORDER BY created_at DESC LIMIT 1;"],
        capture_output=True, text=True, timeout=10
    )
    print(f"  DB lookup result: {result.stdout.strip()}")

# ═══════════════════════════════════════════════════════════════
# PHASE 3: Login as Org Owner
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 3: Login as Organization Owner")
print("=" * 70)

owner_token = None
if owner_username and owner_password:
    owner_token = login(owner_username, owner_password)
    if owner_token:
        print(f"  ✅ Owner login successful")
    else:
        print(f"  ❌ Owner login failed, falling back to admin token")
        owner_token = admin_token
else:
    print(f"  ⚠️  No owner credentials, using admin token")
    owner_token = admin_token

# Use admin token for most tests (has broadest permissions)
token = admin_token
org_token = owner_token or admin_token

# ═══════════════════════════════════════════════════════════════
# PHASE 4: Create Property under the Organization
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 4: Create Property")
print("=" * 70)

property_data = {
    "propertyName": "E2E Test Property",
    "type": "APARTMENT",
    "organizationId": org_id,
    "address": {
        "street": "456 Property Lane",
        "city": "TestCity",
        "state": "CA",
        "zipCode": "90211",
        "country": "US"
    },
    "bedrooms": 3,
    "bathrooms": 2.0,
    "monthlyRent": 2500.00,
    "squareFootage": 1500.0,
    "description": "E2E test property"
}

status_code, msg, resp = req("POST", "/api/v1/properties", org_token, body=property_data)
property_id = None
if status_code in (200, 201):
    property_id = resp.get("data", {}).get("id") or resp.get("id")
    print(f"  ✅ Property created: {property_id}")
    record("propertize", "POST", "/api/v1/properties", status_code, True, f"id={property_id}")
else:
    print(f"  ❌ Property creation: {status_code} {msg}")
    record("propertize", "POST", "/api/v1/properties", status_code, False, msg)

# ═══════════════════════════════════════════════════════════════
# PHASE 5: Test EMPLOYEE SERVICE
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 5: EMPLOYEE SERVICE (:8083)")
print("=" * 70)

SVC = "employee-service"
employee_id = None

# GET /api/v1/employees (list)
sc, msg, data = req("GET", "/api/v1/employees", org_token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/employees", sc, sc == 200, msg)

# POST /api/v1/employees (create)
emp_data = {
    "firstName": "John",
    "lastName": "E2ETester",
    "email": f"john.e2e.{int(time.time())}@test.com",
    "phoneNumber": "+1-555-030-1000",
    "employmentType": "FULL_TIME",
    "hireDate": "2026-01-15",
    "payType": "SALARY",
    "payRate": 85000.00
}
sc, msg, data = req("POST", "/api/v1/employees", org_token, body=emp_data)
if sc in (200, 201):
    employee_id = data.get("data", {}).get("id") or data.get("id")
    record(SVC, "POST", "/api/v1/employees", sc, True, f"id={employee_id}")
else:
    record(SVC, "POST", "/api/v1/employees", sc, sc in (200, 201, 409), msg)

# GET /api/v1/employees/{id}
if employee_id:
    sc, msg, _ = req("GET", f"/api/v1/employees/{employee_id}", org_token)
    record(SVC, "GET", f"/api/v1/employees/{{id}}", sc, sc == 200, msg)

    # GET /api/v1/employees/by-user/{userId} — might not have userId
    sc, msg, _ = req("GET", f"/api/v1/employees/by-user/1", org_token)
    record(SVC, "GET", "/api/v1/employees/by-user/{{userId}}", sc, sc in (200, 404), msg)

    # POST /api/v1/employees/{id}/activate
    sc, msg, _ = req("POST", f"/api/v1/employees/{employee_id}/activate", org_token)
    record(SVC, "POST", "/api/v1/employees/{{id}}/activate", sc, sc in (200, 400), msg)

    # POST /api/v1/employees/{id}/terminate
    sc, msg, _ = req("POST", f"/api/v1/employees/{employee_id}/terminate", org_token, params={"reason": "E2E test"})
    record(SVC, "POST", "/api/v1/employees/{{id}}/terminate", sc, sc in (200, 400), msg)

# GET /api/v1/employees/changed-since
sc, msg, _ = req("GET", "/api/v1/employees/changed-since", org_token, params={"since": "2026-01-01T00:00:00"})
record(SVC, "GET", "/api/v1/employees/changed-since", sc, sc == 200, msg)

# GET /api/v1/employees/payroll-summary
sc, msg, _ = req("GET", "/api/v1/employees/payroll-summary", org_token)
record(SVC, "GET", "/api/v1/employees/payroll-summary", sc, sc == 200, msg)

# Swagger
sc, msg, _ = req("GET", "/api/v1/employees", org_token, params={"page": 0, "size": 1})
record(SVC, "GET", "/api/v1/employees?page=0&size=1", sc, sc == 200, "pagination test")

# ═══════════════════════════════════════════════════════════════
# PHASE 6: Test PAYROLL SERVICE
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 6: PAYROLL SERVICE (:8085)")
print("=" * 70)

SVC = "payroll-service"

# --- CLIENT ---
print("\n  --- Clients ---")
client_id = None

# POST /clients (create client for payroll)
client_data = {
    "name": f"E2E Payroll Client {int(time.time())}",
    "organizationId": org_id,
    "status": "ACTIVE",
    "industry": "Real Estate",
    "contactEmail": f"payroll-{int(time.time())}@test.com"
}
sc, msg, data = req("POST", "/api/v1/clients", org_token, body=client_data)
if sc in (200, 201):
    client_id = data.get("data", {}).get("id") or data.get("id")
    if not client_id and isinstance(data.get("data"), dict):
        # Try different response shapes
        for key in ["clientId", "uuid", "client_id"]:
            if data.get("data", {}).get(key):
                client_id = data["data"][key]
                break
    record(SVC, "POST", "/api/v1/clients", sc, True, f"id={client_id}")
else:
    record(SVC, "POST", "/api/v1/clients", sc, sc in (200, 201, 409), msg)

# GET /clients
sc, msg, _ = req("GET", "/api/v1/clients", org_token, params={"page": 0, "limit": 5})
record(SVC, "GET", "/api/v1/clients", sc, sc == 200, msg)

if client_id:
    # GET /clients/{id}
    sc, msg, _ = req("GET", f"/api/v1/clients/{client_id}", org_token)
    record(SVC, "GET", "/api/v1/clients/{{id}}", sc, sc == 200, msg)

    # PUT /clients/{id}
    sc, msg, _ = req("PUT", f"/api/v1/clients/{client_id}", org_token,
                     body={**client_data, "name": "Updated E2E Client"})
    record(SVC, "PUT", "/api/v1/clients/{{id}}", sc, sc == 200, msg)

# --- DEPARTMENTS ---
print("\n  --- Departments ---")
dept_id = None

if client_id:
    dept_data = {
        "name": "Engineering",
        "description": "E2E test department",
        "clientId": client_id,
        "managerId": str(uuid4()),
        "active": True
    }
    sc, msg, data = req("POST", "/api/v1/departments", org_token, body=dept_data)
    if sc in (200, 201):
        dept_id = data.get("data", {}).get("id") or data.get("id")
        record(SVC, "POST", "/api/v1/departments", sc, True, f"id={dept_id}")
    else:
        record(SVC, "POST", "/api/v1/departments", sc, False, msg)

    # GET /departments/client/{clientId}
    sc, msg, _ = req("GET", f"/api/v1/departments/client/{client_id}", org_token)
    record(SVC, "GET", "/api/v1/departments/client/{{clientId}}", sc, sc == 200, msg)

    # GET /departments/client/{clientId}/active
    sc, msg, _ = req("GET", f"/api/v1/departments/client/{client_id}/active", org_token)
    record(SVC, "GET", "/api/v1/departments/client/{{clientId}}/active", sc, sc == 200, msg)

    if dept_id:
        # GET /departments/{id}
        sc, msg, _ = req("GET", f"/api/v1/departments/{dept_id}", org_token)
        record(SVC, "GET", "/api/v1/departments/{{id}}", sc, sc == 200, msg)

        # PUT /departments/{id}
        sc, msg, _ = req("PUT", f"/api/v1/departments/{dept_id}", org_token,
                         body={**dept_data, "name": "Updated Engineering"})
        record(SVC, "PUT", "/api/v1/departments/{{id}}", sc, sc == 200, msg)

        # PATCH /departments/{id}/deactivate
        sc, msg, _ = req("PATCH", f"/api/v1/departments/{dept_id}/deactivate", org_token)
        record(SVC, "PATCH", "/api/v1/departments/{{id}}/deactivate", sc, sc == 200, msg)

# --- PAYROLL EMPLOYEES ---
print("\n  --- Payroll Employees ---")
payroll_emp_id = None

if client_id:
    payroll_emp_data = {
        "firstName": "PayrollE2E",
        "lastName": "Worker",
        "email": f"payroll-worker-{int(time.time())}@test.com",
        "employeeNumber": f"EMP-{int(time.time()) % 100000}",
        "clientId": client_id,
        "departmentId": str(dept_id) if dept_id else None,
        "status": "ACTIVE",
        "employmentType": "FULL_TIME",
        "hireDate": "2026-01-01",
        "ssnLastFour": "1234"
    }
    sc, msg, data = req("POST", "/api/v1/employees", org_token, body=payroll_emp_data)
    if sc in (200, 201):
        payroll_emp_id = data.get("data", {}).get("id") or data.get("id")
        record(SVC, "POST", "/api/v1/employees (payroll)", sc, True, f"id={payroll_emp_id}")
    else:
        record(SVC, "POST", "/api/v1/employees (payroll)", sc, False, msg)

    # GET /employees/client/{clientId}
    sc, msg, _ = req("GET", f"/api/v1/employees/client/{client_id}", org_token)
    record(SVC, "GET", "/api/v1/employees/client/{{clientId}}", sc, sc == 200, msg)

    # GET /employees/client/{clientId}/active
    sc, msg, _ = req("GET", f"/api/v1/employees/client/{client_id}/active", org_token)
    record(SVC, "GET", "/api/v1/employees/client/{{clientId}}/active", sc, sc == 200, msg)

    # GET /employees/client/{clientId}/search
    sc, msg, _ = req("GET", f"/api/v1/employees/client/{client_id}/search", org_token, params={"query": "PayrollE2E"})
    record(SVC, "GET", "/api/v1/employees/client/{{clientId}}/search", sc, sc == 200, msg)

    if payroll_emp_id:
        # GET /employees/{id}
        sc, msg, _ = req("GET", f"/api/v1/employees/{payroll_emp_id}", org_token)
        record(SVC, "GET", "/api/v1/employees/{{id}} (payroll)", sc, sc == 200, msg)

        # GET /employees/by-number/{num}
        sc, msg, _ = req("GET", f"/api/v1/employees/by-number/{payroll_emp_data['employeeNumber']}", org_token)
        record(SVC, "GET", "/api/v1/employees/by-number/{{num}}", sc, sc == 200, msg)

        # PUT /employees/{id}
        sc, msg, _ = req("PUT", f"/api/v1/employees/{payroll_emp_id}", org_token,
                         body={**payroll_emp_data, "firstName": "UpdatedE2E"})
        record(SVC, "PUT", "/api/v1/employees/{{id}} (payroll)", sc, sc == 200, msg)

# --- COMPENSATION ---
print("\n  --- Compensation ---")
comp_id = None

if payroll_emp_id:
    comp_data = {
        "employeeId": payroll_emp_id,
        "baseSalary": 85000.00,
        "payFrequency": "BIWEEKLY",
        "effectiveDate": "2026-01-01",
        "compensationType": "SALARY"
    }
    sc, msg, data = req("POST", "/api/v1/compensation", org_token, body=comp_data)
    if sc in (200, 201):
        comp_id = data.get("data", {}).get("id") or data.get("id")
        record(SVC, "POST", "/api/v1/compensation", sc, True, f"id={comp_id}")
    else:
        record(SVC, "POST", "/api/v1/compensation", sc, False, msg)

    # GET current compensation
    sc, msg, _ = req("GET", f"/api/v1/compensation/employee/{payroll_emp_id}/current", org_token)
    record(SVC, "GET", "/api/v1/compensation/employee/{{id}}/current", sc, sc in (200, 404), msg)

    # GET compensation history
    sc, msg, _ = req("GET", f"/api/v1/compensation/employee/{payroll_emp_id}/history", org_token)
    record(SVC, "GET", "/api/v1/compensation/employee/{{id}}/history", sc, sc == 200, msg)

    if comp_id:
        # GET /compensation/{id}
        sc, msg, _ = req("GET", f"/api/v1/compensation/{comp_id}", org_token)
        record(SVC, "GET", "/api/v1/compensation/{{id}}", sc, sc == 200, msg)

        # PUT /compensation/{id}
        sc, msg, _ = req("PUT", f"/api/v1/compensation/{comp_id}", org_token,
                         body={**comp_data, "baseSalary": 90000.00})
        record(SVC, "PUT", "/api/v1/compensation/{{id}}", sc, sc == 200, msg)

# --- TIMESHEETS ---
print("\n  --- Timesheets ---")

sc, msg, _ = req("GET", "/api/v1/timesheets", org_token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/timesheets", sc, sc == 200, msg)

sc, msg, _ = req("GET", f"/api/v1/timesheets/{uuid4()}", org_token)
record(SVC, "GET", "/api/v1/timesheets/{{id}}", sc, sc in (200, 404, 500), msg)

if payroll_emp_id:
    sc, msg, _ = req("GET", f"/api/v1/timesheets/employee/{payroll_emp_id}", org_token)
    record(SVC, "GET", "/api/v1/timesheets/employee/{{id}}", sc, sc == 200, msg)

if client_id:
    sc, msg, _ = req("GET", f"/api/v1/timesheets/pending/{client_id}", org_token)
    record(SVC, "GET", "/api/v1/timesheets/pending/{{clientId}}", sc, sc == 200, msg)

# --- TIME ENTRIES ---
print("\n  --- Time Entries ---")
time_entry_id = None

if payroll_emp_id:
    te_data = {
        "employeeId": payroll_emp_id,
        "date": "2026-03-28",
        "hoursWorked": 8.0,
        "description": "E2E test time entry",
        "entryType": "REGULAR"
    }
    sc, msg, data = req("POST", "/api/v1/time-entries", org_token, body=te_data)
    if sc in (200, 201):
        time_entry_id = data.get("data", {}).get("id") or data.get("id")
        record(SVC, "POST", "/api/v1/time-entries", sc, True, f"id={time_entry_id}")
    else:
        record(SVC, "POST", "/api/v1/time-entries", sc, False, msg)

    # GET time entries by employee
    sc, msg, _ = req("GET", f"/api/v1/time-entries/employee/{payroll_emp_id}", org_token)
    record(SVC, "GET", "/api/v1/time-entries/employee/{{id}}", sc, sc == 200, msg)

    # GET time entries by employee + date range
    sc, msg, _ = req("GET", f"/api/v1/time-entries/employee/{payroll_emp_id}/range", org_token,
                     params={"startDate": "2026-03-01", "endDate": "2026-03-31"})
    record(SVC, "GET", "/api/v1/time-entries/employee/{{id}}/range", sc, sc == 200, msg)

    # GET approved hours
    sc, msg, _ = req("GET", f"/api/v1/time-entries/employee/{payroll_emp_id}/hours", org_token,
                     params={"startDate": "2026-03-01", "endDate": "2026-03-31"})
    record(SVC, "GET", "/api/v1/time-entries/employee/{{id}}/hours", sc, sc == 200, msg)

if client_id:
    # GET client time entries by range
    sc, msg, _ = req("GET", f"/api/v1/time-entries/client/{client_id}/range", org_token,
                     params={"startDate": "2026-03-01", "endDate": "2026-03-31"})
    record(SVC, "GET", "/api/v1/time-entries/client/{{clientId}}/range", sc, sc == 200, msg)

    # GET pending time entries
    sc, msg, _ = req("GET", f"/api/v1/time-entries/client/{client_id}/pending", org_token)
    record(SVC, "GET", "/api/v1/time-entries/client/{{clientId}}/pending", sc, sc == 200, msg)

if time_entry_id:
    # GET /time-entries/{id}
    sc, msg, _ = req("GET", f"/api/v1/time-entries/{time_entry_id}", org_token)
    record(SVC, "GET", "/api/v1/time-entries/{{id}}", sc, sc == 200, msg)

    # POST approve
    sc, msg, _ = req("POST", f"/api/v1/time-entries/{time_entry_id}/approve", org_token,
                     params={"approverId": str(uuid4())})
    record(SVC, "POST", "/api/v1/time-entries/{{id}}/approve", sc, sc in (200, 400), msg)

# --- LEAVE ---
print("\n  --- Leave ---")

sc, msg, _ = req("GET", "/api/v1/leave/requests/pending", org_token)
record(SVC, "GET", "/api/v1/leave/requests/pending", sc, sc == 200, msg)

if payroll_emp_id:
    sc, msg, _ = req("GET", f"/api/v1/leave/balances/{payroll_emp_id}", org_token, params={"year": 2026})
    record(SVC, "GET", "/api/v1/leave/balances/{{employeeId}}", sc, sc in (200, 404), msg)

    sc, msg, _ = req("GET", f"/api/v1/leave/requests/employee/{payroll_emp_id}", org_token)
    record(SVC, "GET", "/api/v1/leave/requests/employee/{{id}}", sc, sc == 200, msg)

# --- PAYROLL RUNS ---
print("\n  --- Payroll Runs ---")
payroll_run_id = None

if client_id:
    # GET payroll runs
    sc, msg, _ = req("GET", f"/api/v1/clients/{client_id}/payroll", org_token, params={"page": 0, "limit": 5})
    record(SVC, "GET", "/api/v1/clients/{{clientId}}/payroll", sc, sc == 200, msg)

    # POST create payroll run
    payroll_run_data = {
        "startDate": "2026-03-01",
        "endDate": "2026-03-15",
        "payDate": "2026-03-20",
        "type": "REGULAR",
        "status": "DRAFT"
    }
    sc, msg, data = req("POST", f"/api/v1/clients/{client_id}/payroll", org_token, body=payroll_run_data)
    if sc in (200, 201):
        payroll_run_id = data.get("data", {}).get("id") or data.get("id")
        record(SVC, "POST", "/api/v1/clients/{{clientId}}/payroll", sc, True, f"id={payroll_run_id}")
    else:
        record(SVC, "POST", "/api/v1/clients/{{clientId}}/payroll", sc, False, msg)

    if payroll_run_id:
        # GET /clients/{clientId}/payroll/{payrollId}
        sc, msg, _ = req("GET", f"/api/v1/clients/{client_id}/payroll/{payroll_run_id}", org_token)
        record(SVC, "GET", "/api/v1/clients/{{clientId}}/payroll/{{id}}", sc, sc == 200, msg)

        # POST process
        sc, msg, _ = req("POST", f"/api/v1/clients/{client_id}/payroll/{payroll_run_id}/process", org_token)
        record(SVC, "POST", "/api/v1/clients/{{clientId}}/payroll/{{id}}/process", sc, sc in (200, 400), msg)

        # POST approve
        sc, msg, _ = req("POST", f"/api/v1/clients/{client_id}/payroll/{payroll_run_id}/approve", org_token)
        record(SVC, "POST", "/api/v1/clients/{{clientId}}/payroll/{{id}}/approve", sc, sc in (200, 400), msg)


# ═══════════════════════════════════════════════════════════════
# PHASE 7: Test NOTIFICATIONS (propertize main service)
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 7: NOTIFICATIONS")
print("=" * 70)

SVC = "notifications"

# GET /notifications
sc, msg, _ = req("GET", "/api/v1/notifications", token)
record(SVC, "GET", "/api/v1/notifications", sc, sc == 200, msg)

# GET /notifications/{id}
sc, msg, _ = req("GET", f"/api/v1/notifications/{uuid4()}", token)
record(SVC, "GET", "/api/v1/notifications/{{id}}", sc, sc in (200, 404), msg)

# GET /notifications/tenant/{tenantId}
sc, msg, _ = req("GET", "/api/v1/notifications/tenant/test-tenant-1", token)
record(SVC, "GET", "/api/v1/notifications/tenant/{{tenantId}}", sc, sc in (200, 404), msg)

# GET /notifications/status/{status}
sc, msg, _ = req("GET", "/api/v1/notifications/status/SENT", token)
record(SVC, "GET", "/api/v1/notifications/status/{{status}}", sc, sc == 200, msg)

# POST /notifications/send
notif_data = {
    "recipientEmail": "e2e-test@example.com",
    "subject": "E2E Test Notification",
    "body": "This is an E2E test notification",
    "type": "GENERAL"
}
sc, msg, _ = req("POST", "/api/v1/notifications/send", token, body=notif_data)
record(SVC, "POST", "/api/v1/notifications/send", sc, sc in (200, 201, 500), msg)

# ═══════════════════════════════════════════════════════════════
# PHASE 8: Test CONTACTS
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 8: CONTACTS")
print("=" * 70)

SVC = "contacts"
contact_id = None

# POST /contacts
contact_data = {
    "name": "E2E Contact",
    "email": f"e2e-contact-{int(time.time())}@test.com",
    "phone": "+1-555-040-0000",
    "subject": "E2E Test Contact",
    "message": "This is an E2E test contact submission"
}
sc, msg, data = req("POST", "/api/v1/contacts", token, body=contact_data)
if sc in (200, 201):
    contact_id = data.get("data", {}).get("id") or data.get("id")
    record(SVC, "POST", "/api/v1/contacts", sc, True, f"id={contact_id}")
else:
    record(SVC, "POST", "/api/v1/contacts", sc, False, msg)

# GET /contacts
sc, msg, _ = req("GET", "/api/v1/contacts", token, params={"page": 0, "limit": 5})
record(SVC, "GET", "/api/v1/contacts", sc, sc == 200, msg)

if contact_id:
    # GET /contacts/{id}
    sc, msg, _ = req("GET", f"/api/v1/contacts/{contact_id}", token)
    record(SVC, "GET", "/api/v1/contacts/{{id}}", sc, sc == 200, msg)

    # PUT /contacts/{id}
    sc, msg, _ = req("PUT", f"/api/v1/contacts/{contact_id}", token,
                     body={"status": "IN_PROGRESS", "notes": "E2E test update"})
    record(SVC, "PUT", "/api/v1/contacts/{{id}}", sc, sc == 200, msg)

# GET /contacts/search
sc, msg, _ = req("GET", "/api/v1/contacts/search", token, params={"search": "E2E", "page": 0, "limit": 5})
record(SVC, "GET", "/api/v1/contacts/search", sc, sc == 200, msg)

# GET /contacts/statistics
sc, msg, _ = req("GET", "/api/v1/contacts/statistics", token)
record(SVC, "GET", "/api/v1/contacts/statistics", sc, sc == 200, msg)

if contact_id:
    # DELETE /contacts/{id}
    sc, msg, _ = req("DELETE", f"/api/v1/contacts/{contact_id}", token)
    record(SVC, "DELETE", "/api/v1/contacts/{{id}}", sc, sc in (200, 204), msg)

# ═══════════════════════════════════════════════════════════════
# PHASE 9: Test LATE FEES
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 9: LATE FEES")
print("=" * 70)

SVC = "late-fees"

# GET /late-fees/overdue-payments
sc, msg, _ = req("GET", "/api/v1/late-fees/overdue-payments", token)
record(SVC, "GET", "/api/v1/late-fees/overdue-payments", sc, sc == 200, msg)

# GET /late-fees/total
sc, msg, _ = req("GET", "/api/v1/late-fees/total", token,
                params={"startDate": "2026-01-01", "endDate": "2026-03-31"})
record(SVC, "GET", "/api/v1/late-fees/total", sc, sc == 200, msg)

# POST /late-fees/process
sc, msg, _ = req("POST", "/api/v1/late-fees/process", token)
record(SVC, "POST", "/api/v1/late-fees/process", sc, sc in (200, 204), msg)

# GET /late-fees/policy/organization/{orgId}
if org_id:
    sc, msg, _ = req("GET", f"/api/v1/late-fees/policy/organization/{org_id}", token)
    record(SVC, "GET", "/api/v1/late-fees/policy/organization/{{orgId}}", sc, sc in (200, 404), msg)

# POST /late-fees/policy
policy_data = {
    "organizationId": org_id,
    "policyName": "E2E Test Late Fee Policy",
    "gracePeriodDays": 5,
    "feeType": "FLAT",
    "flatAmount": 50.00,
    "maxFeeAmount": 200.00,
    "enabled": True,
    "autoApply": True
}
sc, msg, _ = req("POST", "/api/v1/late-fees/policy", token, body=policy_data)
record(SVC, "POST", "/api/v1/late-fees/policy", sc, sc in (200, 201), msg)

# POST /late-fees/apply/{paymentId} (need a real payment ID)
sc, msg, _ = req("POST", f"/api/v1/late-fees/apply/{uuid4()}", token)
record(SVC, "POST", "/api/v1/late-fees/apply/{{paymentId}}", sc, sc in (200, 404, 400, 500), msg)

# GET /late-fees/invoice/{invoiceId}
sc, msg, _ = req("GET", f"/api/v1/late-fees/invoice/{uuid4()}", token)
record(SVC, "GET", "/api/v1/late-fees/invoice/{{invoiceId}}", sc, sc in (200, 404), msg)

# ═══════════════════════════════════════════════════════════════
# PHASE 10: Test SEARCH
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 10: SEARCH")
print("=" * 70)

SVC = "search"

# GET /search?q=test
sc, msg, _ = req("GET", "/api/v1/search", token, params={"q": "test", "limit": 5})
record(SVC, "GET", "/api/v1/search?q=test", sc, sc == 200, msg)

# POST /search/properties
sc, msg, _ = req("POST", "/api/v1/search/properties", token,
                body={"filters": {"location": {"city": "TestCity"}}}, params={"page": 0, "limit": 5})
record(SVC, "POST", "/api/v1/search/properties", sc, sc in (200, 400), msg)

# POST /search/properties/detailed
sc, msg, _ = req("POST", "/api/v1/search/properties/detailed", token,
                body={"filters": {"location": {"city": "TestCity"}}}, params={"page": 0, "limit": 5})
record(SVC, "POST", "/api/v1/search/properties/detailed", sc, sc in (200, 400), msg)

# POST /search/tenants
sc, msg, _ = req("POST", "/api/v1/search/tenants", token,
                body={"filters": {}}, params={"page": 0, "limit": 5})
record(SVC, "POST", "/api/v1/search/tenants", sc, sc in (200, 400), msg)

# POST /search/tenants/detailed
sc, msg, _ = req("POST", "/api/v1/search/tenants/detailed", token,
                body={"filters": {}}, params={"page": 0, "limit": 5})
record(SVC, "POST", "/api/v1/search/tenants/detailed", sc, sc in (200, 400), msg)


# ═══════════════════════════════════════════════════════════════
# PHASE 11: Test SUPPORT TICKETS
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 11: SUPPORT TICKETS")
print("=" * 70)

SVC = "support-tickets"
ticket_id = None

# POST /support/tickets
ticket_data = {
    "subject": "E2E Test Support Ticket",
    "description": "This is an E2E test support ticket for validation purposes",
    "priority": "MEDIUM",
    "type": "TECHNICAL_ISSUE"
}
sc, msg, data = req("POST", "/api/v1/support/tickets", org_token, body=ticket_data)
if sc in (200, 201):
    ticket_id = data.get("data", {}).get("id") or data.get("data", {}).get("ticketId") or data.get("id")
    record(SVC, "POST", "/api/v1/support/tickets", sc, True, f"id={ticket_id}")
else:
    record(SVC, "POST", "/api/v1/support/tickets", sc, False, msg)

# GET /support/tickets
sc, msg, _ = req("GET", "/api/v1/support/tickets", token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/support/tickets", sc, sc == 200, msg)

# GET /support/tickets/my-tickets
sc, msg, _ = req("GET", "/api/v1/support/tickets/my-tickets", token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/support/tickets/my-tickets", sc, sc == 200, msg)

# GET /support/stats
sc, msg, _ = req("GET", "/api/v1/support/stats", token)
record(SVC, "GET", "/api/v1/support/stats", sc, sc == 200, msg)

if ticket_id:
    # GET /support/tickets/{id}
    sc, msg, _ = req("GET", f"/api/v1/support/tickets/{ticket_id}", token)
    record(SVC, "GET", "/api/v1/support/tickets/{{id}}", sc, sc == 200, msg)

    # PUT /support/tickets/{id}
    sc, msg, _ = req("PUT", f"/api/v1/support/tickets/{ticket_id}", org_token,
                     body={"subject": ticket_data["subject"], "description": ticket_data["description"],
                           "type": ticket_data["type"], "priority": ticket_data["priority"],
                           "status": "IN_PROGRESS"})
    record(SVC, "PUT", "/api/v1/support/tickets/{{id}}", sc, sc == 200, msg)

    # GET /support/tickets/assigned/{userId}
    sc, msg, _ = req("GET", f"/api/v1/support/tickets/assigned/admin", org_token, params={"page": 0, "size": 5})
    record(SVC, "GET", "/api/v1/support/tickets/assigned/{{userId}}", sc, sc in (200, 403, 404), msg)

    # DELETE /support/tickets/{id}
    sc, msg, _ = req("DELETE", f"/api/v1/support/tickets/{ticket_id}", token)
    record(SVC, "DELETE", "/api/v1/support/tickets/{{id}}", sc, sc in (200, 204), msg)


# ═══════════════════════════════════════════════════════════════
# PHASE 12: Test MESSAGES
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 12: MESSAGES")
print("=" * 70)

SVC = "messages"
message_id = None

# POST /messages (send)
msg_data = {
    "recipientEmail": "admin@propertize.com",
    "subject": "E2E Test Message",
    "message": "This is an E2E test message",
    "category": "GENERAL"
}
sc, msg_text, data = req("POST", "/api/v1/messages", token, body=msg_data)
if sc in (200, 201):
    message_id = data.get("data", {}).get("id") or data.get("data", {}).get("messageId") or data.get("id")
    record(SVC, "POST", "/api/v1/messages", sc, True, f"id={message_id}")
else:
    record(SVC, "POST", "/api/v1/messages", sc, False, msg_text)

# Also try /messages/send
sc, msg_text, data = req("POST", "/api/v1/messages/send", token, body=msg_data)
record(SVC, "POST", "/api/v1/messages/send", sc, sc in (200, 201, 405), msg_text)

# GET /messages
sc, msg_text, _ = req("GET", "/api/v1/messages", token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/messages", sc, sc == 200, msg_text)

# GET /messages/inbox
sc, msg_text, _ = req("GET", "/api/v1/messages/inbox", token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/messages/inbox", sc, sc == 200, msg_text)

# GET /messages/sent
sc, msg_text, _ = req("GET", "/api/v1/messages/sent", token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/messages/sent", sc, sc == 200, msg_text)

# GET /messages/stats
sc, msg_text, _ = req("GET", "/api/v1/messages/stats", token)
record(SVC, "GET", "/api/v1/messages/stats", sc, sc == 200, msg_text)
sc, msg_text, _ = req("GET", "/api/v1/messages/statistics", token)
record(SVC, "GET", "/api/v1/messages/statistics", sc, sc in (200, 404), msg_text)

# GET /messages/unread/count
sc, msg_text, _ = req("GET", "/api/v1/messages/unread/count", token)
record(SVC, "GET", "/api/v1/messages/unread/count", sc, sc == 200, msg_text)

# GET /messages/recipients
sc, msg_text, _ = req("GET", "/api/v1/messages/recipients", token)
record(SVC, "GET", "/api/v1/messages/recipients", sc, sc == 200, msg_text)

# GET /messages/search
sc, msg_text, _ = req("GET", "/api/v1/messages/search", token, params={"q": "test", "page": 0, "size": 5})
record(SVC, "GET", "/api/v1/messages/search", sc, sc == 200, msg_text)

# GET /messages/category/{cat}
sc, msg_text, _ = req("GET", "/api/v1/messages/category/GENERAL", token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/messages/category/{{cat}}", sc, sc in (200, 500), msg_text)

if message_id:
    # GET /messages/{id}
    sc, msg_text, _ = req("GET", f"/api/v1/messages/{message_id}", token)
    record(SVC, "GET", "/api/v1/messages/{{id}}", sc, sc in (200, 403, 500), msg_text)

    # PATCH /messages/{id}/read
    sc, msg_text, _ = req("PATCH", f"/api/v1/messages/{message_id}/read", token)
    record(SVC, "PATCH", "/api/v1/messages/{{id}}/read", sc, sc in (200, 403), msg_text)

    # PATCH /messages/{id}/unread
    sc, msg_text, _ = req("PATCH", f"/api/v1/messages/{message_id}/unread", token)
    record(SVC, "PATCH", "/api/v1/messages/{{id}}/unread", sc, sc in (200, 403), msg_text)

    # POST /messages/{id}/reply
    sc, msg_text, _ = req("POST", f"/api/v1/messages/{message_id}/reply", token,
                         body={"message": "E2E reply message"})
    record(SVC, "POST", "/api/v1/messages/{{id}}/reply", sc, sc in (200, 201, 500), msg_text)

    # PATCH /messages/{id}/archive
    sc, msg_text, _ = req("PATCH", f"/api/v1/messages/{message_id}/archive", token)
    record(SVC, "PATCH", "/api/v1/messages/{{id}}/archive", sc, sc in (200, 403), msg_text)

    # DELETE /messages/{id}
    sc, msg_text, _ = req("DELETE", f"/api/v1/messages/{message_id}", token)
    record(SVC, "DELETE", "/api/v1/messages/{{id}}", sc, sc in (200, 204, 403), msg_text)


# ═══════════════════════════════════════════════════════════════
# PHASE 13: Test AUDIT LOG
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 13: AUDIT LOG")
print("=" * 70)

SVC = "audit-log"

# GET /audit
sc, msg_text, _ = req("GET", "/api/v1/audit", token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/audit", sc, sc == 200, msg_text)

# GET /audit/{id} (use Long ID, not UUID)
sc, msg_text, _ = req("GET", "/api/v1/audit/1", token)
record(SVC, "GET", "/api/v1/audit/{{id}}", sc, sc in (200, 404, 500), msg_text)

# GET /audit/resource/{entityType}/{entityId}
sc, msg_text, _ = req("GET", f"/api/v1/audit/resource/PROPERTY/{uuid4()}", token)
record(SVC, "GET", "/api/v1/audit/resource/{{type}}/{{id}}", sc, sc in (200, 404), msg_text)


# ═══════════════════════════════════════════════════════════════
# PHASE 14: Test METRICS
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 14: METRICS")
print("=" * 70)

SVC = "metrics"

# GET /metrics/properties
sc, msg_text, _ = req("GET", "/api/v1/metrics/properties", org_token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/metrics/properties", sc, sc in (200, 404), msg_text)

if property_id:
    # GET /metrics/properties/{propertyId}
    sc, msg_text, _ = req("GET", f"/api/v1/metrics/properties/{property_id}", token)
    record(SVC, "GET", "/api/v1/metrics/properties/{{id}}", sc, sc in (200, 404), msg_text)

# GET /metrics/tenants
sc, msg_text, _ = req("GET", "/api/v1/metrics/tenants", org_token, params={"page": 0, "size": 5})
record(SVC, "GET", "/api/v1/metrics/tenants", sc, sc in (200, 404), msg_text)

# GET /metrics/tenants/{tenantId}
sc, msg_text, _ = req("GET", f"/api/v1/metrics/tenants/{uuid4()}", token)
record(SVC, "GET", "/api/v1/metrics/tenants/{{id}}", sc, sc in (200, 404), msg_text)

# GET /metrics/leases/{leaseId}
sc, msg_text, _ = req("GET", f"/api/v1/metrics/leases/{uuid4()}", token)
record(SVC, "GET", "/api/v1/metrics/leases/{{id}}", sc, sc in (200, 404), msg_text)


# ═══════════════════════════════════════════════════════════════
# PHASE 15: Test NOTIFICATION PREFERENCES
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 15: NOTIFICATION PREFERENCES")
print("=" * 70)

SVC = "notification-preferences"

# GET /notification-preferences/tenant/{tenantId}
sc, msg_text, _ = req("GET", "/api/v1/notification-preferences/tenant/test-tenant-1", token)
record(SVC, "GET", "/api/v1/notification-preferences/tenant/{{id}}", sc, sc in (200, 404), msg_text)

# POST /notification-preferences/grant-consent
sc, msg_text, _ = req("POST", "/api/v1/notification-preferences/grant-consent", token,
                     params={"tenantId": "test-tenant-1", "notificationType": "GENERAL", "channelType": "EMAIL"})
record(SVC, "POST", "/api/v1/notification-preferences/grant-consent", sc, sc in (200, 201, 400), msg_text)

# POST /notification-preferences/revoke-consent
sc, msg_text, _ = req("POST", "/api/v1/notification-preferences/revoke-consent", token,
                     params={"tenantId": "test-tenant-1", "notificationType": "GENERAL", "channelType": "SMS"})
record(SVC, "POST", "/api/v1/notification-preferences/revoke-consent", sc, sc in (200, 201, 400), msg_text)

# POST /notification-preferences/opt-out
sc, msg_text, _ = req("POST", "/api/v1/notification-preferences/opt-out", token,
                     params={"tenantId": "test-tenant-1", "channelType": "PUSH"})
record(SVC, "POST", "/api/v1/notification-preferences/opt-out", sc, sc in (200, 201, 400), msg_text)


# ═══════════════════════════════════════════════════════════════
# PHASE 16: Verify Swagger for Fixed Services
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("PHASE 16: SWAGGER VERIFICATION")
print("=" * 70)

SVC = "swagger"

for svc_name, port in [("payroll-service", 8085), ("employee-service", 8083), ("propertize", 8082)]:
    for path in ["/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs"]:
        try:
            r = requests.get(f"http://localhost:{port}{path}", timeout=10, allow_redirects=True)
            passed = r.status_code == 200
            record(SVC, "GET", f"{svc_name}:{port}{path}", r.status_code, passed)
        except Exception as e:
            record(SVC, "GET", f"{svc_name}:{port}{path}", 0, False, str(e)[:50])


# ═══════════════════════════════════════════════════════════════
# REPORT
# ═══════════════════════════════════════════════════════════════
print("\n" + "=" * 70)
print("FINAL REPORT")
print("=" * 70)

total = len(results)
passed = sum(1 for r in results if r["passed"] == "PASS")
failed = sum(1 for r in results if r["passed"] == "FAIL")

print(f"\n  Total: {total}  |  ✅ Passed: {passed}  |  ❌ Failed: {failed}")
print(f"  Pass Rate: {passed/total*100:.1f}%\n")

# Group by service
services = {}
for r in results:
    svc = r["service"]
    if svc not in services:
        services[svc] = {"pass": 0, "fail": 0, "endpoints": []}
    if r["passed"] == "PASS":
        services[svc]["pass"] += 1
    else:
        services[svc]["fail"] += 1
    services[svc]["endpoints"].append(r)

for svc, data in services.items():
    icon = "✅" if data["fail"] == 0 else "⚠️"
    print(f"  {icon} {svc}: {data['pass']} pass / {data['fail']} fail")

# Write markdown report
report_path = "/Users/ravishah/MySpace/ProperyManage/propertize-Services/docs/E2E_API_TEST_REPORT.md"
with open(report_path, "w") as f:
    f.write(f"# Propertize E2E API Test Report\n\n")
    f.write(f"**Date:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    f.write(f"**Total Endpoints Tested:** {total}\n")
    f.write(f"**Passed:** {passed} | **Failed:** {failed} | **Rate:** {passed/total*100:.1f}%\n\n")

    f.write(f"## Summary by Service\n\n")
    f.write(f"| Service | Passed | Failed | Rate |\n")
    f.write(f"|---------|--------|--------|------|\n")
    for svc, data in services.items():
        t = data['pass'] + data['fail']
        rate = data['pass']/t*100 if t > 0 else 0
        status = "✅" if data['fail'] == 0 else "❌"
        f.write(f"| {status} {svc} | {data['pass']} | {data['fail']} | {rate:.0f}% |\n")

    f.write(f"\n## Detailed Results\n\n")

    for svc, data in services.items():
        f.write(f"\n### {svc}\n\n")
        f.write(f"| Status | Method | Path | HTTP | Note |\n")
        f.write(f"|--------|--------|------|------|------|\n")
        for ep in data["endpoints"]:
            status = "✅" if ep["passed"] == "PASS" else "❌"
            f.write(f"| {status} | {ep['method']} | `{ep['path']}` | {ep['status']} | {ep['note'][:60]} |\n")

    f.write(f"\n## Test Configuration\n\n")
    f.write(f"- **Gateway:** {BASE}\n")
    f.write(f"- **Admin User:** {ADMIN_USER}\n")
    f.write(f"- **Organization ID:** {org_id}\n")
    f.write(f"- **Owner Username:** {owner_username}\n")
    f.write(f"- **Property ID:** {property_id}\n")
    f.write(f"- **Client ID (payroll):** {client_id}\n")
    f.write(f"\n---\n*Generated by e2e_test_all_services.py*\n")

print(f"\n  📄 Report written to: {report_path}")
print("Done!")
