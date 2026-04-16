# Propertize — Manual & Integration E2E Testing Plan

> **Scope**: Employee → Payroll flow, Messaging feature, Notification feature  
> **Environment**: Full Docker stack via `make up` (all services on `localhost:8080`)  
> **Date**: Generated 2025

---

## Prerequisites

```bash
make rebuild          # Fresh build of all services
make health           # Verify all services healthy
```

All API calls go through `http://localhost:8080` (API Gateway).

---

## Test Credentials

| Role | Username | Password | Organization |
|------|----------|----------|--------------|
| PLATFORM_OVERSIGHT | `platform_admin` | `Test@123` | System-Wide |
| ORGANIZATION_ADMIN | `org_admin` | `Test@123` | Test Property Mgmt LLC |
| PROPERTY_MANAGER | `property_manager` | `Test@123` | Test Property Mgmt LLC |
| ACCOUNTANT | `accountant` | `Test@123` | Test Property Mgmt LLC |
| TENANT | `tenant` | `Test@123` | Test Property Mgmt LLC |

> Verify credentials: `GET /api/v1/test/credentials` (dev/test only)

---

## 1. Authentication Setup (All Tests)

### 1.1 Login & Obtain JWT

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "org_admin",
  "password": "Test@123"
}
```

**Expected**: 200 OK with `accessToken`, `refreshToken`, `user` object containing `organizationId`.

**Save**: `TOKEN` = response `accessToken`, `ORG_ID` = response `user.organizationId`

All subsequent requests require:
```
Authorization: Bearer {TOKEN}
X-Organization-Id: {ORG_ID}
```

---

## 2. Employee → Payroll E2E Flow

### Test Flow: Add Employee → Verify Sync → Create Timesheet → Run Payroll → Approve

---

### 2.1 Create Employee

```http
POST /api/v1/employees
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "firstName": "E2E",
  "lastName": "TestEmployee",
  "email": "e2e-test-{timestamp}@testorg.com",
  "employmentType": "FULL_TIME",
  "hireDate": "2025-01-15",
  "department": "Operations",
  "position": "Maintenance Technician",
  "status": "ACTIVE"
}
```

**Expected**: 201 Created with employee UUID  
**Save**: `EMPLOYEE_ID` = response `data.id`

### 2.2 Verify Employee Created

```http
GET /api/v1/employees/{EMPLOYEE_ID}
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with matching `firstName`, `lastName`, `email`, `status: ACTIVE`

### 2.3 Activate Employee (if status is PENDING)

```http
POST /api/v1/employees/{EMPLOYEE_ID}/activate
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with `status: ACTIVE`

### 2.4 Verify Payroll Summary Includes Employee

```http
GET /api/v1/employees/payroll-summary
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK, response should include the newly created employee in the payroll-ready summary.

### 2.5 Create Compensation Record

```http
POST /api/v1/compensation
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "employeeId": "{EMPLOYEE_ID}",
  "payType": "HOURLY",
  "hourlyRate": 25.00,
  "effectiveDate": "2025-01-15",
  "currency": "USD"
}
```

**Expected**: 201 Created with compensation details  
**Save**: `COMPENSATION_ID` = response `data.id`

### 2.6 Create Time Entry

```http
POST /api/v1/time-entries
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "employeeId": "{EMPLOYEE_ID}",
  "date": "2025-01-20",
  "regularHours": 8.0,
  "overtimeHours": 1.5,
  "description": "E2E test time entry"
}
```

**Expected**: 201 Created  
**Save**: `TIME_ENTRY_ID` = response `data.id`

### 2.7 Approve Time Entry

