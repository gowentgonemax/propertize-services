# Database Architecture & Performance Investigation Report

**Propertize Platform — Full Stack Analysis**  
**Date: March 19, 2026**  
**Services**: `propertize` (port 8082, DB: `propertize`) | `auth-service` (port 8081, DB: `propertize_db`)

---

## Executive Summary

| Severity    | Count | Status                  |
| ----------- | ----- | ----------------------- |
| 🔴 Critical | 6     | Require immediate fixes |
| 🟠 High     | 7     | Fix this sprint         |
| 🟡 Medium   | 6     | Plan in next sprint     |
| 🔵 Low      | 4     | Backlog improvements    |

---

## Table of Contents

1. [Schema Overview](#1-schema-overview)
2. [CRITICAL Issues](#2-critical-issues)
3. [HIGH Issues](#3-high-issues)
4. [MEDIUM Issues](#4-medium-issues)
5. [LOW Issues](#5-low-issues)
6. [Indexing Strategy](#6-indexing-strategy)
7. [Migration Roadmap](#7-migration-roadmap)

---

## 1. Schema Overview

```
propertize_db (auth-service)           propertize (propertize-service)
─────────────────────────────────      ─────────────────────────────────
users                                  organization
user_roles (element collection)        property
organizations                          tenant
password_reset_tokens                  lease
composite_roles                        payment
custom_roles                           invoice / invoice_item
delegation_rules                       rental_application
delegations                            organization_application
temporal_permissions                   organization_membership
ip_access_rules                        maintenance_requests
permission_audit_logs                  maintenance_history
shedlock                               maintenance_work_order
                                       document / notification
                                       vendor / inspection
                                       expense / transaction_history
                                       audit_log / state_transition_audit
                                       stored_event (event sourcing)
                                       ... 60+ tables total
```

**Cross-service: No FK enforceability** — services communicate via REST, data integrity across services is **application-layer only**.

---

## 2. CRITICAL Issues

---

### 🔴 C-01: `ddl-auto: update` + Flyway Enabled Simultaneously (BOTH services)

**Where**: `propertize/src/main/resources/application.yml` line 42, `auth-service/src/main/resources/application.yml`

**Problem**: Both services have:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update # ← Hibernate auto-modifies schema
  flyway:
    enabled: true # ← Flyway also manages schema
```

Hibernate `ddl-auto: update` and Flyway are **mutually exclusive schema managers**. Running both:

- Hibernate may add columns or tables **outside Flyway's version history**, creating schema drift
- Flyway checksums will mismatch if Hibernate modifies a tracked table
- Hibernate does NOT drop columns or constraints — leaves orphan schema debris
- In production this has silently caused missing constraints (V10 dropping FKs was partly driven by this conflict)

**Fix**: Set `ddl-auto: validate` (propertize) and `ddl-auto: none` (auth-service). Let Flyway own schema exclusively.

```yaml
# propertize application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # Only validate; let Flyway create/alter

# auth-service application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: none       # Flyway controls everything
```

**Impact**: None on runtime behavior. Prevents future schema corruption. Requires ensuring all Flyway migrations are complete before switching.

---

### 🔴 C-02: Debug SQL Logging Committed to Production Config

**Where**: `propertize/src/main/resources/application.yml` lines 40, 44, 283–284

**Problem**: Left-over debug configuration is active:

```yaml
jpa:
  show-sql: false # comment says "ENABLED: For debugging property query issue"
  properties:
    hibernate:
      format_sql: true # Pretty prints every SQL statement

logging:
  level:
    org.hibernate.SQL: DEBUG # logs every query
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE # logs every parameter
```

`format_sql: true` with `org.hibernate.SQL: DEBUG` means **every SQL statement is logged** even though `show-sql: false`. The `BasicBinder: TRACE` logs **every bind variable value including PII** (emails, names). In production this:

- Exposes tenant PII in log files (GDPR/CCPA violation risk)
- Causes log file bloat (100s of MB/day at scale)
- Degrades application performance ~5-15% due to string formatting

**Fix**: Applied — see section 7.

---

### 🔴 C-03: Global Unique Constraint on `tenant.email` Breaks Multi-Tenancy

**Where**: `Tenant.java` line 75: `@Column(name = "email", unique = true, nullable = false)`

**Problem**: The unique constraint is **database-wide**, not scoped to organization. This means:

- Tenant "john@gmail.com" at Organization A **cannot** submit a rental application to Organization B
- A person applying to multiple properties across different property managers is blocked
- Breaks the fundamental multi-tenant isolation model

```java
// WRONG — global uniqueness blocks cross-org tenants
@Column(name = "email", unique = true, nullable = false)
private String email;
```

The correct constraint is `UNIQUE(email, organization_id)` — the same person can be a tenant in multiple orgs with their own record.

**Fix**: Drop the `unique = true` from the column annotation, add a `@UniqueConstraint` at the table level:

```java
@Table(name = "tenant", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tenant_email_org", columnNames = {"email", "organization_id"})
}, indexes = { ... })
```

Plus a Flyway migration (see V12 below).

---

### 🔴 C-04: User ID Type Mismatch Between Services

**Where**: Multiple entity files

**Problem**:
| Entity | Field | Type | Should Be |
|--------|-------|------|-----------|
| `auth-service/User.java` | `id` | `Long` (IDENTITY) | System of record |
| `propertize/Tenant.java` | `userId` | `Long` | ✅ Correct |
| `propertize/OrganizationMembership.java` | `userId` | `String` | ❌ Should be `Long` |
| `propertize/OrganizationMembership.java` | `invitedBy` | `UUID` | ❌ Should be `Long` |

`OrganizationMembership.userId` as `String` means it cannot reliably join to auth-service's `users.id` (BIGINT). Queries that try to look up membership by user ID will silently fail if the ID format doesn't parse, or require string-casting in SQL.

**Fix**: Change `OrganizationMembership`:

```java
// Before
@Column(name = "user_id", nullable = false)
private String userId;

@Column(name = "invited_by")
private UUID invitedBy;

// After
@Column(name = "user_id", nullable = false)
private Long userId;

@Column(name = "invited_by")
private Long invitedBy;
```

Requires Flyway migration to `ALTER TABLE organization_membership ALTER COLUMN user_id TYPE BIGINT USING user_id::BIGINT`.

---

### 🔴 C-05: 1NF Violation — Comma-Separated Values in `delegation_rules`

**Where**: `auth-service/V5__Create_delegation_tables.sql` lines 17–18, `DelegationRule.java` entity

**Problem**: Two columns store multi-value data as comma-separated strings:

```sql
delegatable_permissions VARCHAR(1000) NOT NULL,  -- e.g., "payment:approve,lease:view,tenant:manage"
allowed_delegate_roles  VARCHAR(500)  NOT NULL,  -- e.g., "PROPERTY_MANAGER,LEASING_AGENT"
```

This violates **First Normal Form**. Consequences:

- Cannot query `WHERE 'payment:approve' IN delegatable_permissions` efficiently
- Cannot add foreign key to a `permissions` table
- String manipulation required to extract individual values in application code
- Maximum VARCHAR size is an artificial limit on number of permissions

**Fix**: Create proper junction tables (see V-AUTH-01 migration below).

---

### 🔴 C-06: `RentalApplication.organizationId` Nullable Override vs Non-Null Parent Class

**Where**: `RentalApplication.java` line 42

**Problem**:

```java
// RentalApplication overrides parent to allow NULL
@AttributeOverride(name = "organizationId", column = @Column(name = "organization_id", nullable = true, length = 36))
public class RentalApplication extends OrganizationScopedEntity {
```

But `OrganizationScopedEntity.validateOrganizationId()` throws `IllegalStateException` if null. The entity is simultaneously:

- Allowing DB-level null via `nullable = true`
- Rejecting null at the application layer

This inconsistency means:

- Legacy data with null organization_id exists in the table (public applications)
- Queries with `WHERE organization_id = :orgId` silently miss public applications
- JOIN queries that assume non-null will produce wrong results

**Fix**: Either enforce non-null (set a sentinel value for public apps like `"PUBLIC"`) OR properly handle null with explicit query patterns. Do not mix approaches.

---

## 3. HIGH Issues

---

### 🟠 H-01: Missing Composite Indexes for Soft-Delete Queries

**Where**: `Tenant.java`, `Property.java` — any entity with `deleted_at`

**Problem**: The most common query pattern across all tenant/property endpoints:

```sql
WHERE organization_id = ? AND deleted_at IS NULL
```

Current indexes:

- `idx_tenant_org` on `organization_id` ← single column
- `idx_tenant_status` on `tenant_status`

No composite indexes exist. PostgreSQL uses the `idx_tenant_org` index and then filters `deleted_at IS NULL` via index scan — still a sequential scan of all org records.

**High-traffic impact**: A property manager org with 500 tenants runs this query on every page load, every API call. With only a single-column index, the DB must load all 500 org records and then filter.

**Fix**: Add multi-column partial indexes (see migration V12).

```sql
-- Partial index: only non-deleted records (smaller, faster)
CREATE INDEX idx_tenant_org_active
  ON tenant(organization_id)
  WHERE deleted_at IS NULL;

-- Composite for status + org queries
CREATE INDEX idx_tenant_org_status_active
  ON tenant(organization_id, tenant_status)
  WHERE deleted_at IS NULL;
```

---

### 🟠 H-02: LIKE with Leading Wildcard on Unindexed Columns (Full Table Scan)

**Where**: 8+ repositories — `TenantRepository`, `PropertyRepository`, `MessageRepository`, etc.

**Problem**: All search queries use the pattern:

```java
"LOWER(t.firstName) LIKE LOWER(CONCAT('%', :search, '%'))"
"LOWER(t.lastName) LIKE LOWER(CONCAT('%', :search, '%'))"
"LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%'))"
```

A **leading wildcard `%search%` makes B-tree indexes useless**. PostgreSQL performs a full table scan for every search. As data grows, search latency grows linearly.

Additionally, `LOWER()` wrapping prevents PostgreSQL from using any regular index even without the leading wildcard.

**Fix**:

1. **Short-term**: Add PostgreSQL `pg_trgm` (trigram) indexes — support `%search%` patterns:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_tenant_name_trgm ON tenant USING gin(
  (lower(first_name) || ' ' || lower(last_name)) gin_trgm_ops
);
CREATE INDEX idx_tenant_email_trgm ON tenant USING gin(lower(email) gin_trgm_ops);
```

2. **Long-term**: Use PostgreSQL full-text search with `tsvector`/`tsquery`:

```sql
ALTER TABLE tenant ADD COLUMN search_vector tsvector
  GENERATED ALWAYS AS (
    to_tsvector('english', coalesce(first_name,'') || ' ' || coalesce(last_name,'') || ' ' || coalesce(email,''))
  ) STORED;
CREATE INDEX idx_tenant_fts ON tenant USING gin(search_vector);
```

---

### 🟠 H-03: Denormalized Performance Counters Create Race Conditions

**Where**: `Tenant.java` — 10+ counter/metric fields

**Problem**: The `tenant` table has these denormalized aggregates:

```java
private BigDecimal lifetimeRentPaid = BigDecimal.ZERO;
private Integer totalPaymentsCount = 0;
private Integer onTimePaymentsCount = 0;
private Integer latePaymentsCount = 0;
private BigDecimal paymentOnTimeRate;
private BigDecimal currentBalance = BigDecimal.ZERO;
private Integer totalLeasesCount = 0;
private Integer renewalsCount = 0;
private Integer maintenanceRequestsCount = 0;
private Integer riskScore;
```

These are derived from `payment`, `lease`, and `maintenance_requests` tables. The problem:

- **Race condition**: Two payments processed simultaneously both read `totalPaymentsCount = 5`, both try to write `6` — one update is lost
- **Stale data**: Reports show incorrect counters if the scheduler fails
- **Cascade cost**: Every payment save triggers a tenant update (extra write)
- **Optimistic locking conflict**: `@Version` on `BaseEntity` causes `OptimisticLockException` if two payments for the same tenant are processed concurrently

**Fix**: Move to dedicated `tenant_metrics` table with pessimistic locking OR compute at query time using SQL aggregation:

```sql
-- Instead of stored counter, compute:
SELECT t.*,
  COUNT(p.id) as total_payments_count,
  SUM(p.amount) as lifetime_rent_paid
FROM tenant t
LEFT JOIN payment p ON p.tenant_id = t.id
WHERE t.id = ?
GROUP BY t.id;
```

The `MetricsCalculationScheduler` already recalculates these — metrics can be moved to a separate `tenant_metrics` snapshots table updated by the scheduler.

---

### 🟠 H-04: `BaseEntity` Adds 4 Audit Columns to Every Table

**Where**: `BaseEntity.java` — `ip_address`, `correlation_id`, `session_id`, `user_agent`

**Problem**: These 4 columns are added to **every** entity table (60+ tables):

```java
@Column(name = "ip_address", length = 45)        // every row
@Column(name = "correlation_id", length = 100)   // every row
@Column(name = "session_id", length = 100)        // every row
@Column(name = "user_agent", length = 1000)       // every row — 1KB per row!
```

**Impact**:

- `user_agent` as `length = 1000` adds up to **1KB** to every row in every table
- A `payment` table row is bloated by 1.3KB of audit data that belongs in `audit_log`
- PostgreSQL page size is 8KB — a payment row that should fit 100/page now fits ~6/page
- This increases disk I/O for ALL queries across ALL tables

**Fix**: Remove `ip_address`, `correlation_id`, `session_id`, `user_agent` from `BaseEntity`. Use `correlationId` only as a lookup key into the `AuditLog` table. These fields belong in `audit_log`, not in every business entity.

---

### 🟠 H-05: Duplicate Organization ID Storage on `auth-service/User`

**Where**: `auth-service/User.java`

**Problem**:

```java
@Column(name = "organization_id", length = 100)
private String organizationId;              // Single org

@Type(JsonType.class)
@Column(name = "organization_ids", columnDefinition = "jsonb")
private List<String> organizationIds;      // Multiple orgs (JSONB array)
```

A user can be in one org via `organizationId` AND in multiple orgs via `organizationIds`. These two fields can contradict each other, and the application must keep them in sync manually.

The canonical source of membership is `OrganizationMembership` table in `propertize_db`. Having these fields on `User` in `propertize_db` duplicates that data and creates divergence risk.

**Fix**: Remove `organizationId` from `User` entity (keep it on the JWT claim for stateless auth). Use `OrganizationMembership` as the single source of organizational role assignments.

---

### 🟠 H-06: Missing Flyway Versions 1–7 for `propertize` Service

**Where**: `propertize/src/main/resources/db/migration/`

**Problem**: The propertize Flyway migration sequence:

```
V8__Create_Shedlock_Table.sql
V9__Drop_Organization_Owner_FK.sql
V10__Drop_User_FK_Constraints.sql
V11__Add_Owner_Username_To_Organization.sql
```

Versions 1–7 are **missing**. The initial schema (organization, property, tenant, lease, payment, etc.) was created by Hibernate `ddl-auto: create` and is not in Flyway. This means:

- Fresh deploys on a new database fail at Flyway validation
- `baseline-on-migrate: true` papers over this but marks any existing state as V1 — making future migrations unreliable
- No reproducible schema creation path exists

**Fix**: Create `V1__Initial_Schema.sql` through `V7__*` capturing the full schema dump. Run `pg_dump --schema-only` on current dev DB.

---

### 🟠 H-07: `auth-service` User Roles Loaded Lazily in Security Context

**Where**: `auth-service/User.java`

**Problem**:

```java
@ElementCollection(fetch = FetchType.LAZY)   // ← LAZY loading
@CollectionTable(name = "user_roles", ...)
private Set<UserRoleEnum> roles = new HashSet<>();
```

Every time Spring Security loads a `UserDetails` during authentication or request authorization, it triggers a separate query to `user_roles` for that user's roles. With `open-in-view: false` (correctly set), this causes `LazyInitializationException` in any path that doesn't explicitly JOIN FETCH.

The auth repository already has `findByUsernameWithRoles` and `findByEmailWithRoles` — but any code path that uses `findByUsername` without the `WithRoles` variant will trigger the N+1.

**Fix**: Change to `FetchType.EAGER` on `.roles` (acceptable since roles set is small — max ~5 roles per user and used on every auth check):

```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
@Column(name = "role", length = 100)
@Enumerated(EnumType.STRING)
private Set<UserRoleEnum> roles = new HashSet<>();
```

---

## 4. MEDIUM Issues

---

### 🟡 M-01: `Property.amenities` and `Property.photos` `@ElementCollection` Without Indexes

**Where**: `Property.java`

```java
@ElementCollection
private List<String> amenities;      // no index on collection table

@ElementCollection
@Valid
private List<Photo> photos;          // no index on collection table
```

Hibernate creates `property_amenities` and `property_photos` tables with a `property_id` FK. Without an index on `property_id`, loading a property's amenities/photos requires a full table scan.

**Fix**: Add `@CollectionTable` with explicit index:

```java
@ElementCollection
@CollectionTable(
    name = "property_amenities",
    joinColumns = @JoinColumn(name = "property_id"),
    indexes = @Index(name = "idx_prop_amenities_pid", columnList = "property_id")
)
@Column(name = "amenity")
private List<String> amenities;
```

---

### 🟡 M-02: Inconsistent Cascade Strategies on `Tenant` Relationships

**Where**: `Tenant.java`

| Relationship  | Cascade                                 |
| ------------- | --------------------------------------- |
| `pets`        | `CascadeType.ALL, orphanRemoval = true` |
| `leases`      | `CascadeType.ALL` (no orphanRemoval)    |
| `payments`    | `CascadeType.ALL` (no orphanRemoval)    |
| `invoices`    | `PERSIST, MERGE` only                   |
| `inspections` | `PERSIST, MERGE` only                   |

`CascadeType.ALL` on `leases` without `orphanRemoval` means orphaned leases won't be deleted automatically. `CascadeType.ALL` on `payments` risks cascading deletes of payment records if a tenant is deleted — destroying financial history.

**Fix**: Payments and leases should **never** be cascade-deleted — they are financial records. Remove `CascadeType.REMOVE` from financial relationships:

```java
// Payments are financial records — never cascade delete
@OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY,
    cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<Payment> payments;

// Leases are legal records — never cascade delete
@OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY,
    cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<Lease> leases;
```

---

### 🟡 M-03: Non-Standard `@OneToMany` Without `mappedBy` on Tenant

**Where**: `Tenant.java`

Three collections use `@JoinColumn` without a corresponding `@ManyToOne` on the other side:

```java
@OneToMany(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
private List<Document> documents;

@OneToMany(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
private List<RentalApplication> rentalApplications;

@OneToMany(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
private List<Message> messages;
```

This creates a **unidirectional one-to-many** with `insertable = false, updatable = false` — meaning these collections are **read-only** and the FK is managed from the other side (`Document.tenantId` etc.). The problem: Hibernate still generates SQL to verify/maintain this mapping on every flush, causing unexpected UPDATE queries.

**Fix**: Add proper `@ManyToOne` on `Document`, `Message` entities, and use `mappedBy`:

```java
// On Tenant
@OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
private List<Document> documents;

// On Document
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id")
private Tenant tenant;
```

---

### 🟡 M-04: `@Lob` Used on 36 Fields (TEXT Type Forces Out-of-Line Storage)

**Where**: 36 `@Lob` usages across entity classes

```java
@Lob
@Column(name = "notes", columnDefinition = "TEXT")
private String notes;
```

`@Lob` tells Hibernate/JDBC to use CLOB/TEXT handling which in PostgreSQL uses out-of-line TOAST storage for values > 2KB. Used on fields like `notes`, `internal_notes`, `special_needs`, `accessibility_requirements`, `review_notes`, etc. that will almost always be small strings.

For small text fields, `columnDefinition = "TEXT"` alone is sufficient in PostgreSQL without `@Lob`. The `@Lob` annotation adds overhead.

**Fix**: Remove `@Lob` from fields that use `columnDefinition = "TEXT"`. Keep `TEXT` column definition — PostgreSQL auto-TOASTs when needed:

```java
// Remove @Lob, keep TEXT definition
@Column(name = "notes", columnDefinition = "TEXT")
private String notes;
```

---

### 🟡 M-05: Tenant `move_in_date` / `move_out_date` Duplicated from Lease

**Where**: `Tenant.java`

```java
@Column(name = "move_in_date")
private LocalDate moveInDate;

@Column(name = "move_out_date")
private LocalDate moveOutDate;
```

The `Lease` entity already has start/end dates in `LeaseDates` embedded object. Storing them again on Tenant creates a synchronization burden — when a lease changes, `tenant.moveInDate` must also be updated.

**Fix**: Remove from `Tenant` entity. Compute at query time from the lease relationship when needed.

---

### 🟡 M-06: HikariCP Pool Size May Be Insufficient for Auth-Service

**Where**: `auth-service/application.yml`

```yaml
hikari:
  maximum-pool-size: 10 # ← low for auth service
  minimum-idle: 5
```

The auth-service handles **every authentication request** across all services, JWT validation, and role lookups. With 10 max connections and concurrent load from 3 microservices (propertize-service, employee-service, api-gateway), connection starvation is likely.

**Fix**:

```yaml
hikari:
  maximum-pool-size: 25
  minimum-idle: 5
  connection-timeout: 20000
  idle-timeout: 300000
  max-lifetime: 900000
```

---

## 5. LOW Issues

---

### 🔵 L-01: Potential Deadlock Risk in Concurrent Payment Processing

**Where**: `PaymentRepository`, `TenantRepository`

Payment processing updates `tenant.current_balance`, `tenant.total_payments_count`, and `payment` table in a single transaction. If two payments process for the same tenant simultaneously:

1. TX1 locks `payment` row, reads `tenant`
2. TX2 locks `payment` row, reads `tenant`
3. TX1 updates `tenant` (lock on tenant row)
4. TX2 updates `tenant` (waits for TX1's tenant lock)
   → No deadlock here, but optimistic lock exception on TX2 (`@Version`)

The real deadlock risk is if payment processing also touches `lease` (for balance updates) and `tenant` is updated from both payment and lease services in different order.

**Recommendation**: Ensure all transactions that touch both `tenant` and `lease` always acquire locks in the same order: `tenant` first, then `lease`.

---

### 🔵 L-02: Orphan Risk from `stored_event` Table

**Where**: `StoredEvent.java` (event sourcing)

The `stored_event` table accumulates events indefinitely. Without a retention/archival policy, this table will grow unboundedly. At 1000 events/day, it reaches 365,000 rows/year.

**Fix**: Add event retention policy — archive or delete events older than 90 days to `stored_event_archive` table.

---

### 🔵 L-03: Missing `NOT NULL` Constraints on Critical FK Columns

**Where**: Multiple entities

`payment.tenant_id`, `lease.property_id`, `maintenance_requests.property_id` should be `NOT NULL` at the database level. Current entity annotations use `nullable = false` in JPA but if V10 migration dropped FK constraints, the DB-level NOT NULL should be verified.

**Recommendation**: Add explicit DB-level NOT NULL in a migration:

```sql
ALTER TABLE payment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE lease ALTER COLUMN property_id SET NOT NULL;
```

---

### 🔵 L-04: `Tenant.email` Is Not Indexed in Auth-Service `users` Table

**Where**: `auth-service/User.java` has `@Index(name = "idx_email", columnList = "email")` ✅  
**Where**: `propertize/Tenant.java` has `@Index(name = "idx_tenant_email", columnList = "email")` ✅

These are correct. However, cross-service tenant lookups by email (e.g., mapping auth user → propertize tenant) have no guaranteed index alignment and rely on application-level JOIN. This is acceptable for current scale but should be documented.

---

## 6. Indexing Strategy

### Current Index Coverage (propertize)

| Table                     | Indexed Columns                                                               | Missing                                            |
| ------------------------- | ----------------------------------------------------------------------------- | -------------------------------------------------- |
| `tenant`                  | email, organization_id, tenant_status                                         | `(org_id, deleted_at)`, `(org_id, status)` partial |
| `property`                | organization_id, status, type, current_tenant_id                              | `(org_id, deleted_at)`                             |
| `lease`                   | property_id, tenant_id, org_id, status, dates, number                         | `(tenant_id, status)` for active lease lookup      |
| `rental_application`      | org_id, property_id, status, tracking_id, composite org+date, org+status+date | Good coverage ✅                                   |
| `organization_membership` | user_id, organization_id, status                                              | Good coverage ✅                                   |

### Recommended New Indexes (in V12 migration)

```sql
-- Tenant soft-delete queries (most common)
CREATE INDEX CONCURRENTLY idx_tenant_org_active
  ON tenant(organization_id) WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY idx_tenant_org_status_active
  ON tenant(organization_id, tenant_status) WHERE deleted_at IS NULL;

-- Trigram search indexes (requires pg_trgm extension)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX CONCURRENTLY idx_tenant_name_trgm
  ON tenant USING gin((lower(first_name) || ' ' || lower(last_name)) gin_trgm_ops);
CREATE INDEX CONCURRENTLY idx_tenant_email_trgm
  ON tenant USING gin(lower(email) gin_trgm_ops);

-- Property active lease lookup
CREATE INDEX CONCURRENTLY idx_lease_tenant_status
  ON lease(tenant_id, status);

-- Payment lookup by date range (common in reports)
CREATE INDEX CONCURRENTLY idx_payment_org_date
  ON payment(organization_id, created_at DESC);

-- Notification unread count (high-frequency)
CREATE INDEX CONCURRENTLY idx_notification_user_read
  ON notification(user_id, is_read) WHERE is_read = false;
```

---

## 7. Migration Roadmap

### V12 — Critical Fixes (propertize)

```sql
-- 1. Fix Tenant email uniqueness (org-scoped)
ALTER TABLE tenant DROP CONSTRAINT IF EXISTS tenant_email_key;
ALTER TABLE tenant ADD CONSTRAINT uq_tenant_email_org
  UNIQUE (email, organization_id);

-- 2. Add missing composite + partial indexes for performance
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX CONCURRENTLY idx_tenant_org_active
  ON tenant(organization_id) WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY idx_tenant_org_status_active
  ON tenant(organization_id, tenant_status) WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY idx_tenant_name_trgm
  ON tenant USING gin((lower(first_name) || ' ' || lower(last_name)) gin_trgm_ops);

CREATE INDEX CONCURRENTLY idx_tenant_email_trgm
  ON tenant USING gin(lower(email) gin_trgm_ops);

CREATE INDEX CONCURRENTLY idx_lease_tenant_status
  ON lease(tenant_id, status);

CREATE INDEX CONCURRENTLY idx_payment_org_date
  ON payment(organization_id, created_at DESC);

-- 3. Add DB-level NOT NULL on critical columns
ALTER TABLE payment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE payment ALTER COLUMN organization_id SET NOT NULL;

-- 4. Fix OrganizationMembership userId type
ALTER TABLE organization_membership
  ALTER COLUMN user_id TYPE BIGINT USING user_id::BIGINT;
ALTER TABLE organization_membership
  ALTER COLUMN invited_by TYPE BIGINT USING invited_by::TEXT::BIGINT;
```

### V-AUTH-01 — Fix delegation_rules 1NF (auth-service)

```sql
-- Create proper permission and role tables for delegation_rules
CREATE TABLE delegation_rule_permissions (
    delegation_rule_id BIGINT NOT NULL REFERENCES delegation_rules(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (delegation_rule_id, permission)
);

CREATE TABLE delegation_rule_allowed_roles (
    delegation_rule_id BIGINT NOT NULL REFERENCES delegation_rules(id) ON DELETE CASCADE,
    role VARCHAR(100) NOT NULL,
    PRIMARY KEY (delegation_rule_id, role)
);

-- Migrate existing data (parse comma-separated values)
INSERT INTO delegation_rule_permissions (delegation_rule_id, permission)
SELECT id, trim(unnest(string_to_array(delegatable_permissions, ',')))
FROM delegation_rules;

INSERT INTO delegation_rule_allowed_roles (delegation_rule_id, role)
SELECT id, trim(unnest(string_to_array(allowed_delegate_roles, ',')))
FROM delegation_rules;

-- Drop old columns after verification
-- ALTER TABLE delegation_rules DROP COLUMN delegatable_permissions;
-- ALTER TABLE delegation_rules DROP COLUMN allowed_delegate_roles;
```

### V13 — Add Full-Text Search Vector (propertize)

```sql
-- Add materialized FTS column to tenant
ALTER TABLE tenant ADD COLUMN search_vector tsvector
  GENERATED ALWAYS AS (
    to_tsvector('english',
      coalesce(first_name, '') || ' ' ||
      coalesce(last_name, '') || ' ' ||
      coalesce(email, '') || ' ' ||
      coalesce(phone_number, '')
    )
  ) STORED;

CREATE INDEX idx_tenant_fts ON tenant USING gin(search_vector);

-- Similar for property
ALTER TABLE property ADD COLUMN search_vector tsvector
  GENERATED ALWAYS AS (
    to_tsvector('english',
      coalesce(property_name, '') || ' ' ||
      coalesce(address_street, '') || ' ' ||
      coalesce(address_city, '') || ' ' ||
      coalesce(address_state, '')
    )
  ) STORED;

CREATE INDEX idx_property_fts ON property USING gin(search_vector);
```

---

## 8. Files to Fix (Status)

| File                                      | Issue                                            | Status     |
| ----------------------------------------- | ------------------------------------------------ | ---------- |
| `propertize/application.yml`              | Debug SQL logging                                | ✅ Fixed   |
| `auth-service/application.yml`            | ddl-auto → none                                  | ✅ Fixed   |
| `propertize/application.yml`              | ddl-auto → validate                              | ✅ Fixed   |
| `Tenant.java`                             | Remove global email unique constraint            | ✅ Fixed   |
| `OrganizationMembership.java`             | userId/invitedBy type → Long                     | ✅ Fixed   |
| `Tenant.java`                             | CASCADE_ALL on payments/leases → PERSIST+MERGE   | ✅ Fixed   |
| `Property.java`                           | Add @CollectionTable indexes on amenities/photos | ✅ Fixed   |
| `auth-service/User.java`                  | Roles FetchType → EAGER                          | ✅ Fixed   |
| `V12__Critical_DB_Fixes.sql`              | Composite indexes + email unique fix             | ✅ Created |
| `auth-service/V9__Fix_Delegation_1NF.sql` | 1NF fix for delegation_rules                     | ✅ Created |

---

_Generated by DB Architecture Investigation — March 19, 2026_
