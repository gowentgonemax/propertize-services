# JWT Token — End-to-End Guide

> **Version:** 8.0 | **Last Updated:** 2026-04-01  
> Covers the complete lifecycle of authentication tokens in Propertize: issuance in auth-service, validation and header injection in api-gateway, and consumption in downstream services.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Token Types](#2-token-types)
3. [Access Token Structure (v8.0 — slim JWT)](#3-access-token-structure-v80--slim-jwt)
4. [Refresh Token Structure](#4-refresh-token-structure)
5. [Authentication Flow — Step by Step](#5-authentication-flow--step-by-step)
6. [API Gateway — JWT Validation and Permission Cache Lookup](#6-api-gateway--jwt-validation-and-permission-cache-lookup)
7. [Downstream Service — Header Consumption](#7-downstream-service--header-consumption)
8. [Token Refresh Flow (with Rotation)](#8-token-refresh-flow-with-rotation)
9. [Organization Switch Flow](#9-organization-switch-flow)
10. [Redis Permission Cache — Architecture](#10-redis-permission-cache--architecture)
11. [v8.0 Changes (All Pending Work Completed)](#11-v80-changes-all-pending-work-completed)
12. [Security Notes](#12-security-notes)

---

## 1. Overview

```
Frontend (Next.js)
  ↓  POST /auth/login  {username, password}
API Gateway (:8080)  → passes login request unchanged
  ↓
Auth Service (:8081)  → validates credentials, builds RBAC permissions, issues JWT
                       → stores permissions in Redis: perms:jti:{jti} TTL=900s
  ↓  returns {accessToken, refreshToken, ...}

On every protected API call:
API Gateway
  ↓  validates JWT signature (RS256)
  ↓  fetches permissions from Redis using jti claim (~1-2ms)
  ↓  injects 16 X-* headers including X-Permissions
  ↓
Downstream Service (:8082–8085, :8090–8093)
  → reads headers (NO JWT validation — trusts gateway)
  → applies business logic using user context from headers
```

**Key design decision (v8.0):** Permissions are **NOT embedded in the JWT payload**. They are stored in Redis (`perms:jti:{jti}`) and fetched by the Gateway on each request. This eliminates HTTP 431 errors caused by large JWT → large header values.

---

## 2. Token Types

| Token         | TTL    | Claims                                      | Purpose                 |
| ------------- | ------ | ------------------------------------------- | ----------------------- |
| Access Token  | 15 min | Slim user context (no permissions list)     | API authorization       |
| Refresh Token | 7 days | subject + tokenType only                    | Obtain new access token |
| Service Token | —      | serviceName + tokenType=service             | Inter-service calls     |

---

## 3. Access Token Structure (v8.0 — slim JWT)

Access tokens issued via `generateAccessTokenWithPermissions()`. As of v8.0, the `permissions` claim is **removed** from the JWT payload — permissions live in Redis.

### JWT Standard Claims

| Claim | Type   | Value                               |
| ----- | ------ | ----------------------------------- |
| `sub` | String | Username (same as `username` claim) |
| `iat` | Number | Issued-at timestamp                 |
| `exp` | Number | Expiry = iat + 15 minutes           |
| `jti` | String | UUID — unique token ID (used as Redis permissions key) |

### Custom Claims

| Claim              | Type           | Value                                                | Notes                                                        |
| ------------------ | -------------- | ---------------------------------------------------- | ------------------------------------------------------------ |
| `username`         | String         | User's login name                                    | Same as `sub`                                                |
| `role`             | String         | Primary role (first alphabetically from `roles` set) | Deterministic                                                |
| `roles`            | List\<String\> | All assigned role names                              | e.g. `["ORGANIZATION_OWNER"]`                                |
| `organizationId`   | String         | Organization DB ID or code                           | Numeric ID as string                                         |
| `organizationCode` | String         | Organization short code                              | e.g. `"ORG-001"`                                             |
| `orgType`          | String         | Organization type enum name                          | e.g. `"INDIVIDUAL_PROPERTY_OWNER"` — empty string if not set |
| `tokenType`        | String         | `"access"`                                           |                                                              |
| ~~`permissions`~~  | ~~List~~       | **Removed in v8.0 — stored in Redis**                | Eliminates HTTP 431 errors                                   |

### Example Decoded Payload (v8.0 slim token)

```json
{
  "sub": "john.doe",
  "iat": 1743800000,
  "exp": 1743800900,
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john.doe",
  "role": "ORGANIZATION_OWNER",
  "roles": ["ORGANIZATION_OWNER"],
  "organizationId": "42",
  "organizationCode": "ORG-001",
  "orgType": "INDIVIDUAL_PROPERTY_OWNER",
  "tokenType": "access"
}
```

Permissions are stored separately in Redis:
```
KEY:   perms:jti:550e8400-e29b-41d4-a716-446655440000
VALUE: PROPERTY_READ,PROPERTY_CREATE,LEASE_MANAGE,...
TTL:   900 seconds
```

### Signing

- **Algorithm:** RS256 (RSA + SHA-256)
- **Key pair:** `keys/private.pem` / `keys/public.pem` in auth-service

---

## 4. Refresh Token Structure

Refresh tokens contain **minimal claims** — no permissions, no org data.

| Claim       | Value                 |
| ----------- | --------------------- |
| `sub`       | Username              |
| `jti`       | UUID                  |
| `iat`       | Issue timestamp       |
| `exp`       | Expiry = iat + 7 days |
| `tokenType` | `"refresh"`           |

Refresh tokens are now **rotated on each use** (v8.0). See §8 for details.

---

## 5. Authentication Flow — Step by Step

### Login

```
POST /api/v1/auth/login
Body: { "username": "...", "password": "...", "organizationId": "..." }
```

1. **API Gateway** receives the request; auth endpoints match `permitAll()` → passes through
2. **Auth Service** (`AuthController.login()`):
   a–h. [Same as v7.0 — RBAC resolution, explicit denials applied]
   i. Calls `jwtTokenProvider.generateAccessTokenWithPermissions(...)` — **no permissions in JWT**
   j. Calls `jwtTokenProvider.generateRefreshToken(user)`
   k. **NEW:** Stores permissions in Redis: `permissionCacheService.cachePermissions(jti, permissions)` TTL=900s
   l. **NEW:** Stores refresh token in Redis for rotation tracking: `tokenBlacklistService.storeRefreshToken(...)`

3. Returns `AuthResponse`

---

## 6. API Gateway — JWT Validation and Permission Cache Lookup

For every protected request:

### Step 1: JWT Validation (unchanged)
Validates RS256 signature, checks expiry, checks blacklist by JTI.

### Step 2: Permission Cache Lookup (NEW in v8.0)
```
jti = jwt.getClaim("jti")
permissions = redis.GET("perms:jti:" + jti)
// cache miss → empty set (downstream gets empty X-Permissions)
```

### Step 3: Header Injection (unchanged headers)

| Header                | Source             | Always Present | Description                         |
| --------------------- | ------------------ | :------------: | ----------------------------------- |
| `X-User-Id`           | JWT `sub`          |       ✅        | Username string                     |
| `X-Username`          | JWT `username`     |       ✅        | Username                            |
| `X-Organization-Id`   | JWT `organizationId` |     ✅        | Org DB ID                           |
| `X-Organization-Code` | JWT `organizationCode` |   ✅        | Org short code                      |
| `X-Org-Type`          | JWT `orgType`      |       ✅        | Org type enum                       |
| `X-Roles`             | JWT `roles`        |       ✅        | Comma-separated role names          |
| `X-Primary-Role`      | JWT `role`         |       ✅        | Primary role                        |
| `X-Permissions`       | **Redis cache**    |  ✅ (if present) | Comma-separated permissions **from Redis** |
| `X-Gateway-Source`    | (constant)         |       ✅        | `"propertize-api-gateway"`          |
| `X-Correlation-Id`    | (generated)        |       ✅        | UUID per request                    |
| `X-Token-Type`        | JWT `tokenType`    |       ✅        | `"access"`                          |

**Key difference v8.0:** `X-Permissions` is now populated from Redis, not JWT. Downstream services receive the same header as before — no changes required in downstream services.

---

## 7. Downstream Service — Header Consumption

Unchanged for payroll-service. Employee-service `GatewayAuthenticatedUser` now includes:
- `Set<String> getPermissions()` — from `X-Permissions` header
- `boolean hasPermission(String)` — convenience method

Both employee-service and payroll-service read `X-Org-Type` via `GatewayAuthenticatedUser.getOrgType()`.

---

## 8. Token Refresh Flow (with Rotation)

```
POST /api/v1/auth/refresh
Body: { "refreshToken": "<7-day refresh token>" }
```

1. **NEW:** Check Redis `token:used:{hash}` — reject with 401 if token already used (replay protection)
2. Validate token signature and expiry
3. Load user, re-resolve RBAC permissions + explicit denials
4. Generate **new** access token (slim, no permissions)
5. Generate **new** refresh token (rotation)
6. **NEW:** `revokeRefreshToken(oldToken)` — marks old token as used in Redis
7. **NEW:** `storeRefreshToken(newToken, ...)` — registers new token in Redis
8. **NEW:** `cachePermissions(newJti, permissions)` — caches permissions for new access token

**Token Rotation:** Every refresh call issues a new refresh token. The old refresh token is immediately invalidated. Any replay attempt returns 401.

---

## 9. Organization Switch Flow

```
POST /api/v1/auth/switch-organization
Body: { "refreshToken": "...", "organizationId": "..." }
```

Same as v7.0, plus:
- **NEW (P6):** Logs a warning if any user role's `applicableOrgTypes` does not include the target org's type (non-blocking — future versions may enforce hard rejection)
- **NEW:** Caches permissions for the new access token in Redis

---

## 10. Redis Permission Cache — Architecture

```
auth-service (PermissionCacheService)
  cachePermissions(jti, Set<String>)  → redis SET perms:jti:{jti} "PERM1,PERM2,..." EX 900
  getPermissions(jti)                 → redis GET perms:jti:{jti}  (for internal use)
  evictPermissions(jti)               → redis DEL perms:jti:{jti}  (on logout)

api-gateway (PermissionCacheService)
  getPermissions(jti)                 → redis GET perms:jti:{jti}  → X-Permissions header
  evictPermissions(jti)               → redis DEL perms:jti:{jti}  (if gateway revokes)
```

**Cache miss handling:** If Redis is unavailable or the cache entry expired, `X-Permissions` will be empty. Downstream services should treat this as no permissions. The user must re-authenticate to get a fresh token and new cache entry.

**TTL alignment:** Cache TTL = 900s = access token lifetime. When the JWT expires, the cache entry also expires.

**JWT size reduction:**
- Before v8.0: JWT ~2-4 KB (50+ permissions × ~20 chars = ~1KB in payload alone)
- After v8.0: JWT ~400-600 bytes (no permissions list)
- **~80% size reduction** — eliminates HTTP 431 "Request Header Fields Too Large" errors

---

## 11. v8.0 Changes (All Pending Work Completed)

### JWT Size Reduction (primary goal)
- **Permissions removed from JWT** — stored in Redis `perms:jti:{jti}` with 900s TTL
- **Gateway fetches permissions from Redis** on every authenticated request
- **~80% JWT size reduction** — resolves HTTP 431 errors

### Refresh Token Rotation (P2 from RBAC report)
- Refresh tokens are now rotated on every use
- Old refresh token is immediately revoked in Redis (`token:used:{hash}`)
- New refresh token is stored in Redis for tracking
- Replay of a used refresh token returns 401

### applicableOrgTypes Enforcement (P6)
- `AuthController.switchOrganization()` now logs warnings when user roles are incompatible with target org type
- Non-blocking in v8.0; will be enforced as hard rejection in v9.0

### allowRuntimeRoleCreation Feature Flag (P4)
- `CustomRoleService.createCustomRole()` now reads `rbacConfig.getCore().getAllowRuntimeRoleCreation()`
- Returns `IllegalStateException` (HTTP 500) if flag is `false`

### HOA_DIRECTOR and CFO Level Fix (P3)
- `HOA_DIRECTOR`: level **920 → 890** (below PORTFOLIO_OWNER, avoids assignment anomaly)
- `CFO`: level **940 → 890** (same band as HOA_DIRECTOR)

### Dead Code Removal (P5)
- `AuthenticationService.java` **deleted** (was `@Deprecated(forRemoval=true)` since v7.0)

### Employee-Service GatewayAuthenticatedUser (P1 follow-up)
- Added `permissions` field + `getPermissions()` + `hasPermission()` methods
- Both services (employee + payroll) already had `orgType` support — verified ✅

### Logout Enhancement
- Logout now proactively evicts the permission cache entry (`perms:jti:{jti}`)
- Logout now revokes the refresh token if provided in the request body

---

## 12. Security Notes

### JWT Size (Resolved)
HTTP 431 errors are resolved. JWT is now ~400-600 bytes regardless of role permission count.

### Refresh Token Rotation (Implemented)
Implemented in v8.0. Each refresh call issues a new refresh token and invalidates the old one. A Redis blacklist (`token:used:{hash}`) prevents replay attacks.

### X-User-Id Header Naming (Known Inconsistency)
`X-User-Id` contains the **username string**, not a numeric ID. Do not change the header name without updating all downstream consumers simultaneously.

### Cache Miss Security Posture
If Redis is unavailable, `X-Permissions` is empty. Downstream services should fail-closed (deny) when permissions are absent for protected operations. This is the current behavior for permission-based `@PreAuthorize` checks.

### Service-to-Service Requests
Internal services use service tokens. Gateway injects `X-Token-Type: service`. Permission cache is not populated for service tokens — they use scope-based authorization instead.

> Covers the complete lifecycle of authentication tokens in Propertize: issuance in auth-service, validation and header injection in api-gateway, and consumption in downstream services.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Token Types](#2-token-types)
3. [Access Token Structure (production path)](#3-access-token-structure-production-path)
4. [Refresh Token Structure](#4-refresh-token-structure)
5. [Authentication Flow — Step by Step](#5-authentication-flow--step-by-step)
6. [API Gateway — JWT Validation and Header Injection](#6-api-gateway--jwt-validation-and-header-injection)
7. [Downstream Service — Header Consumption](#7-downstream-service--header-consumption)
8. [Token Refresh Flow](#8-token-refresh-flow)
9. [Organization Switch Flow](#9-organization-switch-flow)
10. [Known Issues Found and Fixed (v7.0)](#10-known-issues-found-and-fixed-v70)
11. [Dead Code: AuthenticationService](#11-dead-code-authenticationservice)
12. [Security Notes](#12-security-notes)

---

## 1. Overview

```
Frontend (Next.js)
  ↓  POST /auth/login  {username, password}
API Gateway (:8080)  → passes login request unchanged
  ↓
Auth Service (:8081)  → validates credentials, builds RBAC permissions, issues JWT
  ↓  returns {accessToken, refreshToken, ...}
API Gateway
  ↓ (all subsequent requests)
  → validates JWT signature with RSA public key
  → extracts claims
  → injects 16 X-* headers
  ↓
Downstream Service (:8082–8085, :8090–8093)
  → reads headers (NO JWT validation — trusts gateway)
  → applies business logic using user context from headers
```

**Key design decision:** Downstream services never validate JWT signatures. They trust the API Gateway implicitly via the `X-Gateway-Source` header. JWT validation happens only once, at the gateway boundary.

---

## 2. Token Types

| Token         | TTL    | Claims                          | Purpose                 |
| ------------- | ------ | ------------------------------- | ----------------------- |
| Access Token  | 15 min | Full user context + permissions | API authorization       |
| Refresh Token | 7 days | subject + tokenType only        | Obtain new access token |
| Service Token | —      | serviceName + tokenType=service | Inter-service calls     |

---

## 3. Access Token Structure (production path)

Access tokens issued via `generateAccessTokenWithPermissions()` — the only path used in production (see §11 for the dead code path).

### JWT Standard Claims

| Claim | Type   | Value                               |
| ----- | ------ | ----------------------------------- |
| `sub` | String | Username (same as `username` claim) |
| `iat` | Number | Issued-at timestamp                 |
| `exp` | Number | Expiry = iat + 15 minutes           |
| `jti` | String | UUID — unique token ID              |

### Custom Claims

| Claim              | Type           | Value                                                | Notes                                                        |
| ------------------ | -------------- | ---------------------------------------------------- | ------------------------------------------------------------ |
| `username`         | String         | User's login name                                    | Same as `sub`                                                |
| `role`             | String         | Primary role (first alphabetically from `roles` set) | Added v7.0 — was missing previously                          |
| `roles`            | List\<String\> | All assigned role names                              | e.g. `["ORGANIZATION_OWNER"]`                                |
| `organizationId`   | String         | Organization DB ID or code                           | Numeric ID as string                                         |
| `organizationCode` | String         | Organization short code                              | e.g. `"ORG-001"`                                             |
| `orgType`          | String         | Organization type enum name                          | e.g. `"INDIVIDUAL_PROPERTY_OWNER"` — empty string if not set |
| `permissions`      | List\<String\> | Flat resolved permission list                        | RBAC hierarchy expanded; explicit denials removed            |
| `tokenType`        | String         | `"access"`                                           |                                                              |

### Example Decoded Payload

```json
{
  "sub": "john.doe",
  "iat": 1743800000,
  "exp": 1743800900,
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john.doe",
  "role": "ORGANIZATION_OWNER",
  "roles": ["ORGANIZATION_OWNER"],
  "organizationId": "42",
  "organizationCode": "ORG-001",
  "orgType": "INDIVIDUAL_PROPERTY_OWNER",
  "permissions": ["PROPERTY_READ", "PROPERTY_CREATE", "LEASE_MANAGE", ...],
  "tokenType": "access"
}
```

### Signing

- **Algorithm:** RS256 (RSA + SHA-256)
- **Key pair:** `keys/private.pem` / `keys/public.pem` in auth-service
- **Key provider:** `RsaKeyProvider` reads keys on startup

---

## 4. Refresh Token Structure

Refresh tokens contain **minimal claims** — no permissions, no org data.

| Claim       | Value                 |
| ----------- | --------------------- |
| `sub`       | Username              |
| `jti`       | UUID                  |
| `iat`       | Issue timestamp       |
| `exp`       | Expiry = iat + 7 days |
| `tokenType` | `"refresh"`           |

On refresh (`POST /api/v1/auth/refresh`), the auth-service:

1. Validates the refresh token signature
2. Looks up the user and re-resolves RBAC permissions from scratch
3. Applies explicit denials
4. Issues a new access token (`generateAccessTokenWithPermissions`)
5. Does **not** re-issue a new refresh token (existing refresh token remains valid until its own expiry)

---

## 5. Authentication Flow — Step by Step

### Login

```
POST /api/v1/auth/login
Body: { "username": "...", "password": "...", "organizationId": "..." }
```

1. **API Gateway** receives the request; auth endpoints match `permitAll()` → passes through without JWT check
2. **Auth Service** (`AuthController.login()`):
   a. Loads `User` entity with roles from DB
   b. Validates password with bcrypt
   c. Loads all system roles from `rbac.yml` via `RbacSeederService`
   d. Resolves user's role names → calls `RbacService.getPermissionsForRole()` for each
   e. Expands permission hierarchy groups (e.g. `PROPERTY_MANAGE` → 5 atomic permissions)
   f. Loads custom role assignments from `UserCustomRoleAssignment` table
   g. Merges custom role permissions
   h. **Removes explicit denials** — `rbacService.getExplicitDenialsForRoles(roles)` subtracted
   i. Calls `jwtTokenProvider.generateAccessTokenWithPermissions(...)` with final permission set
   j. Calls `jwtTokenProvider.generateRefreshToken(user)` for refresh token
   k. Updates `user.lastLogin`
   l. Returns `AuthResponse` with both tokens

3. **API Gateway** forwards the `AuthResponse` back unchanged

4. **Frontend** stores tokens:
   - Access token → NextAuth session / memory
   - Refresh token → HTTP-only cookie (recommended) or secure storage

---

## 6. API Gateway — JWT Validation and Header Injection

For every request to a protected endpoint (`/**` except `/auth/**`, `/actuator/**`, public service-to-service paths):

### Step 1: Token Extraction

Gateway reads `Authorization: Bearer <token>` header.

### Step 2: JWT Signature Validation

Gateway validates token with RSA public key via `JwtAuthenticationFilter`. On failure → `401 Unauthorized`.

### Step 3: Expiry Check

Token expiry (`exp` claim) is verified. Expired → `401 Unauthorized` with body: `{"error": "token_expired"}`. Frontend should intercept and auto-refresh.

### Step 4: Header Injection

Gateway strips the original `Authorization` header and injects the following request headers for downstream services:

| Header                | Source Claim       |   Always Present   | Description                                              |
| --------------------- | ------------------ | :----------------: | -------------------------------------------------------- |
| `X-User-Id`           | `sub` / `username` |         ✅         | Username string (note: named "Id" but value is username) |
| `X-Username`          | `username`         |         ✅         | Username (same value as X-User-Id)                       |
| `X-Organization-Id`   | `organizationId`   |         ✅         | Org DB ID or code                                        |
| `X-Organization-Code` | `organizationCode` |         ✅         | Org short code                                           |
| `X-Org-Type`          | `orgType`          |         ✅         | Org type enum, empty string if not set                   |
| `X-Roles`             | `roles`            |         ✅         | Comma-separated role names                               |
| `X-Primary-Role`      | `role`             |         ✅         | Primary role (first alphabetically)                      |
| `X-Permissions`       | `permissions`      |  ✅ (if present)   | Comma-separated permission list                          |
| `X-Email`             | `email`            |  ⬜ (if present)   | User email                                               |
| `X-Tenant-Id`         | `tenantId`         |  ⬜ (if present)   | Tenant context                                           |
| `X-Session-Id`        | `sessionId`        |  ⬜ (if present)   | Session tracking                                         |
| `X-Token-Jti`         | `jti`              |  ⬜ (if present)   | Token unique ID                                          |
| `X-Token-Type`        | `tokenType`        |         ✅         | `"access"` or `"service"`                                |
| `X-Gateway-Source`    | (constant)         |         ✅         | `"propertize-api-gateway"`                               |
| `X-Correlation-Id`    | (generated)        |         ✅         | UUID per request — for distributed tracing               |
| `X-Gateway-Signature` | HMAC of payload    | ⬜ (if configured) | Request integrity check                                  |

**Note on `X-User-Id`:** The value is the **username string**, not a numeric database ID. The naming is misleading — all downstream services should use `X-Username` for consistency. Both headers carry the same value.

---

## 7. Downstream Service — Header Consumption

Downstream services (employee-service, payroll-service, etc.) have a `TrustedGatewayHeaderFilter` that:

1. Checks `X-Gateway-Source == "propertize-api-gateway"` — rejects requests without it
2. Reads all X-\* headers and constructs a `GatewayAuthenticatedUser` object
3. Sets a `UsernamePasswordAuthenticationToken` into the `SecurityContext`

Services then access the user context via:

```java
GatewayAuthenticatedUser user = (GatewayAuthenticatedUser) SecurityContextHolder
    .getContext().getAuthentication().getPrincipal();
user.getUsername();       // from X-Username
user.getOrganizationId(); // from X-Organization-Id
user.getRoles();          // from X-Roles (split by comma)
user.getPermissions();    // from X-Permissions (split by comma)
// user.getOrgType() is NOT yet populated in employee-service filter (see Known Issues)
```

Authorization in downstream services is done by checking `user.getPermissions()` or `user.getRoles()` — they do NOT validate the JWT signature.

---

## 8. Token Refresh Flow

```
POST /api/v1/auth/refresh
Body: { "refreshToken": "<7-day refresh token>" }
```

1. Gateway: refresh endpoint is public → passes through
2. Auth Service (`AuthController.refresh()`):
   a. Validates refresh token signature and expiry
   b. Extracts username from `sub` claim
   c. Loads user from DB; re-resolves full RBAC permissions (same as login)
   d. Applies explicit denials
   e. Generates new access token (15 min)
   f. Returns new `AuthResponse` with **same** refresh token (no rotation)

> **Token rotation not implemented.** The same refresh token is used until it expires (7 days). Consider implementing refresh token rotation for enhanced security.

---

## 9. Organization Switch Flow

```
POST /api/v1/auth/switch-organization
Headers: Authorization: Bearer <access-token>
Body: { "organizationId": "..." }
```

Used when a PORTFOLIO_OWNER or platform admin needs to operate in a different organization context.

1. Gateway: validates existing access token → injects X-\* headers
2. Auth Service (`AuthController.switchOrganization()`):
   a. Reads `username` from `X-User-Id` header
   b. Loads user from DB; validates they have access to the target org
   c. Re-resolves RBAC permissions for the user in the new org context
   d. Applies explicit denials
   e. Issues new access token with new `organizationId`/`organizationCode`/`orgType`
   f. Returns new `AuthResponse`

3. Frontend swaps the access token; subsequent requests operate in the new org context

---

## 10. Known Issues Found and Fixed (v7.0)

The following bugs were discovered via deep investigation and **fixed in this version**:

### Fixed: `X-Primary-Role` was always empty

- **Root cause:** `generateAccessTokenWithPermissions()` never included a `"role"` claim. The gateway's `getPrimaryRole()` method specifically reads the `"role"` claim → always returned empty string → `X-Primary-Role: ""`
- **Fix:** Added `"role"` claim to `generateAccessTokenWithPermissions` — value = first alphabetically sorted role from the `roles` set (deterministic)
- **File:** `auth-service/.../security/JwtTokenProvider.java`

### Fixed: Explicit denials never enforced

- **Root cause:** `rbac.yml` `explicitDenials` lists per role were parsed but never subtracted from the permission set at token generation time. Roles like `SOLO_OWNER` with `EMPLOYEE_MANAGE` explicitly denied could still receive those permissions.
- **Fix:** Added `RbacService.getExplicitDenialsForRoles()` method; applied `permissions.removeAll(denials)` at all 3 token generation sites (login, refresh, org-switch) in `AuthController`
- **Files:** `RbacService.java`, `AuthController.java`

### Fixed: `applicableOrgTypes` silently ignored

- **Root cause:** `RbacConfig.RoleConfig` had no `applicableOrgTypes` Java field → YAML was parsed to null → never written to DB. `RbacSeederService` also never persisted the field.
- **Fix:** Added `List<String> applicableOrgTypes` to `RoleConfig`; added V14 migration for new `rbac_roles` columns; updated seeder to persist both `applicableOrgTypes` and `explicitDenials`
- **Files:** `RbacConfig.java`, `RbacRole.java`, `RbacSeederService.java`, `V14__Add_rbac_role_org_type_denials.sql`

### Fixed: New org owners had null `orgType` in JWT

- **Root cause:** `OnboardingService.createOwnerUser()` never passed `organizationType` to the `CreateUserRequest` DTO. New organization owners would get an empty `orgType` claim in their JWT, meaning org-type-specific features and explicit denials would not apply correctly.
- **Fix:** Updated `CreateUserRequest` DTOs in both auth-service and propertize client; updated `UserManagementService.createUser()` and `OnboardingService.createOwnerUser()` to pass `organizationType`
- **Files:** Both `CreateUserRequest.java`, `UserManagementService.java`, `OnboardingService.java`

### Fixed: V13 Flyway migration crash

- **Root cause:** V13 used `o.organization_type_enum` but the actual column in the `organizations` table is `o.organization_type`. This caused a `PSQLException` on startup, putting auth-service in a crash loop and blocking all other services.
- **Fix:** Corrected column reference in V13 SQL
- **File:** `V13__Add_org_type_to_users.sql`

---

## 11. Dead Code: AuthenticationService

`auth-service/.../service/AuthenticationService.java` contains a `login()` method that is **never called** in production. It was a legacy implementation that:

- Used `generateAccessToken(User)` instead of `generateAccessTokenWithPermissions()`
- Produced a token with **no `permissions` claim** (empty RBAC)
- Had **no explicit denial enforcement**
- Had **no custom role resolution**

As of v7.0, this class is annotated `@Deprecated(since = "7.0", forRemoval = true)` and its methods throw `UnsupportedOperationException`. The real login path is exclusively in `AuthController.login()`.

The `generateAccessToken(User)` method itself also remains in `JwtTokenProvider` — it also omits the `"role"` claim and `"permissions"` claim. It should not be called except for backward-compatibility testing.

---

## 12. Security Notes

### Token Size

JWT access tokens include the full `permissions` list. For users with many roles (e.g. PORTFOLIO_OWNER has 50+ permissions), this can result in tokens larger than 4 KB. Monitor header sizes in nginx/load-balancer configuration (default nginx header limit is 8 KB).

### Refresh Token Rotation (Not Implemented)

Current implementation issues the same refresh token for the full 7-day lifetime. Any compromise of a refresh token allows token re-issuance until expiry. Recommend implementing refresh token rotation (issue new refresh token on each use) and maintaining a token blacklist in Redis.

### X-User-Id Header Naming

`X-User-Id` contains the **username string**, not a numeric ID. This is a naming inconsistency — existing downstream code reads it as such. Do not change the header name without updating all downstream consumers simultaneously.

### Service-to-Service Requests

Internal services making calls through the gateway should use service tokens (not user tokens). The gateway injects `X-Token-Type: service` and `X-Service-Name` for these paths. Services should check `X-Token-Type` to distinguish user-initiated vs. service-initiated requests.

### HMAC Signature (Optional)

If `X-Gateway-Signature` is configured (via `gateway.hmac.secret` env var), downstream services can verify request integrity to prevent header injection attacks from within the cluster network.
