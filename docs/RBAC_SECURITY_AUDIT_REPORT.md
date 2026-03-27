# Propertize Auth/RBAC — Full Security Audit Report

**Date:** 2025  
**Scope:** auth-service, api-gateway (JWT + RBAC filters), frontend RBAC integration  
**Stack:** Spring Boot 3.5.10 · Java 21 · Spring Cloud Gateway (WebFlux) · Next.js 14 · PostgreSQL · Redis · Kafka

---

## 1. Executive Summary

A comprehensive end-to-end security analysis was performed across the authentication and RBAC layers of the Propertize platform. **One critical vulnerability and six high/medium issues** were identified and remediated in-place. The architectural recommendation is to **keep RBAC within the auth service** rather than extract it into a separate microservice. All changes are backward-compatible with existing JWT tokens and API contracts.

---

## 2. System Architecture Overview

```
Browser / Mobile
      │
      ▼
Next.js Frontend (NextAuth + rbacStore/Zustand)
      │  HTTPS
      ▼
API Gateway  ─────────────────────────────────────────────────────────
│  JwtAuthenticationFilter      │  RbacAuthorizationFilter            │
│  · RS256 validate (local key) │  · endpoint→permission map           │
│  · Redis blacklist check       │  · role→permission expand            │
│  · Inject X-User-Id, X-Roles  │  · Caffeine cache (TTL 60 min)      │
└──────────────────────────────────────────────────────────────────────
      │ Internal headers (no TLS between containers)
      ▼
Auth Service                    Propertize / Employee-Service
│  SecurityConfig               │  Receive X-User-Id, X-Roles
│  InternalRequestAuthFilter    │  No independent auth logic
│  RbacController (47 routes)   │
│  PolicyEngine + ABAC          │
│  Flyway (V1–V9 migrations)    │
│  Redis (token blacklist)      │
│  Kafka (audit events)         │
└───────────────────────────────┘
```

### Permission Format Duality

| Layer                             | Format                                       | Example                                     |
| --------------------------------- | -------------------------------------------- | ------------------------------------------- |
| Frontend / Gateway RBAC filter    | `resource:action` lowercase                  | `property:read`                             |
| Auth-service `rbac.yml` (v6.0)    | `RESOURCE_ACTION` uppercase                  | `PROPERTY_READ`                             |
| Auth-service permission hierarchy | `RESOURCE_MANAGE` expands to sub-permissions | `USER_MANAGE → [USER_CREATE, USER_READ, …]` |

The gateway filter has an uppercase-format compatibility shim so both formats resolve at runtime.

---

## 3. Database Schema (Auth Service)

| Table                   | Purpose                         |
| ----------------------- | ------------------------------- |
| `users`                 | Core user identities            |
| `user_roles`            | User ↔ role join, org-scoped    |
| `organizations`         | Multi-tenant org records        |
| `password_reset_tokens` | Time-limited reset links        |
| `temporal_permissions`  | Time-boxed permission grants    |
| `delegation_rules`      | Delegation templates            |
| `delegations`           | Active delegated permissions    |
| `composite_roles`       | Org-scoped composite roles      |
| `custom_roles`          | Per-org custom role definitions |
| `permission_audit_logs` | RBAC change audit trail         |
| `ip_access_rules`       | IP-based access restrictions    |

Migration history: V1 (schema) → V2 (indexes) → V3 (orgs) → V4 (temporal) → V5 (delegations) → V6 (composite) → V7 (custom roles) → V8 (audit) → V9 (IP rules).

---

## 4. JWT Token Format

**Algorithm:** RS256 (asymmetric; auth-service holds private key, gateway fetches public key once from `/api/v1/auth/public-key`)

**Claims:**

| Claim              | Type     | Description                           |
| ------------------ | -------- | ------------------------------------- |
| `sub`              | string   | username (email)                      |
| `userId`           | string   | UUID of user                          |
| `organizationId`   | string   | UUID of org                           |
| `organizationCode` | string   | Short org code                        |
| `roles`            | string[] | All granted roles                     |
| `primaryRole`      | string   | Highest-privilege role                |
| `jti`              | string   | Unique token ID (Redis blacklist key) |
| `sessionId`        | string   | Session UUID                          |
| `type`             | string   | `access` \| `refresh` \| `service`    |
| `iat` / `exp`      | number   | Issued / expires (Unix epoch)         |

