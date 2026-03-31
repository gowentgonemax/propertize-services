# ADR-001: Java 21 Modernization & Shared Exception Library

| Field       | Value           |
| ----------- | --------------- |
| **Status**  | Accepted        |
| **Date**    | 2026-06-24      |
| **Authors** | Propertize Team |

## Context

Propertize runs 5 Java microservices (employee-service, payroll-service, payment-service, auth-service, propertize-core) on Java 21 and Spring Boot 3.5. Each service historically maintained its own exception handling, DTO classes, and error response format.

**Problems identified:**

1. **4 different GlobalExceptionHandlers** with 4 different error response shapes
2. **No shared exception hierarchy** — RuntimeException was the only common parent
3. **Duplicate DTOs** — summary-style DTOs repeated across services
4. **Under-utilization of Java 21** — no records, sealed classes, pattern matching, or virtual threads
5. **No OpenAPI documentation** for 2 of 5 services
6. **No integration tests** using a modern framework

## Decision

### 1. Shared Library: `propertize-commons`

Create `com.propertize:propertize-commons:1.0.0` (JAR) containing:

- `ErrorCode` enum — canonical codes across all services
- `BaseException` — abstract root of exception hierarchy
- `ErrorResponse` — Java 21 record for consistent error bodies
- `PropertizeGlobalExceptionHandler` — drop-in `@RestControllerAdvice` with `@Order(LOWEST_PRECEDENCE)`

### 2. Service Migration Strategy

| Service          | Approach                                                                           | Rationale                                   |
| ---------------- | ---------------------------------------------------------------------------------- | ------------------------------------------- |
| employee-service | Handler **extends** PropertizeGlobalExceptionHandler                               | Clean codebase, no legacy constraints       |
| payroll-service  | Handler **extends** PropertizeGlobalExceptionHandler                               | New service, no existing handler            |
| payment-service  | Exceptions extend BaseException, **keep** existing handler with ApiResponse format | Preserve API contract                       |
| auth-service     | **Keep** existing handler (Spring Security-specific)                               | Security exceptions need custom handling    |
| propertize-core  | **Keep** existing handler with `@Order(0)`, commons auto-scanned as fallback       | Most complex hierarchy, too risky to change |

### 3. Java 21 Features Adopted

| Feature                 | Where Applied                                                                                                                                                            |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Records                 | DTOs (CompensationSummary, PositionSummary, AddressSummary, DepartmentSummary, ManagerSummary, DepartmentDto, PositionDto, ErrorResponse, PayrollContext, PayrollResult) |
| Pattern matching switch | PropertizeGlobalExceptionHandler.httpStatusFor(), CircuitBreakerMonitoringController                                                                                     |
| Virtual threads         | payroll-service VirtualThreadConfig (opt-in via config flag)                                                                                                             |
| Sealed classes          | Future — exception hierarchy candidates                                                                                                                                  |

### 4. Testing & Documentation

- REST-assured integration tests for employee-service
- Playwright e2e tests for frontend (Add User flow)
- OpenAPI 3.1 specs in `docs/openapi/`
- Swagger UI available at `/swagger-ui.html` on all services

## Consequences

### Positive

- Consistent error format across services (ErrorCode + correlationId tracking)
- ~60% reduction in duplicate exception-handling code
- Better debugging via correlation IDs in every error response
- Immutable DTOs prevent accidental mutation
- Swagger documentation auto-generated for all services

### Negative

- `propertize-commons` must be installed to local M2 before any service can compile
- Package declaration in commons may be overwritten by IDE formatters (known VS Code Java extension issue)
- Services with existing API contracts (payment-service, propertize-core) cannot fully adopt commons error format without frontend changes

### Risks

- IDE/formatter overwriting `propertize-commons` package declarations — mitigated by CI workflow that compiles commons first
- Version drift if services pin different commons versions — mitigated by using explicit `1.0.0` version

## Related

- [ARCHITECTURE.md](ARCHITECTURE.md) — system design
- [docs/openapi/employee-service.yaml](openapi/employee-service.yaml) — API specification
