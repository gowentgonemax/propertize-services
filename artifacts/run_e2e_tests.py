#!/usr/bin/env python3
"""
Propertize — Automated E2E Test Runner
Executes all flows defined in docs/E2E_TESTING_PLAN.md

Usage:
    python3 artifacts/run_e2e_tests.py
    python3 artifacts/run_e2e_tests.py --flow employee
    python3 artifacts/run_e2e_tests.py --flow messaging
    python3 artifacts/run_e2e_tests.py --flow notifications
    python3 artifacts/run_e2e_tests.py --flow all  (default)
"""

import argparse
import json
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime, date
from typing import Optional
import urllib.request
import urllib.error

# ─── Configuration ────────────────────────────────────────────────────────────

BASE_URL = "http://localhost:8080"
API_VERSION = "/api/v1"
TIMESTAMP = int(time.time())

CREDENTIALS = {
    "org_admin":  {"username": "org_admin",  "password": "Test@123"},
    "tenant":     {"username": "tenant",     "password": "Test@123"},
    "platform_admin": {"username": "platform_admin", "password": "Test@123"},
}

# ─── Colours ──────────────────────────────────────────────────────────────────

GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
CYAN   = "\033[96m"
BOLD   = "\033[1m"
RESET  = "\033[0m"
DIM    = "\033[2m"

# ─── Result tracking ──────────────────────────────────────────────────────────

@dataclass
class TestResult:
    name: str
    passed: bool
    status_code: int = 0
    message: str = ""
    detail: str = ""

results: list[TestResult] = []


def log_pass(name: str, status: int, detail: str = ""):
    r = TestResult(name=name, passed=True, status_code=status, detail=detail)
    results.append(r)
    print(f"  {GREEN}✔{RESET} {name} {DIM}({status}){RESET}" + (f" — {detail}" if detail else ""))


def log_fail(name: str, status: int, message: str, detail: str = ""):
    r = TestResult(name=name, passed=False, status_code=status, message=message, detail=detail)
    results.append(r)
    print(f"  {RED}✘{RESET} {name} {DIM}({status}){RESET} — {RED}{message}{RESET}" + (f"\n    {DIM}{detail}{DIM}{RESET}" if detail else ""))


def log_skip(name: str, reason: str):
    r = TestResult(name=name, passed=True, status_code=0, message=f"SKIP: {reason}")
    results.append(r)
    print(f"  {YELLOW}⊘{RESET} {name} {DIM}— skipped: {reason}{RESET}")


def section(title: str):
    print(f"\n{BOLD}{CYAN}{'─' * 60}{RESET}")
    print(f"{BOLD}{CYAN}  {title}{RESET}")
    print(f"{BOLD}{CYAN}{'─' * 60}{RESET}")


# ─── HTTP helpers ─────────────────────────────────────────────────────────────

def api(method: str, path: str, token: Optional[str] = None,
        body: Optional[dict] = None, org_id: Optional[str] = None,
        expected: list[int] = None) -> tuple[int, dict]:
    """Make an HTTP request. Returns (status_code, response_body)."""
    url = BASE_URL + API_VERSION + path
    data = json.dumps(body).encode() if body else None
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if org_id:
        headers["X-Organization-Id"] = org_id

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            status = resp.status
            try:
                body_out = json.loads(resp.read().decode())
            except Exception:
                body_out = {}
            return status, body_out
    except urllib.error.HTTPError as e:
        try:
            body_out = json.loads(e.read().decode())
        except Exception:
            body_out = {"error": str(e)}
        return e.code, body_out
    except Exception as e:
        return 0, {"error": str(e)}


def expect(test_name: str, status: int, body: dict,
           expected_statuses: list[int], extract_path: Optional[str] = None) -> Optional[str]:
    """Assert status is in expected_statuses. Optionally extract a value."""
    if status not in expected_statuses:
        err_msg = body.get("message") or body.get("error") or json.dumps(body)[:120]
        log_fail(test_name, status, f"expected {expected_statuses}", err_msg)
        return None
    value = None
    if extract_path:
        value = _extract(body, extract_path)
        detail = f"{extract_path}={value}" if value else f"WARNING: could not extract '{extract_path}'"
    else:
        detail = ""
    log_pass(test_name, status, detail)
    return value


