# Propertize — Work Status Tracker

Last updated: 2025-01

---

## Legend

| Symbol | Meaning                              |
| ------ | ------------------------------------ |
| ✅     | Done & verified                      |
| 🔧     | Fixed, needs backend restart to test |
| ❌     | Known issue, not yet fixed           |
| ⚠️     | Partial fix / workaround in place    |
| 🔍     | Under investigation                  |

---

## Session 1 — Frontend Fixes

| #   | Area       | Issue                                                           | Status | File                       |
| --- | ---------- | --------------------------------------------------------------- | ------ | -------------------------- |
| 1   | Properties | Double spinner on `/dashboard/properties/[id]`                  | ✅     | `properties/[id]/page.tsx` |
| 2   | Leases     | Datatable column redesign (Tenant+ID, Contact, compact address) | ✅     | `leases/page.tsx`          |
| 3   | Sidebar    | Bell icon + first+last name display                             | ✅     | `DashboardSidebar.tsx`     |
| 4   | Users      | AddUserModal shows credentials in success state                 | ✅     | `AddUserModal.tsx`         |
| 5   | Leases     | Pet policy mapping (`lease.restrictions` → `petsAllowed`)       | ✅     | `leases/[id]/page.tsx`     |
| 6   | Employees  | Employee page data extraction from multi-envelope format        | ✅     | `employees/page.tsx`       |
| 7   | Auth       | `auth.ts` — firstName/lastName extracted from JWT               | ✅     | `auth.ts`                  |

---

## Session 2 — Multi-service Fixes

| #   | Area          | Issue                                                       | Root Cause                                                                               | Status | File                                                                 |
| --- | ------------- | ----------------------------------------------------------- | ---------------------------------------------------------------------------------------- | ------ | -------------------------------------------------------------------- |
| 1   | Notifications | Page crash on 403/rate-limit                                | No error state — `toast.error()` only, page re-renders broken                            | ✅     | `notifications/page.tsx`                                             |
| 2   | URL Slug      | `OWN-SJ6IA6X` shown in URL/breadcrumbs instead of real name | `session.user.name` was the user code; `useOrgRoutes` didn't prefer `firstName+lastName` | ✅     | `useOrgRoutes.ts`                                                    |
| 3   | Payroll Runs  | "Failed to load payroll runs"                               | `PayrollController` mapped to `/clients/…` missing `/api/v1` prefix                      | 🔧     | `PayrollController.java`                                             |
| 4   | Timesheets    | "Failed to load timesheets"                                 | No `GET /api/v1/timesheets` list endpoint existed                                        | 🔧     | `TimesheetController.java`, `TimesheetService.java`                  |
| 5   | Payments      | `No static resource api/v1/payments`                        | Controllers deleted from `propertize-core`; payment-service not running                  | ⚠️     | Gateway routes correctly to `lb://payment-service` — needs Docker up |

---

## Known Outstanding Issues

| #   | Area          | Issue                                                   | Notes                                                                                                           |
| --- | ------------- | ------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| 1   | Docker        | `docker compose up -d` failing (exit code 1)            | Backend services not reachable; payroll+timesheet fixes need restart                                            |
| 2   | Payments      | 404 on `/api/v1/payments`                               | `payment-service` IS correctly coded and gateway-routed; service just needs to be running                       |
| 3   | Notifications | Rate limiting on profile + notifications simultaneously | Multiple page-load API calls firing in parallel; consider staggering with sequential awaits                     |
| 4   | RBAC          | `notification:list` permission missing for some roles   | Backend RBAC grant — requires auth-service config change; frontend now shows friendly error instead of crashing |

---

## Backend Services Status

| Service             | Port | Gateway Route                                           | Controller Prefix   | Notes                                                  |
| ------------------- | ---- | ------------------------------------------------------- | ------------------- | ------------------------------------------------------ |
| `propertize` (core) | 8082 | `/api/v1/properties/**` etc.                            | `/api/v1/…`         | Payment controllers removed (moved to payment-service) |
| `auth-service`      | 8081 | `/auth/**`                                              | `/auth/…`           | Handles JWT, RBAC                                      |
| `employee-service`  | 8083 | `/api/v1/employees/**`                                  | `/api/v1/…`         | ✅                                                     |
| `payment-service`   | 8084 | `/api/v1/payments/**`                                   | `/api/v1/…`         | ✅ routed correctly; needs Docker up                   |
| `payroll-service`   | 8085 | `/api/v1/clients/*/payroll/**`, `/api/v1/timesheets/**` | `/api/v1/…` (fixed) | `PayrollController` prefix fixed in Session 2          |

---

## Frontend Service → Gateway Route Map

| Service File              | Endpoint Called                      | Gateway Target                   |
| ------------------------- | ------------------------------------ | -------------------------------- |
| `payroll.service.ts`      | `/api/v1/clients/{clientId}/payroll` | `lb://payroll-service`           |
| `payroll.service.ts`      | `/api/v1/timesheets`                 | `lb://payroll-service`           |
| `payment.service.ts`      | `/api/v1/payments`                   | `lb://payment-service`           |
| `notification.service.ts` | `/api/v1/notifications`              | `lb://propertize-service` (core) |

---

## Quick Restart Checklist

After fixing backend code, run:

```bash
make build-java   # rebuild Java images
make up           # start all services
make health       # verify health endpoints
```

Or targeted:

```bash
docker compose build payroll-service && docker compose up -d payroll-service
```
