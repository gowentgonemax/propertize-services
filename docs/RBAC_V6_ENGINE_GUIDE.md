# Propertize RBAC v6.0 — Engine & Architecture Guide

> **Version**: 6.0-industrial  
> **Engine**: DefaultPolicyEngine v3.0 (RBAC + ABAC)  
> **Date**: March 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Authorization Pipeline](#authorization-pipeline)
4. [Roles & Hierarchy](#roles--hierarchy)
5. [ABAC Evaluators](#abac-evaluators)
6. [Field-Level Permissions](#field-level-permissions)
7. [Data Scopes (Row-Level Security)](#data-scopes-row-level-security)
8. [Conditional Permissions](#conditional-permissions)
9. [Time-Based Restrictions](#time-based-restrictions)
10. [Dynamic Role Composition](#dynamic-role-composition)
11. [REST API Endpoints](#rest-api-endpoints)
12. [Frontend Integration](#frontend-integration)
13. [Configuration Reference](#configuration-reference)
14. [What Changed (Old vs New)](#what-changed-old-vs-new)
15. [Files Modified](#files-modified)

---

## Overview

Propertize uses a centralized RBAC + ABAC (Attribute-Based Access Control) engine hosted in the **auth-service**. All services call the auth-service for authorization decisions — it is the single source of truth.

The engine evaluates access through a multi-step pipeline:

```
Request → Dynamic Role Composition → RBAC Permission Check → ABAC Conditions → ALLOW / DENY
```

Key capabilities:

- **22 roles** across 5 scopes (platform, portfolio, organization, team, self)
- **Permission hierarchy** — parent permissions expand to children
- **Field-level security** — different roles see different columns on the same resource
- **Row-level security** — data scopes restrict which records a role can access
- **Financial limits** — role-based spending caps with secondary approval thresholds
- **Time restrictions** — access limited by day-of-week and time-of-day
- **Dynamic role composition** — roles augmented at runtime based on context
- **Temporal permissions** — time-bound permission grants
- **Permission delegation** — cross-user delegation with audit trail
- **Custom & composite roles** — organization-level role creation
- **IP/geo access control** — whitelist/blacklist at multiple scopes
- **Full audit trail** — every authorization decision is logged

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        auth-service (:8081)                      │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              DefaultPolicyEngine v3.0                     │   │
│  │                                                           │   │
│  │  Step 0: DynamicRoleComposer                              │   │
│  │      ↓                                                    │   │
│  │  Step 1: RbacService.hasPermission(role, resource:action) │   │
│  │      ↓                                                    │   │
│  │  Step 2: ConditionEvaluator pipeline                      │   │
│  │    ├── OwnershipConditionEvaluator                        │   │
│  │    ├── TimeBasedConditionEvaluator                        │   │
│  │    ├── DataScopeConditionEvaluator                        │   │
│  │    └── ConditionalPermissionEvaluator                     │   │
│  │      ↓                                                    │   │
│  │  PolicyDecision { allowed, reason, metadata }             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────┐  ┌────────────────────────┐               │
│  │ FieldLevelPerm   │  │ RbacController (42 API) │               │
│  │ Service           │  │ endpoints)              │               │
│  └──────────────────┘  └────────────────────────┘               │
│                                                                  │
│  Config: rbac.yml (v6.0-industrial)                              │
│  Cache: Spring @Cacheable (per-user, TTL 3600s)                  │
└─────────────────────────────────────────────────────────────────┘
         ↑                    ↑                    ↑
    api-gateway          propertize-core      employee-service
     (:8080)               (:8082)              (:8083)
```

All services call `POST /api/v1/auth/authorize` to verify access. The auth-service reads role-to-permission mappings from `rbac.yml` and evaluates requests through the full ABAC pipeline.

---

## Authorization Pipeline

When a service calls `POST /authorize`, the engine runs this exact sequence:

### Step 0 — Dynamic Role Composition

`DynamicRoleComposer.composeRoles()` checks runtime context attributes and may add temporary roles:

| Condition                                         | Additional Role     |
| ------------------------------------------------- | ------------------- |
| ORGANIZATION_OWNER + org active + user verified   | `FINANCIAL_ANALYST` |
| PORTFOLIO_OWNER + PREMIUM/ENTERPRISE subscription | `ANALYTICS_VIEWER`  |

### Step 1 — RBAC Permission Check

Constructs `{resource}:{action}` (e.g., `property:update`), iterates effective roles, calls `RbacService.hasPermission(role, permission)`.

- Permission hierarchy is applied: `PROPERTY_MANAGE` expands to `property:create`, `property:read`, `property:update`, `property:delete`, `property:archive`.
- If **no role** grants the permission → **DENY** immediately.

### Step 2 — ABAC Condition Evaluators

Each registered `ConditionEvaluator` is checked in sequence. Any failure causes a **DENY**.

1. **Ownership** — If attributes contain `ownerId` or `organizationId`, verifies the user owns or belongs to the same org. Platform admins (`PLATFORM_OVERSIGHT`, `PLATFORM_OPERATIONS`) bypass ownership checks.

2. **Time-Based** — If the role has `timeRestrictions` in rbac.yml, verifies current time falls within `activeHours` and `activeDays` for the configured timezone.

3. **Data Scope** — If attributes contain `resourceType`, applies row-level scope rules (e.g., `own_properties`, `assigned_portfolio`, `self_only`).

4. **Conditional Limits** — If attributes contain `amount` or `cost`, verifies the value is within configured limits (e.g., `max_amount`, `requires_secondary_approval_above`).

### Result

```java
PolicyDecision {
    allowed: boolean,
    reason: String,           // Human-readable explanation
    evaluatedRoles: Set<String>,
    matchedPermission: String,
    configVersion: "3.0.0",
    policyId: "policy-v3",
    fromCache: boolean,
    metadata: Map<String, Object>
}
```

---

## Roles & Hierarchy

### Role Definitions (22 roles, 5 scopes)

| Role                     | Level | Scope        | Category     |
| ------------------------ | ----- | ------------ | ------------ |
| `EMERGENCY_ACCESS`       | 1100  | platform     | —            |
| `PLATFORM_OVERSIGHT`     | 1000  | platform     | platform     |
| `PLATFORM_OPERATIONS`    | 970   | platform     | platform     |
| `PLATFORM_ENGINEERING`   | 950   | platform     | platform     |
| `PLATFORM_ANALYTICS`     | 930   | platform     | platform     |
| `PORTFOLIO_OWNER`        | 920   | portfolio    | portfolio    |
| `ORGANIZATION_OWNER`     | 900   | organization | organization |
| `ORGANIZATION_ADMIN`     | 850   | organization | organization |
| `PROPERTY_MANAGER`       | 800   | organization | organization |
| `LEASING_AGENT`          | 700   | team         | organization |
| `LEASE_SPECIALIST`       | 680   | organization | —            |
| `ACCOUNTANT`             | 650   | team         | organization |
| `MAINTENANCE_SUPERVISOR` | 650   | team         | organization |
| `TEAM_LEAD`              | 550   | team         | —            |
| `TENANT_COORDINATOR`     | 500   | organization | —            |
| `INSPECTOR`              | 450   | organization | —            |
| `MAINTENANCE_TECHNICIAN` | 400   | team         | operational  |
| `TEAM_MEMBER`            | 300   | team         | —            |
| `TENANT`                 | 200   | self         | external     |
| `VENDOR`                 | 150   | self         | external     |
| `APPLICANT`              | 100   | self         | external     |
| `READ_ONLY`              | 50    | self         | external     |

### Permission Hierarchy (10 groups)

Each parent permission expands to all children:

```yaml
USER_MANAGE     → user:create, user:read, user:update, user:delete, user:assign_role
ORG_MANAGE      → organization:create, organization:read, organization:update, organization:delete, organization:manage_members
PROPERTY_MANAGE → property:create, property:read, property:update, property:delete, property:archive
LEASE_MANAGE    → lease:create, lease:read, lease:update, lease:terminate, lease:renew
TENANT_MANAGE   → tenant:create, tenant:read, tenant:update, tenant:delete, tenant:verify
PAYMENT_MANAGE  → payment:create, payment:read, payment:approve, payment:refund, payment:export
MAINT_MANAGE    → maintenance:create, maintenance:read, maintenance:update, maintenance:assign, maintenance:approve
INVOICE_MANAGE  → invoice:create, invoice:read, invoice:update, invoice:send, invoice:void
REPORT_FULL     → report:read, report:create, report:export, report:schedule
SYSTEM_ADMIN    → system:configure, system:monitor, system:audit, system:manage_integrations
```

### Scope Levels

| Scope          | Level | Description             |
| -------------- | ----- | ----------------------- |
| `platform`     | 1000  | Full system access      |
| `portfolio`    | 900   | All orgs in a portfolio |
| `organization` | 800   | Single org access       |
| `team`         | 500   | Team-level access       |
| `self`         | 100   | Own data only           |

`hasPrivilegeLevel(minRole)` compares the user's highest role level against the minimum required level.

---

## ABAC Evaluators

### 1. OwnershipConditionEvaluator

**Condition key**: `ownership`  
**Triggers when**: Attributes contain `ownerId` or `organizationId`

- Compares `context.userId` against `ownerId` attribute
- Compares `context.organizationId` against `organizationId` attribute
- **Bypassed** by: `PLATFORM_OVERSIGHT`, `PLATFORM_OPERATIONS`

### 2. TimeBasedConditionEvaluator

**Condition key**: `time_restriction`  
**Config source**: `rbac.timeRestrictions`

Current restrictions:

| Role                     | Active Hours | Active Days | Timezone         |
| ------------------------ | ------------ | ----------- | ---------------- |
| `MAINTENANCE_TECHNICIAN` | 06:00–22:00  | Mon–Sat     | America/New_York |
| `LEASING_AGENT`          | 08:00–20:00  | Mon–Sat     | America/New_York |
| `ACCOUNTANT`             | 07:00–19:00  | Mon–Fri     | America/New_York |

Supports overnight hour ranges (e.g., 22:00–06:00). Roles without restrictions are always allowed.

### 3. DataScopeConditionEvaluator

**Condition key**: `data_scope`  
**Config source**: `rbac.dataScopes`

| Role                     | Resource    | Scope Rule           |
| ------------------------ | ----------- | -------------------- |
| `PROPERTY_MANAGER`       | property    | `own_properties`     |
| `LEASING_AGENT`          | lease       | `own_properties`     |
| `MAINTENANCE_SUPERVISOR` | maintenance | `own_properties`     |
| `MAINTENANCE_TECHNICIAN` | maintenance | `assigned_portfolio` |
| `ACCOUNTANT`             | payment     | `own_organization`   |
| `TENANT`                 | lease       | `self_only`          |

Scope rules:

- **`all`** — No restriction (platform-level)
- **`assigned_portfolio`** — Only records in the user's assigned portfolio
- **`own_properties`** — Only records belonging to the user's managed properties
- **`own_region`** — Only records in the user's region
- **`own_organization`** — Only records in the user's organization
- **`self_only`** — Only the user's own records

### 4. ConditionalPermissionEvaluator

**Condition key**: `conditional_permission`  
**Config source**: `rbac.conditionalPermissions`

| Role                     | Permission            | Max Amount | Secondary Approval Above |
| ------------------------ | --------------------- | ---------- | ------------------------ |
| `PROPERTY_MANAGER`       | `payment:approve`     | $5,000     | —                        |
| `PROPERTY_MANAGER`       | `maintenance:approve` | $2,500     | —                        |
| `ORGANIZATION_ADMIN`     | `payment:approve`     | $25,000    | $10,000                  |
| `MAINTENANCE_SUPERVISOR` | `maintenance:approve` | $10,000    | —                        |
| `ACCOUNTANT`             | `payment:approve`     | $10,000    | —                        |

Checks `amount` or `cost` in request attributes against the role's configured limits.

---

## Field-Level Permissions

`FieldLevelPermissionService` provides per-resource, per-role field visibility.

**Config source**: `rbac.fieldLevelPermissions`

### Tenant Resource

| Role                     | Visible Fields                                      | Hidden Fields                                                           |
| ------------------------ | --------------------------------------------------- | ----------------------------------------------------------------------- |
| `PROPERTY_MANAGER`       | All                                                 | —                                                                       |
| `LEASING_AGENT`          | name, email, phone, lease_status, move_in_date, ... | ssn, credit_score, bank_account, income                                 |
| `MAINTENANCE_TECHNICIAN` | name, unit_number, phone, maintenance_history       | ssn, credit_score, bank_account, income, lease_details, payment_history |
| `ACCOUNTANT`             | name, email, lease_status, payment_history, ...     | ssn, credit_score, maintenance_history                                  |
| `TENANT`                 | name, email, phone, unit_number, lease_status, ...  | ssn, credit_score, bank_account, background_check                       |

### Property Resource

| Role                     | Visible Fields                                     | Hidden Fields                                            |
| ------------------------ | -------------------------------------------------- | -------------------------------------------------------- |
| `PROPERTY_MANAGER`       | All                                                | —                                                        |
| `LEASING_AGENT`          | name, address, unit_count, vacancy_rate, ...       | purchase_price, mortgage_details, insurance, tax_returns |
| `MAINTENANCE_TECHNICIAN` | name, address, maintenance_log, systems_info       | financial_data, purchase_price, mortgage_details, ...    |
| `ACCOUNTANT`             | name, address, financial_data, purchase_price, ... | maintenance_log, systems_info, inspection_reports        |
| `TENANT`                 | name, address, amenities, contact_info, rules      | financial_data, purchase_price, mortgage_details, ...    |

### Multi-Role Resolution

When a user has multiple roles, `FieldLevelPermissionService` resolves the **most-permissive** union:

- **Visible fields** = union of all roles' visible fields
- **Hidden fields** = intersection of all roles' hidden fields (field must be hidden by ALL roles to remain hidden)

---

## Data Scopes (Row-Level Security)

Data scopes restrict **which records** a role can access (not which fields — that's field-level).

When a service includes `resourceType` in the authorization request attributes, the engine checks the user's scope rule for that resource type and filters results accordingly.

Services must enforce the scope at the query level. The auth-service returns the applicable scope rule; the calling service applies the filter.

---

## Conditional Permissions

Conditional permissions add **value-based constraints** to otherwise-allowed operations. A PROPERTY_MANAGER may have `payment:approve` but only up to $5,000.

When attributes include `amount` or `cost`, the engine:

1. Looks up the role's conditional limit for that permission
2. If value > `max_amount` → **DENY**
3. If value > `requires_secondary_approval_above` → DENY with "secondary approval required" reason
4. Otherwise → **ALLOW**

---

## Time-Based Restrictions

Roles can be restricted to specific hours and days. The `TimeBasedConditionEvaluator` checks:

1. Is the current day in `activeDays`?
2. Is the current time within `activeHours`?

Both checks use the role's configured `timezone`. Supports overnight ranges (start > end wraps past midnight).

---

## Dynamic Role Composition

`DynamicRoleComposer` augments roles at runtime based on context attributes before any permission check runs:

| Base Role            | Condition                                    | Added Role          |
| -------------------- | -------------------------------------------- | ------------------- |
| `ORGANIZATION_OWNER` | `org_status=ACTIVE` AND `user_verified=true` | `FINANCIAL_ANALYST` |
| `PORTFOLIO_OWNER`    | `subscription_plan=PREMIUM` or `ENTERPRISE`  | `ANALYTICS_VIEWER`  |

This allows feature gating without creating new static roles. The composed roles persist only for the duration of that authorization request.

---

## REST API Endpoints

**Base path**: `/api/v1/auth` (42 endpoints)

### Core Authorization

| Method | Path                   | Description                            |
| ------ | ---------------------- | -------------------------------------- |
| POST   | `/authorize`           | Full policy engine evaluation          |
| GET    | `/permissions/{role}`  | All permissions for a role             |
| POST   | `/permissions/resolve` | Resolve permissions for multiple roles |
| POST   | `/permissions/check`   | Batch check multiple permissions       |
| GET    | `/roles`               | List all roles                         |
| GET    | `/roles/{role}`        | Role detail                            |
| GET    | `/roles/scope/{scope}` | Roles by scope                         |
| GET    | `/rbac/config`         | Full RBAC config                       |
| GET    | `/rbac/endpoints`      | Endpoint-permission mappings           |
| POST   | `/cache/invalidate`    | Invalidate caches                      |

### Temporal Permissions

| Method | Path                                           | Description                 |
| ------ | ---------------------------------------------- | --------------------------- |
| POST   | `/temporal-permissions`                        | Grant time-bound permission |
| GET    | `/temporal-permissions/user/{userId}`          | Active permissions for user |
| DELETE | `/temporal-permissions/{id}`                   | Revoke temporal permission  |
| GET    | `/temporal-permissions/granted-by/{grantorId}` | Granted-by lookup           |

### Composite Roles

| Method | Path                    | Description                    |
| ------ | ----------------------- | ------------------------------ |
| POST   | `/composite-roles`      | Create composite role          |
| GET    | `/composite-roles`      | List composite roles           |
| GET    | `/composite-roles/{id}` | Get with effective permissions |
| DELETE | `/composite-roles/{id}` | Deactivate                     |
| POST   | `/resolve-permissions`  | Resolve for ad-hoc role set    |

### Permission Delegation

| Method | Path                 | Description                |
| ------ | -------------------- | -------------------------- |
| POST   | `/delegations`       | Delegate permission        |
| GET    | `/delegations/by-me` | Outgoing delegations       |
| GET    | `/delegations/to-me` | Incoming delegations       |
| DELETE | `/delegations/{id}`  | Revoke delegation          |
| GET    | `/delegation-rules`  | Available delegation rules |

### Custom Roles

| Method | Path                 | Description        |
| ------ | -------------------- | ------------------ |
| POST   | `/custom-roles`      | Create custom role |
| GET    | `/custom-roles`      | List for org       |
| GET    | `/custom-roles/{id}` | Detail             |
| PUT    | `/custom-roles/{id}` | Update             |
| DELETE | `/custom-roles/{id}` | Soft-delete        |

### Audit Trail

| Method | Path                       | Description        |
| ------ | -------------------------- | ------------------ |
| GET    | `/rbac/audit-logs`         | Query by user/date |
| GET    | `/rbac/audit-logs/denials` | Recent denials     |
| GET    | `/rbac/audit-logs/summary` | Org summary        |

### IP/Geo Access Control

| Method | Path                   | Description     |
| ------ | ---------------------- | --------------- |
| POST   | `/rbac/ip-rules`       | Create IP rule  |
| GET    | `/rbac/ip-rules`       | List rules      |
| DELETE | `/rbac/ip-rules/{id}`  | Delete rule     |
| POST   | `/rbac/ip-rules/check` | Check IP access |

### Field-Level, Data Scope, Conditional, Time

| Method | Path                              | Description                 |
| ------ | --------------------------------- | --------------------------- |
| GET    | `/fields/{resource}/{role}`       | Field visibility for role   |
| POST   | `/fields/resolve`                 | Multi-role field resolution |
| GET    | `/fields/resources`               | Resources with field rules  |
| POST   | `/scope/resolve`                  | Resolve data scope          |
| GET    | `/conditions/{role}/{permission}` | Conditional limits          |
| POST   | `/time/check`                     | Time-based access check     |

---

## Frontend Integration

### Core Hook: `useRBAC`

```tsx
const {
  hasRole,
  hasAnyRole,
  hasAllRoles,
  hasPrivilegeLevel,
  isAdmin,
  isSuperAdmin,
  canManageProperties,
  canManageTenants,
  canViewFinancials,
  canManageMaintenance,
  checkPermission,
  hasPermission,
  getUserScope,
  getHighestPrivilege,
  getRoleDisplayName,
} = useRBAC();
```

Permission checks use the backend session — no hardcoded role-to-permission maps on the frontend. The `canManageX()` helpers use `hasPrivilegeLevel()` for clean, level-based checks instead of listing individual roles.

### Advanced ABAC Hooks: `useAdvancedRBAC`

| Hook                                         | Purpose                                                                                            | Backend Call                          |
| -------------------------------------------- | -------------------------------------------------------------------------------------------------- | ------------------------------------- |
| `useFieldPermissions(resource)`              | Get visible/hidden fields for current user on a resource. Provides `isFieldVisible(field)` helper. | `POST /fields/resolve`                |
| `useTimeAccess()`                            | Check time-based restrictions for current roles.                                                   | `POST /time/check`                    |
| `useDataScope(role, resource)`               | Resolve row-level scope for a role/resource pair.                                                  | `POST /scope/resolve`                 |
| `useConditionalPermission(role, permission)` | Get financial limits for a role/permission.                                                        | `GET /conditions/{role}/{permission}` |
| `useAuthorize()`                             | Full policy engine authorization check.                                                            | `POST /authorize`                     |

### Guard Components

```tsx
<PermissionGuard permission="property:update">
  <EditPropertyButton />
</PermissionGuard>

<PermissionGuard anyRole={[Roles.PROPERTY_MANAGER, Roles.ORGANIZATION_ADMIN]}>
  <AdminPanel />
</PermissionGuard>

<FieldGuard resource="tenant" field="ssn">
  <SSNDisplay value={tenant.ssn} />
</FieldGuard>
```

- **`PermissionGuard`** — Renders children only if user has required permission/role. Accepts `permission`, `anyPermission`, `allPermissions`, `role`, or `anyRole`. Optional `fallback` prop.
- **`FieldGuard`** — Renders children only if the user's role allows viewing a specific field on a resource.

---

## Configuration Reference

All RBAC configuration lives in `auth-service/src/main/resources/rbac.yml`.

### Top-Level Sections

| Section              | Key                      | Description                                                        |
| -------------------- | ------------------------ | ------------------------------------------------------------------ |
| Core                 | `core`                   | Default scope, inheritance, caching, custom role limits            |
| Permission Hierarchy | `permissionHierarchy`    | Parent → child permission expansion                                |
| Scopes               | `scopes`                 | Scope definitions with levels                                      |
| Roles                | `roles`                  | Full role definitions with permissions, restrictions, capabilities |
| Endpoints            | `endpoints`              | API path → method → permission (for API Gateway)                   |
| Time Restrictions    | `timeRestrictions`       | Per-role time-of-day/day-of-week rules                             |
| Field-Level          | `fieldLevelPermissions`  | Per-resource, per-role visible/hidden fields                       |
| Data Scopes          | `dataScopes`             | Per-role, per-resource row-level scope rules                       |
| Conditional          | `conditionalPermissions` | Per-role financial limits                                          |
| Dynamic Composition  | `dynamicRoleComposition` | Context-based role augmentation rules                              |

### Adding a New Role

1. Add the role to `rbac.yml` under `roles:` with permissions, scope, level, category
2. Add the role to `propertize-front-end/src/constants/rbac.ts` in the `Roles` constant
3. Add it to `ROLE_LEVELS` and `ROLE_SCOPE_MAP`
4. Add field-level rules if needed under `fieldLevelPermissions`
5. Add data scope rules if needed under `dataScopes`
6. Add time restrictions if needed under `timeRestrictions`
7. Add conditional limits if needed under `conditionalPermissions`

### Adding a New Permission

1. Add to the appropriate role in `rbac.yml`
2. If it's part of a hierarchy, add the parent to `permissionHierarchy`
3. Map endpoints to the permission under `endpoints:`

---

## What Changed (Old vs New)

### Old System (v2.0)

- Pure RBAC — roles map directly to permissions, no attribute checks
- Hardcoded role-to-permission map on the frontend (`roleDefaultPermissions`)
- `canManageProperties()` checked a hardcoded list of 6+ roles
- No field-level or row-level security
- No time restrictions
- No financial limits
- No dynamic role composition
- No temporal permissions or delegation
- Version scattered across comments (v2.0, v5.0 in different files)

### New System (v6.0)

| Feature              | Old                                                | New                                                                                                  |
| -------------------- | -------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Engine               | v2.0 — RBAC only                                   | v3.0 — RBAC + ABAC pipeline                                                                          |
| Permission source    | Duplicated: backend + frontend hardcoded map       | Single source: auth-service backend only                                                             |
| Field-level security | None                                               | Per-resource, per-role visible/hidden fields                                                         |
| Row-level security   | None                                               | 6 scope rules (all, portfolio, properties, region, org, self)                                        |
| Financial limits     | None                                               | Per-role max_amount + secondary approval thresholds                                                  |
| Time restrictions    | None                                               | Per-role activeHours + activeDays with timezone                                                      |
| Dynamic roles        | None                                               | Context-based role augmentation (org status, subscription)                                           |
| Permission checks    | Hardcoded role lists (e.g., `hasAnyRole(6 roles)`) | `hasPrivilegeLevel(minRole)` — level-based                                                           |
| Dependency injection | `@Autowired` field injection                       | Constructor injection via `@RequiredArgsConstructor`                                                 |
| REST endpoints       | ~10                                                | 42                                                                                                   |
| Frontend hooks       | `useRBAC`, `usePermissions`                        | + `useFieldPermissions`, `useTimeAccess`, `useDataScope`, `useConditionalPermission`, `useAuthorize` |
| Guard components     | None                                               | `PermissionGuard`, `FieldGuard`                                                                      |
| Tests                | 205                                                | 205 (all passing)                                                                                    |

### Key Code Changes

**Backend:**

- `DefaultPolicyEngine` — Added ABAC pipeline (Steps 0–2), `DynamicRoleComposer` injection
- `RbacController` — Migrated to constructor injection, added 6 new endpoints (fields, scope, conditions, time, config)
- New evaluators: `TimeBasedConditionEvaluator`, `DataScopeConditionEvaluator`, `ConditionalPermissionEvaluator`
- New services: `FieldLevelPermissionService`, `DynamicRoleComposer`
- `rbac.yml` — Added 5 new config sections (+120 lines)
- `RbacConfig.java` — Added nested config classes for new sections

**Frontend:**

- `rbac.ts` — `Roles` is now the primary export (`RBACv5Roles` is a deprecated alias)
- `usePermissions.ts` — Removed 80+ line hardcoded `roleDefaultPermissions` map; permissions come exclusively from backend session
- `useRBAC.ts` — Replaced hardcoded role lists with `hasPrivilegeLevel()` checks
- `useAdvancedRBAC.ts` — 5 new hooks for ABAC features
- `PermissionGuard.tsx` — `PermissionGuard` + `FieldGuard` components

---

## Files Modified

### Backend (auth-service)

| File                                                  | Change                                       |
| ----------------------------------------------------- | -------------------------------------------- |
| `rbac/engine/DefaultPolicyEngine.java`                | v3.0 with full ABAC pipeline                 |
| `rbac/controller/RbacController.java`                 | Constructor injection, 6 new endpoints, v3.0 |
| `rbac/config/RbacConfig.java`                         | v6.0, new nested config classes              |
| `rbac/evaluators/TimeBasedConditionEvaluator.java`    | **New** — time-of-day/day-of-week            |
| `rbac/evaluators/DataScopeConditionEvaluator.java`    | **New** — row-level security                 |
| `rbac/evaluators/ConditionalPermissionEvaluator.java` | **New** — financial limits                   |
| `rbac/service/FieldLevelPermissionService.java`       | **New** — column-level security              |
| `rbac/service/DynamicRoleComposer.java`               | **New** — runtime role augmentation          |
| `resources/rbac.yml`                                  | +120 lines (5 new config sections)           |
| `test/.../DefaultPolicyEngineTest.java`               | Updated for v3.0 constructor, mocks          |

### Frontend (propertize-front-end)

| File                                         | Change                                      |
| -------------------------------------------- | ------------------------------------------- |
| `src/constants/rbac.ts`                      | `Roles` primary export, v6.0                |
| `src/types/rbac.types.ts`                    | `Role` primary type, v6.0                   |
| `src/features/auth/hooks/usePermissions.ts`  | Removed hardcoded `roleDefaultPermissions`  |
| `src/features/auth/hooks/useRBAC.ts`         | `hasPrivilegeLevel()` instead of role lists |
| `src/features/auth/hooks/useAdvancedRBAC.ts` | **New** — 5 ABAC hooks                      |
| `src/components/auth/PermissionGuard.tsx`    | **New** — PermissionGuard + FieldGuard      |

### Bruno Collection

| File                                             | Description                         |
| ------------------------------------------------ | ----------------------------------- |
| `40-RBAC-Engine/Authorize.bru`                   | POST /authorize                     |
| `40-RBAC-Engine/Get-Role-Permissions.bru`        | GET /permissions/{role}             |
| `40-RBAC-Engine/List-All-Roles.bru`              | GET /roles                          |
| `40-RBAC-Engine/Get-Field-Permissions.bru`       | GET /fields/{resource}/{role}       |
| `40-RBAC-Engine/Resolve-Field-Access.bru`        | POST /fields/resolve                |
| `40-RBAC-Engine/Get-Field-Resources.bru`         | GET /fields/resources               |
| `40-RBAC-Engine/Resolve-Data-Scope.bru`          | POST /scope/resolve                 |
| `40-RBAC-Engine/Get-Conditional-Permissions.bru` | GET /conditions/{role}/{permission} |
| `40-RBAC-Engine/Check-Time-Access.bru`           | POST /time/check                    |
| `40-RBAC-Engine/Get-RBAC-Config.bru`             | GET /rbac/config                    |
| `40-RBAC-Engine/Resolve-Permissions.bru`         | POST /resolve                       |
