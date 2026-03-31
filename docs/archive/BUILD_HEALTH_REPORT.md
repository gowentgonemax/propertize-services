# Propertize Services — Build Health Report

**Date:** 2026-03-29  
**Scope:** Full backend + frontend code review, test fixes, integration validation, Docker runtime check

---

## 1. Summary

| Category                   | Status                      | Details                        |
| -------------------------- | --------------------------- | ------------------------------ |
| propertize-commons compile | ✅ Fixed                    | Package corruption repaired    |
| All Java services compile  | ✅ Pass                     | 6 services build clean         |
| propertize-commons tests   | ✅ 6/6 pass                 |                                |
| auth-service tests         | ✅ 205/205 pass             |                                |
| payment-service tests      | ✅ 84/84 pass               |                                |
| payroll-service tests      | ✅ 30/30 pass               | Fixed DeductionStrategyTest    |
| employee-service tests     | ✅ 19/19 pass               | Fixed unit + integration tests |
| propertize-core tests      | ✅ 602/602 pass             | 3 intentional skips            |
| Frontend (Next.js) build   | ✅ Pass                     | 116 pages, 0 TypeScript errors |
| Docker Compose runtime     | ✅ 23/23 containers healthy | All services up                |

---

## 2. Root Causes & Fixes

### 2.1 propertize-commons — Package Corruption (Recurring VS Code Bug)

**File:** `propertize-commons/src/main/java/com/propertize/commons/dto/ErrorResponse.java`  
**File:** `propertize-commons/src/test/java/com/propertize/commons/exception/PropertizeGlobalExceptionHandlerTest.java`

**Root Cause:** VS Code's Java extension corrupts package declarations by prepending `main.java.` or `test.java.` to package paths.

| File                                        | Corrupted Package                            | Correct Package                    |
| ------------------------------------------- | -------------------------------------------- | ---------------------------------- |
| `ErrorResponse.java`                        | `main.java.com.propertize.commons.dto`       | `com.propertize.commons.dto`       |
| `PropertizeGlobalExceptionHandlerTest.java` | `test.java.com.propertize.commons.exception` | `com.propertize.commons.exception` |

**Fix:** Corrected both package declarations. This is a recurring issue — run `mvn clean install` (not just `mvn install`) after VS Code Java extension activity.

---

### 2.2 employee-service — Test Package Corruption

**File:** `employee-service/src/test/java/.../service/EmployeeServiceTest.java`

**Root Cause:** Same VS Code package corruption bug.

| File                       | Corrupted                                                | Correct                                        |
| -------------------------- | -------------------------------------------------------- | ---------------------------------------------- |
| `EmployeeServiceTest.java` | `test.java.com.propertize.platform.employecraft.service` | `com.propertize.platform.employecraft.service` |

---

### 2.3 payroll-service — DeductionStrategyTest Type Mismatches

**File:** `payroll-service/src/test/java/.../calculation/DeductionStrategyTest.java`

**Root Cause:** The test was written against a different version of `PayrollContext` record. Multiple type mismatches existed.

| Issue                                 | Wrong Value              | Correct Value                     |
| ------------------------------------- | ------------------------ | --------------------------------- |
| `employeeId` parameter                | `UUID.randomUUID()`      | `1L` (Long)                       |
| `regularHours` parameter              | `BigDecimal.valueOf(80)` | `80` (int)                        |
| `overtimeHours` parameter             | `BigDecimal.ZERO`        | `0` (int)                         |
| `FederalTaxStrategy.name()` assertion | `"federalIncomeTax"`     | `"FederalIncomeTax"` (PascalCase) |
| `containsKeys()` assertion            | `"federalIncomeTax"`     | `"FederalIncomeTax"`              |
| Dangling `}` from removed test        | Present                  | Removed                           |
| Unused `import java.util.UUID`        | Present                  | Removed                           |

---

### 2.4 employee-service — Integration Test (Multiple Issues)