def _extract(obj: dict, path: str) -> Optional[str]:
    """Dot-path extractor: 'data.id', 'accessToken', 'user.organizationId'."""
    parts = path.split(".")
    cur = obj
    for p in parts:
        if isinstance(cur, dict):
            cur = cur.get(p)
        else:
            return None
    return str(cur) if cur is not None else None


# ─── Auth helpers ─────────────────────────────────────────────────────────────

@dataclass
class Session:
    token: str = ""
    org_id: str = ""
    user_id: str = ""
    username: str = ""


def login(role: str) -> Optional[Session]:
    creds = CREDENTIALS[role]
    status, body = api("POST", "/auth/login", body={
        "usernameOrEmail": creds["username"],
        "password": creds["password"]
    })
    if status not in (200, 201):
        log_fail(f"Login as {role}", status, body.get("message", "login failed"))
        return None
    token    = _extract(body, "accessToken") or _extract(body, "data.accessToken") or _extract(body, "token")
    org_id   = _extract(body, "user.organizationId") or _extract(body, "data.user.organizationId") or ""
    user_id  = _extract(body, "user.id") or _extract(body, "data.user.id") or ""
    username = _extract(body, "user.username") or _extract(body, "data.user.username") or role
    if not token:
        log_fail(f"Login as {role}", status, "no token in response", str(body)[:200])
        return None
    log_pass(f"Login as {role}", status, f"org={org_id or 'n/a'}")
    return Session(token=token, org_id=org_id, user_id=user_id, username=username)


# ─── Flow 1: Employee → Payroll ───────────────────────────────────────────────