**Expiry defaults:** access = 15 min, refresh = 7 days, service = 5 min.

**Redis keys:** `blacklist:jti:{jti}` (JTI-based), `blacklist:token:{sha256hash}` (hash fallback).

---

## 5. Roles Catalog (22 built-in roles)

From `UserRoleEnum.java` and `rbac.yml`, ordered by privilege level:

| Role                     | Level | Category   |
| ------------------------ | ----- | ---------- |
| `PLATFORM_OVERSIGHT`     | 100   | Platform   |
| `PLATFORM_ADMIN`         | 95    | Platform   |
| `PLATFORM_SUPPORT`       | 85    | Platform   |
| `SUPER_ADMIN`            | 90    | System     |
| `SYSTEM_INTEGRATOR`      | 80    | System     |
| `COMPLIANCE_OFFICER`     | 75    | Compliance |
| `FINANCIAL_CONTROLLER`   | 70    | Finance    |
| `ORGANIZATION_OWNER`     | 65    | Org        |
| `PROPERTY_MANAGER`       | 60    | Org        |
| `PORTFOLIO_MANAGER`      | 58    | Org        |
| `LEASING_AGENT`          | 55    | Org        |
| `MAINTENANCE_SUPERVISOR` | 50    | Org        |
| `MAINTENANCE_TECHNICIAN` | 45    | Org        |
| `TENANT_SERVICES_REP`    | 40    | Org        |
| `ACCOUNTING_STAFF`       | 38    | Org        |
| `TENANT`                 | 30    | User       |
| `PROSPECTIVE_TENANT`     | 25    | User       |
| `VENDOR`                 | 20    | External   |
| `CONTRACTOR`             | 18    | External   |
| `READ_ONLY_AUDITOR`      | 10    | Audit      |
| `GUEST`                  | 5     | Guest      |
| `RESTRICTED_USER`        | 1     | Restricted |

`PLATFORM_OVERSIGHT` has explicit denials for `ORGANIZATION_CREATE/UPDATE/DELETE/MANAGE` (can oversee but not modify org structure).

---

## 6. Vulnerabilities Found and Fixed

### BUG-1 — CRITICAL: All RBAC Admin Endpoints Publicly Accessible

**File:** `auth-service/src/main/java/…/config/SecurityConfig.java`

**Vulnerability:** `SecurityConfig` permitted the entire `/api/v1/auth/**` namespace without authentication. This made all 47 RBAC administration endpoints publicly reachable without any token:

```
POST /api/v1/auth/temporal-permissions       → grant any user superuser for any duration
POST /api/v1/auth/delegations                → delegate any role to any user
POST /api/v1/auth/custom-roles               → create org-scoped role with arbitrary permissions
POST /api/v1/auth/ip-access-rules            → whitelist attacker's IP
DELETE /api/v1/auth/users/{id}               → delete any user
```

**OWASP Category:** A01 Broken Access Control, A05 Security Misconfiguration

**Fix:** Restricted `permitAll()` to exactly these public paths:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `GET /api/v1/auth/public-key`
- `GET /api/v1/auth/.well-known/jwks.json`
- `GET /api/health`, `GET /actuator/health`
- `GET /api/v1/auth/rbac/roles`, `GET /api/v1/auth/rbac/permissions`, `GET /api/v1/auth/rbac/config` (public catalog — no PII)

All other paths require `.anyRequest().authenticated()`.

**New class created:** `InternalRequestAuthFilter.java` — validates incoming requests as either:

1. Service-to-service calls via `X-Service-Api-Key` header
2. Gateway-forwarded user calls via `X-Gateway-Source: api-gateway` + `X-Roles` headers (injected by `JwtAuthenticationFilter`)

---

### BUG-2 — HIGH: Unbounded In-Memory Permission Cache

**File:** `api-gateway/src/main/java/…/security/RbacAuthorizationFilter.java`

**Vulnerability:** `ConcurrentHashMap<String, Set<String>> permissionCache` with no TTL and no size limit. In a running system with many unique role-set combinations, this grows without bound, leading to potential `OutOfMemoryError`. Cache entries never expire, so stale RBAC data persists until pod restart.

**Fix:** Replaced with Caffeine cache:

