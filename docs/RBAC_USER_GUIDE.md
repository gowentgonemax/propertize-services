# Propertize RBAC User Guide

> **Version:** 7.0 | **Last Updated:** 2026-04-04  
> Authoritative reference for **who can do what** in Propertize. Intended for administrators, support staff, and technical reviewers.

---

## Table of Contents

1. [Role Hierarchy Overview](#1-role-hierarchy-overview)
2. [Platform Roles](#2-platform-roles)
3. [Portfolio Roles](#3-portfolio-roles)
4. [Organization Roles](#4-organization-roles)
5. [Functional Specialist Roles](#5-functional-specialist-roles)
6. [Team & Operational Roles](#6-team--operational-roles)
7. [External Roles](#7-external-roles)
8. [Org-Type-Specific Roles](#8-org-type-specific-roles)
9. [Who Can Assign Which Roles](#9-who-can-assign-which-roles)
10. [Explicit Denials — What Is Always Blocked](#10-explicit-denials--what-is-always-blocked)
11. [Custom Role Creation](#11-custom-role-creation)
12. [Permission Reference](#12-permission-reference)

---

## 1. Role Hierarchy Overview

Roles are ordered by **level** (higher = more privilege). A user can only assign roles **below their own level**.

| Level | Role                   | Category     | Scope        | Org Types                 |
| ----: | ---------------------- | ------------ | ------------ | ------------------------- |
|  1000 | PLATFORM_OVERSIGHT     | platform     | platform     | All                       |
|   970 | PLATFORM_OPERATIONS    | platform     | platform     | All                       |
|   950 | PLATFORM_ENGINEERING   | platform     | platform     | All                       |
|   999 | EMERGENCY_ACCESS       | special      | platform     | All (break-glass)         |
|   940 | CFO                    | organization | organization | CORPORATE                 |
|   930 | PLATFORM_ANALYTICS     | platform     | platform     | All                       |
|   920 | PORTFOLIO_OWNER        | portfolio    | portfolio    | All                       |
|   920 | HOA_DIRECTOR           | organization | organization | HOUSING_ASSOCIATION       |
|   900 | ORGANIZATION_OWNER     | organization | organization | All (single org)          |
|   870 | SOLO_OWNER             | organization | organization | INDIVIDUAL_PROPERTY_OWNER |
|   850 | ORGANIZATION_ADMIN     | organization | organization | All                       |
|   820 | HR_MANAGER             | organization | organization | CORPORATE, PMC            |
|   800 | PROPERTY_MANAGER       | organization | organization | All                       |
|   780 | OPERATIONS_MANAGER     | organization | organization | CORPORATE, PMC            |
|   750 | LEASE_SPECIALIST       | organization | team         | All                       |
|   750 | PORTFOLIO_ANALYST      | organization | organization | REI, CORPORATE            |
|   730 | INVESTOR_RELATIONS     | organization | organization | REI, CORPORATE            |
|   700 | LEASING_AGENT          | organization | team         | All                       |
|   700 | OWNER_RELATIONS        | organization | organization | PMC                       |
|   650 | ACCOUNTANT             | organization | team         | All                       |
|   650 | COMMUNITY_MANAGER      | organization | organization | HOUSING_ASSOCIATION       |
|   600 | MAINTENANCE_SUPERVISOR | organization | team         | All                       |
|   600 | BOARD_MEMBER           | organization | organization | HOUSING_ASSOCIATION       |
|   550 | TENANT_COORDINATOR     | organization | team         | All                       |
|   550 | CASE_WORKER            | organization | self         | HOUSING_ASSOCIATION       |
|   500 | TEAM_LEAD              | organization | team         | All                       |
|   450 | INSPECTOR              | operational  | team         | All                       |
|   400 | MAINTENANCE_TECHNICIAN | operational  | team         | All                       |
|   300 | TEAM_MEMBER            | operational  | team         | All                       |
|   200 | VENDOR                 | external     | self         | All (external)            |
|   150 | TENANT                 | external     | self         | All (external)            |
|   100 | APPLICANT              | external     | self         | All (external)            |
|    50 | READ_ONLY              | external     | self         | All                       |

**Org Type Abbreviations:** PMC = Property Management Company | REI = Real Estate Investor | HA = Housing Association | IPO = Individual Property Owner | CORP = Corporate

---

## 2. Platform Roles

Platform roles are for **Propertize staff** — they oversee all organizations but cannot modify org data.

### PLATFORM_OVERSIGHT (Level 1000)

- **Who is this for:** Executive-level platform admins
- **What they can do:** Read everything across all orgs; manage users across all orgs; full reporting & analytics; full system admin; end sessions
- **What they CANNOT do:** Create/update/delete/configure organizations or properties; create/modify employee records or payroll
- **Requires:** MFA enforced

### PLATFORM_OPERATIONS (Level 970)

- **Who is this for:** Platform operations team
- **What they can do:** Manage organizations (CRUD); manage users; read employee & payroll data; generate reports; monitor system; view platform dashboard
- **What they CANNOT do:** Manage employees/payroll (write), delete properties

### PLATFORM_ENGINEERING (Level 950)

- **Who is this for:** Infrastructure/DevOps team
- **What they can do:** Full system admin (configure, backup, restore, update); read audit logs
- **What they CANNOT do:** Access tenant/lease/payment/property data

### PLATFORM_ANALYTICS (Level 930)

- **Who is this for:** Business intelligence analysts
- **What they can do:** Full reporting & analytics export; read property, payment, employee, payroll data; view platform dashboard
- **What they CANNOT do:** Write anything (read-only)

### EMERGENCY_ACCESS (Level 999)

- **Who is this for:** Break-glass emergency scenario only
- **What they can do:** Everything — all permissions across the platform
- **Restrictions:** MFA required; all actions audited; max session = 1 hour
- **Must be time-limited and revoked immediately after use**

---

## 3. Portfolio Roles

### PORTFOLIO_OWNER (Level 920)

- **Who is this for:** Real estate investors managing multiple organizations/portfolios
- **What they can do:** Full property, lease, tenant, payment, maintenance, invoice, employee, payroll management across owned orgs; full reporting; audit access
- **What they CANNOT do:** Create or delete organizations; system admin
- **Key difference from ORGANIZATION_OWNER:** Has `EMPLOYEE_MANAGE` and `PAYROLL_MANAGE` — manages staff across properties

---

## 4. Organization Roles

### ORGANIZATION_OWNER (Level 900)

- **Who is this for:** Single-organization landlords
- **What they can do:** Full property, lease, tenant, maintenance, vendor management; read payments & invoices; manage users in their org; generate reports
- **What they CANNOT do:** `EMPLOYEE_MANAGE`, `PAYROLL_MANAGE` (explicitly denied); create/delete organizations
- **Key point:** This role is intentionally restricted to landlord activities. For employee/payroll management, use PORTFOLIO_OWNER.

### SOLO_OWNER (Level 870) — IPO only

- **Who is this for:** Individual landlords managing 1-3 properties with no staff
- **What they can do:** Own org read/update; full property, lease, tenant, maintenance management; read payments; manage vendors and tasks; manage documents
- **What they CANNOT do:** All employee and payroll operations (explicitly denied); create/delete users; manage other users

### ORGANIZATION_ADMIN (Level 850)

- **Who is this for:** Day-to-day org administrators (e.g., office managers)
- **What they can do:** Read org; create/read/update/list users + invite; full property, lease, tenant, maintenance, invoice management; read payments; manage vendors, tasks, contacts, documents; generate reports
- **What they CANNOT do:** Delete the organization; employee manage; payroll manage

---

## 5. Functional Specialist Roles

### PROPERTY_MANAGER (Level 800)

Full property, lease, tenant, maintenance management. Reads employee list (for assignment). Cannot modify payroll.

### LEASE_SPECIALIST (Level 750)

Full lease management; read properties and tenants; upload/download documents.

### LEASING_AGENT (Level 700)

Read properties; create/read/update leases; full tenant management; create invoices; leasing dashboard.

### ACCOUNTANT (Level 650)

Full payment and invoice management; read payroll and employee data; full reporting export; audit log read.

### MAINTENANCE_SUPERVISOR (Level 600)

Full maintenance management; read properties and employee list; maintenance dashboard.

### TENANT_COORDINATOR (Level 550)

Read/update tenants; communicate with tenants; read leases and properties; read/create maintenance; send notifications.

---

## 6. Team & Operational Roles

### TEAM_LEAD (Level 500)

Read properties, tenants, leases, maintenance; assign maintenance; read employees; read reports; manager dashboard.

### INSPECTOR (Level 450)

Read properties and maintenance; create and update maintenance records; upload documents.

### MAINTENANCE_TECHNICIAN (Level 400)

Update and complete maintenance work orders; read own property context.
_Active hours: Mon–Sat 06:00–22:00 EST_

### TEAM_MEMBER (Level 300)

Read properties, tenants, leases, maintenance; dashboard view.

---

## 7. External Roles

### VENDOR (Level 200)

Update maintenance work orders assigned to them; create and read invoices for their work.

### TENANT (Level 150)

Read own lease, make payments, create maintenance requests, read property info, upload documents, portal dashboard.

### APPLICANT (Level 100)

View available properties, read lease terms, upload application documents.

### READ_ONLY (Level 50)

Read-only access to all data domains — for auditors and observers. Cannot write anything.

---

## 8. Org-Type-Specific Roles

These roles are only meaningful in the indicated organization type. They can be assigned in any org but are auto-seeded only for the correct org type.

### Individual Property Owner (IPO)

| Role       | Level | What it enables                                            |
| ---------- | ----: | ---------------------------------------------------------- |
| SOLO_OWNER |   870 | Simplified owner for solo landlords — no staff, no payroll |

### Real Estate Investor (REI) / Corporate (CORP)

| Role               | Level | What it enables                            |
| ------------------ | ----: | ------------------------------------------ |
| PORTFOLIO_ANALYST  |   750 | Analytics, ROI analysis, market reporting  |
| INVESTOR_RELATIONS |   730 | Owner/investor reporting and communication |

### Housing Association (HA)

| Role              | Level | What it enables                                    |
| ----------------- | ----: | -------------------------------------------------- |
| HOA_DIRECTOR      |   920 | Full governance, employee management, payroll read |
| BOARD_MEMBER      |   600 | Read-only board member with dashboard access       |
| COMMUNITY_MANAGER |   650 | Resident welfare and social program management     |
| CASE_WORKER       |   550 | Individual resident support case management        |

### Corporate (CORP)

| Role               | Level | What it enables                                               |
| ------------------ | ----: | ------------------------------------------------------------- |
| CFO                |   940 | Full financial control, payroll approval, financial reporting |
| HR_MANAGER         |   820 | Employee lifecycle and payroll management                     |
| OPERATIONS_MANAGER |   780 | Property and maintenance operations                           |

### Property Management Company (PMC)

| Role               | Level | What it enables                                              |
| ------------------ | ----: | ------------------------------------------------------------ |
| HR_MANAGER         |   820 | Employee lifecycle and payroll management (shared with CORP) |
| OPERATIONS_MANAGER |   780 | Property and maintenance operations (shared with CORP)       |
| OWNER_RELATIONS    |   700 | Owner portal access, property owner reporting                |

---

## 9. Who Can Assign Which Roles

**Rule:** A user can only assign roles whose **level is strictly lower than their own level**. Additionally, the assignor must have `USER_MANAGE` or `USER_INVITE` permission.

| If you are...       | Level | You can assign roles up to level... | Examples of assignable roles                                  |
| ------------------- | ----: | :---------------------------------: | ------------------------------------------------------------- |
| PLATFORM_OVERSIGHT  |  1000 |            999 and below            | All roles except EMERGENCY_ACCESS                             |
| PLATFORM_OPERATIONS |   970 |            969 and below            | All non-platform roles (PORTFOLIO_OWNER and below)            |
| PORTFOLIO_OWNER     |   920 |            919 and below            | ORGANIZATION_OWNER, ORGANIZATION_ADMIN, all functional roles  |
| HOA_DIRECTOR        |   920 |            919 and below            | All standard org roles below 920                              |
| CFO (CORP)          |   940 |            939 and below            | Most org roles                                                |
| ORGANIZATION_OWNER  |   900 |            899 and below            | ORGANIZATION_ADMIN, PROPERTY_MANAGER, and all below           |
| SOLO_OWNER          |   870 |       **Cannot assign users**       | Explicitly denied `USER_CREATE`, `USER_MANAGE`                |
| ORGANIZATION_ADMIN  |   850 |            849 and below            | PROPERTY_MANAGER, LEASE_SPECIALIST, ACCOUNTANT, and all below |
| HR_MANAGER          |   820 |            819 and below            | Functional roles 800 and below                                |
| PROPERTY_MANAGER    |   800 |            799 and below            | LEASE_SPECIALIST, LEASING_AGENT, ACCOUNTANT, and all below    |

> **Important:** Role assignment is also governed by the `CustomRoleService` privilege check — you cannot grant permissions you don't possess yourself.

---

## 10. Explicit Denials — What Is Always Blocked

The following roles have **hard-blocked permissions** that are removed from the JWT at every login, refresh, and org-switch — even if a role's raw permission list or a custom role attempts to include them.

| Role               | Explicitly Denied Permissions                                                                                                               |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| PLATFORM_OVERSIGHT | ORGANIZATION_CREATE/UPDATE/DELETE/CONFIGURE/MANAGE, PROPERTY_CREATE/UPDATE/DELETE/MANAGE, EMPLOYEE_CREATE/UPDATE/DELETE, all PAYROLL writes |
| PORTFOLIO_OWNER    | ORGANIZATION_CREATE, ORGANIZATION_DELETE, SYSTEM_ADMIN                                                                                      |
| ORGANIZATION_OWNER | All EMPLOYEE writes, all PAYROLL writes, ORGANIZATION_CREATE/DELETE                                                                         |
| SOLO_OWNER         | All EMPLOYEE writes, all PAYROLL writes, USER_CREATE/DELETE/MANAGE                                                                          |
| ORGANIZATION_ADMIN | EMPLOYEE_MANAGE, PAYROLL_MANAGE, ORGANIZATION_DELETE                                                                                        |
| PORTFOLIO_ANALYST  | EMPLOYEE_MANAGE, PAYROLL_MANAGE, TENANT_CREATE/UPDATE/DELETE                                                                                |
| INVESTOR_RELATIONS | EMPLOYEE_MANAGE, PAYROLL_MANAGE                                                                                                             |
| COMMUNITY_MANAGER  | PAYMENT_CREATE, PAYROLL_MANAGE                                                                                                              |
| HR_MANAGER         | ORGANIZATION_DELETE, PROPERTY_DELETE                                                                                                        |
| OPERATIONS_MANAGER | PAYROLL_MANAGE, ORGANIZATION_DELETE                                                                                                         |
| OWNER_RELATIONS    | EMPLOYEE_MANAGE, PAYROLL_MANAGE, TENANT_CREATE/DELETE                                                                                       |

---

## 11. Custom Role Creation

Organizations can create custom roles via `POST /api/v1/rbac/custom-roles` (requires `USER_MANAGE` permission).

### Constraints

- **Max custom roles per org:** 10 (configurable via `maxCustomRolesPerClient`)
- **Permission scope:** Requested permissions must be a **subset of the creator's** own permissions — you cannot grant what you don't have
- **Level cap:** Custom role's `maxLevel` must be **lower than the creator's level** — cannot create higher-power roles
- **Inheritance:** Custom roles can inherit from any system base role; inherited permissions are included in the escalation check
- **Uniqueness:** Role name must be unique within the organization

### Example: Creating a "Senior Leasing Agent" custom role

```json
POST /api/v1/rbac/custom-roles
{
  "roleName": "SENIOR_LEASING_AGENT",
  "description": "Leasing agent with lease approval capability",
  "organizationId": 42,
  "inheritsFrom": "LEASING_AGENT",
  "permissions": ["LEASE_APPROVE"],
  "maxLevel": 720
}
```

The caller must have `LEASE_APPROVE` in their own permissions. The role level 720 must be below the caller's level.

---

## 12. Permission Reference

### Core Permission Groups

| Group               | Atomic Permissions                                                        |
| ------------------- | ------------------------------------------------------------------------- |
| USER_MANAGE         | USER_CREATE, USER_READ, USER_UPDATE, USER_DELETE, USER_LIST               |
| ORGANIZATION_MANAGE | ORGANIZATION_CREATE, READ, UPDATE, DELETE, LIST, CONFIGURE                |
| PROPERTY_MANAGE     | PROPERTY_CREATE, READ, UPDATE, DELETE, LIST                               |
| LEASE_MANAGE        | LEASE_CREATE, READ, UPDATE, DELETE, LIST, APPROVE, TERMINATE, RENEW       |
| TENANT_MANAGE       | TENANT_CREATE, READ, UPDATE, DELETE, LIST, SCREEN, COMMUNICATE            |
| PAYMENT_MANAGE      | PAYMENT_CREATE, READ, UPDATE, DELETE, LIST, PROCESS, RECONCILE            |
| MAINTENANCE_MANAGE  | MAINTENANCE_CREATE, READ, UPDATE, DELETE, LIST, ASSIGN, APPROVE, COMPLETE |
| INVOICE_MANAGE      | INVOICE_CREATE, READ, UPDATE, DELETE, LIST, SEND                          |
| EMPLOYEE_MANAGE     | EMPLOYEE_CREATE, READ, UPDATE, DELETE, LIST                               |
| PAYROLL_MANAGE      | PAYROLL_CREATE, READ, UPDATE, DELETE, LIST, PROCESS, APPROVE              |
| REPORT_FULL         | REPORT_CREATE, READ, UPDATE, DELETE, LIST, GENERATE, EXPORT, CUSTOMIZE    |
| SYSTEM_ADMIN        | SYSTEM_CONFIGURE, MONITOR, BACKUP, RESTORE, UPDATE, TROUBLESHOOT          |
| VENDOR_MANAGE       | VENDOR_CREATE, READ, UPDATE, DELETE, LIST                                 |
| DOCUMENT_MANAGE     | DOCUMENT_CREATE, READ, UPDATE, DELETE, LIST, UPLOAD, DOWNLOAD             |

### Special Permissions

| Permission                                         | Description                      |
| -------------------------------------------------- | -------------------------------- |
| ADMIN_ACCESS                                       | Platform admin access flag       |
| USER_INVITE                                        | Invite new users by email        |
| AUDIT_LOG_READ                                     | Read audit trail                 |
| AUDIT_LOG_EXPORT                                   | Export audit records             |
| ANALYTICS_READ/ANALYZE/EXPORT                      | Business intelligence            |
| DASHBOARD_READ_PLATFORM_DASHBOARD                  | Platform overview dashboard      |
| DASHBOARD_READ_PORTFOLIO_DASHBOARD                 | Portfolio overview dashboard     |
| DASHBOARD_READ_MANAGER                             | Organization manager dashboard   |
| DASHBOARD_READ_LEASING_DASHBOARD                   | Leasing operations dashboard     |
| DASHBOARD_READ_FINANCIAL_DASHBOARD                 | Financial dashboard (Accountant) |
| DASHBOARD_READ_MAINTENANCE_DASHBOARD               | Maintenance ops dashboard        |
| DASHBOARD_PORTAL                                   | Tenant self-service portal       |
| SESSION_LIST/TERMINATE/TERMINATE_ALL               | Session management               |
| RENTAL_APPLICATION_LIST/READ/APPROVE/REJECT/REVIEW | Application workflow             |
