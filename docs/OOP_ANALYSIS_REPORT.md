# Propertize Services — OOP Analysis Report

**Generated:** April 12, 2026  
**Scope:** Full codebase scan across all 12+ services (Java, Python, TypeScript)  
**Verdict:** OOP is **extensively implemented** across the Java backend, with identified gaps in Python services and select inconsistencies between microservices.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [OOP Concept Detection Matrix](#2-oop-concept-detection-matrix)
3. [Detailed OOP Findings by Service](#3-detailed-oop-findings-by-service)
4. [Advantages of OOP in This Project](#4-advantages-of-oop-in-this-project)
5. [Where OOP Can Be Further Implemented](#5-where-oop-can-be-further-implemented)
6. [Backend-Wide Consistency Evaluation](#6-backend-wide-consistency-evaluation)
7. [Recommended OOP Classes & Responsibilities](#7-recommended-oop-classes--responsibilities)
8. [Refactoring Roadmap](#8-refactoring-roadmap)

---

## 1. Executive Summary

The Propertize codebase demonstrates **strong, deliberate OOP implementation** in its Java services, with multiple GoF design patterns, a well-defined entity inheritance hierarchy, and proper use of abstraction and polymorphism. However, the level of OOP maturity varies significantly across the stack:

| Layer | OOP Maturity | Rating |
|-------|-------------|--------|
| **Core Java (`propertize`)** | Advanced — CQRS, DDD, Strategy, Factory, Template Method, State Machine, Event Sourcing, AOP | ⭐⭐⭐⭐⭐ |
| **Payroll Service** | Strong — Strategy pattern for deductions, Java 21 records, clean separation | ⭐⭐⭐⭐ |
| **Auth Service** | Strong — PolicyEngine interface with ABAC evaluators, Strategy + polymorphic dispatch | ⭐⭐⭐⭐ |
| **Payment Service** | Good — Gateway abstraction, entity hierarchy, but services lack interfaces | ⭐⭐⭐½ |
| **Employee Service** | Basic — Auditable base entity, simple service class, minimal abstraction | ⭐⭐½ |
| **Python Services** | Minimal — Procedural/functional, no classes, raw SQL with module-level functions | ⭐½ |
| **Frontend (Next.js)** | Moderate — Functional React paradigm (by design), `HttpApiError` class, Zustand stores | ⭐⭐⭐ |
| **Propertize Commons** | Good — Shared `BaseException` hierarchy, `ErrorCode` enum, Kafka topic constants | ⭐⭐⭐½ |

---

## 2. OOP Concept Detection Matrix

### ✅ Concepts Fully Implemented

| Concept | Evidence | Location |
|---------|----------|----------|
| **Classes & Objects** | 60+ entity classes, 40+ service classes, 20+ DTOs per service | All Java services |
| **Inheritance** | 3-level entity hierarchy: `BaseEntity` → `AuditableEntity` → `OrganizationScopedEntity`. Event hierarchy: `ApplicationEvent` → `BaseApplicationEvent` → 12+ concrete events. Exception hierarchy: `RuntimeException` → `BusinessException` → `ResourceNotFoundException`, etc. | `propertize/entity/base/`, `event/`, `exception/` |
| **Polymorphism** | `NotificationSender` interface with `EmailNotificationSender`, `InAppNotificationSender`, `SmsNotificationSender`. `DeductionStrategy` with `FederalTaxStrategy`, `Contribution401kStrategy`. `ConditionEvaluator` with 4 evaluators. `PolicyEngine` → `DefaultPolicyEngine`. Spring DI resolves implementations at runtime. | `services/sender/`, `payroll/calculation/`, `auth/rbac/engine/` |
| **Abstraction** | `Command`, `Query`, `DomainEvent`, `NotificationSender`, `DeductionStrategy`, `ConditionEvaluator`, `PaymentGatewayService`, `PolicyEngine` — all are interfaces/abstract classes hiding implementation. `AggregateRoot` defines abstract `getId()`. `BaseDashboardService<T>` template. | Across all Java services |
| **Encapsulation** | Lombok `@Getter @Setter` on entities (not `@Data`). Private fields with controlled access. Immutable Java 21 records (`PayrollContext`, `PayrollResult`). `@Embeddable` value objects (Address, LeaseFinancialTerms). Private helper methods in factories. | All entities, DTOs, value objects |

### ⚠️ Concepts Partially Implemented

| Concept | Current State | Gap |
|---------|--------------|-----|
| **Interface Segregation** | Some service interfaces exist (`PaymentGatewayService`, `PolicyEngine`, `SecurityService`, `ResourceAccessService`, `NotificationSender`) but most service classes (40+ in `propertize`) are concrete with no interface. | Only 4 service interfaces across 60+ service classes |
| **Composition over Inheritance** | `@Embedded` value objects used extensively (65+ embeddables), but some entities still use deep inheritance chains. | Some entities inherit 3 levels when composition would suffice |
| **Open/Closed Principle** | Strategy pattern (payroll deductions, notification senders, condition evaluators) allows extension without modification — but other areas (e.g., dashboard services) would benefit from the same. | Dashboard enrichment logic is hardcoded rather than pluggable |
| **Cross-service OOP consistency** | `propertize-commons` provides shared `BaseException` + `ErrorCode`, but each service independently re-declares its own `BaseEntity`, `AuditableEntity`, `GlobalExceptionHandler`. | Entity base classes are not shared from commons |

### ❌ Concepts Missing or Weak

| Concept | Current State | Impact |
|---------|--------------|--------|
| **Python OOP** | All 6 Python services use procedural/functional style — module-level functions, no classes, no inheritance. `shared/db.py` uses a context manager (good), but no repository pattern, no service layer abstraction. | Hard to test, extend, or mock. Business logic mixed with I/O. |
| **Service Layer Interfaces (most services)** | `PropertyService`, `TenantService`, `LeaseService`, `PaymentService`, etc. are all concrete classes. Only `PaymentGatewayService` has an interface. | Tight coupling, makes testing harder, prevents alternative implementations. |
| **Domain Model richness** | `AggregateRoot` and `DomainEvent` exist but are not widely used — most entities are anemic (data holders with no business methods beyond `isDeleted()`, `markAsDeleted()`). | Business logic lives in 40+ large service classes instead of domain entities. |
| **Liskov Substitution** | Not explicitly violated, but the inconsistent base entity hierarchies across services (each service has its own `BaseEntity`) means substitutability is service-scoped only. | Cannot share entity-level utilities across services. |

---

## 3. Detailed OOP Findings by Service

### 3.1 Core Service (`propertize` — Port 8082)

**OOP Rating: ⭐⭐⭐⭐⭐ (Advanced)**

This is the most OOP-mature service. Key patterns found:

#### Entity Hierarchy (Inheritance + Encapsulation)
```
BaseEntity (abstract, @MappedSuperclass)
  ├── version, createdAt, updatedAt, ipAddress, correlationId, sessionId, userAgent
  ├── @PrePersist/@PreUpdate initializeVersion()
  │
  └── AuditableEntity (abstract, extends BaseEntity)
        ├── createdBy, updatedBy, deletedAt, deletedBy, deletionReason
        ├── isDeleted(), markAsDeleted(), restore()
        │
        └── OrganizationScopedEntity (abstract, extends AuditableEntity)
              ├── organizationId
              ├── validateOrganizationId()
              │
              ├── Property, Tenant, Lease, Payment, MaintenanceRequests, ...
              └── (65+ entity classes extend this hierarchy)
```

#### Design Patterns Implemented

| Pattern | Implementation | Files |
|---------|---------------|-------|
| **Strategy** | `NotificationSender` interface → `EmailNotificationSender`, `InAppNotificationSender`, `SmsNotificationSender` | `factory/notification/`, `services/sender/` |
| **Abstract Factory** | `NotificationSenderFactory` auto-discovers strategies via Spring DI, provides O(1) lookup, fallback chains | `factory/notification/NotificationSenderFactory.java` |
| **Factory Method** | `NotificationFactory` creates notification entities; `ResourceNotFoundException` has static factory methods (`organization()`, `user()`, `tenant()`, etc.) | `factory/NotificationFactory.java`, `exception/ResourceNotFoundException.java` |
| **Template Method** | `BaseDashboardService<T>` defines algorithm skeleton (`getDashboard()` is final), subclasses override `fetchDashboardData()`, `validateAccess()`, hook methods for caching/enrichment | `services/domain/BaseDashboardService.java` |
| **State Machine** | `LeaseStateMachine` and `RentalApplicationStateMachine` — validate/enforce state transitions via `EnumMap<Status, Set<Status>>` | `statemachine/` |
| **CQRS** | `Command`/`Query` interfaces, `CommandHandler<C,R>`/`QueryHandler<Q,R>` generics, `CqrsDispatcher` mediator, `AbstractCommand` base class, `CreatePropertyCommand` + handler | `cqrs/` |
| **DDD (Domain-Driven Design)** | `AggregateRoot` (event tracking, version control), `DomainEvent` interface, `AbstractDomainEvent` base, `StoredEvent` persistence model | `domain/common/`, `eventsourcing/` |
| **Event Sourcing** | `EventStoreService` persists `StoredEvent` from `DomainEvent`, replay via `AggregateRoot.loadFromHistory()` | `eventsourcing/` |
| **Observer/Event** | Spring `ApplicationEvent` hierarchy — `BaseApplicationEvent` → 14+ events (`ApplicationApprovedEvent`, `LeaseCreatedEvent`, `PaymentCompletedEvent`, etc.) with listeners | `event/`, `event/listener/` |
| **AOP (Cross-cutting)** | `SecurityAuditAspect` (pointcuts for controllers, security validators, data access methods), `DatabaseConnectivityAspect` | `aspect/` |
| **Value Object** | 65+ `@Embeddable` classes — `Address`, `LeaseFinancialTerms`, `LeaseDates`, `PropertyFeatures`, `TenantFinancialInfo`, etc. | `entity/embedded/` |
| **Builder** | `CreatePropertyCommand.Builder`, Lombok `@Builder` on DTOs | `cqrs/command/`, `dto/` |
| **Repository (Data Access)** | `BaseRepository<T, ID>` extends `JpaRepository` + `JpaSpecificationExecutor`, adds `findAllActive()` | `repository/BaseRepository.java` |
| **Mapper** | 23 MapStruct mappers for entity ↔ DTO conversion | `mapper/` |

#### Exception Hierarchy
```
RuntimeException
  ├── PropertizeException (base, with ErrorCode)
  └── BusinessException (abstract, with ErrorCode + HttpStatus)
        ├── ResourceNotFoundException (with static factory methods)
        ├── BadRequestException
        ├── ConflictException
        ├── ForbiddenException
        ├── UnauthorizedException
        ├── ValidationException
        ├── PaymentProcessingException
        ├── RateLimitExceededException
        ├── EntityAlreadyExistsException
        ├── InvalidApplicationStateException
        └── ... (20 total exception classes)
```

### 3.2 Payroll Service (`payroll-service` — Port 8085)

**OOP Rating: ⭐⭐⭐⭐ (Strong)**

#### Strategy Pattern for Payroll Calculation
```
DeductionStrategy (interface, @FunctionalInterface)
  ├── calculate(PayrollContext): BigDecimal
  └── name(): String (default method)
        │
        ├── FederalTaxStrategy (@Component)
        ├── Contribution401kStrategy (@Component)
        └── [extensible — add new @Component to auto-register]

PayrollCalculationEngine (orchestrator)
  └── List<DeductionStrategy> strategies (auto-injected by Spring)
```

- **Java 21 Records**: `PayrollContext` (immutable parameter object with compact constructor validation) and `PayrollResult` (immutable result with defensive copy).
- **Entity base**: Own `BaseEntity` in `entity/base/` (not shared from commons).
- **AOP**: `LoggingAspect` for cross-cutting concerns.
- **44 entity classes** with embedded value objects.

### 3.3 Auth Service (`auth-service` — Port 8081)

**OOP Rating: ⭐⭐⭐⭐ (Strong)**

#### Policy Engine (Strategy + Polymorphism)
```
PolicyEngine (interface)
  └── DefaultPolicyEngine (implements, @Component)
        ├── evaluate(), hasPermission(), listPermissions(), ...
        └── Uses List<ConditionEvaluator> for ABAC

ConditionEvaluator (interface)
  ├── evaluate(PolicyContext, condition, attributes): boolean
  └── supports(condition): boolean
        │
        ├── OwnershipConditionEvaluator
        ├── TimeBasedConditionEvaluator
        ├── DataScopeConditionEvaluator
        └── ConditionalPermissionEvaluator
```

- Rich entity model: `User`, `RbacRole`, `CompositeRole`, `CustomRole`, `Delegation`, `TemporalPermission`, `PermissionAuditLog`, `IpAccessRule`.
- 22 service classes handling authentication, authorization, delegation, session management, rate limiting, etc.
- `DynamicRoleComposer` for runtime role composition.

### 3.4 Payment Service (`payment-service` — Port 8084)

**OOP Rating: ⭐⭐⭐½ (Good)**

- **Interface abstraction**: `PaymentGatewayService` → `StripePaymentService` (enables swapping payment providers).
- **Entity hierarchy**: `BaseEntity` → `AuditableEntity` → `OrganizationScopedEntity` (own copies, not from commons).
- **13 entity classes** with entity listeners and embedded value objects.
- **Gap**: Most service classes (`PaymentService`, `PaymentMethodService`, `PromoCodeService`, etc.) are concrete — no interfaces.

### 3.5 Employee Service (`employee-service` — Port 8083)

**OOP Rating: ⭐⭐½ (Basic)**

- Has `AuditableEntity` base class (own copy using Spring Data JPA auditing).
- 3 entity classes (`Employee`, `Department`, `Position`) with embedded value objects.
- Only 2 service classes (`EmployeeService`, `EmployeeNumberGenerator`).
- Anti-Corruption Layer: `AuthServiceClient` (Feign interface for cross-service calls).
- **Gaps**: No service interfaces, no design patterns beyond basic layering, very thin compared to other services.

### 3.6 Python Services (Ports 8090–8093 + Workers)

**OOP Rating: ⭐½ (Minimal)**

All 6 Python services (`report-service`, `vendor-matching`, `document-service`, `search-reranker`, `analytics-worker`, `screening-worker`, `payment-worker`) follow a **procedural/functional** style:

| Aspect | Current State |
|--------|--------------|
| Classes | Only Pydantic `BaseModel` subclasses for request/response schemas (`MatchRequest`, `VendorScore`). No domain classes. |
| Inheritance | None. No base classes for services, repositories, or workers. |
| Polymorphism | None. No interfaces, no substitutable implementations. |
| Encapsulation | Module-level global variables (`_model = None`, `_pool = None`). No private state. |
| Design Patterns | None. `get_connection()` is a context manager (good), but not a class. |
| Code Organization | Single-file services (`main.py`, `matcher.py`, `aggregate.py`, `pipeline.py`). Business logic, DB access, and HTTP handling in one file. |

### 3.7 Frontend (`propertize-front-end` — Port 3000)

**OOP Rating: ⭐⭐⭐ (Moderate — appropriate for React)**

The frontend uses a functional/reactive paradigm appropriate for React/Next.js:

- **`HttpApiError` class** extends `Error` — proper OOP exception with typed fields (`code`, `details`, `validationErrors`, `fieldErrors`).
- **Service modules** (`*.service.ts`) are exported functions, not classes — idiomatic for React.
- **Zustand stores** use closures, not classes — appropriate for the framework.
- **TypeScript interfaces** define strong contracts for all API responses and component props.
- No OOP anti-patterns here — functional React is the correct paradigm.

### 3.8 Shared Commons (`propertize-commons`)

**OOP Rating: ⭐⭐⭐½ (Good foundation)**

```
BaseException (abstract)
  ├── errorCode: ErrorCode
  ├── context: Object[]
  └── ResourceNotFoundException extends BaseException
      InvalidStateTransitionException extends BaseException
      UpstreamServiceException extends BaseException

ErrorCode (enum with HTTP status mapping)
KafkaTopics (constants class)
```

**Gap**: Commons provides exception hierarchy and enums, but does NOT provide shared:
- `BaseEntity` / `AuditableEntity` (each service re-implements these)
- `BaseRepository` (only exists in `propertize`)
- DTO wrappers like `ApiResponse<T>` (each service has its own)

---

## 4. Advantages of OOP in This Project

### 4.1 Architecture-Level Benefits

| Benefit | Specific Example in Propertize |
|---------|-------------------------------|
| **Multi-tenancy safety** | `OrganizationScopedEntity` base class ensures every entity has `organizationId` with validation — a single inheritance point prevents data leakage across tenants. |
| **Payment provider flexibility** | `PaymentGatewayService` interface allows swapping Stripe for Square, PayPal, etc. without changing business logic in `PaymentService`. |
| **Deduction engine extensibility** | Adding a new payroll deduction (state tax, benefits, garnishment) requires only a new `@Component` implementing `DeductionStrategy` — zero changes to `PayrollCalculationEngine`. |
| **Notification channel scaling** | `NotificationSenderFactory` auto-discovers all `NotificationSender` beans. Adding push notifications = one new class. |
| **Audit trail consistency** | `AuditableEntity` + `AuditListener` provides `createdBy`, `updatedBy`, `deletedAt` for every entity without manual wiring. |
| **State machine enforcement** | `LeaseStateMachine` and `RentalApplicationStateMachine` centralize business rules for status transitions, preventing invalid state changes scattered across service methods. |

### 4.2 Maintainability Benefits

- **Entity hierarchy** eliminates ~20 duplicated fields (version, timestamps, audit fields) across 100+ entities.
- **MapStruct mappers** with interfaces prevent manual mapping errors in 23+ entity ↔ DTO conversions.
- **Strategy + Factory** patterns localize change impact — e.g., modifying the federal tax bracket only touches `FederalTaxStrategy`, not `PayrollCalculationEngine`.
- **Template Method** in `BaseDashboardService` enforces consistent caching/logging/error handling across 7+ role-specific dashboards.

### 4.3 Testability Benefits

- **Strategy interfaces** (`DeductionStrategy`, `NotificationSender`, `ConditionEvaluator`) can be individually unit-tested with mocked contexts.
- **CQRS** separates read/write concerns — command handlers can be tested independently from query handlers.
- **Factory pattern** allows injecting test doubles for notification senders.
- **Interface abstraction** (`PaymentGatewayService`) enables mocking Stripe in unit tests.

### 4.4 Scalability Benefits

- **Event Sourcing + CQRS** foundation enables future read replicas and event replay.
- **Polymorphic dispatch** in `PolicyEngine` scales to unlimited ABAC condition types.
- **Embeddable value objects** reduce JOIN complexity — a single `Property` entity embeds 20+ complex types.

---

## 5. Where OOP Can Be Further Implemented

### 5.1 Python Services — HIGH PRIORITY

**Current State**: Pure procedural code in single files.  
**Recommendation**: Introduce OOP to enable testability and reuse.

#### Proposed Class Hierarchy for Python Services

```python
# python-services/shared/base_service.py
class BaseService:
    """Abstract base for all Python microservices."""
    def __init__(self, db_pool):
        self.db = db_pool
    
    def health_check(self) -> dict:
        return {"status": "ok", "service": self.__class__.__name__}

# python-services/shared/base_repository.py
class BaseRepository:
    """Encapsulates database access with connection pooling."""
    def __init__(self, pool):
        self._pool = pool
    
    def query(self, sql: str, params: list = None) -> pd.DataFrame:
        with self._get_connection() as conn:
            return pd.read_sql(sql, conn, params=params)
    
    def execute(self, sql: str, params: list = None):
        with self._get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)

# python-services/report-service/repositories/financial_repository.py
class FinancialRepository(BaseRepository):
    def get_income_by_type(self, org_id: str, start: str, end: str) -> pd.DataFrame:
        return self.query("""
            SELECT payment_type, SUM(amount) as total, COUNT(*) as count
            FROM payment WHERE organization_id = %s ...
        """, [org_id, start, end])

# python-services/report-service/services/financial_report_service.py
class FinancialReportService(BaseService):
    def __init__(self, repo: FinancialRepository, renderer: ReportRenderer):
        self.repo = repo
        self.renderer = renderer
    
    def generate_pdf(self, org_id: str, start: str, end: str) -> bytes:
        data = self.repo.get_income_by_type(org_id, start, end)
        return self.renderer.render_pdf(data)
```

**Benefits**:
- Repositories are independently testable with mocked connections
- Services are testable with mocked repositories
- Report renderers can be swapped (PDF → Excel → CSV) via polymorphism
- Shared `BaseRepository` eliminates duplicated SQL boilerplate across 6 services

### 5.2 Service Layer Interfaces — MEDIUM PRIORITY

**Current State**: 4 service interfaces out of 60+ service classes.  
**Recommendation**: Add interfaces for services with potential alternative implementations or those needing mockability.

| Service | Proposed Interface | Rationale |
|---------|-------------------|-----------|
| `PropertyService` | `IPropertyService` | Most complex service (~50 methods); enables caching decorator, testing |
| `TenantService` | `ITenantService` | Called cross-service; interface enables Feign-style client abstraction |
| `LeaseService` | `ILeaseService` | Complex state transitions; interface enables workflow engine swap |
| `EmailService` | `IEmailService` | Already has `NotificationSender`, but core `EmailService` is concrete |
| `SearchService` | `ISearchService` | Backend could swap between DB search and Elasticsearch |
| `AuditLogService` | `IAuditLogService` | Could swap between DB audit and MongoDB audit sink |

### 5.3 Shared Entity Base Classes — MEDIUM PRIORITY

**Current State**: `BaseEntity` and `AuditableEntity` are independently implemented in `propertize`, `employee-service`, `payment-service`, and `payroll-service`.  
**Recommendation**: Move to `propertize-commons`.

```
propertize-commons/
  └── src/main/java/com/propertize/commons/
        ├── entity/
        │     ├── BaseEntity.java           (from propertize/entity/base/)
        │     ├── AuditableEntity.java       (from propertize/entity/base/)
        │     └── OrganizationScopedEntity.java
        ├── repository/
        │     └── BaseRepository.java        (from propertize/repository/)
        └── dto/
              └── ApiResponse.java           (unified response wrapper)
```

### 5.4 Domain Model Enrichment — LOW PRIORITY

**Current State**: Most entities are **anemic** — data holders with getters/setters only. Business logic lives in large service classes.

**Recommendation**: Move domain logic into entities where it belongs:

```java
// BEFORE: Logic in LeaseService (procedural)
public void activateLease(String leaseId) {
    Lease lease = leaseRepository.findById(leaseId).orElseThrow();
    leaseStateMachine.validateTransition(lease.getStatus(), ACTIVE, leaseId);
    lease.setStatus(ACTIVE);
    lease.setActivatedAt(LocalDateTime.now());
    leaseRepository.save(lease);
}

// AFTER: Logic in Lease entity (rich domain model)
public class Lease extends OrganizationScopedEntity {
    public void activate(LeaseStateMachine stateMachine) {
        stateMachine.validateTransition(this.status, ACTIVE, this.id);
        this.status = ACTIVE;
        this.activatedAt = LocalDateTime.now();
        this.raiseEvent(new LeaseActivatedEvent(this));
    }
}
```

**Candidates for domain enrichment**:
- `Lease.activate()`, `Lease.terminate()`, `Lease.renew()`
- `Payment.process()`, `Payment.refund()`
- `RentalApplication.approve()`, `RentalApplication.reject()`
- `Tenant.onboard()`, `Tenant.deactivate()`

### 5.5 Missing Design Patterns — LOW PRIORITY

| Pattern | Where to Apply | Benefit |
|---------|---------------|---------|
| **Specification Pattern** | Complex query filtering in `PropertyService`, `TenantService` (currently uses JPA Specification Executor but with ad-hoc Specification construction) | Reusable, composable query predicates |
| **Chain of Responsibility** | `ScreeningWorkflow` — screening steps could be a chain of validators | Each screening step is independently testable and reorderable |
| **Decorator** | `CacheService` wrapping `PropertyService`, `TenantService` | Transparent caching without modifying service implementations |
| **Strategy** (more) | Report generation in Python services — PDF vs Excel vs CSV renderers | Runtime format selection |
| **Observer** (cross-service) | Kafka event publishing currently done manually — could use Spring `@EventListener` → Kafka bridge pattern | Decouple local events from Kafka publishing |

---

## 6. Backend-Wide Consistency Evaluation

### 6.1 Inconsistency Map

| Aspect | `propertize` | `payroll-service` | `auth-service` | `payment-service` | `employee-service` |
|--------|-------------|-------------------|----------------|-------------------|-------------------|
| Base Entity | `BaseEntity` → `AuditableEntity` → `OrganizationScopedEntity` | Own `BaseEntity` | No base entity (flat) | Own `BaseEntity` → `AuditableEntity` → `OrganizationScopedEntity` | Own `AuditableEntity` |
| Exception Hierarchy | `PropertizeException`, `BusinessException` → 20 subclasses | `ResourceNotFoundException`, `ValidationException` (standalone) | Inline exceptions | `GlobalExceptionHandler` with generic catches | Minimal exception handling |
| Design Patterns | Strategy, Factory, Template Method, CQRS, DDD, State Machine, AOP, Event Sourcing | Strategy, Records | Strategy (PolicyEngine), ABAC evaluators | Gateway interface | Basic layering only |
| Service Interfaces | 2 (`SecurityService`, `ResourceAccessService`) | 0 | 0 | 1 (`PaymentGatewayService`) | 1 (`AuthServiceClient` — Feign) |
| DTO Pattern | Separate `*Request`/`*Response` with `@Builder` | Separate `*Request`/`*Response` | Mixed (some combined) | Separate `*Request`/`*Response` | Separate `*Request`/`*Response` |
| AOP | `SecurityAuditAspect`, `DatabaseConnectivityAspect` | `LoggingAspect` | None | None | None |

### 6.2 Recommendations for Consistency

1. **Unify base entity classes** in `propertize-commons` so all services share the same `BaseEntity` → `AuditableEntity` → `OrganizationScopedEntity` chain.
2. **Unify exception hierarchy** — `propertize-commons` already has `BaseException`, but not all services use it. Mandate `commons.exception.BaseException` everywhere.
3. **Mandate `ApiResponse<T>` wrapper** from commons for all service responses.
4. **Add logging aspects** (`LoggingAspect`) to auth-service, payment-service, and employee-service (currently only propertize and payroll have AOP).
5. **Standardize Python services** with shared `BaseService`, `BaseRepository`, and Pydantic model hierarchy.

---

## 7. Recommended OOP Classes & Responsibilities

### Java — New Classes to Create

| Class | Package | Responsibility | Priority |
|-------|---------|---------------|----------|
| `commons.entity.BaseEntity` | `propertize-commons` | Shared optimistic locking + timestamps for all services | Medium |
| `commons.entity.AuditableEntity` | `propertize-commons` | Shared audit fields (createdBy, updatedBy, soft delete) | Medium |
| `commons.entity.OrganizationScopedEntity` | `propertize-commons` | Shared multi-tenancy base with organizationId validation | Medium |
| `commons.repository.BaseRepository<T,ID>` | `propertize-commons` | Shared `JpaRepository` + `JpaSpecificationExecutor` + `findAllActive()` | Medium |
| `commons.dto.ApiResponse<T>` | `propertize-commons` | Unified API response envelope (success, error, timestamp, correlationId) | Medium |
| `IPropertyService` | `propertize` | Interface for property CRUD, search, metrics | Low |
| `ILeaseService` | `propertize` | Interface for lease lifecycle management | Low |
| `ITenantService` | `propertize` | Interface for tenant CRUD and onboarding | Low |
| `ISearchService` | `propertize` | Interface for universal search (enables DB ↔ Elasticsearch swap) | Low |
| `StateTaxStrategy` | `payroll-service` | Concrete `DeductionStrategy` for state income tax | Low |
| `BenefitsDeductionStrategy` | `payroll-service` | Concrete `DeductionStrategy` for health/dental/vision benefits | Low |
| `GarnishmentStrategy` | `payroll-service` | Concrete `DeductionStrategy` for court-ordered garnishments | Low |

### Python — New Classes to Create

| Class | Module | Responsibility | Priority |
|-------|--------|---------------|----------|
| `BaseRepository` | `shared/base_repository.py` | Encapsulated DB access with connection pooling, query/execute methods | High |
| `BaseService` | `shared/base_service.py` | Abstract service with health check, logging, error handling | High |
| `ReportRenderer` | `shared/renderers.py` | Abstract renderer with `PdfRenderer`, `ExcelRenderer` subclasses | High |
| `FinancialRepository` | `report-service/repositories/` | Financial data queries extracted from `main.py` | High |
| `OccupancyRepository` | `report-service/repositories/` | Occupancy data queries extracted from `main.py` | High |
| `FinancialReportService` | `report-service/services/` | Business logic for financial report generation | High |
| `VendorRepository` | `vendor-matching/repositories/` | Vendor data queries extracted from `matcher.py` | High |
| `VendorMatchingService` | `vendor-matching/services/` | Matching logic with strategy pattern (semantic vs keyword) | High |
| `MatchingStrategy` | `vendor-matching/strategies/` | Abstract matching strategy with `SemanticStrategy`, `KeywordStrategy` | Medium |
| `AnalyticsRepository` | `analytics-worker/repositories/` | Aggregation queries extracted from `aggregate.py` | Medium |

---

## 8. Refactoring Roadmap

### Phase 1: Python OOP Foundation (Priority: HIGH, Effort: 2-3 weeks)

**Goal**: Transform procedural Python services into testable, OOP-structured code.

| Task | Files Affected | Effort |
|------|---------------|--------|
| Create `BaseRepository` in `shared/` | `shared/base_repository.py` | 1 day |
| Create `BaseService` in `shared/` | `shared/base_service.py` | 1 day |
| Refactor `report-service/main.py` → repositories + services + routes | 5+ new files | 3 days |
| Refactor `vendor-matching/matcher.py` → repository + service + strategy | 4+ new files | 2 days |
| Refactor `analytics-worker/aggregate.py` → repository + service | 3+ new files | 1 day |
| Refactor remaining workers (`payment-worker`, `screening-worker`, `document-service`) | 9+ new files | 3 days |
| Add unit tests for new classes | `tests/` directories | 3 days |

### Phase 2: Commons Unification (Priority: MEDIUM, Effort: 1-2 weeks)

**Goal**: Centralize shared OOP constructs to eliminate cross-service duplication.

| Task | Files Affected | Effort |
|------|---------------|--------|
| Move `BaseEntity`, `AuditableEntity`, `OrganizationScopedEntity` to `propertize-commons` | Commons + all 5 Java services | 2 days |
| Move `BaseRepository` to `propertize-commons` | Commons + all 5 Java services | 1 day |
| Create unified `ApiResponse<T>` in commons | Commons + all 5 Java services | 1 day |
| Ensure all services extend `commons.exception.BaseException` | All Java services | 2 days |
| Add `LoggingAspect` to auth-service, payment-service, employee-service | 3 services | 1 day |
| Update all POMs to depend on `propertize-commons` for entity/exception/dto | All Java POMs | 1 day |

### Phase 3: Service Layer Interfaces (Priority: MEDIUM, Effort: 1 week)

**Goal**: Add interfaces for key services to improve testability and enable future flexibility.

| Task | Files Affected | Effort |
|------|---------------|--------|
| Extract `IPropertyService` from `PropertyService` | `propertize` | 0.5 day |
| Extract `ILeaseService` from `LeaseService` | `propertize` | 0.5 day |
| Extract `ITenantService` from `TenantService` | `propertize` | 0.5 day |
| Extract `ISearchService` from `SearchService` | `propertize` | 0.5 day |
| Extract `IAuditLogService` from `AuditLogService` | `propertize` | 0.5 day |
| Add missing `DeductionStrategy` implementations | `payroll-service` | 1 day |
| Update Spring wiring to use interfaces | All affected services | 1 day |

### Phase 4: Domain Model Enrichment (Priority: LOW, Effort: 2-3 weeks)

**Goal**: Move business logic from service classes into domain entities.

| Task | Files Affected | Effort |
|------|---------------|--------|
| Add `Lease.activate()`, `terminate()`, `renew()` domain methods | `Lease.java`, `LeaseService.java` | 2 days |
| Add `Payment.process()`, `refund()`, `validate()` domain methods | `Payment.java`, `PaymentService.java` | 2 days |
| Add `RentalApplication.approve()`, `reject()`, `submit()` domain methods | `RentalApplication.java`, `RentalApplicationService.java` | 2 days |
| Add `Tenant.onboard()`, `deactivate()`, `updateProfile()` domain methods | `Tenant.java`, `TenantService.java` | 2 days |
| Integrate `AggregateRoot` event publishing into enriched entities | Domain entities | 3 days |
| Comprehensive test coverage for new domain methods | Test files | 3 days |

---

## Appendix: Key File References

| Pattern / Concept | Primary File(s) |
|-------------------|-----------------|
| Entity Inheritance | `propertize/entity/base/BaseEntity.java`, `AuditableEntity.java`, `OrganizationScopedEntity.java` |
| Strategy Pattern | `payroll-service/.../calculation/DeductionStrategy.java`, `propertize/.../notification/NotificationSender.java` |
| Factory Pattern | `propertize/.../factory/NotificationFactory.java`, `NotificationSenderFactory.java` |
| Template Method | `propertize/.../services/domain/BaseDashboardService.java` |
| CQRS | `propertize/.../cqrs/Command.java`, `CommandHandler.java`, `CqrsDispatcher.java` |
| DDD / Event Sourcing | `propertize/.../domain/common/AggregateRoot.java`, `DomainEvent.java`, `eventsourcing/StoredEvent.java` |
| State Machine | `propertize/.../statemachine/LeaseStateMachine.java`, `RentalApplicationStateMachine.java` |
| AOP | `propertize/.../aspect/SecurityAuditAspect.java`, `payroll-service/.../aspect/LoggingAspect.java` |
| Policy Engine | `auth-service/.../rbac/engine/PolicyEngine.java`, `DefaultPolicyEngine.java` |
| Value Objects | `propertize/.../entity/embedded/Address.java` (+ 65 others) |
| Exception Hierarchy | `propertize/.../exception/BusinessException.java`, `commons/.../exception/BaseException.java` |
| Repository Base | `propertize/.../repository/BaseRepository.java` |
| Java 21 Records | `payroll-service/.../calculation/PayrollContext.java`, `PayrollResult.java` |
| Python (procedural) | `python-services/report-service/main.py`, `vendor-matching/matcher.py` |
| Frontend Class | `propertize-front-end/src/services/httpClient.ts` (`HttpApiError`) |