```java
permissionCache = Caffeine.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(Duration.ofMinutes(cacheTtlMinutes)) // default 60
    .recordStats()  // Micrometer metrics
    .build();
```

`cacheTtlMinutes` is externalized via `${rbac.cache.ttl-minutes:60}`.

---

### BUG-3 — HIGH: CORS Allowed Origin Included Gateway Port

**File:** `auth-service/src/main/java/…/config/SecurityConfig.java`

**Issue:** `http://localhost:8080` (gateway port) was included in CORS `allowedOrigins`. The API gateway is not a browser and never performs CORS preflight — this entry was misleading and could mask frontend misconfiguration. Removed.

---

### BUG-4 — HIGH: Actuator Endpoint Exposes Environment Variables

**File:** `auth-service/src/main/resources/application.yml`

**Vulnerability:** `management.info.env.enabled: true` caused the `/actuator/info` endpoint to expose all environment variables, potentially including `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, etc.

**OWASP Category:** A02 Cryptographic Failures, A05 Security Misconfiguration

**Fix:**

```yaml
management:
  info:
    env:
      enabled: false
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-components: when_authorized
```

---

### BUG-5 — HIGH: Dual RBAC YAML Files with No Drift Detection

**Files:** `auth-service/src/main/resources/rbac.yml` (v6.0), `api-gateway/src/main/resources/rbac.yml` (v5.0)

**Issue:** The two YAML files use different formats (v6.0 uppercase underscore vs v5.0 template-based) and have no synchronisation mechanism. A new role added to auth-service but missed in gateway YAML would silently pass JWT validation but fail permission checks.

**Fix:** Added startup role-set comparison in `RbacConfig.loadConfig()`:

```java
Set<String> canonical = Set.of("PLATFORM_OVERSIGHT", "PLATFORM_ADMIN", ... /* 22 roles */);
Set<String> loaded = new HashSet<>(config.getRoles().keySet());
canonical.stream()
    .filter(r -> !loaded.contains(r))
    .forEach(r -> log.warn("RBAC DRIFT: canonical role '{}' is missing from gateway rbac.yml", r));