```http
POST /api/v1/time-entries/{TIME_ENTRY_ID}/approve?approverId={ADMIN_USER_ID}
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with `status: APPROVED`

### 2.8 Create Payroll Run

```http
POST /api/v1/clients/{ORG_ID}/payroll
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "payPeriodStart": "2025-01-16",
  "payPeriodEnd": "2025-01-31",
  "payrollType": "REGULAR",
  "description": "E2E test payroll run"
}
```

**Expected**: 201 Created with `status: PENDING`  
**Save**: `PAYROLL_RUN_ID` = response `data.id`

### 2.9 Process Payroll Run

```http
POST /api/v1/clients/{ORG_ID}/payroll/{PAYROLL_RUN_ID}/process
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with `status: PROCESSING` → `COMPLETED`  
**Verify**: `totals.totalGrossPay` > 0 (should reflect the employee's hours × rate)

### 2.10 Approve Payroll Run

```http
POST /api/v1/clients/{ORG_ID}/payroll/{PAYROLL_RUN_ID}/approve
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with `status: APPROVED`

### 2.11 Verify Payroll Run Details

```http
GET /api/v1/clients/{ORG_ID}/payroll/{PAYROLL_RUN_ID}
Authorization: Bearer {TOKEN}
```

**Expected**:
- `status: APPROVED`
- `totals.totalGrossPay` = (8 × $25) + (1.5 × $25 × 1.5) = $256.25
- `payPeriod.startDate` = "2025-01-16"
- `payPeriod.endDate` = "2025-01-31"

### 2.12 Cleanup — Terminate Test Employee

```http
POST /api/v1/employees/{EMPLOYEE_ID}/terminate?reason=E2E+test+cleanup
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with `status: TERMINATED`

---

## 3. Messaging Feature E2E

### Test Flow: Get Recipients → Send Message → Verify Inbox → Read Message → Check Stats

---

### 3.1 Get Available Recipients

```http
GET /api/v1/messages/recipients?search=tenant
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with list of users matching "tenant"  
**Save**: `RECIPIENT_ID` from a result

### 3.2 Send a Message

```http
POST /api/v1/messages/send
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "recipientId": "{RECIPIENT_ID}",
  "subject": "E2E Test Message",
  "body": "This is an automated E2E test message. Please disregard.",
  "priority": "NORMAL"
}
```

**Expected**: 201 Created with message ID  
**Save**: `MESSAGE_ID` = response `data.id`

### 3.3 Verify Message in Sent Folder

```http
GET /api/v1/messages/sent?page=0&size=10
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK, list should contain the sent message with matching `subject`

### 3.4 Login as Recipient & Check Inbox

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "tenant",
  "password": "Test@123"
}
```

**Save**: `TENANT_TOKEN` = response `accessToken`

```http
GET /api/v1/messages/inbox?page=0&size=10
Authorization: Bearer {TENANT_TOKEN}
```

**Expected**: 200 OK, inbox contains the message with `subject: "E2E Test Message"`, `read: false`

### 3.5 Get Unread Count

```http
GET /api/v1/messages/unread/count
Authorization: Bearer {TENANT_TOKEN}
```

**Expected**: 200 OK with count ≥ 1

### 3.6 Read the Message

```http
GET /api/v1/messages/{MESSAGE_ID}
Authorization: Bearer {TENANT_TOKEN}
```

**Expected**: 200 OK with full message body, `read` should become `true`

### 3.7 Verify Message Statistics

```http
GET /api/v1/messages/statistics
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with stats (total, sent, received, unread counts)

---

## 4. Notification Feature E2E

### Test Flow: Send Notification → Verify Delivery → Mark Read → Check Preferences

---

### 4.1 Send Email Notification

```http
POST /api/v1/notifications/send
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "recipientEmail": "tenant@testorg.com",
  "subject": "E2E Test Notification",
  "body": "This is an automated E2E test notification.",
  "tenantId": "{TENANT_ID}"
}
```

**Expected**: 200/201 with notification ID  
**Save**: `NOTIFICATION_ID` = response `data.id`

### 4.2 Verify Notification Created

```http
GET /api/v1/notifications/{NOTIFICATION_ID}
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with `status: PENDING` or `SENT`

### 4.3 List Notifications by Status

```http
GET /api/v1/notifications/status/SENT
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with list containing the test notification

### 4.4 List Tenant Notifications

```http
GET /api/v1/notifications/tenant/{TENANT_ID}
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK, list should include the test notification

### 4.5 Mark Notification as Read (Frontend)

```http
PATCH /api/v1/notifications/{NOTIFICATION_ID}/read
Authorization: Bearer {TENANT_TOKEN}
```

**Expected**: 200 OK

### 4.6 Initialize Notification Preferences

```http
POST /api/v1/notification-preferences/initialize/{TENANT_ID}
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with default preferences created

### 4.7 Get Notification Preferences

```http
GET /api/v1/notification-preferences/tenant/{TENANT_ID}
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with preference list (email, sms channels for various notification types)

### 4.8 Grant Consent

```http
POST /api/v1/notification-preferences/grant-consent?tenantId={TENANT_ID}&notificationType=PAYMENT_REMINDER&channelType=EMAIL
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK

### 4.9 Check Consent

