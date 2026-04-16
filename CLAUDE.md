# Propertize Services — AI Agent Initialization (`/init`)

> **Every AI assistant (GitHub Copilot, Claude, Cursor, GPT-4, etc.) working on this codebase
> MUST read this file completely before generating any code or making any suggestions.**
>
> This file is the single source of truth for coding rules, architecture constraints, and
> project conventions.

---

## ⚠️ MANDATORY: Read Before Coding

All AI assistants must adhere to the following rules without exception. If any requirement is
unclear, **ask before coding — never assume or guess.**

---

## RULE 1 — Explain Reasoning Before Writing Code

Before writing any code, provide:

1. **Architectural reasoning** — how the solution fits the existing service structure
2. **Java OOP / SOLID considerations** — which principles apply and why
3. **Trade-offs and alternatives** — what other approaches were considered and rejected
4. **How the solution fits the project structure** — package placement, layer responsibility

> **No code may appear until the reasoning section is complete.**

---

## RULE 2 — Follow Java Coding Style and Architecture Strictly

### Naming Conventions
| Element | Convention | Example |
|---|---|---|
| Classes | `PascalCase` | `EmployeeService`, `PayrollCalculator` |
| Methods / variables | `camelCase` | `calculateNetPay()`, `employeeId` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_RETRY_ATTEMPTS`, `WINDOW_MS` |
| Packages | `lowercase` | `com.propertize.payroll.service` |
| DB columns | `snake_case` (Hibernate auto-converts) | `organization_id` |

### Package Structure (mandatory for every Java service)
```
src/main/java/com/propertize/[service]/
├── controller/     # @RestController — thin, delegates to service layer only
├── service/        # Business logic — @Transactional here, NEVER on controller/repo
├── repository/     # JpaRepository<Entity, ID> — no business logic
├── entity/         # @Entity — extend BaseEntity or AuditableEntity
│   ├── base/       # BaseEntity, AuditableEntity
│   └── embedded/   # @Embeddable value objects
├── dto/            # Separate *Request and *Response DTOs; use ApiResponse<T> wrapper
├── config/         # @Configuration beans
├── filter/         # Servlet/WebFlux filters
├── enums/          # Domain enumerations
├── exception/      # Custom exceptions + @ControllerAdvice
└── client/         # OpenFeign / RestTemplate clients
```

### Entity Rules
- Extend `BaseEntity` (id, timestamps) or `AuditableEntity` (adds `createdBy`, `updatedBy`, `deletedAt`)
- Use `@Getter @Setter` — **NEVER `@Data`** (breaks JPA `equals`/`hashCode`)
- `@GeneratedValue(IDENTITY)` with `Long` for auto-increment primary keys
- `UUID` for distributed IDs where sequential keys are a security risk
- **NEVER** mix `UUID` and `Long` in FK relationships

### DTO Rules
- **Always** separate `*Request` and `*Response` classes
- Use `@Builder` on DTOs
- Responses must use the `ApiResponse<T>` wrapper defined in each service's `dto/common/`
- **Nested response structure** for complex resources:
  ```java
  // Good
  PropertyResponseDTO {
      BasicInfo basicInfo;
      Financial financial;
      Amenities amenities;
  }
  // Bad — flat DTO
  PropertyResponseDTO { String name; BigDecimal rent; List<String> amenities; }
  ```

### Lombok
```java
@Getter @Setter          // on entities (not @Data)
@RequiredArgsConstructor // on services and controllers for constructor injection
@Builder                 // on DTOs
@Slf4j                   // for logging (log.info, log.warn, log.error)
```

### Transactional Placement
```java
// CORRECT
@Service
public class PayrollService {
    @Transactional
    public PayrollRun processPayroll(UUID orgId) { ... }
}