**File:** `employee-service/src/test/resources/application-test.yml`  
**File:** `employee-service/src/test/java/.../integration/EmployeeApiIntegrationTest.java`  
**File:** `employee-service/src/main/java/.../config/AuditorAwareImpl.java`

#### 2.4.1 YAML Structure Corruption

`spring.datasource` and `spring.jpa` were nested under `eureka:` instead of `spring:`. This prevented H2 in-memory DB from being configured in tests.

**Fix:** Corrected YAML structure and added:

- `spring.kafka.bootstrap-servers: localhost:9092`
- `spring.kafka.admin.fail-fast: false`
- `propertize.api.url: http://localhost:8082`
- `services.auth.url: http://localhost:8081`

#### 2.4.2 Integration Test Wrong Path & Headers

The test used path `/api/v1/clients/{CLIENT_ID}/employees` (gateway-facing) but the service controller is mapped to `/api/v1/employees`. Also sent wrong headers for the gateway filter.

| Issue                     | Wrong                              | Correct                                                   |
| ------------------------- | ---------------------------------- | --------------------------------------------------------- |
| Base path                 | `/api/v1/clients/{UUID}/employees` | `/api/v1/employees`                                       |
| Auth header               | `X-User-ID`, `X-User-Roles`        | `X-Gateway-Source`, `X-User-Id`, `X-Roles`                |
| Missing header            | —                                  | `X-Organization-Id: 00000000-0000-0000-0000-000000000001` |
| Missing header            | —                                  | `X-Username: admin`                                       |
| Expected status on create | `"ACTIVE"`                         | `"PENDING"` (service creates employees as PENDING)        |
| Wrong enum value          | `"payType": "SALARIED"`            | `"payType": "SALARY"` (`PayTypeEnum.SALARY`)              |

#### 2.4.3 AuditorAwareImpl Null Username NPE

`AuditorAwareImpl.getCurrentAuditor()` called `Optional.of(user.getUsername())` but `username` could be null (if `X-Username` header is absent), causing NPE → 500.

**Fix:** Changed to `Optional.of(username != null ? username : user.getUserId())` + added `X-Username` header in integration test.

#### 2.4.4 Kafka in Integration Tests

The real `EmployeeEventPublisher` uses `KafkaTemplate.send()`. Without Kafka running in tests, send succeeds asynchronously but fails silently. Added `@MockBean EmployeeEventPublisher` to avoid any Kafka interaction.

---

### 2.5 Frontend — Missing `toast` Import

**File:** `propertize-front-end/src/app/(dashboard)/dashboard/notifications/page.tsx`

**Root Cause:** `toast.success()` and `toast.error()` were called but `import toast from 'react-hot-toast'` was missing.

**Fix:** Added the import.

---

### 2.6 Frontend — Unused `DSAlertSuccess` Import

**File:** `propertize-front-end/src/components/modals/AddUserModal.tsx`

**Root Cause:** `DSAlertSuccess` was imported but never used. TypeScript strict mode treats this as an error.

**Fix:** Removed `DSAlertSuccess` from the import statement.

---

## 3. Backend Test Results

| Service            | Tests Run | Pass    | Fail  | Skip  |
| ------------------ | --------- | ------- | ----- | ----- |
| propertize-commons | 6         | 6       | 0     | 0     |
| auth-service       | 205       | 205     | 0     | 0     |
| payment-service    | 84        | 84      | 0     | 0     |
| payroll-service    | 30        | 30      | 0     | 0     |
| employee-service   | 19        | 19      | 0     | 0     |
| propertize-core    | 602       | 602     | 0     | 3\*   |
| **Total**          | **946**   | **946** | **0** | **3** |

\* 3 skips in propertize-core are intentional (marked `@Disabled` or `Assumptions`).

---

## 4. Frontend Build