```http
GET /api/v1/notification-preferences/check-consent?tenantId={TENANT_ID}&channelType=EMAIL&notificationType=PAYMENT_REMINDER
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK with `consented: true`

### 4.10 Revoke Consent

```http
POST /api/v1/notification-preferences/revoke-consent?tenantId={TENANT_ID}&notificationType=PAYMENT_REMINDER&channelType=EMAIL
Authorization: Bearer {TOKEN}
```

**Expected**: 200 OK

### 4.11 SMS Notification Test

```http
POST /api/v1/notifications/sms/send
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "phoneNumber": "+15551234567",
  "message": "E2E SMS test notification"
}
```

**Expected**: 200 OK (may fail if Twilio not configured — verify error message is meaningful)

---

## 5. Frontend Integration Tests (Manual)

### 5.1 Add Employee → Run Payroll (UI Flow)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as `org_admin` / `Test@123` at `localhost:3000` | Dashboard loads with correct org |
| 2 | Navigate to Employees page | Employee list displays |
| 3 | Click "Add Employee" | Employee creation form appears |
| 4 | Fill form: E2E Test, testpayroll@test.com, Full-time, Hire: today | All fields accept input |
| 5 | Submit | Success toast, employee appears in list |
| 6 | Navigate to Payroll page | Payroll runs list loads |
| 7 | Click "Run Payroll" | Modal opens with date range fields |
| 8 | Set period: this month start → end, type: Regular | Form validates |
| 9 | Submit | Payroll run created, appears in table as PENDING |
| 10 | Click "Process" on the new run | Status changes to PROCESSING → COMPLETED |

### 5.2 Messaging (UI Flow)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as `org_admin` | Dashboard loads |
| 2 | Navigate to Messages page | Inbox/Sent tabs visible |
| 3 | Click "Compose" or "New Message" | Compose form opens |
| 4 | Search recipient: "tenant" | Recipient dropdown shows tenant user |
| 5 | Enter subject and body | Fields accept input |
| 6 | Click Send | Success toast, message in Sent tab |
| 7 | Login as `tenant` in new tab | Tenant dashboard loads |
| 8 | Navigate to Messages | Inbox shows unread message with badge |
| 9 | Click message | Message body renders, marked as read |
| 10 | Check unread count updates | Badge count decrements |

### 5.3 Notifications (UI Flow)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as `org_admin` | Dashboard loads |
| 2 | Navigate to Notifications page | Notification list/feed visible |
| 3 | Check filter tabs (All / Unread) | Tabs switch correctly, counts update |
| 4 | Click "Mark All as Read" | All notifications marked as read |
| 5 | Trigger an action that creates a notification (e.g., approve a maintenance request) | New notification appears |
| 6 | Navigate to Notification Preferences | Preference toggles visible |
| 7 | Toggle a preference off/on | Saves correctly, persists on refresh |

---

## 6. Cross-Service Integration Checks

| Check | How to Verify | Expected |
|-------|---------------|----------|
| Employee Kafka sync | Create employee via employee-service, check payroll-service has it | Employee available in payroll system |
| Notification persistence | Send notification via API, check MongoDB for audit event | Audit event stored |
| Auth token validity | Use expired token to call any endpoint | 401 Unauthorized |
| RBAC enforcement | Call `/api/v1/employees` as `tenant` role | 403 Forbidden |
| Rate limiting | Rapid-fire 20 requests in 1 second | 429 Too Many Requests after threshold |
| Organization isolation | Login as org_admin of Org A, request Org B data | 403 or empty response |

---

## 7. Edge Cases & Failure Paths

| Scenario | Test Steps | Expected |
|----------|-----------|----------|
| Create employee with duplicate email | POST same email twice | 400/409 with clear error |
| Run payroll with no employees | Create payroll run for org with 0 employees | Appropriate error or $0 run |
| Send message to invalid recipient | POST with non-existent recipientId | 400/404 error |
| Access notifications without permission | Login as low-privilege user, call notifications API | 403 Forbidden |
| Submit timesheet for past locked period | POST time entry for a closed pay period | 400 with "period closed" error |
| Process already-completed payroll | POST /process on COMPLETED payroll | 400 with "already processed" error |

---

## 8. Test Execution Checklist

- [ ] All services healthy (`make health` shows green)
- [ ] **Employee → Payroll flow**: Create → Activate → Compensate → Time Entry → Payroll → Approve
- [ ] **Messaging**: Send → Receive → Read → Stats
- [ ] **Notifications**: Send → List → Read → Preferences → Consent
- [ ] **Frontend UI**: Employee add, payroll run, messaging, notifications all functional
- [ ] **RBAC**: Role-based access properly enforced
- [ ] **Cross-service**: Kafka sync, audit events, org isolation verified
- [ ] **Edge cases**: All failure paths return meaningful errors

---

*Generated from API endpoint analysis of employee-service, payroll-service, propertize (main), and auth-service.*