// WRONG — never on controller or repository
@RestController
public class PayrollController {
    @Transactional  // ← FORBIDDEN
    public ResponseEntity<?> runPayroll() { ... }
}
```

### Code Quality Checklist (every generated file)
- [ ] No unused imports
- [ ] No magic numbers (extract to named constants)
- [ ] Proper access modifiers (`private` unless there's a reason to be `public`)
- [ ] JavaDoc on all `public` classes and methods
- [ ] Methods ≤ 30 lines (extract helpers if longer)
- [ ] No deep nesting (≥ 3 levels → extract methods or use early returns)
- [ ] No God classes (single responsibility)

---

## RULE 3 — Ask Questions When Unclear

If **any** requirement is unclear:
1. Stop.
2. List the specific questions.
3. Wait for answers.

**Never assume default behavior, invent business logic, or guess at intent.**

---

## RULE 4 — Never Invent Libraries or APIs

Only use:
- Java 21 SDK
- Spring Boot 3.5.x / Spring Cloud 2025.0.0
- Libraries explicitly listed in the service's `pom.xml`
- Existing project dependencies

If you believe a missing library would improve the solution, **ask before adding it**.

### Explicitly Banned
- `mvn dependency:go-offline` in Dockerfiles — breaks transitive deps in Maven 3.8+ Docker builds
- Ehcache — blocked HTTP repos in Docker
- `@Data` on JPA entities
- `@AuthenticationPrincipal` — gateway handles auth; services read `X-User-*` headers
- Hardcoded Kafka topic strings — always use `KafkaTopics.*` constants from `propertize-commons`
- Hardcoded credentials or secrets in code

---

## RULE 5 — Provide File-by-File Output

When delivering code changes:
- Show each file separately with its **full path**
- Include correct package declaration
- Include all import statements
- Show only files that change (never repeat unchanged files)
- Maintain existing folder structure

---

## RULE 6 — Apply OOP and Clean-Code Principles

| Principle | Application in This Project |
|---|---|
| **Encapsulation** | DTOs expose only what the consumer needs; entities have controlled setters |
| **Abstraction** | Service interfaces over implementations where multiple strategies exist |
| **Composition over Inheritance** | Prefer `@Embedded` value objects; use delegation patterns |
| **Polymorphism** | Use strategy pattern over `if/else` chains for business rule variations |
| **SRP** | One class = one reason to change; separate command from query |
| **Open/Closed** | Extend via new classes / enums, not by modifying existing service logic |
| **DIP** | Inject interfaces, not concrete classes |

### Design Patterns in Use (follow these when applicable)
- **Decorator** — `ResilientEmployeeClient` wraps Feign with circuit breaker
- **Token Bucket** — `RateLimitBucket` for rate limiting
- **Chain of Responsibility** — Gateway filter chain
- **Factory** — `CacheConfig` Caffeine spec factories
- **Builder** — All DTO construction
- **Repository** — All data access
- **Anti-Corruption Layer** — `AuthServiceAntiCorruptionLayer`
- **Event-Driven** — Kafka publishers/consumers

---

## RULE 7 — Exception Handling

```java
// GOOD — meaningful custom exception
throw new EmployeeNotFoundException(
    String.format("Employee %s not found in organization %s", employeeId, orgId));

// BAD — swallowing exception
try { ... } catch (Exception e) { } // ← FORBIDDEN

// BAD — leaking low-level details
throw new RuntimeException(e); // ← wrap with context

// GOOD — wrap with context
throw new PayrollCalculationException(
    "Failed to calculate gross pay for employee: " + employeeId, e);
```

### Exception Hierarchy
Every service must have:
- `BaseException` (or service-specific equivalent) extending `RuntimeException`
- Resource-specific exceptions: `*NotFoundException`, `*ValidationException`
- `@ControllerAdvice` handler mapping exceptions to `ApiResponse<T>` error responses

---

## RULE 8 — Performance and Thread Safety

```java
// String concatenation in loops — ALWAYS use StringBuilder
StringBuilder sb = new StringBuilder();
for (Permission p : permissions) {
    sb.append(p.getCode()).append(",");
}

// Streams — use responsibly; avoid for simple loops
permissions.stream()
    .filter(p -> p.isActive())
    .map(PermissionDto::from)
    .collect(Collectors.toList());

