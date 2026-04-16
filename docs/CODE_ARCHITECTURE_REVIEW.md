# Propertize Services — Deep Code & Architecture Review

> **Date**: Session 3 Review | **Scope**: All services, frontend, infrastructure  
> **Methodology**: Automated static analysis + manual code review across all Java services, Python services, frontend, and configuration files.

---

## Executive Summary

The Propertize codebase is generally well-structured with consistent patterns across services. However, the review identified **3 critical security issues**, **2 architecture violations**, and several medium-priority improvements. The most urgent findings involve missing authorization on sensitive RBAC endpoints and a forbidden `@AuthenticationPrincipal` usage in the payroll service.

| Category | 🔴 Critical | 🟡 Medium | 🟢 Good |
|----------|:-----------:|:---------:|:-------:|
| Security | 3 | 2 | 4 |
| Architecture | 2 | 1 | 3 |
| Dead Code | 0 | 0 | ✅ Clean |
| Database | 0 | 1 | 3 |
| Error Handling | 0 | 1 | 3 |
| Configuration | 0 | 2 | 2 |
| Frontend | 0 | 6+ | 3 |

---

## 1. Security Issues

### 🔴 Critical: Missing Authorization on RBAC Endpoints

**File**: `auth-service/src/main/java/com/propertize/platform/auth/controller/RbacController.java`  
**Lines**: 80, 108, 128

Public endpoints without `@PreAuthorize`:
- `POST /api/v1/auth/authorize` — any unauthenticated request can resolve permissions
- `POST /api/v1/auth/permissions/resolve`
- `POST /api/v1/auth/permissions/check`

**Risk**: Permission resolution is exposed publicly. An attacker could enumerate all permissions for any role.

**Fix**: Add `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` or validate that requests originate from trusted internal services only (check `X-Forwarded-For` or use a service-to-service auth token).

---

### 🔴 Critical: `@AuthenticationPrincipal` in PayrollController (Banned)

**File**: `payroll-service/src/main/java/com/propertize/payroll/controller/PayrollController.java`  
**Line**: 79

```java
@PostMapping("/{payrollId}/approve")
public ResponseEntity<PayrollRun> approvePayrollRun(
    @PathVariable UUID clientId,
    @PathVariable UUID payrollId,
    @AuthenticationPrincipal Object principal) {  // ← FORBIDDEN per CLAUDE.md
```

**CLAUDE.md Rule**: "Gateway handles auth; services read `X-User-*` headers. NEVER use `@AuthenticationPrincipal`"

**Fix**: Replace with `@RequestHeader("X-Username") String username` extracted via `TrustedGatewayHeaderFilter`.

---

### 🔴 Critical: Hardcoded Database Credentials in Docker Configs

**Files**:
- `auth-service/src/main/resources/application-docker.yml` (line 14)
- `employee-service/src/main/resources/application-docker.yml` (line 34)
- `propertize/src/main/resources/application-docker.yml` (line 38)

**Pattern**: `${DB_PASSWORD:${POSTGRES_PASSWORD:dbpassword}}` — fallback `dbpassword` exposed.

**Fix**: Use `${DB_PASSWORD:?DB_PASSWORD is required}` to fail-hard if not set.

---

### 🟡 High: Hardcoded CORS Origins

**Files**:
- `payroll-service/.../security/SecurityConfig.java` (lines 76-78)
- `auth-service/.../config/SecurityConfig.java` (lines 105-108)

Localhost origins (`3000`, `3001`, `3002`) hardcoded in Java. Must be externalized to `application.yml` properties for environment portability.

---

### 🟡 High: Test Data with Hashed Passwords in Migrations

**File**: `payroll-service/db/migration/V3__insert_test_users.sql` (lines 10-15)

BCrypt-hashed passwords for test users committed to source. While hashed, this reveals test credential patterns. Consider gating test data behind a profile or using a separate seed script.

---

### ✅ Good Security Practices Observed

- JWT validation via HMAC signature in `TrustedGatewayHeaderFilter`
- `@Valid` consistently applied on all controller request bodies
- Parameterized JPQL queries throughout (no raw string concatenation)
- Native queries use named `:parameter` placeholders (no SQL injection risk)

---

## 2. Architecture Violations

### 🔴 Missing `@PreAuthorize` on Payroll EmployeeController

**File**: `payroll-service/.../controller/EmployeeController.java`

5+ endpoints without authorization:
- `GET /{id}` (line 52)
- `POST /direct` (line 113)
- `GET /by-number/{number}` (line 120)
- `GET /client/{clientId}/active` (line 126)
- `GET /search` (line 132)

**Risk**: Any authenticated user can access any employee's data regardless of organization.

---

### 🟡 TODO Markers Indicating Incomplete Features

| File | Line | TODO |
|------|------|------|
| `payroll-service/.../PaystubService.java` | 297 | "Get actual hours from timesheets" |
| `payroll-service/.../GarnishmentStrategy.java` | 29 | "Add garnishmentAmount fields" |
| `auth-service/.../PasswordResetService.java` | 55 | "Send email via email service" |
| `propertize-front-end/.../preferences/page.tsx` | 26 | "Save preferences to backend" |
| `propertize/.../TechnicianDashboardService.java` | 94 | Multiple incomplete features |

These represent unfinished business logic. Track in project board.

---

### ✅ Architecture Compliance