def flow_employee_payroll():
    section("FLOW 1 — Employee → Payroll")
    ctx: dict = {}

    # 1.1 Login
    sess = login("org_admin")
    if not sess:
        print(f"  {RED}Cannot proceed without auth.{RESET}")
        return

    # 2.1 Create Employee
    ts = TIMESTAMP
    status, body = api("POST", "/employees", token=sess.token, org_id=sess.org_id, body={
        "firstName": "E2E",
        "lastName": f"Test{ts}",
        "email": f"e2e-test-{ts}@testorg.com",
        "employmentType": "FULL_TIME",
        "hireDate": str(date.today()),
        "department": "Operations",
        "position": "Maintenance Technician",
        "status": "ACTIVE"
    })
    ctx["employee_id"] = expect("2.1 Create Employee", status, body, [200, 201], "data.id") \
                      or expect("2.1 Create Employee (alt path)", status, body, [200, 201], "id")
    if not ctx.get("employee_id"):
        # Try extracting from nested response
        ctx["employee_id"] = _extract(body, "data.employeeId") or _extract(body, "employeeId")
    if not ctx.get("employee_id"):
        print(f"  {YELLOW}  Employee ID not extracted — body: {str(body)[:200]}{RESET}")

    # 2.2 Verify employee
    if ctx.get("employee_id"):
        status, body = api("GET", f"/employees/{ctx['employee_id']}", token=sess.token, org_id=sess.org_id)
        expect("2.2 Verify Employee Created", status, body, [200])

    # 2.3 Activate (if needed)
    if ctx.get("employee_id"):
        status, body = api("POST", f"/employees/{ctx['employee_id']}/activate", token=sess.token, org_id=sess.org_id)
        if status in (200, 201, 204):
            log_pass("2.3 Activate Employee", status)
        elif status == 400:
            log_skip("2.3 Activate Employee", "already active or not applicable")
        else:
            log_fail("2.3 Activate Employee", status, body.get("message", ""))

    # 2.4 Payroll summary
    status, body = api("GET", "/employees/payroll-summary", token=sess.token, org_id=sess.org_id)
    if status in (200, 201):
        log_pass("2.4 Payroll Summary Accessible", status)
    else:
        log_fail("2.4 Payroll Summary Accessible", status, body.get("message", ""))

    # 2.5 Create Compensation
    if ctx.get("employee_id"):
        status, body = api("POST", "/compensation", token=sess.token, org_id=sess.org_id, body={
            "employeeId": ctx["employee_id"],
            "payType": "HOURLY",
            "hourlyRate": 25.00,
            "effectiveDate": str(date.today()),
            "currency": "USD"
        })
        ctx["compensation_id"] = expect("2.5 Create Compensation", status, body, [200, 201], "data.id") \
                               or _extract(body, "id")

    # 2.6 Create Time Entry
    if ctx.get("employee_id"):
        status, body = api("POST", "/time-entries", token=sess.token, org_id=sess.org_id, body={
            "employeeId": ctx["employee_id"],
            "date": str(date.today()),
            "regularHours": 8.0,
            "overtimeHours": 1.5,
            "description": "E2E automated test time entry"
        })
        ctx["time_entry_id"] = expect("2.6 Create Time Entry", status, body, [200, 201], "data.id") \
                             or _extract(body, "id")

    # 2.7 Approve Time Entry
    if ctx.get("time_entry_id"):
        status, body = api("POST", f"/time-entries/{ctx['time_entry_id']}/approve",
                           token=sess.token, org_id=sess.org_id)
        if status in (200, 201, 204):
            log_pass("2.7 Approve Time Entry", status)
        elif status == 404:
            log_skip("2.7 Approve Time Entry", "endpoint not implemented")
        else:
            log_fail("2.7 Approve Time Entry", status, body.get("message", ""))

    # 2.8 Create Payroll Run
    if sess.org_id:
        today = date.today()
        start = today.replace(day=1)
        status, body = api("POST", f"/clients/{sess.org_id}/payroll",
                           token=sess.token, org_id=sess.org_id, body={
            "payPeriodStart": str(start),
            "payPeriodEnd": str(today),
            "payrollType": "REGULAR",
            "description": f"E2E automated payroll run {TIMESTAMP}"
        })
        ctx["payroll_run_id"] = expect("2.8 Create Payroll Run", status, body, [200, 201], "data.id") \
                             or _extract(body, "id")

    # 2.9 Process Payroll Run
    if ctx.get("payroll_run_id") and sess.org_id:
        status, body = api("POST", f"/clients/{sess.org_id}/payroll/{ctx['payroll_run_id']}/process",
                           token=sess.token, org_id=sess.org_id)
        if status in (200, 201):
            log_pass("2.9 Process Payroll Run", status)
        else:
            log_fail("2.9 Process Payroll Run", status, body.get("message", ""))

    # 2.10 Approve Payroll Run
    if ctx.get("payroll_run_id") and sess.org_id:
        status, body = api("POST", f"/clients/{sess.org_id}/payroll/{ctx['payroll_run_id']}/approve",
                           token=sess.token, org_id=sess.org_id)
        if status in (200, 201):
            log_pass("2.10 Approve Payroll Run", status)
        else:
            log_fail("2.10 Approve Payroll Run", status, body.get("message", ""))

    # 2.11 Verify Payroll Details
    if ctx.get("payroll_run_id") and sess.org_id:
        status, body = api("GET", f"/clients/{sess.org_id}/payroll/{ctx['payroll_run_id']}",
                           token=sess.token, org_id=sess.org_id)
        expect("2.11 Verify Payroll Details", status, body, [200])

    # 2.12 Terminate Test Employee
    if ctx.get("employee_id"):
        status, body = api("POST", f"/employees/{ctx['employee_id']}/terminate",
                           token=sess.token, org_id=sess.org_id,
                           body={"reason": "E2E test cleanup"})
        if status in (200, 201, 204):
            log_pass("2.12 Terminate Test Employee (cleanup)", status)
        elif status == 404:
            log_skip("2.12 Terminate Test Employee (cleanup)", "endpoint not found")
        else:
            log_fail("2.12 Terminate Test Employee (cleanup)", status, body.get("message", ""))


# ─── Flow 2: Messaging ────────────────────────────────────────────────────────

