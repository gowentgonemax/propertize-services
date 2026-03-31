# Java 21 Modernization — Migration Rollout Plan

> Companion to [ADR-001](ADR-001-java21-modernization.md). Last updated: 2026-04-02.

---

## 1. Phased Rollout Strategy

### Phase 0 — Shared Library (DONE ✅)

| Item                                                           | Status |
| -------------------------------------------------------------- | ------ |
| `propertize-commons` v1.0.0 published to local M2              | ✅     |
| BaseException hierarchy (ErrorCode, 3 concrete exceptions)     | ✅     |
| ErrorResponse Java 21 record with factory methods              | ✅     |
| PropertizeGlobalExceptionHandler (`@Order(LOWEST_PRECEDENCE)`) | ✅     |
| CI job `commons-test` in `.github/workflows/ci.yml`            | ✅     |

### Phase 1 — Low-Risk Services (DONE ✅)

> employee-service, payroll-service — most actively developed, best test coverage.

| Item                                                                | Status |
| ------------------------------------------------------------------- | ------ |
| propertize-commons dependency added                                 | ✅     |
| Domain exceptions extend `BaseException`                            | ✅     |
| `GlobalExceptionHandler` extends `PropertizeGlobalExceptionHandler` | ✅     |
| Dockerfile fixed (removed `mvn dependency:go-offline`)              | ✅     |
| Summary DTOs converted to Java 21 records                           | ✅     |
| springdoc-openapi added + `OpenApiConfig.java`                      | ✅     |
| REST-assured integration tests (employee-service)                   | ✅     |
| Virtual threads enabled (payroll-service)                           | ✅     |
| Strategy/Factory patterns (payroll-service calculation)             | ✅     |
| Circuit Breaker (ResilientEmployeeClient)                           | ✅     |
| ObservabilityConfig (MDC, metrics)                                  | ✅     |

### Phase 2 — Supporting Services (DONE ✅)

> payment-service, auth-service — more sensitive, preserve API contracts.

| Item                                                         | Status |
| ------------------------------------------------------------ | ------ |
| propertize-commons dependency added                          | ✅     |
| Domain exceptions extend `BaseException`                     | ✅     |
| Existing handlers preserved (ApiResponse / Security formats) | ✅     |
| auth-service Dockerfile fixed                                | ✅     |
| springdoc-openapi added                                      | ✅     |

### Phase 3 — Core Service (DONE ✅)

> propertize-core — complex exception hierarchy, highest risk.

| Item                                                          | Status |
| ------------------------------------------------------------- | ------ |
| propertize-commons dependency added                           | ✅     |
| `@Order(0)` on existing handler (takes priority over commons) | ✅     |
| Full BusinessException/ErrorCode hierarchy preserved          | ✅     |

### Phase 4 — Remaining Work (Future)

| Item                                                      | Target                                   |
| --------------------------------------------------------- | ---------------------------------------- |
| api-gateway — add propertize-commons dependency           | When gateway exception handling migrated |
| SequencedCollection adoption (.getFirst(), List.copyOf()) | Per-service PRs                          |
| Sealed exception classes (ErrorCode → sealed permits)     | After team alignment                     |
| Virtual threads in employee-service + payment-service     | Load-test first                          |
| Pattern matching in switch (instanceof chains → switch)   | Incremental, per-PR                      |

---

## 2. Feature Flags — Existing Inventory

The project already uses `@ConditionalOnProperty` and `${ENV_VAR:default}` extensively. Below is the canonical list for Java 21 features:

### Backend (Spring Boot `@ConditionalOnProperty` / YAML)

| Flag                       | Service                                    | Default | Purpose                              |
| -------------------------- | ------------------------------------------ | ------- | ------------------------------------ |
| `VIRTUAL_THREADS_ENABLED`  | payroll-service                            | `true`  | Java 21 virtual threads (Loom)       |
| `TRACING_ENABLED`          | propertize-core, auth-service              | `false` | OpenTelemetry distributed tracing    |
| `HIKARI_MONITORING`        | propertize-core                            | `false` | HikariCP pool health monitor         |
| `RATE_LIMIT_ENABLED`       | auth-service, api-gateway                  | `true`  | Rate limiting                        |
| `STRIPE_ENABLED`           | payment-service                            | `true`  | Stripe payment integration           |
| `KEY_ROTATION_ENABLED`     | auth-service, api-gateway                  | `false` | JWT RSA key rotation                 |
| `SERVICE_AUTH_ENABLED`     | auth-service                               | `true`  | Inter-service API key auth           |
| `EUREKA_ENABLED`           | auth-service, api-gateway, propertize-core | `true`  | Service discovery registration       |
| `test.credentials.enabled` | propertize-core                            | `false` | Test credentials API (dev/test only) |
| `SWAGGER_ENABLED`          | payment-service                            | `true`  | springdoc-openapi UI                 |
| `DATABASE_STATS_ENABLED`   | propertize-core                            | `false` | DB statistics monitoring             |

