# Propertize — Technical Concepts & Interview Preparation Guide

> **Purpose:** A deep-dive reference covering every significant design decision, pattern, and
> implementation in the Propertize platform. Use this document to prepare for technical interviews
> or to onboard new engineers quickly.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Rate Limiting](#2-rate-limiting)
3. [Caching Strategy](#3-caching-strategy)
4. [JWT Authentication & Token Lifecycle](#4-jwt-authentication--token-lifecycle)
5. [RBAC — Role-Based Access Control](#5-rbac--role-based-access-control)
6. [Circuit Breaker & Resilience Patterns](#6-circuit-breaker--resilience-patterns)
7. [Event-Driven Architecture (Kafka)](#7-event-driven-architecture-kafka)
8. [Service Discovery (Eureka)](#8-service-discovery-eureka)
9. [Inter-Service Communication (OpenFeign)](#9-inter-service-communication-openfeign)
10. [Security Headers & Defense-in-Depth](#10-security-headers--defense-in-depth)
11. [Multi-Tenancy](#11-multi-tenancy)
12. [Database Design Patterns](#12-database-design-patterns)
13. [Zero-Trust Network Model](#13-zero-trust-network-model)
14. [Design Patterns Used](#14-design-patterns-used)
15. [Performance & Scalability Decisions](#15-performance--scalability-decisions)
16. [Python Microservices Layer](#16-python-microservices-layer)
17. [Common Interview Q&A](#17-common-interview-qa)

---

## 1. Architecture Overview

```
Browser / Mobile
      │
      ▼
 API Gateway :8080  ←── Single entry point. Validates JWT, enforces RBAC,
      │                   applies rate limiting, adds X-User-* headers.
      ├── Auth Service :8081       (JWT issuance, RBAC engine)
      ├── Propertize Core :8082    (properties, leases, tenants)
      ├── Employee Service :8083   (employees, departments, attendance)
      ├── Payment Service :8084    (invoices, billing, subscriptions)
      ├── Payroll Service :8085    (payroll runs, timesheets)
      ├── Report Service :8090     (Python/FastAPI, PDF reports)
      ├── Vendor Matching :8091    (Python/ML, vendor search)
      ├── Document Service :8092   (Python/FastAPI, MinIO)
      └── Search Reranker :8093   (Python/ML, semantic search)

Infrastructure
  PostgreSQL 16  (shared DB — each service owns its tables)
  MongoDB 7      (audit trail, event log)
  Redis 7        (token blacklist, permission cache, sessions)
  Kafka          (async events between services)
  MinIO          (object storage for documents)
  Eureka :8761   (service registry, health monitoring)
```

**Key architectural principles:**
- **Gateway-first auth** — JWT validated once at the gateway; downstream services consume
  trusted `X-User-*` headers only.
- **Event-driven decoupling** — Services communicate asynchronously via Kafka topics; no
  direct DB sharing.
- **Single PostgreSQL instance** — reduces ops overhead; each service manages its own tables
  via Hibernate DDL.
- **Polyglot persistence** — Postgres for relational data, MongoDB for event/audit, Redis for
  ephemeral state.

---

## 2. Rate Limiting

### Location
`api-gateway/.../filter/RateLimitingFilter.java`

### Algorithm: Token Bucket
The gateway implements a **Token Bucket** algorithm — the industry standard for smooth,
burst-tolerant rate limiting.

```
                 ┌──────────────────────────────┐
Tokens refill ──►│  RateLimitBucket             │
  gradually      │  tokens: AtomicInteger        │
                 │  burstLimit = limit × 1.5     │──► tryConsume() → true / false
                 │  window: 60 seconds           │
                 └──────────────────────────────┘
```

### How It Works (Step by Step)

1. **Key selection** (priority order):
   - `user:{userId}:org:{orgId}` — authenticated, multi-tenant request (most specific)
   - `user:{userId}` — authenticated, no org context
   - `ip:{clientIp}` — anonymous request (extracted from `X-Forwarded-For` → `X-Real-IP` → socket)

2. **Limit tiers** (configurable via `application.yml`):

   | Tier | Default RPM | Use Case |
   |------|------------|---------|
   | Auth endpoints (`/api/v1/auth/**`) | 10 | Login, token refresh — brute-force protection |
   | Authenticated users | 120 | Normal API usage |
   | Anonymous / IP | 60 | Public endpoints |

3. **Burst allowance** — `burstLimit = limit × 1.5` — allows short bursts without
   immediately throttling (e.g., page load with 15 parallel requests).

4. **Token refill** — Two modes:
   - **Window reset**: if `elapsed >= 60s`, tokens reset to `burstLimit`.
   - **Gradual refill**: tokens added proportionally to elapsed time — prevents thundering herd.

5. **CAS (Compare-And-Swap) safety** — `tokens.compareAndSet()` ensures thread-safety without locks.

6. **Response headers** added on every request:
   ```
   X-RateLimit-Remaining: 47
   X-RateLimit-Limit: 60
   X-RateLimit-Reset: 23          ← seconds until window resets
   Retry-After: 23                ← only on 429 responses
   ```

7. **HTTP 429** returned with JSON body when bucket is empty.

8. **Cleanup** — `ConcurrentHashMap` entries with `lastRefillTime > 5 min` are purged every minute,
   preventing memory leaks in high-traffic scenarios.

### Filter Ordering
`getOrder()` returns **`-200`** — runs **before** `JwtAuthenticationFilter` (`-100`) so anonymous
requests are rate-limited before auth processing.

### Interview Talking Points
- **Why Token Bucket over Fixed Window?** Fixed window has boundary issues (2× burst at window
  edges). Token bucket smooths traffic naturally.
- **Thread safety without locks?** `AtomicInteger` + CAS loop — lock-free, high throughput.
- **Why in-memory (not Redis)?** Gateway is a single instance. Redis adds ~2ms latency per
  request; in-memory is sub-millisecond. If horizontal scaling is needed, the key design is
  ready for Redis migration.

---

## 3. Caching Strategy

### Three-Level Architecture

```
Request
  │
  ▼
L1: Caffeine (in-process, sub-millisecond)
  │  miss
  ▼
L2: Redis (distributed, ~1-2ms)
  │  miss
  ▼
L3: Database / Auth Service call (~5-50ms)
```

### Gateway Cache (`api-gateway/.../config/CacheConfig.java`)
Implements `CachingConfigurer` with named cache regions:

| Cache Region | TTL | Capacity | Purpose |
|---|---|---|---|
| `tokenValidation` | 4 min | 10,000 | Validated JWT results |
| `serviceTokenValidation` | 4 min | 5,000 | Service-to-service tokens |
| `rbacPermissions` | 10 min | 50,000 | User permission sets |
| `userRoles` | 10 min | 10,000 | Role assignments |
| `blacklist` | 15 min | 5,000 | Invalidated token JTIs |
| `rateLimitBuckets` | 1 min | 20,000 | Rate limit state |

**Custom error handler** — cache misses/errors log warnings but never fail the request.

**Custom key generator** — method name + parameters → deterministic cache key.

**Removal listener** — logs `EXPIRED` vs `SIZE` eviction for observability.

### Payroll Service Cache (`payroll-service/.../config/CacheConfig.java`)
Implements a **two-level `CompositeCacheManager`**:

```java
CompositeCacheManager composite = new CompositeCacheManager();
composite.setCacheManagers(List.of(caffeineCacheManager, redisCacheManager));
composite.setFallbackToNoOpCache(false);
```

- **L1 Caffeine**: 500 entries, 5-minute TTL — hot-path payroll lookups.
- **L2 Redis**: 1-hour TTL, `GenericJackson2JsonRedisSerializer` — cross-instance consistency
  after Kafka sync events.

**Cache invalidation via Kafka**: `EmployeeEventConsumer` annotates the listener with
`@CacheEvict(value = "employees", key = "#event.employeeId")` — when an employee event
arrives, the local cache is automatically purged and the next read fetches fresh data.

### Employee Service Cache (`employee-service/.../config/CacheConfig.java`)
Single-level Caffeine with multiple named regions:

| Region | TTL write/access | Capacity | Notes |
|---|---|---|---|
| `employees` | 10 min / 5 min | 5,000 | Dual expiry policy |
| `departments` | — | — | Shared Caffeine config |
| `attendance` | — | — | High-volume read cache |
| `organizationStaff` | — | — | Org-scoped queries |

### Auth Service Cache (`auth-service/.../config/CacheConfig.java`)
`ConcurrentMapCacheManager` with `permissions` and `userPermissions` regions — lightweight,
no expiry (permissions change infrequently and are refreshed on login).

### Spring Annotations Used
```java
@Cacheable("employees")           // Read-through
@CachePut("employees")            // Write-through  
@CacheEvict("employees")          // Cache invalidation
@Caching(evict = {...}, put = {}) // Composite operations
```

### Interview Talking Points
- **Why Caffeine over Ehcache?** Maven 3.8+ blocks HTTP repositories in Docker; Ehcache pulls
  from `http://maven.java.net`. Caffeine is pure Maven Central.
- **Why two-level cache in payroll?** L1 (in-process) gives sub-millisecond payroll calculation
  reads; L2 (Redis) keeps multiple service instances consistent after Kafka events.
- **Cache TTL for JWT tokens is 4 min, but tokens expire in 15 min — why?** Security buffer.
  If a token is revoked and blacklisted, the 4-minute TTL ensures the cache entry ages out before
  the window where the blacklist entry could be missed.

---

## 4. JWT Authentication & Token Lifecycle

### Token Types
| Type | TTL | Purpose |
|---|---|---|
| Access Token | 15 min | API authorization |
| Refresh Token | 7 days | Obtain new access tokens |

### Signing Algorithm: RS256 (RSA + SHA-256)
- **Asymmetric key pair** generated with `generate-rsa-keys.sh` in `auth-service/keys/`.
- Auth service holds the **private key** (signs tokens).
- All other services hold the **public key** (verify tokens only) — key distributed via mounted volume.
- Rationale: even if a downstream service is compromised, it cannot forge tokens.

### Authentication Flow (Gateway)

```
Client Request
  │
  ▼
[1] RateLimitingFilter (order -200)
  │
  ▼
[2] JwtAuthenticationFilter (order -100)
    ├─ Extract Bearer token from Authorization header
    ├─ Validate JWT signature (RS256 public key) via EnhancedJwtTokenProvider (~1ms)
    ├─ Validate claims: exp, iss, aud (~0.5ms)
    ├─ Check JTI against Redis blacklist (~2ms)
    ├─ Load permissions from Redis (perms:jti:{jti}) (~1-2ms)
    └─ Inject headers: X-User-Id, X-Username, X-Email, X-Organization-Id,
                       X-Roles, X-Permissions, X-Primary-Role, X-Correlation-Id
  │
  ▼
[3] RbacAuthorizationFilter
    └─ Match request path + method → required permission → check X-Roles / X-Permissions
  │
  ▼
[4] SecurityHeadersFilter — add OWASP headers
  │
  ▼
  Downstream Service (trusts X-User-* headers, reads via TrustedGatewayHeaderFilter)
```

**Total overhead target: < 5ms** (measured at gateway level).

### Token Blacklist (Logout / Revocation)

Implemented in two places:
- **Auth Service** (`auth-service/.../service/TokenBlacklistService.java`) — canonical Redis store
- **Gateway** (`api-gateway/.../service/TokenBlacklistService.java`) — reactive Redis with in-memory fallback

Operations:
| Operation | Redis Key | TTL |
|---|---|---|
| Blacklist access token | `blacklist:{SHA-256(token)}` | Token's remaining TTL |
| Blacklist by JTI | `blacklist:jti:{jti}` | Token's remaining TTL |
| Track refresh token | `refresh:{SHA-256(token)}` | 7 days |
| Mark refresh as used | `used:{SHA-256(token)}` | 7 days |
| Session termination | `session:{sessionId}` | Session TTL |

**Replay attack prevention** — `isRefreshTokenUsed()` detects rotation attacks where a stolen
refresh token is used after the legitimate client has already rotated it.

### Permissions in Redis (not in JWT)
Permissions are **not embedded** in the JWT payload — they are stored in Redis under
`perms:jti:{jti}` at login time. The gateway reads them per-request.

**Why?** Embedding large permission sets caused `HTTP 431 Request Header Fields Too Large`
errors when the gateway forwarded the `X-Permissions` header. Storing in Redis keeps the JWT
small and the header within browser limits.

### Interview Talking Points
- **Why RS256 instead of HS256?** HS256 uses a shared secret — every service that validates
  tokens must know the secret. RS256 is asymmetric; services only need the public key.
- **How does logout work?** JTI (JWT ID) is added to Redis blacklist. Every request checks
  the blacklist before processing. TTL matches the token's remaining validity window.
- **What happens when Redis is down?** Gateway `TokenBlacklistService` has an in-memory fallback
  (`ConcurrentHashMap`). Existing authenticated sessions continue; blacklist is soft-degraded.

---

## 5. RBAC — Role-Based Access Control

### Architecture
```
rbac.yml (source of truth)
    │
    ├── RbacConfig.java        — @ConfigurationProperties binding
    ├── RbacSeederService.java — seeds rbac.yml roles to rbac_roles DB table on startup
    ├── RbacService.java       — runtime permission resolution, inheritance expansion
    └── RbacAuthorizationFilter.java — gateway-level enforcement
```

### Role Hierarchy (5 Scope Levels)
| Level | Scope | Examples |
|---|---|---|
| 1000 | Platform | `PLATFORM_OVERSIGHT` |
| 800 | Portfolio | `PORTFOLIO_MANAGER` |
| 500 | Organization | `PROPERTY_MANAGER`, `ORG_ADMIN` |
| 300 | Team | `LEASING_AGENT`, `MAINTENANCE_SUPERVISOR` |
| 100 | Self | `TENANT`, `VENDOR` |

### Permission Format
```
resource:action
e.g.: tenant:read, property:update, lease:approve
```

### Inheritance & Expansion
- Roles can `inherits` from parent roles — permissions accumulate.
- `getBasePermissionsForRole()` → only directly defined permissions (stored in JWT).
- `getPermissionsForRole()` → full expanded set (used for runtime checks).
- `explicitDenials` list overrides inherited permissions (deny-wins).

### How Gateway Enforces RBAC

`RbacAuthorizationFilter` maintains an `endpointPermissions` map:
```
/api/v1/tenants/** + GET  → tenant:read
/api/v1/tenants/** + POST → tenant:create
...
```

For each request:
1. Extract `X-Roles` header.
2. Expand roles to permissions (with `PermissionCacheService` — Caffeine backed).
3. Match path + method to required permission.
4. Allow or return HTTP 403.

### Custom Roles
Organizations can define custom roles (`CustomRoleService`) that extend base system roles.
Stored in `user_custom_role_assignments` table — org-scoped, runtime-composable.

### Interview Talking Points
- **Why YAML-based RBAC?** Version-controlled, human-readable, no code changes for permission
  adjustments. Seeded to DB on startup for runtime querying.
- **Why not Spring's `@PreAuthorize`?** Gateway-level enforcement means we can't use Spring
  Security annotations on controllers — the gateway doesn't share the Spring Security context
  with downstream services. Gateway enforces; services trust headers.
- **Deny-wins semantics?** `explicitDenials` ensure certain permissions can never be inherited
  regardless of role composition — critical for compliance use cases.

---

## 6. Circuit Breaker & Resilience Patterns

### Location
`api-gateway/.../config/CircuitBreakerConfig.java`
`payroll-service/.../client/ResilientEmployeeClient.java`
`api-gateway/.../resilience/AuthServiceCircuitBreaker.java`

### States
```
CLOSED ──(failures > threshold)──► OPEN ──(wait duration)──► HALF-OPEN
   ▲                                                              │
   └──────────────(test calls succeed)──────────────────────────┘
```

### Auth Service Circuit Breaker (strict)
| Parameter | Value | Rationale |
|---|---|---|
| Failure rate threshold | 50% | Auth is critical — open faster |
| Slow call threshold | 80% (>1s) | Auth should be fast |
| Wait in OPEN state | 30s | Time for auth-service to recover |
| Calls in HALF-OPEN | 3 | Cautious re-entry |
| Sliding window | 10 calls | Small window for quick detection |
| Timeout | 2 seconds | Fail fast for auth checks |

### Default Circuit Breaker (lenient)
| Parameter | Value |
|---|---|
| Failure rate threshold | 60% |
| Slow call threshold | 85% (>3s) |
| Wait in OPEN state | 20s |
| Calls in HALF-OPEN | 5 |
| Sliding window | 20 calls |
| Timeout | 5 seconds |

### Payroll → Employee Service (`ResilientEmployeeClient`)
Uses the **Decorator Pattern** — wraps `EmployecraftFeignClient` with Resilience4j annotations:
```java
@CircuitBreaker(name = "employecraft", fallbackMethod = "employeeFallback")
@Retry(name = "employecraft")
public Optional<EmployeeDto> getEmployee(UUID employeeId, String authorization) { ... }

private Optional<EmployeeDto> employeeFallback(..., Throwable cause) {
    log.warn("Circuit breaker OPEN — returning empty for employee={}", employeeId);
    return Optional.empty();
}
```

**Why Optional return?** Callers can decide whether to fail the entire payroll run or continue
with a degraded result — business-level resilience decision separated from infrastructure.

### Feign Client Retry (`FeignClientConfig`)
```
Initial interval: 100ms → Max interval: 1000ms → Max attempts: 3
```
Exponential backoff prevents thundering herd on downstream service recovery.

### Interview Talking Points
- **What's the difference between retry and circuit breaker?** Retry handles transient failures
  (network blip); circuit breaker handles sustained failures (service down). Using both together
  provides layered protection.
- **Why Resilience4j over Hystrix?** Hystrix is in maintenance mode. Resilience4j is thread-safe,
  non-blocking, and integrates natively with Spring Boot Actuator for metrics.
- **What happens when circuit is OPEN and payroll is running?** `employeeFallback()` returns
  `Optional.empty()`. Payroll service uses its local `EmployeeEntity` cache (populated via Kafka)
  as the authoritative source during outages.

---

## 7. Event-Driven Architecture (Kafka)

### Topics & Data Flow

| Topic | Publisher | Consumer(s) | Purpose |
|---|---|---|---|
| `employee-events` | `employee-service` | `payroll-service` | Employee data sync |
| `user-events` | `auth-service` | `propertize` | User profile sync |
| `screening.initiated` | `propertize` | `screening-worker` (Python) | Rental application processing |
| `screening.completed` | `screening-worker` (Python) | `propertize` | Risk score delivery |
| `payment-events` | `payment-service` | `propertize` | Audit, notifications |
| `audit-events` | any service | MongoDB sink | Immutable audit trail |
| `analytics-events` | `propertize` | `analytics-worker` | Usage analytics |

### Producer Configuration (Idempotent)
```java
config.put(ProducerConfig.ACKS_CONFIG, "all");          // All replicas must ack
config.put(ProducerConfig.RETRIES_CONFIG, 3);           // Retry on transient error
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Exactly-once producer
```
`ACKS=all` + `ENABLE_IDEMPOTENCE=true` → at-least-once delivery with duplicate prevention.

### Cache Invalidation via Kafka
```java
@KafkaListener(topics = "employee-events", groupId = "payroll-service")
@Transactional
@CacheEvict(value = "employees", key = "#event.employeeId")
public void handleEmployeeEvent(EmployeeEvent event) { ... }
```
**Pattern**: Kafka event triggers both DB upsert AND cache eviction atomically (via `@Transactional`).

### Python Kafka Consumer (Screening Worker)
```python
consumer = KafkaConsumer(
    "screening.initiated",
    group_id="screening-worker",
    auto_offset_reset="earliest",
    enable_auto_commit=False,  # Manual commit for at-least-once processing
)

for message in consumer:
    try:
        process_event(message.value, producer)
        consumer.commit()          # Only commit on success
    except Exception as e:
        pass                       # Message reprocessed on restart (no dead-letter yet)
```

### Topic Partitioning
Auth service creates topics with **3 partitions, 1 replica** — allows horizontal consumer
scaling to 3 parallel workers while maintaining order per partition.

### Interview Talking Points
- **Why Kafka over REST for employee sync?** Decoupling — payroll-service doesn't need
  employee-service to be running. Events are durable. Multiple consumers can subscribe.
- **How do you ensure no duplicate processing?** `ENABLE_IDEMPOTENCE_CONFIG=true` on producer.
  Consumer uses manual commit + idempotent upsert on the DB side.
- **What is the outbox pattern and do you use it?** Not explicitly — `@Transactional` on the
  publisher service method ensures the DB write and Kafka send happen together, but Kafka
  send is not part of the DB transaction. For full transactional outbox, a CDC tool (Debezium)
  would be added.

---

## 8. Service Discovery (Eureka)

### How It Works
1. Every Java service registers on startup with `@EnableDiscoveryClient`.
2. Heartbeat every 5 seconds; deregistered after 90s missed heartbeats.
3. API Gateway can route via `lb://service-name` for load-balanced calls.
4. Self-preservation **disabled** in dev (prevents stale registrations from blocking updates).

### Custom JSON Endpoint
Standard `/eureka/apps` returns XML. The `RegistryInfoController` exposes:
- `GET /registry/apps` — JSON list of all registered services
- `GET /registry/health` — aggregate instance up/down counts

### Startup Order (Docker Compose)
```
postgres (healthy)
    │
    ▼
service-registry (healthy)
    │
    ├── auth-service (healthy)
    │       │
    │       ▼
    │   api-gateway (starts last)
    │
    └── propertize, employee-service, payment-service, payroll-service
```

---

## 9. Inter-Service Communication (OpenFeign)

### Pattern: Feign Interface + Resilient Decorator
```
Controller
    │ calls
    ▼
ResilientEmployeeClient   ← Decorator: @CircuitBreaker + @Retry
    │ delegates to
    ▼
EmployecraftFeignClient   ← @FeignClient: HTTP contract
    │ HTTP/REST
    ▼
employee-service
```

### Feign Client Configuration
```java
@FeignClient(
    name = "employecraft-service",
    url = "${employecraft.api.url}",     // URL injected — Eureka lb:// alternative
    configuration = FeignClientConfig.class
)
```

| Config | Value | Purpose |
|---|---|---|
| Connect timeout | 5s / 5000ms | Fail fast on network issues |
| Read timeout | 30s / 10s | Allow slow response for heavy queries |
| Logger level | `BASIC` | Log request/response lines only |
| Retryer | 100ms → 1000ms, 3 attempts | Exponential backoff |

### Error Decoder
`FeignErrorDecoder` translates HTTP status codes → typed exceptions:
```
400 → BadRequestException
401 → UnauthorizedException
403 → ForbiddenException
404 → NotFoundException
503 → ServiceUnavailableException
```
Typed exceptions allow service-specific handling without `instanceof` checks on `FeignException`.

---

## 10. Security Headers & Defense-in-Depth

### Gateway Layer (`SecurityHeadersFilter`)
Added to **every response** (OWASP recommendations):

| Header | Value | Purpose |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | Prevent MIME sniffing |
| `X-Frame-Options` | `DENY` | Prevent clickjacking |
| `X-XSS-Protection` | `1; mode=block` | Legacy XSS filter |
| `Strict-Transport-Security` | `max-age=31536000` | Force HTTPS |
| `Content-Security-Policy` | `default-src 'self'` | Restrict resource loading |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Control referrer info |
| `Permissions-Policy` | `camera=(), microphone=()` | Disable browser APIs |
| `Cache-Control` | `no-store, no-cache` | Prevent caching auth responses |
| `Cross-Origin-Opener-Policy` | `same-origin` | Prevent cross-origin window attacks |

### Service Layer (`employee-service/.../SecurityHeadersFilter`)
Defense-in-depth: headers also set at service level in case of direct access bypass of gateway.

### CORS Configuration
| Layer | Allowed Origins |
|---|---|
| Gateway | All origins (`*`) — dev; restrict in prod |
| Auth Service | `frontendUrl` + `localhost:3000` |
| Employee / Payroll | `localhost:3000`, `localhost:8080` |

### Cookie Stripping
Gateway strips the `Cookie` header before forwarding requests — prevents `HTTP 431 Request
Header Fields Too Large` errors from large cookie payloads.

---

## 11. Multi-Tenancy

### Organization Isolation
- Every resource is scoped to an `organizationId` (UUID).
- JWT contains `organizationId` claim → gateway forwards as `X-Organization-Id`.
- Rate limiting key: `user:{userId}:org:{orgId}` — per-org limits.
- RBAC check: `X-Organization-Id` header required for all protected endpoints (missing → 403).

### Org Switching
Auth service supports `POST /api/v1/auth/org-switch` — issues a new JWT scoped to a
different organization without re-authentication.

### Custom Roles per Org
`CustomRoleService` allows each organization to define custom roles that extend system roles.
`UserCustomRoleAssignment` maps users to these org-scoped custom roles.

---

## 12. Database Design Patterns

### Single Shared PostgreSQL
All services use one PostgreSQL instance (`propertize_db`). Each service manages its own
tables via Hibernate `ddl-auto: update`.

**Trade-off** — Reduces operational complexity (one DB to backup/monitor) at the cost of
potential table namespace collisions. Mitigated by service-specific table prefixes.

### Entity Base Classes
```java
BaseEntity        → id (Long, IDENTITY), createdAt, updatedAt
AuditableEntity   → extends BaseEntity + createdBy, updatedBy, deletedAt (soft delete)
```

### @Embedded Value Objects
Embeddable classes encapsulate related fields (e.g., `CompensationDetails`, `AddressInfo`).

**Critical rule**: If `@Embeddable` maps column `ytd_deductions`, the owning `@Entity` must
NOT also declare a field mapping to `ytd_deductions` → `DuplicateMappingException`.

### UUID vs Long IDs
- `Long` (`IDENTITY`) — for auto-increment (employees, payments, payroll runs).
- `UUID` — for distributed IDs where predictable sequences are a security risk (organizations, tenants).
- FK columns must match their referenced PK type — mixing UUID and Long causes type mismatch errors.

### Flyway Migrations
Auth service uses Flyway for versioned schema migrations (`V*.sql` in `db/migration/`).
Other services rely on Hibernate DDL for dev/docker; `validate` in prod.

---

## 13. Zero-Trust Network Model

### Principle
Even though the API Gateway validates JWTs, every downstream service independently verifies
the JWT signature using the Auth Service's RS256 public key.

### Implementation (`ZeroTrustJwtValidationFilter` in employee-service)
```java
@Order(Ordered.HIGHEST_PRECEDENCE + 10)  // Runs BEFORE TrustedGatewayHeaderFilter
public class ZeroTrustJwtValidationFilter extends OncePerRequestFilter {
    // Loads public key from PEM file on startup
    // Validates Bearer token on every non-public request
    // Stores Claims in request attributes for downstream use
}
```

**Defense scenario**: If an attacker bypasses the gateway (internal network access, misconfigured
proxy), the service still validates the token independently.

**Graceful disable**: If `security.jwt.public-key-path` is not configured, the filter logs a
warning and disables itself — allows development without the key setup overhead.

---

## 14. Design Patterns Used

| Pattern | Where | Purpose |
|---|---|---|
| **API Gateway** | `api-gateway` | Single entry point, cross-cutting concerns |
| **Chain of Responsibility** | Filter chain (RateLimit → JWT → RBAC → SecurityHeaders) | Ordered processing pipeline |
| **Token Bucket** | `RateLimitingFilter` | Smooth rate limiting with burst tolerance |
| **Decorator** | `ResilientEmployeeClient` | Adds resilience to Feign client without changing interface |
| **Circuit Breaker** | `ReactiveResilience4JCircuitBreakerFactory` | Prevent cascading failures |
| **Factory Method** | `CacheConfig` (per-region Caffeine specs) | Configurable cache construction |
| **Composite** | `CompositeCacheManager` | L1+L2 cache with fallthrough |
| **Anti-Corruption Layer** | `AuthServiceAntiCorruptionLayer` (employee-service) | Isolates external domain model |
| **Observer / Event** | Kafka publishers/consumers | Loose coupling between services |
| **Builder** | All DTOs (`@Builder`) | Readable, immutable object construction |
| **Strategy** | `determineLimit()` / `determineRateLimitKey()` | Interchangeable algorithms |
| **Repository** | `JpaRepository` implementations | Data access abstraction |
| **Value Object** | `@Embeddable` classes | Encapsulate related fields |
| **Saga (implicit)** | Screening flow (Kafka choreography) | Distributed transaction coordination |

---

## 15. Performance & Scalability Decisions

### Sub-Millisecond Authentication
- JWT validated locally at gateway (no Auth Service call) using cached public key.
- Caffeine L1 cache for token validation — `~0.1ms` cache hit vs `~50ms` Auth Service call.
- Total auth overhead: **< 5ms** target.

### Permission Storage in Redis (not JWT)
- JWT stays small (no `HTTP 431` errors).
- Permissions fetched from Redis in `~1-2ms`.
- Permissions updated immediately on role change (no token re-issue needed).

### Reactive Gateway
- Spring Cloud Gateway uses **WebFlux (Project Reactor)** — non-blocking, event-loop based.
- `Mono<Void>` filter chain — no thread-per-request overhead.
- `ConcurrentHashMap` + `AtomicInteger` for lock-free rate limiting.

### Async Event Processing
- `CompletableFuture<SendResult>` for Kafka sends — non-blocking producer.
- `@EnableAsync` on payment-service — long-running payment operations don't block API threads.
- `@EnableScheduling` on payment-service — APScheduler-style cron jobs.

### Connection Pooling
- Python services use `psycopg2` connection pool (shared `db.py` module).
- HikariCP (Spring Boot default) for Java services.

---

## 16. Python Microservices Layer

### Services
| Service | Port | Technology | Purpose |
|---|---|---|---|
| `report-service` | 8090 | FastAPI + pandas | PDF/Excel report generation |
| `vendor-matching` | 8091 | FastAPI + ML (HuggingFace) | Smart vendor matching |
| `document-service` | 8092 | FastAPI + MinIO | Document upload/retrieval |
| `search-reranker` | 8093 | FastAPI + ML | Semantic search reranking |

### Key Conventions
- **No ORM** — raw SQL via `psycopg2` + `pandas.read_sql()` for data-heavy operations.
- **Health endpoint**: `GET /health → {"status": "ok"}` — required on all services.
- **ML model cache**: `HF_HOME=/tmp/.cache` — writable in Docker without root.
- Shared utilities via symlinked `shared/` directory (`db.py`, `config.py`).

### Screening Worker (Python Kafka Consumer)
```
Kafka: screening.initiated
    │
    ▼
pipeline.py → fetch_application_data() → score tenant → save_screening_result()
    │
    ▼
Kafka: screening.completed
```

---

## 17. Common Interview Q&A

**Q: How does a request flow from browser to database?**
A: Browser → API Gateway (JWT validation, rate limit, RBAC, header injection) → downstream
   service (reads X-User-* headers via TrustedGatewayHeaderFilter, processes business logic,
   queries PostgreSQL) → response.

**Q: How do you handle service failures gracefully?**
A: Three layers: (1) Resilience4j circuit breaker prevents cascading failures. (2) Feign retry
   with exponential backoff handles transient errors. (3) Fallback methods return safe defaults
   (e.g., `Optional.empty()` for employee lookups, allowing payroll to use local cache).

**Q: How do you ensure data consistency across services?**
A: Kafka event-driven sync with idempotent producers and at-least-once delivery. `@CacheEvict`
   on Kafka listeners ensures stale data is purged. For critical flows, services maintain local
   shadow copies (employee data in payroll-service) updated via events.

**Q: How do you prevent brute-force attacks on the login endpoint?**
A: Three mechanisms: (1) Rate limiting: 10 RPM on `/api/v1/auth/**`. (2) BCrypt with cost
   factor 12 for password hashing. (3) Account lockout after N failed attempts (auth-service
   configurable).

**Q: How does multi-tenancy work?**
A: `organizationId` is embedded in JWT → forwarded as `X-Organization-Id`. Every query
   filters by this ID. RBAC rules are org-scoped. Custom roles are per-organization.

**Q: What is the token rotation strategy?**
A: Access tokens expire in 15 min. Refresh tokens (7 days) are tracked in Redis. On refresh,
   the old refresh token is marked `used` (replay attack prevention) and a new pair is issued.
   On logout, both tokens are blacklisted by JTI.

**Q: Why is there a `ZeroTrustJwtValidationFilter` in employee-service if the gateway already validates?**
A: Defense-in-depth. If someone gains internal network access and bypasses the gateway, the
   service itself will reject the forged request. "Never trust, always verify."

**Q: How do you avoid the N+1 query problem?**
A: `@EntityGraph` or `JOIN FETCH` in JPQL for eager loading in specific queries. `@Cacheable`
   for repeated lookups. Kafka-based local caching to avoid cross-service calls in hot paths.

**Q: How do you handle schema migrations?**
A: Auth service uses Flyway for versioned migrations. Other services use `ddl-auto: update`
   in dev and `validate` in production. For prod deployments, Flyway should be adopted
   across all services (documented as a known improvement).

**Q: Why store permissions in Redis instead of JWT?**
A: JWTs with full permission sets caused `HTTP 431` errors (browser header size limits). Redis
   lookup adds ~1-2ms but keeps JWT small and allows immediate permission revocation without
   token expiry.

---

*Document maintained by the Propertize Platform Team.*
*Last updated: April 2026.*