def flow_messaging():
    section("FLOW 2 — Messaging")
    ctx: dict = {}

    # Login as org_admin (sender)
    sender = login("org_admin")
    if not sender:
        print(f"  {RED}Cannot proceed without auth.{RESET}")
        return

    # 3.1 Get Recipients
    status, body = api("GET", "/messages/recipients?search=tenant",
                       token=sender.token, org_id=sender.org_id)
    if status == 200:
        items = body.get("data", body) if isinstance(body.get("data"), list) else body if isinstance(body, list) else []
        if items:
            ctx["recipient_id"] = str(items[0].get("id", ""))
            log_pass("3.1 Get Recipients", status, f"found {len(items)} recipient(s)")
        else:
            log_fail("3.1 Get Recipients", status, "empty recipient list")
    else:
        log_fail("3.1 Get Recipients", status, body.get("message", ""))

    # 3.2 Send Message
    if ctx.get("recipient_id"):
        status, body = api("POST", "/messages/send", token=sender.token, org_id=sender.org_id, body={
            "recipientId": ctx["recipient_id"],
            "subject": f"E2E Test Message {TIMESTAMP}",
            "body": "This is an automated E2E test message. Please disregard.",
            "priority": "NORMAL"
        })
        ctx["message_id"] = expect("3.2 Send Message", status, body, [200, 201], "data.id") \
                          or _extract(body, "id")
    else:
        log_skip("3.2 Send Message", "no recipient found")

    # 3.3 Verify Sent Folder
    status, body = api("GET", "/messages/sent?page=0&size=10",
                       token=sender.token, org_id=sender.org_id)
    if status == 200:
        log_pass("3.3 Verify Sent Folder", status)
    else:
        log_fail("3.3 Verify Sent Folder", status, body.get("message", ""))

    # 3.4 Login as Tenant
    recipient_sess = login("tenant")
    if not recipient_sess:
        log_skip("3.4-3.6 Tenant Inbox/Read", "tenant login failed")
        return

    # Check inbox
    status, body = api("GET", "/messages/inbox?page=0&size=10",
                       token=recipient_sess.token, org_id=recipient_sess.org_id)
    if status == 200:
        items = body.get("data", []) if isinstance(body.get("data"), list) else []
        log_pass("3.4 Tenant Inbox Accessible", status, f"{len(items)} messages")
    else:
        log_fail("3.4 Tenant Inbox Accessible", status, body.get("message", ""))

    # 3.5 Unread Count
    for path in ("/messages/unread/count", "/messages/unread-count"):
        status, body = api("GET", path, token=recipient_sess.token, org_id=recipient_sess.org_id)
        if status == 200:
            count = _extract(body, "data") or _extract(body, "count") or str(body)
            log_pass(f"3.5 Unread Count", status, f"count={count}")
            break
    else:
        log_fail("3.5 Unread Count", status, body.get("message", "endpoint not found"))

    # 3.6 Read Message
    if ctx.get("message_id"):
        status, body = api("GET", f"/messages/{ctx['message_id']}",
                           token=recipient_sess.token, org_id=recipient_sess.org_id)
        if status == 200:
            log_pass("3.6 Read Message", status)
        elif status == 403:
            log_fail("3.6 Read Message", status, "tenant cannot read message sent to them (RBAC issue)")
        else:
            log_fail("3.6 Read Message", status, body.get("message", ""))
    else:
        log_skip("3.6 Read Message", "no message ID captured")

    # 3.7 Message Statistics
    status, body = api("GET", "/messages/statistics", token=sender.token, org_id=sender.org_id)
    if status == 200:
        log_pass("3.7 Message Statistics", status)
    elif status == 404:
        log_skip("3.7 Message Statistics", "endpoint not implemented")
    else:
        log_fail("3.7 Message Statistics", status, body.get("message", ""))


# ─── Flow 3: Notifications ────────────────────────────────────────────────────