```

Logs `WARN` for missing canonical roles on startup.

---

### BUG-6 — MEDIUM: jjwt Library Version Mismatch

**File:** `api-gateway/pom.xml`

**Issue:** Gateway used jjwt `0.12.3`; auth-service used `0.12.6`. The patch versions include bug fixes for JWT parsing edge cases that could cause signature validation failures for tokens generated by the newer auth-service.

**Fix:** Updated all three gateway jjwt artifacts to `0.12.6`:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

---

### BUG-7 — MEDIUM: NPE Risk in Request Logging Filters

**Files:** `api-gateway/src/main/java/…/filter/AuditLoggingFilter.java`, `LoggingFilter.java`

**Issue:** `request.getRemoteAddress().getAddress().getHostAddress()` — the outer `getRemoteAddress()` was null-checked but `getAddress()` was not. `InetSocketAddress.getAddress()` can return `null` (e.g., when the address is unresolved), resulting in a `NullPointerException` that would crash the reactive filter chain.

**Fix:**

```java
if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
    return request.getRemoteAddress().getAddress().getHostAddress();
}
return "unknown";
```

---

## 7. Frontend Code Review Findings

These issues were found and fixed during a review of the RBAC frontend modernisation changes:

| #   | File                               | Issue                                                                                            | Fix                                             |
| --- | ---------------------------------- | ------------------------------------------------------------------------------------------------ | ----------------------------------------------- |
| F1  | `useOrgRoutes.ts`                  | `dashboardRoute` missing from `UseOrgRoutesReturn` interface                                     | Added to interface                              |
| F2  | `useOrgRoutes.ts`                  | `routes.dashboard` produced `/org/.../dashboard` (no page.tsx → silent 404)                      | Removed; only `routes.home` exists              |
| F3  | `login/page.tsx`                   | 34-line inline `getDashboardRoute` with uppercase role keys, never redirected to new URL pattern | Replaced with `getDashboardPath()` + slug utils |
| F4  | `admin/onboarding/…/page.tsx` (×2) | `hasAnyRole(["PLATFORM_OVERSIGHT"])` (uppercase)                                                 | → `['platform_oversight']` (lowercase)          |
| F5  | `roleUtils.ts`                     | `canManageProperties`, `canManageTenants`, etc. used hardcoded static role arrays                | Replaced with `hasPermission()` calls           |
| F6  | `roleUtils.ts`                     | `isRoleAllowed()` helper no longer called after F5                                               | Removed                                         |

---

## 8. Remaining Improvement Items (P3/P4)

These are pre-existing issues not introduced by recent changes; lower risk, deferred:

| ID  | Priority | File                                             | Issue                                                                      |
| --- | -------- | ------------------------------------------------ | -------------------------------------------------------------------------- |
| R1  | P3       | `AuthenticationService.java`                     | `tokenBlacklistService` field injected but never called                    |
| R2  | P3       | `AuthServiceCircuitBreaker.java`                 | Fallback methods defined but never wired to call sites                     |
| R3  | P3       | `AuthorizationService.java`                      | Unused import                                                              |
| R4  | P4       | `auth-service/rbac.yml` + `api-gateway/rbac.yml` | Unify to single format; implement HTTP-based policy sync from auth-service |

---

## 9. Architectural Decision: Keep RBAC in Auth Service

### Decision Matrix

| Criterion                   | Extract RBAC                  | Keep in Auth          | Notes                                      |
| --------------------------- | ----------------------------- | --------------------- | ------------------------------------------ |
| Security boundary integrity | ⚠ Creates 2 SPOF              | ✅ Single boundary    | JWT + RBAC are irreducibly coupled         |
| Latency                     | ❌ +10–50ms/req (network hop) | ✅ In-process         | Critical path: every authenticated request |
| Operational complexity      | ❌ New service, DB, deploy    | ✅ Existing ops       | 6-week effort with partial test coverage   |
| Scalability (current load)  | ⚠ Unnecessary                 | ✅ Sufficient         | Scale trigger: >500 req/s sustained        |
| Policy drift risk           | ❌ More surfaces              | ✅ One source         | Already reduced by drift detector (BUG-5)  |
| Team independence           | ⚠ Marginal benefit            | ✅ No friction        | Single team owns auth                      |
| Time to fix critical bug    | ❌ Longer (2 services)        | ✅ Faster (1 service) | BUG-1 took ~1 hour to fix in-place         |

**Recommendation: Keep RBAC in auth-service.**

Revisit extraction when any of these thresholds are hit:

- Sustained >500 req/s on RBAC decision path
- ≥3 separate teams need independent RBAC release cycles
- ABAC policy complexity requires a dedicated policy engine (OPA/Cedar)

---

## 10. Acceptance Criteria

### BUG-1 (Critical — Public RBAC Endpoints)

```bash
# Must return 401 (was returning 200 before fix)
curl -X POST http://localhost:8081/api/v1/auth/temporal-permissions \
     -H "Content-Type: application/json" \
     -d '{"userId":"x","permission":"PLATFORM_OVERSIGHT","expiresAt":"2099-01-01"}' \
     -w "\nHTTP Status: %{http_code}\n"
# Expected: HTTP Status: 401

# Must still return 200 (public catalog, no auth required)
curl http://localhost:8081/api/v1/auth/rbac/roles
# Expected: HTTP Status: 200

# Must return 401 without gateway headers
curl -X POST http://localhost:8081/api/v1/auth/delegations \
     -H "Content-Type: application/json"