// Thread safety for shared state
private final AtomicInteger tokens = new AtomicInteger(burstLimit);
private final ConcurrentHashMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

// Avoid unnecessary object creation in hot paths
// BAD: new ObjectMapper() on every call
// GOOD: inject @Bean ObjectMapper
```

### Generics
- Always parameterize collections: `List<EmployeeDto>` not raw `List`
- Use bounded wildcards where appropriate: `List<? extends BaseEntity>`

---

## RULE 9 — Testing Standards

Every new service method must have corresponding tests:

```java
// AAA pattern — Arrange, Act, Assert
@Test
@DisplayName("Should calculate gross pay correctly for hourly employee")
void shouldCalculateGrossPayForHourlyEmployee() {
    // Arrange
    EmployeeDto employee = EmployeeDto.builder()
        .payType(PayType.HOURLY)
        .hourlyRate(BigDecimal.valueOf(25.00))
        .build();
    TimesheetEntry entry = TimesheetEntry.builder()
        .regularHours(40)
        .overtimeHours(5)
        .build();

    // Act
    BigDecimal grossPay = payrollCalculator.calculateGrossPay(employee, entry);

    // Assert
    assertThat(grossPay).isEqualByComparingTo(BigDecimal.valueOf(1187.50));
}

// Mock external dependencies
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {
    @Mock
    private ResilientEmployeeClient employeeClient;

    @Mock
    private PayrollRepository payrollRepository;

    @InjectMocks
    private PayrollService payrollService;
    // ...
}
```

- Use `JUnit 5` + `Mockito`
- Test profile: `application-test.yml` with H2 in-memory DB (`./mvnw test` auto-activates)
- Always test **edge cases** and **failure paths** (null inputs, service down, empty results)
- Mock all external service calls — no real HTTP in unit tests

---

## RULE 10 — Consistency Across All Services

All services must follow the same patterns. **Never introduce a new pattern in one service
without a plan to apply it consistently.**

| Concern | Standard |
|---|---|
| **Authentication** | Read `X-User-Id`, `X-Username`, `X-Email`, `X-Roles` headers via `TrustedGatewayHeaderFilter` |
| **Error response** | `ApiResponse<T>` wrapper with `status`, `message`, `data`, `timestamp` |
| **Logging** | `@Slf4j` → `log.info("Action [correlationId={}]", correlationId)` |
| **Dependency injection** | `@RequiredArgsConstructor` (constructor injection) |
| **API base path** | `/api/v1/[resource]` |
| **Kafka topics** | `KafkaTopics.*` constants from `propertize-commons` |
| **Entity IDs** | `Long` (IDENTITY) or `UUID` — never mix in FK |
| **Soft delete** | `deletedAt` field via `AuditableEntity` |
| **Health endpoint** | `/actuator/health` (Java), `GET /health` (Python) |
| **Profiles** | `local` (dev JVM), `docker` (all containers), `test` (H2) |

---

## Project-Specific Architecture Rules

### Authentication Flow (Critical)
```
                   ┌─────────────────────────────────────────────┐
                   │              API Gateway :8080               │
                   │  1. JWT validated (RS256 public key)         │
                   │  2. Token blacklist checked (Redis)          │
                   │  3. Permissions loaded (Redis perms:jti:*)   │
                   │  4. Cookie header STRIPPED (prevents 431)    │
                   │  5. X-User-* headers INJECTED                │
                   └─────────────┬───────────────────────────────┘
                                 │ Downstream services trust these headers:
                                 │ X-User-Id, X-Username, X-Email
                                 │ X-Organization-Id, X-Roles, X-Permissions
                                 │ X-Primary-Role
                                 ▼
                   ┌─────────────────────────────────────────────┐
                   │         Downstream Service (e.g. :8083)      │
                   │  TrustedGatewayHeaderFilter reads headers    │
                   │  Sets Spring Security context                │
                   │  NEVER validates JWT directly               │
                   │  NEVER uses @AuthenticationPrincipal        │
                   └─────────────────────────────────────────────┘