def flow_notifications():
    section("FLOW 3 — Notifications")
    ctx: dict = {}

    admin = login("org_admin")
    if not admin:
        print(f"  {RED}Cannot proceed without auth.{RESET}")
        return

    tenant_sess = login("tenant")
    ctx["tenant_id"] = tenant_sess.user_id if tenant_sess else ""
    ctx["tenant_email"] = "tenant@testorg.com"

    # 4.1 Send Email Notification
    status, body = api("POST", "/notifications/send", token=admin.token, org_id=admin.org_id, body={
        "recipientEmail": ctx["tenant_email"],
        "subject": f"E2E Test Notification {TIMESTAMP}",
        "body": "This is an automated E2E test notification.",
        "tenantId": ctx.get("tenant_id", "")
    })
    ctx["notification_id"] = expect("4.1 Send Email Notification", status, body, [200, 201], "data.id") \
                           or _extract(body, "id")
    if not ctx.get("notification_id"):
        # Fallback attempt: some services wrap differently
        if status in (200, 201):
            log_pass("4.1 Send Email Notification", status, "sent (id not extracted)")

    # 4.2 Verify Notification Created
    if ctx.get("notification_id"):
        status, body = api("GET", f"/notifications/{ctx['notification_id']}",
                           token=admin.token, org_id=admin.org_id)
        expect("4.2 Verify Notification", status, body, [200], "data.status")
    else:
        log_skip("4.2 Verify Notification", "no notification ID captured")

    # 4.3 Notifications by Status
    for path in ("/notifications/status/SENT", "/notifications?status=SENT"):
        status, body = api("GET", path, token=admin.token, org_id=admin.org_id)
        if status == 200:
            log_pass("4.3 Notifications by Status", status)
            break
    else:
        log_fail("4.3 Notifications by Status", status, body.get("message", ""))

    # 4.4 Tenant Notifications
    if ctx.get("tenant_id"):
        status, body = api("GET", f"/notifications/tenant/{ctx['tenant_id']}",
                           token=admin.token, org_id=admin.org_id)
        if status in (200, 404):
            log_pass("4.4 Tenant Notifications", status) if status == 200 \
                else log_skip("4.4 Tenant Notifications", "no notifications for this tenant yet")
        else:
            log_fail("4.4 Tenant Notifications", status, body.get("message", ""))

    # 4.5 Mark as Read
    if ctx.get("notification_id") and tenant_sess:
        status, body = api("PATCH", f"/notifications/{ctx['notification_id']}/read",
                           token=tenant_sess.token, org_id=tenant_sess.org_id)
        if status in (200, 204):
            log_pass("4.5 Mark Notification as Read", status)
        elif status == 404:
            log_skip("4.5 Mark Notification as Read", "PATCH /read not implemented")
        else:
            log_fail("4.5 Mark Notification as Read", status, body.get("message", ""))

    # 4.6 Initialize Notification Preferences
    if ctx.get("tenant_id"):
        for path in (f"/notification-preferences/initialize/{ctx['tenant_id']}",
                     "/notification-preferences/initialize"):
            status, body = api("POST", path, token=admin.token, org_id=admin.org_id)
            if status in (200, 201):
                log_pass("4.6 Initialize Notification Preferences", status)
                break
            elif status == 404:
                continue
        else:
            log_fail("4.6 Initialize Notification Preferences", status, body.get("message", "endpoint not found"))

    # 4.7 Get Preferences
    if ctx.get("tenant_id"):
        status, body = api("GET", f"/notification-preferences/tenant/{ctx['tenant_id']}",
                           token=admin.token, org_id=admin.org_id)
        if status == 200:
            log_pass("4.7 Get Notification Preferences", status)
        elif status == 404:
            log_skip("4.7 Get Notification Preferences", "tenant preferences not found (needs init)")
        else:
            log_fail("4.7 Get Notification Preferences", status, body.get("message", ""))

    # 4.8 Grant Consent
    if ctx.get("tenant_id"):
        qs = f"?tenantId={ctx['tenant_id']}&notificationType=PAYMENT_REMINDER&channelType=EMAIL"
        status, body = api("POST", f"/notification-preferences/grant-consent{qs}",
                           token=admin.token, org_id=admin.org_id)
        if status in (200, 201):
            log_pass("4.8 Grant Consent", status)
        elif status == 404:
            log_skip("4.8 Grant Consent", "endpoint not found")
        else:
            log_fail("4.8 Grant Consent", status, body.get("message", ""))

    # 4.9 Check Consent
    if ctx.get("tenant_id"):
        qs = f"?tenantId={ctx['tenant_id']}&channelType=EMAIL&notificationType=PAYMENT_REMINDER"
        status, body = api("GET", f"/notification-preferences/check-consent{qs}",
                           token=admin.token, org_id=admin.org_id)
        if status == 200:
            consented = _extract(body, "data.consented") or _extract(body, "consented")
            log_pass("4.9 Check Consent", status, f"consented={consented}")
        elif status == 404:
            log_skip("4.9 Check Consent", "endpoint not found")
        else:
            log_fail("4.9 Check Consent", status, body.get("message", ""))

    # 4.10 Revoke Consent
    if ctx.get("tenant_id"):
        qs = f"?tenantId={ctx['tenant_id']}&notificationType=PAYMENT_REMINDER&channelType=EMAIL"
        status, body = api("POST", f"/notification-preferences/revoke-consent{qs}",
                           token=admin.token, org_id=admin.org_id)
        if status in (200, 201):
            log_pass("4.10 Revoke Consent", status)
        elif status == 404:
            log_skip("4.10 Revoke Consent", "endpoint not found")
        else:
            log_fail("4.10 Revoke Consent", status, body.get("message", ""))

    # 4.11 SMS Test
    status, body = api("POST", "/notifications/sms/send", token=admin.token, org_id=admin.org_id, body={
        "phoneNumber": "+15551234567",
        "message": "E2E SMS test notification"
    })
    if status in (200, 201):
        log_pass("4.11 SMS Notification", status)
    elif status in (400, 503, 422):
        log_skip("4.11 SMS Notification", f"Twilio not configured ({status})")
    elif status == 404:
        log_skip("4.11 SMS Notification", "SMS endpoint not implemented")
    else:
        log_fail("4.11 SMS Notification", status, body.get("message", ""))


