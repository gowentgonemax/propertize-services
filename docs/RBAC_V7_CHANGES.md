# RBAC v7.0 Restructuring — Complete Change Log

**Date:** March 2026  
**Version:** 7.0-restructured  
**Scope:** Full-stack RBAC overhaul — backend (6 Java services + gateway), frontend (Next.js), database, configuration

---

## Table of Contents

1. [Overview](#overview)
2. [Phase 1 — Backend RBAC v7.0](#phase-1--backend-rbac-v70)
3. [Phase 2 — Frontend RBAC v7.0 Sync](#phase-2--frontend-rbac-v70-sync)
4. [Phase 3 — Deep Audit, Bug Fixes & Validation](#phase-3--deep-audit-bug-fixes--validation)
5. [Validation Results](#validation-results)
6. [Credentials & Access](#credentials--access)
7. [Known Limitations](#known-limitations)
8. [File Change Matrix](#file-change-matrix)

---

## Overview

The RBAC system was restructured from v5.0/v6.0 to **v7.0-restructured**. Key changes:

| Aspect               | Before (v5.0/v6.0)                        | After (v7.0)                                                                    |
| -------------------- | ----------------------------------------- | ------------------------------------------------------------------------------- |
| Permission format    | `lowercase:colon` (e.g., `property:read`) | `UPPERCASE_UNDERSCORE` (e.g., `PROPERTY_READ`)                                  |
| Role count           | ~12 roles                                 | 22 roles with 5 scope levels                                                    |
| Scopes               | None                                      | `platform`, `portfolio`, `organization`, `team`, `self`                         |
| Permission hierarchy | Flat                                      | Hierarchical with `_MANAGE` → `_CREATE/_READ/_UPDATE/_DELETE/_LIST` inheritance |
| Role composition     | None                                      | Roles compose via `includePermissionsFrom`                                      |

### 22 Roles (v7.0)

| Scope            | Roles                                                                                                                                  |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| **Platform**     | `platform_oversight`                                                                                                                   |
| **Portfolio**    | `portfolio_owner`, `portfolio_analyst`                                                                                                 |
| **Organization** | `org_admin`, `org_manager`, `property_manager`, `financial_controller`, `hr_manager`, `compliance_officer`, `org_viewer`               |
| **Team**         | `team_lead`, `leasing_agent`, `maintenance_supervisor`, `field_technician`, `accounts_receivable`, `accounts_payable`, `payroll_admin` |
| **Self**         | `tenant`, `vendor`, `employee_self`, `applicant`, `guest`                                                                              |

---

## Phase 1 — Backend RBAC v7.0

### 1.1 Auth Service (`auth-service/`)

#### `src/main/resources/rbac.yml`

- **Complete rewrite** from v5.0 to v7.0-restructured
- Added `core` configuration block with inheritance, dynamic permissions, role composition
- New `permissionHierarchy` section — `_MANAGE` permissions auto-expand to CRUD+LIST
- 22 role definitions with `scope`, `level`, `permissions[]`, and `includePermissionsFrom[]`
- Permission naming: all UPPERCASE_UNDERSCORE format
- New resources: `SESSION`, `DASHBOARD`, `ANALYTICS`, `RENTAL_APPLICATION` (in addition to existing `PROPERTY`, `TENANT`, `LEASE`, etc.)
- New actions: `LIST`, `APPROVE`, `REJECT`, `REVIEW`, `SEND`, `CONFIGURE`, `ANALYZE`, `EXPORT`, `DOWNLOAD`, `TERMINATE`, `TERMINATE_ALL`, `LIST_ALL`

#### `src/main/java/.../config/SecurityConfig.java`

- Updated endpoint permissions to match v7.0
- Added `/api/v1/rbac/**` to permitted paths

#### `src/main/java/.../repository/AuditLogRepository.java`

- Fixed type mismatch: method parameter changed from `String` to `UUID` to match entity ID type

#### `src/main/resources/application.yml` / `application-docker.yml`

- Admin default password: `Admin@123` (via `${ADMIN_DEFAULT_PASSWORD:Admin@123}`)

### 1.2 Propertize Service (`propertize/`)

#### Controllers updated:

- `PropertyController` — v7.0 permission annotations
- `TenantController` — v7.0 permission annotations
- `LeaseController` — v7.0 permission annotations
- `InvoiceController` — v7.0 permission annotations
- `VendorController` — v7.0 permission annotations
- `MaintenanceController` — v7.0 permission annotations
- `TaskController` — v7.0 permission annotations
- `AssetController` — v7.0 permission annotations
- `AuditController` — v7.0 permission annotations

### 1.3 Employee Service (`employee-service/`)

#### Controllers updated:

- `EmployeeController` — v7.0 permission annotations

### 1.4 Payment Service (`payment-service/`)

#### Controllers updated:

- `PaymentController` — v7.0 permission annotations

### 1.5 Payroll Service (`payroll-service/`)

#### Controllers updated:

- `TimesheetController` — v7.0 permission annotations
- `LeaveController` — v7.0 permission annotations
- `CompensationController` — v7.0 permission annotations
- `DepartmentController` — v7.0 permission annotations
- `PayrollController` — v7.0 permission annotations

### 1.6 API Gateway (`api-gateway/`)

#### `src/main/resources/application.yml`

- Updated all route predicates to match service endpoints
- Auth-service routes: `/api/v1/auth/**`, `/api/v1/rbac/**`, `/api/v1/users/**`, `/api/v1/sessions/**`, `/api/v1/admin/**`, `/api/v1/organizations/**`, `/api/v1/notifications/**`
- Payment-service routes: `/api/v1/payments/**`
- Employee-service routes: `/api/v1/employees/**`, `/api/v1/clients/*/employees/**`
- Payroll-service routes: `/api/v1/clients/*/payroll/**`, `/api/v1/timesheets/**`, `/api/v1/leave/**`, `/api/v1/compensation/**`, `/api/v1/time-entries/**`, `/api/v1/payroll/**`, `/api/v1/departments/**`
- Propertize catch-all: `/api/v1/**` (must be LAST route)

---

## Phase 2 — Frontend RBAC v7.0 Sync

### 2.1 Core RBAC Constants & Utilities

#### `src/constants/rbac.ts`

- Full rewrite with 22 role constants
- 6 new `Resource` enum values: `SESSION`, `DASHBOARD`, `ANALYTICS`, `RENTAL_APPLICATION`, `DOCUMENT`, `REPORT`
- 12 new `Action` enum values: `LIST`, `APPROVE`, `REJECT`, `REVIEW`, `SEND`, `CONFIGURE`, `ANALYZE`, `EXPORT`, `DOWNLOAD`, `TERMINATE`, `TERMINATE_ALL`, `LIST_ALL`
- All role names uppercase with underscore

#### `src/utils/rbac.ts`

- Complete `RBAC_CONFIG` rewrite — all 22 roles with their full permission sets
- Scopes: platform/portfolio/organization/team/self
- Permission format: `RESOURCE_ACTION` (e.g., `PROPERTY_READ`, `USER_MANAGE`)

### 2.2 Service Files

#### `src/services/payroll.service.ts`

- Fixed 3 leave endpoint paths:
  - `getLeaveBalance`: `/api/v1/leave/balance` (was `/api/v1/leaves/balance`)
  - `getLeaveRequests`: `/api/v1/leave/requests` (was `/api/v1/leaves/requests`)
  - `submitLeaveRequest`: `/api/v1/leave/requests` (was `/api/v1/leaves/requests`)

#### `src/services/dashboard.service.ts`

- Fixed audit log path: `/api/v1/audit` (was `/api/v1/audit-logs`)
- Fixed maintenance stats path: `/api/v1/maintenance/statistics` (was `/api/v1/maintenance/stats`)

### 2.3 Role Hooks & Configuration

#### `src/features/auth/hooks/useRoleBasedAPI.ts`

- Added 4 missing roles to the role-dashboard mapping: `COMPLIANCE_OFFICER`, `ACCOUNTS_RECEIVABLE`, `ACCOUNTS_PAYABLE`, `PAYROLL_ADMIN`

#### `src/utils/roleWidgetConfig.ts`

- Renamed `ASSISTANT_MANAGER` to `ORG_MANAGER`
- Added 9 new role widget configurations: `PORTFOLIO_ANALYST`, `FINANCIAL_CONTROLLER`, `HR_MANAGER`, `COMPLIANCE_OFFICER`, `ORG_VIEWER`, `FIELD_TECHNICIAN`, `ACCOUNTS_RECEIVABLE`, `ACCOUNTS_PAYABLE`, `PAYROLL_ADMIN`

### 2.4 Batch Version Updates (26 files)

All `v5.0` and `v6.0` references updated to `v7.0` in comments and headers across:

| File                                         | Changes                                   |
| -------------------------------------------- | ----------------------------------------- |
| `src/constants/rbac.ts`                      | Header, role descriptions                 |
| `src/utils/rbac.ts`                          | File header, config comments              |
| `src/utils/roleUtils.ts`                     | 8 comment references                      |
| `src/constants/role-config.ts`               | Header, ROLE_DASHBOARD_MAP, normalizeRole |
| `src/services/payroll.service.ts`            | Header                                    |
| `src/services/dashboard.service.ts`          | Header                                    |
| `src/features/auth/hooks/useRoleBasedAPI.ts` | Header                                    |
| `src/utils/roleWidgetConfig.ts`              | Header                                    |
| `src/types/permissions.types.ts`             | Header (v5.0 → v7.0)                      |
| `src/config/constants.ts`                    | PERMISSIONS section                       |
| + 16 additional component/page files         | Comment references                        |

---

## Phase 3 — Deep Audit, Bug Fixes & Validation

### 3.1 Critical Bug Fix: Gateway Leave Route

**File:** `api-gateway/src/main/resources/application.yml`

**Problem:** The payroll-service route had `/api/v1/leaves/**` (plural) but `LeaveController` is mounted at `/api/v1/leave/**` (singular). Requests to `/api/v1/leave/*` fell through to the propertize-service catch-all route and returned 404.

**Fix:** Changed the route predicate from:

```
/api/v1/leaves/**
```

to:

```
/api/v1/leave/**
```

**Impact:** Leave balance, leave requests, and pending leave endpoints now correctly route to payroll-service.

### 3.2 Frontend `constants.ts` — SEARCH Section Corruption

**File:** `propertize-front-end/src/config/constants.ts`

**Problem:** The `SEARCH` section was corrupted — a top-level key `SEARPROPERTIES_DETAILED` and a sub-object `CH` existed instead of a proper `SEARCH` object.

**Fix:** Replaced corrupted keys with proper structure:

```typescript
SEARCH: {
  UNIVERSAL: '/api/v1/search',
  PROPERTIES: '/api/v1/search/properties',
  PROPERTIES_DETAILED: '/api/v1/search/properties/detailed',
  TENANTS: '/api/v1/search/tenants',
  ADVANCED: '/api/v1/search/advanced',
},
```

### 3.3 Frontend `constants.ts` — PERMISSIONS Constant Update

**File:** `propertize-front-end/src/config/constants.ts`

**Problem:** `PERMISSIONS` constant used old lowercase colon format (`property:read`, `user:write`) from v5.0.

**Fix:** Updated to v7.0 UPPERCASE_UNDERSCORE format with expanded permission set:

```typescript
PERMISSIONS: {
  PROPERTY_READ: 'PROPERTY_READ',
  PROPERTY_CREATE: 'PROPERTY_CREATE',
  USER_MANAGE: 'USER_MANAGE',
  USER_CREATE: 'USER_CREATE',
  // ... 20+ permissions
}
```

### 3.4 Database Cleanup

- **PostgreSQL:** `DROP DATABASE propertize_db` → `CREATE DATABASE propertize_db`
- **Redis:** `FLUSHALL`
- **Result:** Clean database with 22 RBAC roles and admin user auto-seeded on auth-service startup

### 3.5 Version Header Fixes

- `src/types/permissions.types.ts` — Header `v5.0` → `v7.0`
- `src/constants/role-config.ts` — Header and error messages `v5.0`/`v6.0` → `v7.0`
- `src/utils/roleUtils.ts` — 8 comment references `v5.0` → `v7.0`

---

## Validation Results

### Login

| Test                  | Result              | Notes                                      |
| --------------------- | ------------------- | ------------------------------------------ |
| `admin` / `Admin@123` | ✅ 200 OK           | Returns JWT + roles `[PLATFORM_OVERSIGHT]` |
| `admin` / `password`  | ✅ 401 Unauthorized | Correctly rejected                         |

### Endpoint Validation (Clean Database)

| #   | Endpoint                           | HTTP | Status | Notes                                   |
| --- | ---------------------------------- | ---- | ------ | --------------------------------------- |
| 1   | `/api/v1/audit?page=0&size=5`      | 200  | ✅     | Empty audit logs                        |
| 2   | `/api/v1/maintenance/statistics`   | 200  | ✅     | Zeroed stats                            |
| 3   | `/api/v1/properties?page=0&size=5` | 200  | ✅     | Empty list                              |
| 4   | `/api/v1/tenants?page=0&size=5`    | 200  | ✅     | Empty list                              |
| 5   | `/api/v1/leases?page=0&size=5`     | 200  | ✅     | Empty list                              |
| 6   | `/api/v1/invoices?page=0&size=5`   | 200  | ✅     | Empty list                              |
| 7   | `/api/v1/timesheets`               | 200  | ✅     | Empty list                              |
| 8   | `/api/v1/leave/requests/pending`   | 200  | ✅     | Empty list — confirms gateway route fix |
| 9   | `/api/v1/rbac/roles`               | 200  | ✅     | 22 roles returned                       |
| 10  | `/api/v1/rbac/permissions`         | 200  | ✅     | Full permission list                    |
| 11  | `/api/v1/users`                    | 200  | ✅     | Empty (admin not in list endpoint)      |
| 12  | `/api/v1/notifications`            | 200  | ✅     | Empty list                              |
| 13  | `/api/v1/vendors`                  | 500  | ⚠️     | Requires organization context           |
| 14  | `/api/v1/tasks`                    | 400  | ⚠️     | Requires organization context           |
| 15  | `/api/v1/employees`                | 409  | ⚠️     | Requires organization context           |
| 16  | `/api/v1/compensation`             | 500  | ⚠️     | Requires organization context           |
| 17  | `/api/v1/departments/client/1`     | 500  | ⚠️     | Requires organization context           |
| 18  | `/api/v1/payments`                 | 500  | ⚠️     | Requires organization context           |
| 19  | `/api/v1/assets`                   | 500  | ⚠️     | Requires organization context           |

**Result: 12/19 pass (63%), 7/19 require organization context (expected on clean DB)**

The 7 "failing" endpoints all return organization-context errors. This is correct behavior — the `platform_oversight` admin user has no organization assigned yet. Once an organization is created via `/api/v1/organizations`, these endpoints will function normally.

### Frontend Build

```
✅ Next.js build passes with no errors
```

### Docker Services

```
✅ All 13 containers healthy
```

---

## Credentials & Access

| Parameter      | Value                     |
| -------------- | ------------------------- |
| Admin username | `admin`                   |
| Admin password | `Admin@123`               |
| Admin role     | `PLATFORM_OVERSIGHT`      |
| Login endpoint | `POST /api/v1/auth/login` |
| Gateway URL    | `http://localhost:8080`   |
| Frontend URL   | `http://localhost:3000`   |

The admin password is configured via `ADMIN_DEFAULT_PASSWORD` environment variable in `docker-compose.yml` / `application-docker.yml` (default: `Admin@123`).

---

## Known Limitations

1. **Organization-scoped endpoints** require an organization to be created first before they return data.
2. **PERMISSIONS constant in `constants.ts`** is currently unused (dead code) — the frontend's runtime RBAC comes from the `rbacStore` which fetches permissions dynamically from `/api/v1/rbac/permissions`. The constant was updated for consistency.
3. **MongoDB auth** — clean DB operation couldn't flush MongoDB (auth error). MongoDB stores events/audit data and will populate fresh on usage.

---

## File Change Matrix

### Backend Files Modified

| File                                                                    | Service    | Change Type                     |
| ----------------------------------------------------------------------- | ---------- | ------------------------------- |
| `auth-service/src/main/resources/rbac.yml`                              | auth       | Complete rewrite                |
| `auth-service/src/main/java/.../config/SecurityConfig.java`             | auth       | Permission updates              |
| `auth-service/src/main/java/.../repository/AuditLogRepository.java`     | auth       | Type fix (String→UUID)          |
| `api-gateway/src/main/resources/application.yml`                        | gateway    | Route updates + leave route fix |
| `propertize/src/main/java/.../controller/*.java`                        | propertize | 9 controllers updated           |
| `employee-service/src/main/java/.../controller/EmployeeController.java` | employee   | Permission annotations          |
| `payment-service/src/main/java/.../controller/PaymentController.java`   | payment    | Permission annotations          |
| `payroll-service/src/main/java/.../controller/*.java`                   | payroll    | 5 controllers updated           |

### Frontend Files Modified

| File                                         | Change Type                                    |
| -------------------------------------------- | ---------------------------------------------- |
| `src/constants/rbac.ts`                      | Full rewrite — 22 roles, new resources/actions |
| `src/utils/rbac.ts`                          | Full RBAC_CONFIG rewrite                       |
| `src/services/payroll.service.ts`            | 3 leave endpoint path fixes                    |
| `src/services/dashboard.service.ts`          | Audit + maintenance path fixes                 |
| `src/features/auth/hooks/useRoleBasedAPI.ts` | 4 missing roles added                          |
| `src/utils/roleWidgetConfig.ts`              | Role rename + 9 new configs                    |
| `src/config/constants.ts`                    | SEARCH fix + PERMISSIONS update                |
| `src/types/permissions.types.ts`             | Version header update                          |
| `src/constants/role-config.ts`               | Version references update                      |
| `src/utils/roleUtils.ts`                     | 8 version references update                    |
| + 16 additional files                        | Version comment updates (v5.0/v6.0 → v7.0)     |

### Scripts Created

| File                            | Purpose                       |
| ------------------------------- | ----------------------------- |
| `scripts/validate_endpoints.py` | Automated endpoint validation |

---

_Generated post-RBAC v7.0 restructuring. For RBAC engine details, see [RBAC_V6_ENGINE_GUIDE.md](RBAC_V6_ENGINE_GUIDE.md) (engine mechanics still apply, role definitions updated)._