### Frontend (`FEATURE_FLAGS` in `src/config/constants.ts`)

| Flag               | Env Variable                   | Default | Purpose               |
| ------------------ | ------------------------------ | ------- | --------------------- |
| `ENABLE_GRAPHQL`   | `NEXT_PUBLIC_ENABLE_GRAPHQL`   | `false` | GraphQL API layer     |
| `ENABLE_DEBUG`     | `NEXT_PUBLIC_ENABLE_DEBUG`     | `false` | Debug tooling         |
| `ENABLE_ANALYTICS` | `NEXT_PUBLIC_ENABLE_ANALYTICS` | `false` | Analytics integration |
| `ENABLE_PWA`       | `NEXT_PUBLIC_ENABLE_PWA`       | `false` | Progressive Web App   |

### Adding New Feature Flags

Backend pattern:

```yaml
# application.yml
propertize:
  features:
    sequenced-collections: ${SEQUENCED_COLLECTIONS_ENABLED:false}
```

```java
@ConditionalOnProperty(name = "propertize.features.sequenced-collections",
                        havingValue = "true", matchIfMissing = false)
```

Frontend pattern:

```typescript
// src/config/constants.ts
export const FEATURE_FLAGS = {
  // ...existing
  ENABLE_NEW_FEATURE: process.env.NEXT_PUBLIC_ENABLE_NEW_FEATURE === "true",
} as const;
```

---

## 3. Canary Deployment Pattern

```
┌──────────────┐  100%  ┌────────────────────┐
│   Gateway    │───────▷│  Stable (current)   │
│  (8080)      │        └────────────────────┘
│              │   5%   ┌────────────────────┐
│ Weight-based │───────▷│  Canary (new JAR)   │
│  routing     │        └────────────────────┘
└──────────────┘
```

Spring Cloud Gateway supports weighted routing via `Weight` predicate:

```yaml
# api-gateway application.yml — canary route example
spring.cloud.gateway.routes:
  - id: payroll-canary
    uri: lb://payroll-service-canary
    predicates:
      - Path=/api/v1/payroll/**
      - Weight=payroll-group, 5 # 5% to canary
  - id: payroll-stable
    uri: lb://payroll-service
    predicates:
      - Path=/api/v1/payroll/**
      - Weight=payroll-group, 95 # 95% to stable
```

Shift weight 5 → 25 → 50 → 100 over successive deployments.

---

## 4. Rollback Procedures

### Instant Rollback (< 1 min)

- **Feature flag**: Set `VIRTUAL_THREADS_ENABLED=false`, restart pod.
- **Docker**: `docker compose up -d payroll-service` with previous image tag.
- **Makefile**: `make rebuild SERVICE=payroll-service` after reverting code.

### Standard Rollback (< 5 min)

- **Git revert**: `git revert <commit>`, rebuild, redeploy.
- **Commons downgrade**: Remove `propertize-commons` dependency from service pom.xml, restore old exception classes from git.

### Emergency Rollback

- **All services**: `make down && git checkout main && make build && make up`
- **Database**: No schema changes in this migration — Hibernate `ddl-auto: update` is additive only.

### What Does NOT Require Rollback

- Java 21 record DTOs — Jackson handles records natively, binary-compatible.
- springdoc-openapi — opt-in via `SWAGGER_ENABLED`, no runtime impact when disabled.
- `@Order` annotations on exception handlers — no behavioral change if commons removed.

---

## 5. Monitoring Checklist

After each phase deployment, verify:

- [ ] `GET /actuator/health` returns `UP` for all services
- [ ] Error rate in logs stays below baseline
- [ ] P95 latency unchanged (check ObservabilityConfig MDC correlation)
- [ ] No `ClassNotFoundException` or `NoSuchMethodError` (binary compat)
- [ ] No `DuplicateMappingException` from Hibernate
- [ ] Kafka consumer lag stable (payroll-service employee-events topic)
- [ ] Circuit breaker metrics normal (`resilience4j.circuitbreaker.*`)

---

## 6. Timeline

| Week      | Action                                                                      |
| --------- | --------------------------------------------------------------------------- |
| W1 (Done) | Phase 0-3: commons, all 5 services migrated, DTOs→records, springdoc, tests |
| W2        | Team code review of all changes                                             |
| W3        | Deploy to staging, run full E2E suite                                       |
| W4        | Canary deploy payroll-service (5%), monitor 48h                             |
| W5        | Ramp to 100%, proceed with employee-service                                 |
| W6        | Deploy remaining services, mark migration complete                          |