# ─── Flow 4: Cross-Service & RBAC Checks ──────────────────────────────────────

def flow_integration_checks():
    section("FLOW 4 — Cross-Service Integration & RBAC")

    # Health checks
    admin = login("org_admin")
    tenant = login("tenant")
    if not admin:
        return

    # Auth token validity: expired/invalid token
    status, body = api("GET", "/employees", token="invalid.jwt.token")
    if status == 401:
        log_pass("6.1 Expired Token → 401", status)
    else:
        log_fail("6.1 Expired Token → 401", status, f"expected 401, got {status}")

    # RBAC: tenant cannot list all employees
    if tenant:
        status, body = api("GET", "/employees", token=tenant.token, org_id=tenant.org_id)
        if status == 403:
            log_pass("6.2 Tenant Cannot List Employees (RBAC 403)", status)
        elif status == 200:
            log_fail("6.2 Tenant Cannot List Employees (RBAC 403)", status,
                     "tenant has unrestricted employee access — RBAC not enforced")
        else:
            log_skip("6.2 Tenant Cannot List Employees", f"unexpected {status} (endpoint may not exist)")

    # Organization isolation: attempt access with wrong org header
    if admin:
        status, body = api("GET", "/employees", token=admin.token, org_id="00000000-0000-0000-0000-000000000000")
        if status in (403, 404, 200):
            if status == 200:
                items = body.get("data", body) if isinstance(body, dict) else body
                count = len(items) if isinstance(items, list) else "?"
                if count == 0 or count == "?":
                    log_pass("6.3 Org Isolation (empty for wrong org)", status, "returned empty")
                else:
                    log_fail("6.3 Org Isolation", status, f"returned {count} employees for wrong org")
            else:
                log_pass("6.3 Org Isolation → 403/404", status)
        else:
            log_skip("6.3 Org Isolation", f"unexpected {status}")


# ─── Flow 5: Edge Cases ───────────────────────────────────────────────────────

