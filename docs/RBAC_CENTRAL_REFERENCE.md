# RBAC Central Reference — Single Source of Truth

> **Version:** 7.0-restructured  
> **Last Updated:** 2026-04-04  
> **Canonical File:** `auth-service/src/main/resources/rbac.yml`

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    RBAC Data Flow                            │
│                                                             │
│  rbac.yml (auth-service)  ──→  RbacConfig.java             │
│         │                      RbacService.java             │
│         │                      RbacSeederService.java       │
│         │                           │                       │
│         │                    ┌──────┴──────┐                │
│         │                    │  rbac_roles  │  (PostgreSQL)  │
│         │                    │    table     │                │
│         │                    └──────┬──────┘                │
│         │                           │                       │
│         ▼                           ▼                       │
│  api-gateway/rbac.yml    REST: /api/v1/rbac/roles           │
│  propertize/rbac.yml     REST: /api/v1/auth/permissions/*   │
│  (synced copies)         REST: /api/v1/auth/rbac/config     │
└─────────────────────────────────────────────────────────────┘
```

**Single source of truth:** `auth-service/src/main/resources/rbac.yml`

Gateway and propertize services maintain synced copies for fast local permission resolution.
At startup, `RbacSeederService` upserts all YAML roles into the `rbac_roles` DB table.

---

## Role Hierarchy Overview

| Level | Role                   | Scope        | Category     | Description                                                            |
| ----- | ---------------------- | ------------ | ------------ | ---------------------------------------------------------------------- |
| 999   | EMERGENCY_ACCESS       | platform     | special      | Break-glass emergency access (time-limited)                            |
| 1000  | PLATFORM_OVERSIGHT     | platform     | platform     | Full oversight, READ-ONLY for org data, can read employee/payroll      |
| 970   | PLATFORM_OPERATIONS    | platform     | platform     | Manage organizations, can read employee/payroll                        |
| 950   | PLATFORM_ENGINEERING   | platform     | platform     | System/infrastructure only                                             |
| 930   | PLATFORM_ANALYTICS     | platform     | platform     | Analytics + read employee/payroll for reporting                        |
| 920   | PORTFOLIO_OWNER        | portfolio    | portfolio    | **Multi-org full business operations** — employee & payroll management |
| 900   | ORGANIZATION_OWNER     | organization | organization | **Single org landlord** — NO employee/payroll                          |
| 850   | ORGANIZATION_ADMIN     | organization | organization | Day-to-day org admin operations                                        |
| 800   | PROPERTY_MANAGER       | organization | organization | Property, lease, tenant, maintenance operations                        |
| 750   | LEASE_SPECIALIST       | team         | organization | Lease agreements and contracts                                         |
| 700   | LEASING_AGENT          | team         | organization | Tenant acquisition and leasing                                         |
| 650   | ACCOUNTANT             | team         | organization | Financial ops — payments, invoices, payroll reads                      |
| 600   | MAINTENANCE_SUPERVISOR | team         | organization | Maintenance operations management                                      |
| 550   | TENANT_COORDINATOR     | team         | organization | Tenant relations and communication                                     |
| 500   | TEAM_LEAD              | team         | organization | Team/department lead                                                   |
| 450   | INSPECTOR              | team         | operational  | Property inspections                                                   |
| 400   | MAINTENANCE_TECHNICIAN | team         | operational  | Maintenance execution                                                  |
| 300   | TEAM_MEMBER            | team         | operational  | General team member                                                    |
| 200   | VENDOR                 | self         | external     | External vendor portal                                                 |
| 150   | TENANT                 | self         | external     | Tenant self-service portal                                             |
| 100   | APPLICANT              | self         | external     | Rental application                                                     |
| 50    | READ_ONLY              | self         | external     | Observer/auditor (read-only enforced)                                  |

---

## Key Business Rules (v7.0)

### 1. Organization Owner ≠ Portfolio Owner

| Aspect                | Organization Owner  | Portfolio Owner           |
| --------------------- | ------------------- | ------------------------- |
| **Scope**             | Single organization | Multiple organizations    |
| **Employee Access**   | ❌ NONE             | ✅ FULL (EMPLOYEE_MANAGE) |
| **Payroll Access**    | ❌ NONE             | ✅ FULL (PAYROLL_MANAGE)  |
| **Payment**           | 👁 READ-ONLY        | ✅ FULL (PAYMENT_MANAGE)  |
| **Invoice**           | 👁 READ-ONLY        | ✅ FULL (INVOICE_MANAGE)  |
| **Property**          | ✅ FULL             | ✅ FULL                   |
| **Lease**             | ✅ FULL             | ✅ FULL                   |
| **Tenant**            | ✅ FULL             | ✅ FULL                   |
| **Maintenance**       | ✅ FULL             | ✅ FULL                   |
| **Org Create/Delete** | ❌                  | ❌ (Platform Ops only)    |

**Rationale:** Organization Owner is a landlord with one property/org. They don't employ staff or process payroll. Portfolio Owner manages multiple orgs as a business operator — they need full HR/financial access.

### 2. Platform Roles + Employee/Payroll

Platform roles are **READ-ONLY** for organization data (properties, orgs, leases, tenants) but **CAN ACCESS** employee and payroll data in read mode:

- `PLATFORM_OVERSIGHT`: EMPLOYEE_READ, EMPLOYEE_LIST, PAYROLL_READ, PAYROLL_LIST
- `PLATFORM_OPERATIONS`: EMPLOYEE_READ, EMPLOYEE_LIST, PAYROLL_READ, PAYROLL_LIST
- `PLATFORM_ANALYTICS`: EMPLOYEE_READ, EMPLOYEE_LIST, PAYROLL_READ, PAYROLL_LIST
- `PLATFORM_ENGINEERING`: System/infra only — no employee/payroll

### 3. Read-Only Enforcement

Roles with `restrictions.readOnly: true` in rbac.yml are blocked from POST/PUT/PATCH/DELETE by the API Gateway's `RbacAuthorizationFilter`. Currently applies to:

- `PLATFORM_ANALYTICS`
- `READ_ONLY`

### 4. Explicit Denials

Some roles have `explicitDenials` that override any inherited/expanded permissions:

- `PLATFORM_OVERSIGHT`: Cannot create/update/delete orgs, properties, employees, payroll
- `PORTFOLIO_OWNER`: Cannot create/delete organizations (Platform Ops only), no system admin
- `ORGANIZATION_OWNER`: Cannot manage employees or payroll
- `ORGANIZATION_ADMIN`: Cannot manage employees or payroll, cannot delete org

---

## Permission Hierarchy (auto-expansion)

When a role has a `_MANAGE` permission, it automatically includes all sub-permissions:

| Macro Permission      | Expands To                                                                                                                                                    |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `USER_MANAGE`         | USER_CREATE, USER_READ, USER_UPDATE, USER_DELETE, USER_LIST                                                                                                   |
| `ORGANIZATION_MANAGE` | ORGANIZATION_CREATE, ORGANIZATION_READ, ORGANIZATION_UPDATE, ORGANIZATION_DELETE, ORGANIZATION_LIST, ORGANIZATION_CONFIGURE                                   |
| `PROPERTY_MANAGE`     | PROPERTY_CREATE, PROPERTY_READ, PROPERTY_UPDATE, PROPERTY_DELETE, PROPERTY_LIST                                                                               |
| `LEASE_MANAGE`        | LEASE_CREATE, LEASE_READ, LEASE_UPDATE, LEASE_DELETE, LEASE_LIST, LEASE_APPROVE, LEASE_TERMINATE, LEASE_RENEW                                                 |
| `TENANT_MANAGE`       | TENANT_CREATE, TENANT_READ, TENANT_UPDATE, TENANT_DELETE, TENANT_LIST, TENANT_SCREEN, TENANT_COMMUNICATE                                                      |
| `PAYMENT_MANAGE`      | PAYMENT_CREATE, PAYMENT_READ, PAYMENT_UPDATE, PAYMENT_DELETE, PAYMENT_LIST, PAYMENT_PROCESS, PAYMENT_RECONCILE                                                |
| `MAINTENANCE_MANAGE`  | MAINTENANCE_CREATE, MAINTENANCE_READ, MAINTENANCE_UPDATE, MAINTENANCE_DELETE, MAINTENANCE_LIST, MAINTENANCE_ASSIGN, MAINTENANCE_APPROVE, MAINTENANCE_COMPLETE |
| `INVOICE_MANAGE`      | INVOICE_CREATE, INVOICE_READ, INVOICE_UPDATE, INVOICE_DELETE, INVOICE_LIST, INVOICE_SEND                                                                      |
| `EMPLOYEE_MANAGE`     | EMPLOYEE_CREATE, EMPLOYEE_READ, EMPLOYEE_UPDATE, EMPLOYEE_DELETE, EMPLOYEE_LIST                                                                               |
| `PAYROLL_MANAGE`      | PAYROLL_CREATE, PAYROLL_READ, PAYROLL_UPDATE, PAYROLL_DELETE, PAYROLL_LIST, PAYROLL_PROCESS, PAYROLL_APPROVE                                                  |
| `REPORT_FULL`         | REPORT_CREATE, REPORT_READ, REPORT_UPDATE, REPORT_DELETE, REPORT_LIST, REPORT_GENERATE, REPORT_EXPORT, REPORT_CUSTOMIZE                                       |
| `SYSTEM_ADMIN`        | SYSTEM_CONFIGURE, SYSTEM_MONITOR, SYSTEM_BACKUP, SYSTEM_RESTORE, SYSTEM_UPDATE, SYSTEM_TROUBLESHOOT                                                           |

---

## Role → Service Access Matrix

| Role                   | Auth          | Propertize        | Employee | Payment         | Payroll | Reports     |
| ---------------------- | ------------- | ----------------- | -------- | --------------- | ------- | ----------- |
| EMERGENCY_ACCESS       | ✅ Full       | ✅ Full           | ✅ Full  | ✅ Full         | ✅ Full | ✅ Full     |
| PLATFORM_OVERSIGHT     | ✅ Full Users | 👁 Read           | 👁 Read  | 👁 Read         | 👁 Read | ✅ Full     |
| PLATFORM_OPERATIONS    | ✅ Full       | 👁 Read           | 👁 Read  | ❌              | 👁 Read | 📊 Generate |
| PLATFORM_ENGINEERING   | ❌            | ❌                | ❌       | ❌              | ❌      | ❌          |
| PLATFORM_ANALYTICS     | ❌            | 👁 Read           | 👁 Read  | 👁 Read         | 👁 Read | ✅ Full     |
| PORTFOLIO_OWNER        | ✅ Full Users | ✅ Full           | ✅ Full  | ✅ Full         | ✅ Full | ✅ Full     |
| ORGANIZATION_OWNER     | ✅ Org Users  | ✅ Full Props     | ❌       | 👁 Read         | ❌      | 📊 Generate |
| ORGANIZATION_ADMIN     | ✅ Org Users  | ✅ Full Props     | ❌       | 👁 Read         | ❌      | 📊 Generate |
| PROPERTY_MANAGER       | ❌            | ✅ Full Props     | 👁 Read  | 👁 Read         | ❌      | 📊 Generate |
| LEASE_SPECIALIST       | ❌            | ✅ Leases         | ❌       | ❌              | ❌      | ❌          |
| LEASING_AGENT          | ❌            | ✅ Leases+Tenants | ❌       | ❌              | ❌      | ❌          |
| ACCOUNTANT             | ❌            | ❌                | 👁 Read  | ✅ Full         | 👁 Read | 📊 Generate |
| MAINTENANCE_SUPERVISOR | ❌            | 👁 Props Read     | 👁 Read  | ❌              | ❌      | ❌          |
| TENANT_COORDINATOR     | ❌            | 👁 Read           | ❌       | ❌              | ❌      | ❌          |
| TEAM_LEAD              | ❌            | 👁 Read           | 👁 Read  | ❌              | ❌      | 👁 Read     |
| INSPECTOR              | ❌            | 👁 Props          | ❌       | ❌              | ❌      | ❌          |
| MAINTENANCE_TECHNICIAN | ❌            | ✏️ Maint Only     | ❌       | ❌              | ❌      | ❌          |
| TEAM_MEMBER            | ❌            | 👁 Read           | ❌       | ❌              | ❌      | ❌          |
| VENDOR                 | ❌            | ✏️ Maint Only     | ❌       | ❌              | ❌      | ❌          |
| TENANT                 | ❌            | 👁 Self Only      | ❌       | ✏️ Own Payments | ❌      | ❌          |
| APPLICANT              | ❌            | 👁 Props          | ❌       | ❌              | ❌      | ❌          |
| READ_ONLY              | ❌            | 👁 Read           | 👁 Read  | 👁 Read         | 👁 Read | ❌          |

Legend: ✅ Full access, 👁 Read-only, 📊 Generate/export, ✏️ Limited write, ❌ No access

---

## Test Users for Validation

Create these users via the auth-service registration/admin API to validate RBAC:

| #   | Username           | Email                      | Role                   | Purpose                           |
| --- | ------------------ | -------------------------- | ---------------------- | --------------------------------- |
| 1   | `admin`            | admin@propertize.com       | PLATFORM_OVERSIGHT     | Platform oversight (auto-created) |
| 2   | `platform-ops`     | ops@propertize.com         | PLATFORM_OPERATIONS    | Org management                    |
| 3   | `platform-eng`     | eng@propertize.com         | PLATFORM_ENGINEERING   | System/infra                      |
| 4   | `platform-analyst` | analyst@propertize.com     | PLATFORM_ANALYTICS     | Analytics                         |
| 5   | `portfolio-owner`  | portfolio@propertize.com   | PORTFOLIO_OWNER        | Multi-org full access             |
| 6   | `org-owner`        | owner@propertize.com       | ORGANIZATION_OWNER     | Single org landlord               |
| 7   | `org-admin`        | orgadmin@propertize.com    | ORGANIZATION_ADMIN     | Org admin                         |
| 8   | `prop-manager`     | propmanager@propertize.com | PROPERTY_MANAGER       | Property ops                      |
| 9   | `accountant`       | accountant@propertize.com  | ACCOUNTANT             | Financial                         |
| 10  | `leasing-agent`    | leasing@propertize.com     | LEASING_AGENT          | Leasing                           |
| 11  | `maint-supervisor` | maintsup@propertize.com    | MAINTENANCE_SUPERVISOR | Maintenance mgmt                  |
| 12  | `tenant-user`      | tenant@propertize.com      | TENANT                 | Tenant portal                     |
| 13  | `vendor-user`      | vendor@propertize.com      | VENDOR                 | Vendor portal                     |
| 14  | `readonly-user`    | readonly@propertize.com    | READ_ONLY              | Observer                          |
| 15  | `applicant-user`   | applicant@propertize.com   | APPLICANT              | Rental applicant                  |

---

## Gateway Endpoint Permission Map

The API Gateway (`RbacAuthorizationFilter`) maps endpoints to permissions:

| Endpoint Pattern        | Method    | Required Permission |
| ----------------------- | --------- | ------------------- |
| `/api/v1/properties`    | GET       | property:list       |
| `/api/v1/properties`    | POST      | property:create     |
| `/api/v1/properties/**` | GET       | property:read       |
| `/api/v1/properties/**` | PUT/PATCH | property:update     |
| `/api/v1/properties/**` | DELETE    | property:delete     |
| `/api/v1/tenants`       | GET       | tenant:list         |
| `/api/v1/tenants`       | POST      | tenant:create       |
| `/api/v1/tenants/**`    | GET       | tenant:read         |
| `/api/v1/tenants/**`    | PUT/PATCH | tenant:update       |
| `/api/v1/tenants/**`    | DELETE    | tenant:delete       |
| `/api/v1/leases`        | GET       | lease:list          |
| `/api/v1/leases`        | POST      | lease:create        |
| `/api/v1/leases/**`     | GET       | lease:read          |
| `/api/v1/leases/**`     | PUT/PATCH | lease:update        |
| `/api/v1/leases/**`     | DELETE    | lease:delete        |
| `/api/v1/employees`     | GET       | employee:list       |
| `/api/v1/employees`     | POST      | employee:create     |
| `/api/v1/employees/**`  | GET       | employee:read       |
| `/api/v1/employees/**`  | PUT       | employee:update     |
| `/api/v1/employees/**`  | DELETE    | employee:delete     |
| `/api/v1/payroll`       | GET       | payroll:list        |
| `/api/v1/payroll/**`    | GET       | payroll:read        |
| `/api/v1/payroll/**`    | POST      | payroll:process     |
| `/api/v1/payroll/**`    | PUT       | payroll:update      |
| `/api/v1/salaries/**`   | \*        | payroll:manage      |
| `/api/v1/deductions/**` | \*        | payroll:manage      |
| `/api/v1/payslips/**`   | GET       | payroll:read        |
| `/api/v1/timesheets/**` | GET       | payroll:read        |
| `/api/v1/timesheets/**` | POST      | payroll:create      |
| `/api/v1/payments`      | GET       | payment:list        |
| `/api/v1/payments`      | POST      | payment:create      |
| `/api/v1/payments/**`   | GET       | payment:read        |
| `/api/v1/invoices`      | GET       | invoice:list        |
| `/api/v1/invoices`      | POST      | invoice:create      |
| `/api/v1/invoices/**`   | GET       | invoice:read        |

---

## Syncing RBAC Configuration

When updating RBAC:

1. **Edit** `auth-service/src/main/resources/rbac.yml` (the single source of truth)
2. **Copy** the file to:
   - `api-gateway/src/main/resources/rbac.yml`
   - `propertize/src/main/resources/rbac.yml`
   - `propertize/src/test/resources/rbac.yml`
3. **Restart** services to pick up changes (or call cache invalidation endpoint)

```bash
# Sync command
cp auth-service/src/main/resources/rbac.yml api-gateway/src/main/resources/rbac.yml
cp auth-service/src/main/resources/rbac.yml propertize/src/main/resources/rbac.yml
cp auth-service/src/main/resources/rbac.yml propertize/src/test/resources/rbac.yml
```

---

## Files That Reference RBAC

| File                           | Service                             | Purpose                              |
| ------------------------------ | ----------------------------------- | ------------------------------------ |
| `rbac.yml`                     | auth-service                        | **Canonical** RBAC config            |
| `rbac.yml`                     | api-gateway                         | Synced copy for gateway enforcement  |
| `rbac.yml`                     | propertize                          | Synced copy for service-level checks |
| `RbacConfig.java`              | auth-service                        | Spring config binding                |
| `RbacConfig.java`              | api-gateway                         | SnakeYAML manual parser              |
| `RbacConfig.java`              | propertize                          | Spring config binding                |
| `RbacService.java`             | auth-service                        | Permission resolution engine         |
| `RbacService.java`             | propertize                          | Local permission resolution          |
| `RbacSeederService.java`       | auth-service                        | Seeds roles to DB at startup         |
| `RbacAuthorizationFilter.java` | api-gateway                         | Gateway RBAC enforcement             |
| `UserRoleEnum.java`            | auth-service / propertize / payroll | Role enum (must match rbac.yml)      |
| `RoleConstants.java`           | auth-service / propertize           | String constants for role names      |
| `PermissionConstants.java`     | auth-service / propertize           | String constants for permissions     |
| `RbacController.java`          | auth-service                        | REST API for RBAC queries            |
| `RbacPublicController.java`    | auth-service                        | Public role/permission catalog       |