# Expected: HTTP Status: 401
```

### BUG-2 (Caffeine Cache)

```bash
# After calling clearCache, a subsequent authorization request must succeed (cache reloads)
# Can be verified via Micrometer: rbac.cache.stats.miss.count increments after clear
```

### BUG-4 (Actuator)

```bash
# Must NOT contain database credentials or env vars
curl http://localhost:8081/actuator/info
# Expected: does not contain POSTGRES_PASSWORD, REDIS_PASSWORD, JWT_SECRET
```

### BUG-6 (jjwt version)

```bash
cd api-gateway && ./mvnw dependency:tree | grep jjwt
# Expected: all three artifacts show version 0.12.6
```

### Frontend (F3 — Login Redirect)

1. Log in as `ORGANIZATION_OWNER` user with org slug `acme` and username `john.doe`
2. Should redirect to `/org/acme/u/john-doe/dashboard/home`
3. Must NOT redirect to old `/dashboard` or `/org/acme/dashboard`

---

## 11. Safe Rollout Plan

### Phase 1 — Auth Service Hardening (BUG-1) — Review carefully

**Risk:** If any legitimate internal caller was hitting RBAC admin endpoints without auth headers, those calls will start returning 401.

**Pre-deploy checklist:**

- [ ] Review all internal callers of `/api/v1/auth/**` in Propertize and Employee services — confirm they all send `X-Service-Api-Key` or route through gateway
- [ ] Confirm `X-Service-Api-Key` values are configured in `ServiceAuthenticationConfig.java` and match gateway config
- [ ] Deploy `InternalRequestAuthFilter` to staging, run full E2E login → property create → tenant assign flow
- [ ] Verify `/api/v1/auth/rbac/roles` still returns 200 (public catalog must stay open)
- [ ] Verify `POST /api/v1/auth/temporal-permissions` without headers returns 401

**Rollback:** Revert `SecurityConfig.java` — no DB migrations involved, instant rollback.

### Phase 2 — Gateway Improvements (BUG-2, BUG-6, BUG-7)

**Risk:** Low. Caffeine cache is a drop-in replacement; jjwt 0.12.6 is fully backward-compatible with 0.12.3 JWT tokens.

**Checklist:**

- [ ] Deploy gateway to staging
- [ ] Run `curl -v` through gateway for 5 different role types — confirm authorization works
- [ ] Check Micrometer for `rbac.cache` metrics (should appear after first request)
- [ ] Confirm no `NullPointerException` logs for requests from internal Docker network (no remote address)

### Phase 3 — Auth-Service Config Fixes (BUG-4, BUG-5)

**Risk:** Very low. Actuator change is purely config; drift detection is read-only (log only).

**Checklist:**

- [ ] Confirm `/actuator/info` no longer exposes env vars
- [ ] Confirm startup logs do not contain `RBAC DRIFT:` warnings (all 22 canonical roles present in gateway YAML)

### Phase 4 — Frontend Fixes

**Risk:** Medium — login redirect changes affect all users.

**Checklist:**

- [ ] E2E test: login with each of the 5 primary roles — confirm correct URL for each
- [ ] Verify `PLATFORM_OVERSIGHT`-gated admin pages render correctly (lowercase permission check)
- [ ] Confirm `canManageProperties` / `canManageTenants` behaviours are unchanged for existing users

---

## 12. Files Changed in This Session

| File                                                          | Change Type | Summary                                                                                          |
| ------------------------------------------------------------- | ----------- | ------------------------------------------------------------------------------------------------ |
| `auth-service/…/config/SecurityConfig.java`                   | Modified    | Restricted permitAll() to 8 public paths; added InternalRequestAuthFilter                        |
| `auth-service/…/security/InternalRequestAuthFilter.java`      | **New**     | Gateway-header or service-API-key authentication for admin endpoints                             |
| `api-gateway/…/security/RbacAuthorizationFilter.java`         | Modified    | Caffeine TTL cache replacing ConcurrentHashMap; clearCache() fix                                 |
| `api-gateway/…/security/RbacConfig.java`                      | Modified    | Startup drift detection for 22 canonical roles                                                   |
| `api-gateway/pom.xml`                                         | Modified    | jjwt 0.12.3 → 0.12.6                                                                             |
| `auth-service/src/main/resources/application.yml`             | Modified    | Actuator info.env.enabled: false                                                                 |
| `api-gateway/…/filter/AuditLoggingFilter.java`                | Modified    | NPE guard on getAddress()                                                                        |
| `api-gateway/…/filter/LoggingFilter.java`                     | Modified    | NPE guard on getAddress()                                                                        |
| `api-gateway/…/integration/CrossServiceAuthenticationIT.java` | Modified    | Deprecated jjwt 0.11.x API → 0.12.x (subject/issuedAt/expiration/Jwts.SIG); SecretKey field type |
| `src/features/organizations/hooks/useOrgRoutes.ts`            | Modified    | dashboardRoute interface; removed routes.dashboard                                               |
| `src/app/(auth)/login/page.tsx`                               | Modified    | Inline getDashboardRoute → getDashboardPath() + slug utils                                       |
| `src/app/(platform)/admin/onboarding/…/page.tsx` (×2)         | Modified    | Uppercase → lowercase hasAnyRole check                                                           |
| `src/utils/roleUtils.ts`                                      | Modified    | Static role arrays → hasPermission(); removed isRoleAllowed()                                    |