- ✅ All services follow correct package structure (`controller/service/repository/entity/dto/...`)
- ✅ `@Transactional` only on service layer (no violations on controllers/repositories)
- ✅ `@Data` NOT used on any JPA entity (only on DTOs where it's acceptable)
- ✅ `@RequiredArgsConstructor` for dependency injection across all services
- ✅ `ApiResponse<T>` wrapper used consistently via `propertize-commons`

---

## 3. Dead Code & Cleanup

### ✅ No Dead Code Detected

- No unused imports in sampled files
- No commented-out code blocks
- No `@Deprecated` classes without replacements
- TODO markers are legitimate development items (not dead code)

---

## 4. Database Issues

### 🟡 Missing Indexes on Search Columns

**File**: `payroll-service/.../repository/EmployeeEntityRepository.java` (lines 35-37)

```sql
LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
```

Columns `first_name`, `last_name`, `email` lack dedicated indexes for LIKE queries. For large datasets, add:
```java
@Table(indexes = {
    @Index(name = "idx_employee_first_name", columnList = "first_name"),
    @Index(name = "idx_employee_last_name", columnList = "last_name"),
    @Index(name = "idx_employee_email", columnList = "email")
})
```

Note: `LIKE '%search%'` queries cannot use B-tree indexes efficiently. Consider PostgreSQL `pg_trgm` extension with GIN indexes for full substring search.

---

### ✅ Good Database Practices

- Consistent ID types across FK relationships (no UUID/Long mixing)
- Proper `@OneToMany(cascade=ALL, orphanRemoval=true)` relationships
- Lazy loading defaults (no EAGER fetching risks)
- Composite indexes defined on auth entities (`RbacRole`: `is_system`, `organization_id`, `is_active`)

---

## 5. Error Handling

### 🟡 Exception Swallowing in EmployeeSyncService

**File**: `payroll-service/.../service/EmployeeSyncService.java` (lines 67, 111)

```java
catch (Exception e) {
    log.error("Failed to sync employee", e);
    return null;  // ← silently swallows, caller gets null
}
```

**Fix**: Throw a `EmployeeSyncException` or return `Optional.empty()` with caller awareness.

---

### ✅ Strong Error Handling

- `GlobalExceptionHandler` extends `PropertizeGlobalExceptionHandler` in all services
- Centralized exception handling via `propertize-commons`
- No empty catch blocks found
- All handlers map to `ApiResponse<T>` error responses with proper HTTP status codes

---

## 6. Configuration Issues

### 🟡 Registry Default Credentials

**File**: `service-registry/src/main/resources/application.yml` (line 15)

```yaml
password: ${EUREKA_PASSWORD:admin}
```

Defaults to `admin` if env var unset. Use fail-hard: `${EUREKA_PASSWORD:?required}`.

---

### ✅ Profile Configuration

- Proper 4-profile structure: `local`, `docker`, `test`, `prod`
- Test profile uses H2 in-memory (auto-activated by `./mvnw test`)
- Health checks present on all services (`/actuator/health`, `/health`)

---

## 7. Frontend Issues

### 🟡 `any` Type Usage (TypeScript Strict Mode)

| File | Usage | Severity |
|------|-------|----------|
| `components/dashboard/charts/BarChart.tsx:80` | `function (context: any)` | Medium — Chart.js callback |
| `components/dashboard/charts/LineChart.tsx:81` | `function (context: any)` | Medium — Chart.js callback |
| `components/dashboard/charts/DonutChart.tsx:47` | `function (chart: any)` | Medium — Chart.js callback |
| `components/payments/PaymentList.tsx:205` | `const columns: any[]` | High — should be typed |
| `services/httpClient.ts:8` | `let authStore: any` | Medium — lazy import pattern |
| `lib/redis-cache.ts:13` | `Map<string, { value: any }>` | Low — cache generic |

**Note**: Chart.js `any` types are partially acceptable due to library type limitations. `PaymentList.tsx` columns should be properly typed with a column definition interface.

---

### ✅ Good Frontend Practices

- `ErrorBoundary.tsx` and `GlobalErrorBoundary.tsx` wrap root layout
- All API calls go through singleton `httpClient` with consistent error handling
- Zustand for client state, React Query for server state (no Redux)
- Proper `ApiResponse<T>` generic interface for API responses

---

## Priority Action Items

### P0 — This Sprint (Security Critical)

| # | Action | File | Impact |
|---|--------|------|--------|
| 1 | Add auth to RBAC endpoints | `RbacController.java` | Prevents permission enumeration |
| 2 | Remove `@AuthenticationPrincipal` | `PayrollController.java:79` | Architecture compliance |
| 3 | Add `@PreAuthorize` to employee endpoints | `EmployeeController.java` (payroll) | Prevents cross-org data access |
| 4 | Remove hardcoded `dbpassword` fallbacks | 3x `application-docker.yml` | Secrets hygiene |

### P1 — Next Sprint

| # | Action | File | Impact |
|---|--------|------|--------|
| 5 | Externalize CORS origins to properties | 2x `SecurityConfig.java` | Environment portability |
| 6 | Fix exception swallowing | `EmployeeSyncService.java` | Error visibility |
| 7 | Add search column indexes | Employee entity | Query performance |
| 8 | Fail-hard Eureka password | `service-registry/application.yml` | Prod security |

### P2 — Backlog

| # | Action | Impact |
|---|--------|--------|
| 9 | Type `PaymentList` columns | TypeScript strictness |
| 10 | Resolve all TODO markers | Feature completeness |
| 11 | Audit `react-icons` unused imports | Build stability |
| 12 | Add `pg_trgm` GIN indexes for search | Search performance at scale |

---

*Generated by deep codebase analysis — Propertize Platform Team*
