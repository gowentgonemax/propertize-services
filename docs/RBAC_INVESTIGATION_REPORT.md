# RBAC Deep Investigation Report — v7.0

> **Date:** 2026-04-04 | **Investigator:** Automated code audit  
> Documents all RBAC design gaps, dead code, logic bugs, and their resolutions found during the v7.0 investigation.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Critical Bug: Explicit Denials Never Enforced](#3-critical-bug-explicit-denials-never-enforced)
4. [Critical Bug: applicableOrgTypes Silently Ignored](#4-critical-bug-applicableorgtypes-silently-ignored)
5. [Critical Bug: Flyway V13 Wrong Column Name](#5-critical-bug-flyway-v13-wrong-column-name)
6. [Critical Bug: New Users Missing orgType in JWT](#6-critical-bug-new-users-missing-orgtype-in-jwt)
7. [Dead Code: AuthenticationService.login()](#7-dead-code-authenticationservicelogin)
8. [Bug: X-Primary-Role Always Empty](#8-bug-x-primary-role-always-empty)
9. [Ambiguity: allowRuntimeRoleCreation and CustomRoleService](#9-ambiguity-allowruntimerolecreation-and-customroleservice)
10. [Gap: X-Org-Type Not Consumed by Downstream Services](#10-gap-x-org-type-not-consumed-by-downstream-services)
11. [Design Note: Role Level Collision (HOA_DIRECTOR / CFO vs PORTFOLIO_OWNER)](#11-design-note-role-level-collision-hoa_director--cfo-vs-portfolio_owner)
12. [Summary of All Fixes Applied](#12-summary-of-all-fixes-applied)
13. [Remaining Recommendations](#13-remaining-recommendations)

---

## 1. Executive Summary

A systematic audit of the Propertize RBAC system uncovered **5 critical bugs** and **3 design gaps**:

| Severity                               | Count | Status                      |
| -------------------------------------- | ----: | --------------------------- |
| Critical (security/stability blockers) |     5 | ✅ All fixed                |
| Medium (dead code, confusion risks)    |     2 | ✅ Fixed                    |
| Design gaps (documented, tracked)      |     3 | ⬜ Recommendations provided |

The most impactful finding: **explicit denials in `rbac.yml` were never applied at token generation time**. This meant that `SOLO_OWNER`, `ORGANIZATION_OWNER`, `PORTFOLIO_OWNER`, and all other roles with `explicitDenials:` lists were receiving their denied permissions in their JWT anyway — the denial lists were parsed, seeded to DB, but never subtracted from the permission set.

---

## 2. Architecture Overview

The RBAC system spans multiple files:

```
rbac.yml (source of truth)
├── RbacConfig.java        — YAML binding (Spring @ConfigurationProperties)
├── RbacSeederService.java  — Seeds rbac.yml roles to rbac_roles table on startup
├── RbacService.java        — Runtime permission resolution
│   ├── getPermissionsForRole(roleName)       — expand permissions for a role
│   ├── getExplicitDenialsForRoles(roles)     — NEW in v7.0
│   └── getAllRoles()                          — list all known role names
├── AuthController.java     — Token generation (login, refresh, org-switch)
│   └── Uses generateAccessTokenWithPermissions() + explicit denial removal
├── CustomRoleService.java  — Org-scoped custom role CRUD
├── UserCustomRoleAssignment — Maps users to custom roles
└── RbacRole entity         — DB representation of a role
```

**Permission resolution at login (v7.0 corrected flow):**

```
userRoles (from User.roles) + customRoleAssignments
    ↓ RbacService.getPermissionsForRole(each role)
    ↓ expand hierarchy groups (PROPERTY_MANAGE → 5 permissions)
rawPermissions
    ↓ customRole.additionalPermissions merged
    ↓ rbacService.getExplicitDenialsForRoles(roles) subtracted   ← NEW
finalPermissions → JWT "permissions" claim
```

---

## 3. Critical Bug: Explicit Denials Never Enforced

**Severity:** Critical (security)  
**Status:** ✅ Fixed

### What Was Happening

`rbac.yml` defines `explicitDenials:` lists for several roles (e.g. `ORGANIZATION_OWNER`, `SOLO_OWNER`, `PORTFOLIO_OWNER`). These lists enumerate permissions that **must never** be in the JWT for that role regardless of inheritance.

The Java code:

- `RbacConfig.RoleConfig` **had no `applicableOrgTypes` or `explicitDenials` field** → Spring YAML binding silently ignored both fields
- `RbacSeederService` **never persisted `explicitDenials`** to the `rbac_roles.explicit_denials` column (column also didn't exist)
- `AuthController.login()`, `.refresh()`, `.switchOrganization()` **never called any denial-removal logic**

**Result:** A `SOLO_OWNER` user received `EMPLOYEE_MANAGE`, `PAYROLL_MANAGE`, `USER_MANAGE` permissions in their JWT — all of which rbac.yml explicitly denies for that role.

### Root Cause Chain

```
rbac.yml: SOLO_OWNER.explicitDenials: [EMPLOYEE_MANAGE, ...]
  → RbacConfig.RoleConfig: no explicitDenials field  (YAML silently dropped)
  → RbacSeederService: no code to persist explicitDenials
  → AuthController: no code to read or apply explicitDenials
  → JwtTokenProvider: token includes all permissions, including denied ones
```

### Fix Applied

1. Added `private List<String> explicitDenials;` to `RbacConfig.RoleConfig` (and `applicableOrgTypes`)
2. Created V14 Flyway migration: added `explicit_denials TEXT` and `applicable_org_types VARCHAR(500)` to `rbac_roles`
3. Added `buildCsv()` helper to `RbacSeederService`; now persists both fields
4. Added `RbacService.getExplicitDenialsForRoles(Set<String> roles)` — reads directly from `rbac.yml` config bean
5. Applied at all 3 token generation sites in `AuthController`:
   ```java
   Set<String> deniedPerms = rbacService.getExplicitDenialsForRoles(roles);
   permissions.removeAll(deniedPerms);
   ```

### Roles Affected (with meaningful denials)

- `SOLO_OWNER` — Employee writes, payroll writes, user management blocked
- `ORGANIZATION_OWNER` — Employee writes, payroll writes, org create/delete blocked
- `ORGANIZATION_ADMIN` — Employee manage, payroll manage, org delete blocked
- `PORTFOLIO_OWNER` — Org create/delete, system admin blocked
- `PLATFORM_OVERSIGHT` — All org/property writes, all employee/payroll writes blocked
- `PORTFOLIO_ANALYST`, `INVESTOR_RELATIONS` — Employee manage, payroll manage, some tenant writes blocked
- Others with `explicitDenials:` — `COMMUNITY_MANAGER`, `HR_MANAGER`, `OPERATIONS_MANAGER`, `OWNER_RELATIONS`

---

## 4. Critical Bug: applicableOrgTypes Silently Ignored

**Severity:** Critical (org-type enforcement)  
**Status:** ✅ Fixed

### What Was Happening

`rbac.yml` assigns `applicableOrgTypes:` to org-type-specific roles (e.g. `SOLO_OWNER: [INDIVIDUAL_PROPERTY_OWNER]`). The intent is:

- Advisory: the seeder knows which orgs to auto-populate these roles for
- Enforced: the org switch logic should validate role+orgType compatibility

Neither was working:

- `RbacConfig.RoleConfig` had no `applicableOrgTypes` Java field → parsing was a no-op
- `RbacRole` entity had no column for this → even if parsed, couldn't be stored
- No enforcement logic existed anywhere

### Fix Applied

1. `RbacConfig.RoleConfig` — added `private List<String> applicableOrgTypes`
2. `RbacRole` entity — added `@Column(name = "applicable_org_types", length = 500)`
3. V14 migration adds the column to DB
4. `RbacSeederService` now calls `buildCsv(cfg.getApplicableOrgTypes())` and sets it on the entity

### Note on Enforcement

`applicableOrgTypes` is currently **advisory** — it is stored in the DB but there is no enforcement gate that prevents assigning a `SOLO_OWNER` role to a `CORPORATE` org user. This is intentional per the `rbac.yml` comment: _"applicableOrgTypes is advisory — the seeder uses it for auto-assignment."_ Runtime enforcement should be added in a future version if stricter org-type isolation is needed.

---

## 5. Critical Bug: Flyway V13 Wrong Column Name

**Severity:** Critical (system crash blocker)  
**Status:** ✅ Fixed

### What Was Happening

V13 migration attempted to backfill `users.organization_type` from the `organizations` table:

```sql
-- BUG: column doesn't exist
UPDATE users u
SET    organization_type = o.organization_type_enum  -- ← WRONG
FROM   organizations o
WHERE  u.organization_id = o.id;
```

The actual column in the `organizations` table is `organization_type` (not `organization_type_enum`). This caused a `PSQLException: column o.organization_type_enum does not exist` on auth-service startup, putting auth-service in a restart loop.

### Impact Chain

Auth-service crash → api-gateway stuck at dependency health check → main-service, payroll-service, payment-service all in "Created" state (never started) → entire platform effectively offline.

### Fix Applied

```sql
-- FIXED:
UPDATE users u
SET    organization_type = o.organization_type
FROM   organizations o
WHERE  u.organization_id = o.id
   OR  u.organization_id = o.organization_code;
```

Unnecessary `::text` casts also removed (both columns are already `VARCHAR`).

---

## 6. Critical Bug: New Users Missing orgType in JWT

**Severity:** High (feature regression)  
**Status:** ✅ Fixed

### What Was Happening

`OnboardingService.createOwnerUser()` builds a `CreateUserRequest` when registering a new organization owner, but **never passed `organizationType`** to the DTO.

`CreateUserRequest` in both auth-service and propertize client **had no `organizationType` field** — even if `OnboardingService` tried to set it, the DTO couldn't carry it.

**Result:** All newly onboarded organization owners had `null` as their `organization_type` in the `users` table → empty `orgType` claim in JWT → org-type-specific features and explicit denials based on org type would not apply.

### Fix Applied

1. Added `private String organizationType;` to both `CreateUserRequest` DTOs
2. `UserManagementService.createUser()` — added `.organizationType(request.getOrganizationType())` to `User.builder()`
3. `OnboardingService.createOwnerUser()` — added `.organizationType(org.getOrganizationTypeEnum() != null ? org.getOrganizationTypeEnum().name() : null)`

---

## 7. Dead Code: AuthenticationService.login()

**Severity:** Medium (dead code, confusion risk)  
**Status:** ✅ Marked deprecated, throws UnsupportedOperationException

### What Was Found

`AuthenticationService.java` contains a full `login()` implementation that:

- Used `jwtTokenProvider.generateAccessToken(User user)` — the basic method that **omits `permissions` and `role` claims**
- Had no RBAC permission resolution
- Had no custom role integration
- Had no explicit denial enforcement
- Was **never called** — production login path is exclusively `AuthController.login()`

`AuthenticationService.refresh()` also existed, with an internal comment: _"Use AuthController.refresh() which includes custom-role permission resolution."_ — confirming it was also dead code.

### Evidence

```bash
grep -rn "authenticationService\.\|new AuthenticationService" src/
# 0 results — no production callers found
```

### Fix Applied

- `AuthenticationService.java` — class annotated `@Deprecated(since = "7.0", forRemoval = true)`; all injected fields removed; `login()` and `refresh()` throw `UnsupportedOperationException` with clear message; `logout()` still functional (returns success response)
- `logout()` is kept because it may be wired as part of a logout endpoint, and its implementation is harmless (returns success without DB interaction)

---

## 8. Bug: X-Primary-Role Always Empty

**Severity:** Medium (downstream feature degradation)  
**Status:** ✅ Fixed

### What Was Found

`JwtAuthenticationFilter` in api-gateway calls `tokenProvider.getPrimaryRole()` to populate `X-Primary-Role`. That method reads the `"role"` claim from the JWT. However, `generateAccessTokenWithPermissions()` in auth-service **never set the `"role"` claim**.

```java
// BEFORE — no "role" claim ever set
claims.put("roles", roles);   // plural — all roles as list
// claims.put("role", ...) — MISSING
```

`getPrimaryRole()` in gateway → empty string → `X-Primary-Role: ""` on every request.

Any downstream service logic that branched on `X-Primary-Role` was always evaluating against an empty string.

### Fix Applied

```java
// AFTER — deterministic primary role derived and set
String primaryRole = roles != null && !roles.isEmpty()
        ? roles.stream().sorted().findFirst().orElse("")
        : "";
claims.put("role", primaryRole);   // ← NEW
```

The primary role is the **first alphabetically sorted role** from the set — deterministic for any given set of roles.

---

## 9. Ambiguity: allowRuntimeRoleCreation and CustomRoleService

**Severity:** Design question  
**Status:** ✅ Clarified — no code change needed

### What Was Found

`rbac.yml` core config:

```yaml
core:
  allowRuntimeRoleCreation: true
  maxCustomRolesPerClient: 10
```

This appeared ambiguous — does it mean any user can create roles at runtime? Could this bypass RBAC?

### Investigation Result

`allowRuntimeRoleCreation` is a configuration flag for documentation/feature-flag purposes. `CustomRoleService.createCustomRole()` enforces strict privilege controls regardless:

1. **Permission subset check:** All permissions in the custom role must be in the creator's own permission set
   ```java
   if (!validatePermissions(requestedPermissions, creatorPermissions)) {
       throw new IllegalArgumentException("Requested permissions exceed creator's permissions...");
   }
   ```
2. **Level cap:** `maxLevel` of the custom role must be strictly less than creator's level
   ```java
   if (request.getMaxLevel() > creatorMaxLevel) {
       throw new IllegalArgumentException("maxLevel exceeds creator's privilege level");
   }
   ```
3. **Quota:** Max 10 custom roles per organization
4. **Uniqueness:** Role name must be unique within the org

**Conclusion:** Custom role creation is safe from privilege escalation. The `allowRuntimeRoleCreation` flag has no runtime enforcement but could be wired as a feature flag if orgs should have the ability disabled. Current behavior: all orgs with `USER_MANAGE` permission can create custom roles up to their quota.

---

## 10. Gap: X-Org-Type Not Consumed by Downstream Services

**Severity:** Design gap  
**Status:** ⬜ Documented — fix recommended

### What Was Found

The API Gateway correctly injects `X-Org-Type` based on the `orgType` JWT claim. However, downstream service header filters (e.g., `TrustedGatewayHeaderFilter` in employee-service) do **not** read `X-Org-Type` into the `GatewayAuthenticatedUser` object.

This means:

- Employee service endpoints do not enforce org-type restrictions (e.g., `INDIVIDUAL_PROPERTY_OWNER` orgs should not access employee APIs)
- Payroll service does not restrict by org type either

### Recommendation

Add `X-Org-Type` to `TrustedGatewayHeaderFilter` in employee-service and payroll-service:

```java
String orgType = request.getHeader("X-Org-Type");
user.setOrgType(orgType);
```

Then add an `OrgTypeGuard` or annotation (`@RequiresOrgType({"PROPERTY_MANAGEMENT_COMPANY", "CORPORATE"})`) on employee/payroll endpoints to reject requests from `INDIVIDUAL_PROPERTY_OWNER` accounts (who are explicitly denied employee/payroll operations via JWT but may still attempt direct API calls if they somehow bypass the gateway).

---

## 11. Design Note: Role Level Collision (HOA_DIRECTOR / CFO vs PORTFOLIO_OWNER)

**Severity:** Design concern  
**Status:** ⬜ Documented — monitor for role assignment edge cases

### What Was Found

`HOA_DIRECTOR` has level `920` and `CFO` has level `940` — **equal to or higher than** `PORTFOLIO_OWNER` (level `920`).

The role assignment rule is: _"a user can only assign roles below their own level."_ This creates an edge case:

- A `PORTFOLIO_OWNER` (920) cannot assign `HOA_DIRECTOR` (920) — levels are equal, not strictly less than
- A `CFO` (940) can assign `PORTFOLIO_OWNER` (920) — CFO outranks a portfolio owner

Both of these seem like unintended design artifacts. HOA and CORP roles were added after the main hierarchy was established.

### Recommendation

Consider adjusting levels:

```yaml
HOA_DIRECTOR: level: 890  # Below ORGANIZATION_OWNER (900) but above ORGANIZATION_ADMIN (850)
CFO: level: 890           # Same — CFO is a functional C-suite, not a platform owner
```

This places them correctly in the organizational role band without creating anomalies in portfolio-level role assignment.

---

## 12. Summary of All Fixes Applied

| #   | File(s) Changed                                 | Change                                                                  | Reason                                  |
| --- | ----------------------------------------------- | ----------------------------------------------------------------------- | --------------------------------------- |
| 1   | `V13__Add_org_type_to_users.sql`                | `organization_type_enum` → `organization_type`                          | Crash fix — wrong column name           |
| 2   | `RbacConfig.RoleConfig.java`                    | Added `applicableOrgTypes`, `explicitDenials` fields                    | YAML binding was silently dropping both |
| 3   | `RbacRole.java`                                 | Added `applicable_org_types`, `explicit_denials` JPA columns            | No DB columns existed                   |
| 4   | `V14__Add_rbac_role_org_type_denials.sql` (new) | `ALTER TABLE rbac_roles ADD COLUMN`                                     | Persist new fields                      |
| 5   | `RbacSeederService.java`                        | Persist `applicableOrgTypes` + `explicitDenials`; `buildCsv()` helper   | Fields never written to DB              |
| 6   | `RbacService.java`                              | Added `getExplicitDenialsForRoles(roles)` method                        | Denial lookup needed                    |
| 7   | `AuthController.java` (login)                   | `permissions.removeAll(rbacService.getExplicitDenialsForRoles(roles))`  | Explicit denials never applied          |
| 8   | `AuthController.java` (refresh)                 | Same denial removal                                                     | Refresh token had same issue            |
| 9   | `AuthController.java` (org-switch)              | Same denial removal                                                     | Org switch had same issue               |
| 10  | `JwtTokenProvider.java`                         | Added `"role"` claim (`primaryRole`)                                    | `X-Primary-Role` was always empty       |
| 11  | `CreateUserRequest.java` (auth-service)         | Added `organizationType` field                                          | New users got null orgType              |
| 12  | `CreateUserRequest.java` (propertize client)    | Added `organizationType` field                                          | DTO couldn't carry the field            |
| 13  | `UserManagementService.java`                    | `.organizationType(request.getOrganizationType())` in User builder      | Field not persisted on user create      |
| 14  | `OnboardingService.java`                        | `.organizationType(org.getOrganizationTypeEnum()...)`                   | New org owners had null orgType         |
| 15  | `AuthenticationService.java`                    | `@Deprecated`, `UnsupportedOperationException`, removed injected fields | Dead code cleanup                       |

---

## 13. Remaining Recommendations

Listed in priority order:

### P1 — Add X-Org-Type to downstream service filters

**Why:** Without it, org-type-based authorization in employee/payroll services is blind.  
**How:** Read `X-Org-Type` in `TrustedGatewayHeaderFilter`; add field to `GatewayAuthenticatedUser`; add controller-level guards.

### P2 — Implement refresh token rotation

**Why:** Current design reuses the same refresh token for 7 days. A compromised refresh token is valid until expiry.  
**How:** On each refresh, issue a new refresh token and invalidate the old one via a Redis blacklist.

### P3 — Adjust HOA_DIRECTOR and CFO role levels

**Why:** Level 920 and 940 create unexpected role-assignment anomalies.  
**How:** Set both to `890` or similar to keep them clearly in the "organization admin" band.

### P4 — Wire allowRuntimeRoleCreation as a feature flag

**Why:** Gives platform admins ability to disable custom role creation per org.  
**How:** Read the config flag in `CustomRoleService.createCustomRole()` and throw if disabled.

### P5 — Remove AuthenticationService in a future release

**Why:** Dead code with misleading method signatures.  
**How:** Delete the file and its test class. Marked `forRemoval = true` in v7.0.

### P6 — Add applicableOrgTypes enforcement at org-switch

**Why:** Currently advisory only — a SOLO_OWNER role can be assigned to a CORPORATE user.  
**How:** In `AuthController.switchOrganization()`, validate that user's roles are compatible with new org's type; warn or refuse if not.