- **Framework:** Next.js 16.2.1 with Turbopack
- **TypeScript:** Strict mode — 0 errors
- **Pages Generated:** 116 static/dynamic pages
- **Status:** ✅ BUILD SUCCESS

---

## 5. Docker Runtime Status

All 23 services are running. Health check summary:

| Service           | Port      | Status     |
| ----------------- | --------- | ---------- |
| api-gateway       | 8080      | ✅ healthy |
| auth-service      | 8081      | ✅ healthy |
| propertize (core) | 8082      | ✅ healthy |
| employee-service  | 8083      | ✅ healthy |
| payment-service   | 8084      | ✅ healthy |
| payroll-service   | 8085      | ✅ healthy |
| report-service    | 8090      | ✅ healthy |
| document-service  | 8092      | ✅ healthy |
| postgres          | 5432      | ✅ healthy |
| redis             | 6379      | ✅ healthy |
| kafka             | 9092      | ✅ healthy |
| mongodb           | 27017     | ✅ healthy |
| minio             | 9000/9001 | ✅ healthy |
| analytics-worker  | —         | ✅ running |
| payment-worker    | —         | ✅ running |
| screening-worker  | —         | ✅ running |

---

## 6. Code Review Findings (Non-Breaking)

> These are observations found during the review. No code changes were made for these as they are outside the scope of the current fixes.

### 6.1 Recurring VS Code Package Corruption Risk

The VS Code Java extension is known to rewrite package declarations when refactoring across src/main and src/test directories. All package declarations in main and test sources should be verified after any IDE-triggered refactoring.

**Recommendation:** Add a pre-commit hook or CI step that validates package declarations match their directory structure.

### 6.2 AuditorAwareImpl — Defensive Null Handling

Fixed in this session (NPE on null username). The production path always sends `X-Username`, but the defensive fallback to `userId` ensures robustness.

### 6.3 Employee Integration Test — E2E Coverage

The 5 integration tests now cover create, read, list, validation error, and not-found scenarios through the full HTTP stack with H2 in-memory DB. No Kafka, Feign, or external service calls are made during tests.

### 6.4 Integration Test vs Gateway Path Mismatch

The employee controller exposes `/api/v1/employees`. The API gateway routes the external path `/api/v1/clients/{id}/employees` to this service. Integration tests must exercise the internal service path directly.

### 6.5 PayTypeEnum Values

`PayTypeEnum` has `SALARY`, `HOURLY`, `SALARY_PLUS_COMMISSION`. The word "SALARIED" is not a valid value. Any external client sending "SALARIED" will receive a 400 Bad Request. Consider adding a `@JsonAlias("SALARIED")` annotation or documenting valid values in the OpenAPI schema.

---

## 7. Manual Verification Checklist

- [ ] Start all services with `make up` and verify `make health` shows all green
- [ ] Verify gateway routing: `GET http://localhost:8080/api/v1/employees` → 200
- [ ] Create an employee via gateway: `POST http://localhost:8080/api/v1/clients/{clientId}/employees`
- [ ] Check payroll endpoint: `GET http://localhost:8080/api/v1/clients/{clientId}/payroll`
- [ ] Login via frontend: `http://localhost:3000`
- [ ] Verify employee list page loads in dashboard
- [ ] Check Kafka connectivity: topic `employee-events` exists via `http://localhost:8086`

---

## 8. Preventive Measures

1. **VS Code Java Extension**: After any package rename/move, run `mvn clean install -DskipTests` before committing. The `clean` is critical to purge stale `.class` files from the corrupted build.
2. **Test profile YAML**: When adding new Feign clients, always add a test property (e.g., `service.url: http://localhost:XXXX`) to `application-test.yml`.
3. **Integration test headers**: All integration tests bypassing the gateway must include `X-Gateway-Source: api-gateway`, `X-Organization-Id`, `X-User-Id`, `X-Roles`, and `X-Username`.
4. **Enum values in tests**: Always cross-reference test enum string values against the actual `enum` class constants before committing.