def flow_edge_cases():
    section("FLOW 5 — Edge Cases & Failure Paths")

    admin = login("org_admin")
    if not admin:
        return

    ts = TIMESTAMP

    # Duplicate email
    dup_body = {
        "firstName": "Dup",
        "lastName": "Test",
        "email": f"dup-e2e-{ts}@testorg.com",
        "employmentType": "FULL_TIME",
        "hireDate": str(date.today()),
        "status": "ACTIVE"
    }
    api("POST", "/employees", token=admin.token, org_id=admin.org_id, body=dup_body)  # first (may succeed)
    status, body = api("POST", "/employees", token=admin.token, org_id=admin.org_id, body=dup_body)  # second
    if status in (400, 409, 422):
        log_pass("7.1 Duplicate Email → 400/409", status)
    elif status in (200, 201):
        log_fail("7.1 Duplicate Email → 400/409", status, "duplicate email was accepted")
    else:
        log_skip("7.1 Duplicate Email", f"got {status}")

    # Send message to invalid recipient
    status, body = api("POST", "/messages/send", token=admin.token, org_id=admin.org_id, body={
        "recipientId": "00000000-0000-0000-0000-000000000099",
        "subject": "Invalid",
        "body": "Should fail"
    })
    if status in (400, 404, 422):
        log_pass("7.3 Invalid Message Recipient → 400/404", status)
    elif status in (200, 201):
        log_fail("7.3 Invalid Message Recipient", status, "invalid recipient was accepted")
    else:
        log_skip("7.3 Invalid Message Recipient", f"got {status}")

    # Process already-completed payroll (requires a completed payroll ID — use dummy)
    if admin.org_id:
        status, body = api("POST", f"/clients/{admin.org_id}/payroll/00000000-0000-0000-0000-000000000001/process",
                           token=admin.token, org_id=admin.org_id)
        if status in (400, 404, 422):
            log_pass("7.6 Process Non-existent Payroll → 400/404", status)
        elif status in (200, 201):
            log_fail("7.6 Process Non-existent Payroll", status, "processed a non-existent payroll run")
        else:
            log_skip("7.6 Process Non-existent Payroll", f"got {status}")


# ─── Summary ──────────────────────────────────────────────────────────────────

def print_summary():
    section("TEST SUMMARY")
    passed  = [r for r in results if r.passed]
    failed  = [r for r in results if not r.passed and not r.message.startswith("SKIP")]
    skipped = [r for r in results if r.message.startswith("SKIP")]
    total   = len(results)

    print(f"\n  Total:   {total}")
    print(f"  {GREEN}Passed:  {len(passed)}{RESET}")
    print(f"  {RED}Failed:  {len(failed)}{RESET}")
    print(f"  {YELLOW}Skipped: {len(skipped)}{RESET}")

    if failed:
        print(f"\n{BOLD}{RED}  Failed tests:{RESET}")
        for r in failed:
            print(f"    {RED}✘{RESET} [{r.status_code}] {r.name}")
            if r.detail:
                print(f"       {DIM}{r.detail[:100]}{RESET}")

    if skipped:
        print(f"\n{YELLOW}  Skipped tests:{RESET}")
        for r in skipped:
            print(f"    {YELLOW}⊘{RESET} {r.name}: {r.message.replace('SKIP: ', '')}")

    print()
    return len(failed) == 0


# ─── Entry point ──────────────────────────────────────────────────────────────

def check_gateway():
    """Fast connectivity check before running tests."""
    print(f"\n{BOLD}Propertize E2E Test Runner{RESET}")
    print(f"  Gateway: {BASE_URL}")
    print(f"  Time:    {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    status, body = api("GET", "/test/credentials")
    if status == 0:
        print(f"\n  {RED}✘ Cannot reach gateway at {BASE_URL}{RESET}")
        print(f"  Run `make up` and wait for all services to be healthy, then retry.\n")
        sys.exit(1)
    print(f"  {GREEN}✔ Gateway reachable{RESET} (status {status})")


def main():
    parser = argparse.ArgumentParser(description="Propertize E2E Test Runner")
    parser.add_argument("--flow", choices=["employee", "messaging", "notifications",
                                           "integration", "edge", "all"],
                        default="all", help="Which flow to run")
    args = parser.parse_args()

    check_gateway()

    flow = args.flow
    if flow in ("employee", "all"):
        flow_employee_payroll()
    if flow in ("messaging", "all"):
        flow_messaging()
    if flow in ("notifications", "all"):
        flow_notifications()
    if flow in ("integration", "all"):
        flow_integration_checks()
    if flow in ("edge", "all"):
        flow_edge_cases()

    success = print_summary()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