```

### Gateway Route Order (Critical)
Routes in `api-gateway/src/main/resources/application.yml` are **order-sensitive**.
Specific service routes MUST appear **above** the `propertize-catchall` (`/api/v1/**`).

```yaml
# CORRECT order
- id: auth-service
  uri: lb://auth-service
  predicates:
    - Path=/api/v1/auth/**          # ← specific FIRST

- id: employee-service
  uri: lb://employee-service
  predicates:
    - Path=/api/v1/employees/**     # ← specific FIRST

- id: propertize-catchall           # ← catch-all LAST
  uri: lb://propertize-service
  predicates:
    - Path=/api/v1/**
```

### Kafka Topic Usage
```java
// CORRECT
kafkaTemplate.send(KafkaTopics.EMPLOYEE_EVENTS, key, event);

// WRONG — hardcoded string causes silent mismatches
kafkaTemplate.send("employee-events", key, event);
```

### Hibernate / JPA Critical Rules
```java
// RULE: @Embedded column names must not duplicate entity field names
@Entity
public class PayrollEmployee {
    @Embedded
    private YtdSummary ytdSummary;  // defines ytd_deductions column

    // WRONG — DuplicateMappingException
    @Column(name = "ytd_deductions")
    private BigDecimal ytdDeductions;
}

// RULE: JPQL uses Java field names, NOT column names
@Query("SELECT e FROM Employee e WHERE e.organizationId = :orgId")
// NOT: WHERE e.organization_id = :orgId  ← compile error

// RULE: UUID fields need UUID parameters
UUID findByEmployeeId(UUID employeeId);  // ← correct
UUID findByEmployeeId(String employeeId); // ← type mismatch error
```

### Dockerfile Rules
```dockerfile
# CORRECT — single stage, no go-offline
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

# WRONG — breaks transitive deps in Maven 3.8+
RUN mvn dependency:go-offline  # ← NEVER ADD THIS
```

---

## Quick Reference: File Locations

| Concern | File |
|---|---|
| Rate limiting | `api-gateway/.../filter/RateLimitingFilter.java` |
| JWT auth (gateway) | `api-gateway/.../security/JwtAuthenticationFilter.java` |
| Token blacklist | `api-gateway/.../service/TokenBlacklistService.java` |
| RBAC config | `auth-service/src/main/resources/rbac.yml` |
| RBAC service | `auth-service/.../service/RbacService.java` |
| Cache (gateway) | `api-gateway/.../config/CacheConfig.java` |
| Cache (payroll) | `payroll-service/.../config/CacheConfig.java` |
| Circuit breaker | `api-gateway/.../config/CircuitBreakerConfig.java` |
| Kafka topics | `propertize-commons/.../KafkaTopics.java` |
| Service routes | `api-gateway/src/main/resources/application.yml` |
| Permission constants | `auth-service/.../rbac/constants/PermissionConstants.java` |
| Zero-trust filter | `employee-service/.../security/filter/ZeroTrustJwtValidationFilter.java` |
| Trusted header filter | `[service]/.../filter/TrustedGatewayHeaderFilter.java` |

---

## Developer Commands Reference

```bash
# Start infrastructure (Postgres, Redis, Kafka, MongoDB, Eureka)
make dev

# Run a specific service locally
cd payroll-service && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Full Docker stack
make up                        # start all
make down                      # stop (keep volumes)
make rebuild                   # down → build → up
make health                    # check all endpoints
make logs-payroll              # tail payroll-service logs
make db-shell                  # psql shell

# Python services
cd python-services && docker compose -f docker-compose.python.yml up -d --build

# Frontend
cd propertize-front-end && npm install && npm run dev
npx tsc --noEmit               # TypeScript type check

# Tests
./mvnw test                    # runs with H2 (test profile auto-activated)
./mvnw verify                  # full build + test
```

---

*This file is read automatically by Claude, GitHub Copilot, and other AI agents as part of
the project's `/init` configuration. Do not delete or move this file.*
*Last updated: April 2026 — Propertize Platform Team.*

