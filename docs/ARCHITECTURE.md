# Propertize Platform — Architecture Guide

> **Complete reference for the platform structure, service responsibilities, data flows, and operational runbook.**

---

## Table of Contents

1. [Platform Overview](#1-platform-overview)
2. [Project Structure](#2-project-structure)
3. [Architecture Diagram](#3-architecture-diagram)
4. [Services — Deep Dive](#4-services--deep-dive)
   - 4.1 [Infrastructure Layer](#41-infrastructure-layer)
   - 4.2 [Java Microservices Layer](#42-java-microservices-layer)
   - 4.3 [Python Services Layer](#43-python-services-layer)
   - 4.4 [Frontend Layer](#44-frontend-layer)
5. [Port Reference](#5-port-reference)
6. [Data Architecture](#6-data-architecture)
7. [Security Architecture](#7-security-architecture)
8. [Communication Patterns](#8-communication-patterns)
9. [One-Click Docker Operations](#9-one-click-docker-operations)
10. [Environment Configuration](#10-environment-configuration)
11. [Development Workflow](#11-development-workflow)

---

## 1. Platform Overview

**Propertize** is a multi-tenant property management SaaS platform built on a polyglot microservices architecture. It handles the full lifecycle of property management: tenant onboarding, lease management, rent collection, maintenance scheduling, vendor coordination, employee management, and financial reporting.

### Technology Stack Summary

| Component        | Technology                                | Purpose                                       |
| ---------------- | ----------------------------------------- | --------------------------------------------- |
| API Gateway      | Spring Cloud Gateway (WebFlux)            | Single entry point, routing, JWT validation   |
| Service Registry | Spring Cloud Netflix Eureka               | Service discovery and health tracking         |
| Auth Service     | Spring Boot 3, PostgreSQL                 | JWT issuance, RBAC, user identity             |
| Propertize Core  | Spring Boot 3, PostgreSQL, MongoDB, Kafka | All property management business logic        |
| Employee Service | Spring Boot 3, PostgreSQL                 | HR — employees, departments, attendance       |
| Report Service   | FastAPI, Pandas, ReportLab                | PDF/Excel report generation                   |
| Vendor Matching  | FastAPI, SentenceTransformers             | Semantic NLP vendor-to-request matching       |
| Document Service | FastAPI, MinIO, PyMuPDF                   | File storage, OCR, PII detection              |
| Search Reranker  | FastAPI, BM25                             | Full-text search result reranking             |
| Analytics Worker | APScheduler, PostgreSQL                   | Background aggregation pipeline               |
| Payment Worker   | APScheduler, Kafka                        | Automated rent generation and late fees       |
| Screening Worker | Kafka Consumer, PostgreSQL                | Rental applicant risk scoring                 |
| Frontend         | Next.js 14, TypeScript, Tailwind          | Admin and tenant-facing web UI                |
| PostgreSQL 16    | Primary relational database               | All transactional data (shared schema)        |
| MongoDB 7        | Document store                            | Notifications, audit logs, analytics events   |
| Redis 7          | Cache + session store                     | Token blacklist, rate limiting, session cache |
| Apache Kafka     | Message broker                            | Async domain events, background job queuing   |
| MinIO            | S3-compatible object storage              | Tenant documents, lease PDFs, photos          |

---

## 2. Project Structure

```
propertize-Services/                    ← mono-repo root
│
├── docker-compose.yml                  ← ONE FILE — starts entire platform
├── Makefile                            ← make up / make down / make build
├── start-all-local.sh                  ← legacy local runner (pre-Docker)
├── scripts/
│   ├── db/                             ← SQL init scripts + DB management
│   │   ├── init-propertize-users.sql
│   │   ├── init-superadmin.sql
│   │   ├── fix-database-schema.sh
│   │   ├── reset-database.sh
│   │   └── setup-postgres-local.sh
│   └── test/                           ← validation and smoke test scripts
│       ├── test-auth-endpoints.sh
│       ├── validate-auth-fix.sh
│       └── verify-entity-consolidation.sh
│
├── service-registry/                   ← Eureka Server (Spring Boot)
│   ├── Dockerfile
│   └── src/main/resources/application.yml
│
├── auth-service/                       ← JWT Auth + RBAC (Spring Boot)
│   ├── Dockerfile
│   ├── keys/                           ← RSA key pair for JWT signing
│   │   ├── private_key.pem
│   │   └── public_key.pem
│   └── src/
│
├── api-gateway/                        ← API Gateway (Spring Cloud Gateway)
│   ├── Dockerfile
│   ├── config/keys/                    ← RSA public key for JWT validation
│   └── src/
│
├── propertize/                         ← Core property management (Spring Boot)
│   ├── Dockerfile
│   ├── migrations/                     ← Flyway SQL migrations
│   └── src/main/java/com/propertize/
│       ├── client/                     ← HTTP clients (AuthServiceClient, PythonServiceClient)
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       ├── rbac/                       ← Role-based access control
│       ├── repository/
│       ├── security/
│       └── services/                   ← All business logic (75+ service classes)
│
├── employee-service/                   ← HR/Payroll (Spring Boot)
│   ├── Dockerfile
│   └── src/
│
├── python-services/                    ← All Python microservices
│   ├── docker-compose.python.yml       ← (legacy — superseded by root docker-compose.yml)
│   ├── shared/                         ← Shared config and DB utilities
│   │   ├── config.py
│   │   └── db.py
│   ├── report-service/                 ← FastAPI, port 8090
│   ├── vendor-matching/                ← FastAPI, port 8091
│   ├── document-service/               ← FastAPI, port 8092
│   ├── search-reranker/                ← FastAPI, port 8093
│   ├── analytics-worker/               ← APScheduler (no HTTP port)
│   ├── payment-worker/                 ← APScheduler + Kafka (no HTTP port)
│   └── screening-worker/               ← Kafka consumer (no HTTP port)
│
├── propertize-front-end/               ← Next.js 14 App Router frontend
│   ├── Dockerfile
│   ├── next.config.js                  ← output: 'standalone' enabled
│   └── src/app/
│       ├── (dashboard)/                ← Protected dashboard pages
│       ├── (auth)/                     ← Login / register pages
│       └── api/                        ← Next.js API routes (server-side)
│
└── docs/                               ← All documentation
    └── ARCHITECTURE.md                 ← This file
```

---

## 3. Architecture Diagram

```
                              ┌─────────────────────────────────────────┐
                              │            BROWSER / MOBILE              │
                              │      http://localhost:3000               │
                              └─────────────────┬───────────────────────┘
                                                │ HTTP/WebSocket
                              ┌─────────────────▼───────────────────────┐
                              │           FRONTEND (Next.js)             │
                              │  Container: propertize-frontend :3000    │
                              │  • App Router (RSC + Client Components)  │
                              │  • Tailwind CSS + shadcn/ui              │
                              │  • Server-side: API_URL=api-gateway:8080 │
                              │  • Client-side: NEXT_PUBLIC_API_URL=:8080│
                              └─────────────────┬───────────────────────┘
                                                │ HTTP
                              ┌─────────────────▼───────────────────────┐
                              │          API GATEWAY (WebFlux)           │
                              │  Container: propertize-api-gateway :8080 │
                              │  • JWT validation (RSA public key)       │
                              │  • Rate limiting (Redis)                 │
                              │  • CORS enforcement                      │
                              │  • Route → auth-service  /api/v1/auth/** │
                              │  • Route → propertize    /api/v1/**      │
                              │  • Route → employee-svc  /api/v1/employees│
                              └──────┬──────────┬──────────┬────────────┘
                                     │          │          │
               ┌─────────────────────▼──┐  ┌───▼───┐  ┌──▼──────────────┐
               │    AUTH SERVICE        │  │PROPERT│  │EMPLOYEE SERVICE  │
               │    :8081               │  │IZE    │  │  :8083           │
               │  • JWT issue/refresh   │  │:8082  │  │• Employees       │
               │  • RBAC enforcement    │  │• All  │  │• Departments     │
               │  • RSA-signed tokens   │  │  biz  │  │• Attendance      │
               │  • Kafka auth events   │  │  logic│  │• Timesheets      │
               └────────┬───────────────┘  └───┬───┘  └──────────────────┘
                        │                      │
  ┌─────────────────────▼──────────────────────▼──────────────────────────────┐
  │                    SERVICE REGISTRY (Eureka) :8761                         │
  │  • Health monitoring • Service discovery • Round-robin load balancing      │
  └──────────────────────────────────────────────────────────────────────────-─┘

  ─────────────────────────────────────────────────────────────
  PYTHON SERVICES LAYER  (called by Propertize Core via REST)
  ─────────────────────────────────────────────────────────────
  ┌───────────────┐  ┌─────────────────┐  ┌──────────────────┐  ┌────────────┐
  │ Report Svc    │  │ Vendor Matching  │  │ Document Svc     │  │Search Rank │
  │ :8090         │  │ :8091           │  │ :8092            │  │ :8093      │
  │ PDF/Excel gen │  │ NLP semantic    │  │ MinIO upload/    │  │ BM25 rerank│
  │ Pandas+Report │  │ SentenceTransf. │  │ PyMuPDF OCR+PII  │  │ 70/30 blend│
  └───────────────┘  └─────────────────┘  └──────────────────┘  └────────────┘
  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
  │ Analytics Worker     │  │ Payment Worker        │  │ Screening Worker     │
  │ (no HTTP port)       │  │ (no HTTP port)        │  │ (no HTTP port)       │
  │ APScheduler 5-min    │  │ Cron: rent gen,       │  │ Kafka consumer       │
  │ UPSERT aggregation   │  │ late fees, reminders  │  │ Risk scoring engine  │
  └──────────────────────┘  └──────────────────────┘  └──────────────────────┘

  ─────────────────────────────────────────────────────────────
  INFRASTRUCTURE LAYER
  ─────────────────────────────────────────────────────────────
  ┌─────────────┐  ┌─────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
  │ PostgreSQL  │  │  MongoDB    │  │  Redis   │  │  Kafka   │  │  MinIO   │
  │ :5432       │  │  :27017     │  │  :6379   │  │  :9092   │  │  :9000   │
  │ Primary DB  │  │  propertize │  │  Token   │  │  Events  │  │  Object  │
  │ propertize  │  │  _db        │  │ blacklist│  │  queue   │  │ storage  │
  │ _db         │  │             │  │  cache   │  │          │  │          │
  └─────────────┘  └─────────────┘  └──────────┘  └──────────┘  └──────────┘
```

---

## 4. Services — Deep Dive

### 4.1 Infrastructure Layer

#### PostgreSQL 16 (`:5432`)

- **Purpose:** Primary relational store for all transactional data
- **Database:** `propertize_db` (single schema shared by all Java services)
- **Credentials:** `dbuser` / `dbpassword`
- **Schema managed by:** Flyway migrations in each service's `src/main/resources/db/migration/`
- **Management UI:** Adminer at `http://localhost:8088`

#### MongoDB 7 (`:27017`)

- **Purpose:** Document store for semi-structured data
- **Use cases:** Audit logs, notification payloads, analytics events, GraphQL flexible data
- **Credentials:** `admin` / `mongo_secure_pass`
- **Management UI:** Mongo Express at `http://localhost:8089`

#### Redis 7 (`:6379`)

- **Purpose:** Cache layer and token blacklist
- **Use cases:**
  - JWT token blacklist (logout invalidation)
  - Rate limiting counters (API Gateway)
  - Session cache (Spring Session)
- **Password:** `redis_secure_pass`

#### Apache Kafka (`:9092`)

- **Purpose:** Async event streaming between services
- **Topics in use:**
  - `screening.initiated` — rental application triggers screening worker
  - `screening.completed` — screening results back to propertize core
  - `payment.events` — payment lifecycle events
  - `auth.events` — login/logout audit events
- **Internal listener:** `kafka:29092` (container-to-container)
- **External listener:** `localhost:9092` (host machine tools)
- **Management UI:** Kafka UI at `http://localhost:8086`

#### MinIO (`:9000` / `:9001`)

- **Purpose:** S3-compatible object storage for documents
- **Use cases:** Lease PDFs, tenant documents, property photos, maintenance attachments
- **Credentials:** `propertize` / `propertize123`
- **Console UI:** `http://localhost:9001`
- **Accessed by:** Python document-service (direct MinIO SDK), Propertize core (via document-service REST API)

---

### 4.2 Java Microservices Layer

#### Service Registry — Eureka (`:8761`)

**Container:** `propertize-service-registry`  
**Tech:** Spring Cloud Netflix Eureka Server 3.x, Java 21

- Standalone Eureka server — does not register with itself
- All Java services register on startup and heartbeat every 5s
- Dashboard: `http://localhost:8761` (admin/admin)
- Self-preservation disabled in dev (enabled for production via `EUREKA_SELF_PRESERVATION=true`)

**Key env vars:**
| Variable | Default | Purpose |
|---|---|---|
| `EUREKA_HOSTNAME` | `localhost` | Hostname advertised to clients |
| `EUREKA_USERNAME` | `admin` | Dashboard auth |
| `EUREKA_PASSWORD` | `admin` | Dashboard auth |

---

#### Auth Service (`:8081`)

**Container:** `propertize-auth-service`  
**Tech:** Spring Boot 3.5, Java 21, PostgreSQL, Redis, Kafka

**Responsibilities:**

- User registration and login (JWT issuance)
- Password change and reset flow
- JWT access token (15 min) + refresh token (7 days) lifecycle
- RSA-signed tokens: `private_key.pem` signs, `public_key.pem` validates
- RBAC: `SUPER_ADMIN`, `ORG_ADMIN`, `MANAGER`, `TECHNICIAN`, `TENANT` roles
- Token blacklisting on logout (stored in Redis)
- Multi-tenant organization scoping

**Key endpoints:**
| Endpoint | Description |
|---|---|
| `POST /api/v1/auth/login` | Returns access + refresh JWT |
| `POST /api/v1/auth/refresh` | Refreshes access token via refresh token |
| `POST /api/v1/auth/logout` | Blacklists token in Redis |
| `POST /api/v1/auth/register` | Create user account |
| `POST /api/v1/auth/change-password` | Authenticated password change |

**Key env vars:**
| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection |
| `SPRING_DATA_REDIS_HOST` | Redis for token blacklist |
| `EUREKA_URL` | Service registry URL |
| `RSA_PRIVATE_KEY_PATH` | JWT signing key |
| `RSA_PUBLIC_KEY_PATH` | JWT validation key |
| `KAFKA_BOOTSTRAP_SERVERS` | Event publishing |

---

#### API Gateway (`:8080`) — Primary Entry Point

**Container:** `propertize-api-gateway`  
**Tech:** Spring Cloud Gateway 4.x (WebFlux, reactive), Java 21

**This is the ONLY service the frontend and external clients call.**

**Routing table:**
| Path Pattern | Forwards to | Service |
|---|---|---|
| `/api/v1/auth/**` | `auth-service:8081` | Authentication |
| `/api/v1/employees/**`, `/api/v1/departments/**`, `/api/v1/attendance/**`, `/api/v1/timesheets/**`, `/api/v1/tax/**` | `employee-service:8083` | HR |
| `/api/v1/**` (all others) | `propertize:8082` | Core business |
| `/graphql`, `/graphiql` | `propertize:8082` | GraphQL |
| `/admin/**` | `propertize:8082` (rewritten to `/api/v1/admin/`) | Admin shortcut |

**Cross-cutting concerns applied at gateway:**

- JWT validation using RSA public key (no auth-service call needed)
- CORS enforcement for `localhost:3000`
- Rate limiting via Redis (100 req/s default, 200 burst)
- Global timeout: 30s request / 60s per-route

**Key env vars (all support Docker override via env vars):**
| Variable | Purpose |
|---|---|
| `AUTH_SERVICE_URL` | `http://auth-service:8081` in Docker |
| `PROPERTIZE_SERVICE_URL` | `http://propertize:8082` in Docker |
| `EMPLOYEE_SERVICE_URL` | `http://employee-service:8083` in Docker |
| `REDIS_HOST` | Rate limit backend |
| `RSA_PUBLIC_KEY_PATH` | JWT validation |
| `EUREKA_URL` | Optional service discovery |

---

#### Propertize Core Service (`:8082`)

**Container:** `propertize-main-service`  
**Tech:** Spring Boot 3.5, Java 21, PostgreSQL + MongoDB + Kafka + Redis

**The largest service** — 75+ service classes, all core property management logic.

**Key domains:**
| Domain | Description |
|---|---|
| **Properties** | Units, floors, buildings, amenities, availability |
| **Tenants** | Profiles, documents, contacts, history |
| **Leases** | Agreements, terms, renewal workflows |
| **Payments** | Invoices, transactions, payment methods, Stripe integration |
| **Maintenance** | Requests, scheduling, vendor assignment, status tracking |
| **Organizations** | Multi-tenant org management, onboarding, settings |
| **Users** | User management, preferences, RBAC enforcement |
| **Screening** | Rental application processing, risk score intake |
| **Vendors** | Vendor registry, categories, ratings |
| **Analytics** | Dashboard queries, report generation requests |
| **Notifications** | Delivery, preferences, templates |
| **Search** | Full-text search with Python reranking |

**Python delegation (via `PythonServiceClient`):**
| Java method | Delegates to | Fallback |
|---|---|---|
| `DocumentService.upload()` | `document-service:8092` | Local placeholder URL |
| `DocumentService.delete()` | `document-service:8092` | Skip (non-critical) |
| `VendorMatchingEngine.match()` | `vendor-matching:8091` | Keyword algorithm |
| Search reranking | `search-reranker:8093` | Original ordering |

**Key env vars:**
| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL |
| `SPRING_DATA_MONGODB_URI` | MongoDB |
| `SPRING_DATA_REDIS_HOST` / `PASSWORD` | Session cache |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Event publishing |
| `PYTHON_DOCUMENT_SERVICE_URL` | Document storage |
| `PYTHON_VENDOR_MATCHING_URL` | NLP vendor matching |
| `PYTHON_REPORT_SERVICE_URL` | PDF/Excel reports |
| `PYTHON_SEARCH_RERANKER_URL` | Search reranking |

---

#### Employee Service — Employecraft (`:8083`)

**Container:** `propertize-employee-service`  
**Tech:** Spring Boot 3.5, Java 21, PostgreSQL

**Responsibilities:**

- Employee profiles, onboarding, offboarding
- Department and team structure
- Attendance tracking, timesheets
- Tax document management
- Registernts with Eureka for gateway routing

---

### 4.3 Python Services Layer

Python services are **stateless** REST APIs and **background workers**. They are called by the Java core (via `PythonServiceClient`) and share the same PostgreSQL database.

#### Report Service (`:8090`) — FastAPI

**Container:** `propertize-report-service`

Generates downloadable PDF and Excel reports using Pandas + ReportLab. Replaces direct iText/Apache POI calls from Java that caused blocking on the JVM thread pool.

**Endpoints:**

- `GET /reports/financial/{org_id}` — P&L, cash flow, rent roll
- `GET /reports/maintenance/{org_id}` — Open tickets, resolution time
- `GET /reports/occupancy/{org_id}` — Vacancy rate, turnover analysis
- `GET /health`

---

#### Vendor Matching (`:8091`) — FastAPI + SentenceTransformers

**Container:** `propertize-vendor-matching`

Uses `all-MiniLM-L6-v2` model to embed maintenance request descriptions and match semantically to vendor service categories.

**Score breakdown:** Category match 50% + Location proximity 30% + Rating 20%  
**Performance:** 30% → 70–80% vendor match accuracy improvement over keyword search.

**Endpoints:**

- `POST /match-vendors` — `{category, description, org_id, limit}` → ranked vendor list
- `GET /health`

---

#### Document Service (`:8092`) — FastAPI + MinIO + PyMuPDF

**Container:** `propertize-document-service`

Handles all file operations: upload to MinIO, signed URL generation, text extraction via PyMuPDF, and PII detection before storing sensitive documents.

**Endpoints:**

- `POST /documents/upload` — multipart file → MinIO key + presigned URL
- `DELETE /documents/{object_name}` — remove from MinIO
- `GET /documents/{object_name}/text` — OCR text extraction
- `GET /health`

---

#### Search Reranker (`:8093`) — FastAPI + BM25

**Container:** `propertize-search-reranker`

Takes PostgreSQL full-text search results (uniform 1.0 tsvector scores) and applies BM25 reranking blended with the original DB score: final = 0.7 × BM25 + 0.3 × DB_score.

**Endpoints:**

- `POST /rerank` — `{query, results[]}` → reordered results with scores
- `GET /health`

---

#### Analytics Worker (no HTTP port) — APScheduler

**Container:** `propertize-analytics-worker`

Runs every 5 minutes via APScheduler. Performs UPSERT aggregations into summary tables:

```
monthly_revenue_summary, maintenance_summary, occupancy_summary, tenant_summary
```

Replaces a slow full-table-scan approach (was 2–8s per dashboard load → now 10ms).

---

#### Payment Worker (no HTTP port) — APScheduler

**Container:** `propertize-payment-worker`

Three cron jobs:

1. **Monthly (1st, 08:00):** Generate rent invoices for all active leases
2. **Daily (02:00):** Apply late fees on overdue invoices (> 5 days past due)
3. **Daily (09:00):** Send payment reminders via Kafka event

Replaces the empty `processOverduePayments()` method in Java.

---

#### Screening Worker (no HTTP port) — Kafka Consumer

**Container:** `propertize-screening-worker`

Listens to `screening.initiated` Kafka topic. Computes multi-factor risk score:

| Factor               | Weight |
| -------------------- | ------ |
| Credit score         | 35%    |
| Income-to-rent ratio | 30%    |
| Criminal background  | 25%    |
| Employment stability | 10%    |

Publishes result to `screening.completed` topic, consumed by the Propertize core service.

---

### 4.4 Frontend Layer

#### Propertize Frontend (`:3000`) — Next.js 14

**Container:** `propertize-frontend`

Built with Next.js 14 App Router, TypeScript, Tailwind CSS, and shadcn/ui.

**Key pages:**
| Route | Description |
|---|---|
| `/` (redirect) | Login |
| `/dashboard` | Main dashboard by role |
| `/dashboard/properties` | Property listings and detail |
| `/dashboard/tenants` | Tenant management |
| `/dashboard/leases` | Lease creation, renewal |
| `/dashboard/maintenance` | Maintenance request lifecycle |
| `/dashboard/payments` / `/invoices` | Payment tracking |
| `/dashboard/vendors` | Vendor registry |
| `/dashboard/employees` | HR (Employecraft) |
| `/dashboard/reports` | Report generation |
| `/dashboard/analytics` | Charts and KPIs |
| `/dashboard/platform-oversight` | Super-admin view (org health, system metrics) |
| `/dashboard/settings` | Profile, notifications, org settings |

**Dual URL config:**

- `NEXT_PUBLIC_API_URL=http://localhost:8080` — used by browser (client-side fetch)
- `API_URL=http://api-gateway:8080` — used by Next.js server-side routes (container-to-container)

**Build output:** `output: 'standalone'` — produces self-contained `server.js` for minimal Docker image.

---

## 5. Port Reference

| Port      | Container                     | Service                            |
| --------- | ----------------------------- | ---------------------------------- |
| **3000**  | `propertize-frontend`         | Next.js frontend                   |
| **8080**  | `propertize-api-gateway`      | API Gateway — **main entry point** |
| **8081**  | `propertize-auth-service`     | Auth Service                       |
| **8082**  | `propertize-main-service`     | Propertize Core Service            |
| **8083**  | `propertize-employee-service` | Employee Service                   |
| **8086**  | `propertize-kafka-ui`         | Kafka UI (management)              |
| **8088**  | `propertize-adminer`          | Adminer (PostgreSQL UI)            |
| **8089**  | `propertize-mongo-express`    | Mongo Express (MongoDB UI)         |
| **8090**  | `propertize-report-service`   | Python Report Service              |
| **8091**  | `propertize-vendor-matching`  | Python Vendor Matching             |
| **8092**  | `propertize-document-service` | Python Document Service            |
| **8093**  | `propertize-search-reranker`  | Python Search Reranker             |
| **8761**  | `propertize-service-registry` | Eureka Dashboard                   |
| **9000**  | `propertize-minio`            | MinIO S3 API                       |
| **9001**  | `propertize-minio`            | MinIO Console UI                   |
| **9092**  | `propertize-kafka`            | Kafka (external listener)          |
| **27017** | `propertize-mongodb`          | MongoDB                            |
| **6379**  | `propertize-redis`            | Redis                              |
| **5432**  | `propertize-postgres`         | PostgreSQL                         |
| **2181**  | `propertize-zookeeper`        | Zookeeper                          |

---

## 6. Data Architecture

### PostgreSQL Schema (shared `propertize_db`)

All Java services and Python services read from/write to the same database. Schema is managed by Flyway migrations in `propertize/src/main/resources/db/migration/`.

**Core tables:**

```
organizations         → Multi-tenant root (org_id is FK in most tables)
users                 → All user types (tenants, managers, admins)
properties            → Property listings with org_id
units                 → Individual rental units
tenants               → Tenant profiles linked to users
leases                → Lease agreements (tenant ↔ unit)
invoices              → Rent invoices generated monthly
transactions          → Payment records
maintenance_requests  → Work orders with status lifecycle
vendors               → Vendor directory with categories
employees             → Employecraft HR records
departments           → Department hierarchy
attendance_records    → Daily attendance
timesheets            → Weekly hour tracking

Summary tables (written by analytics-worker):
monthly_revenue_summary
maintenance_summary
occupancy_summary
tenant_summary
```

### MongoDB Collections (`propertize_db`)

```
notifications         → Notification payloads with delivery status
audit_logs            → Immutable event log (who did what when)
analytics_events      → Custom tracking events for dashboards
```

### Redis Keys

```
blacklist::{token_jti}          → Blacklisted JWTs (TTL = token expiry)
rate_limit::{ip}::{route}       → Rate limit counters (TTL = 1s)
session::{session_id}           → Spring Session data
```

### Kafka Topics

```
screening.initiated              → Rental application triggers screening
screening.completed              → Screening results back to core
payment.events                   → Payment lifecycle events
auth.events                      → Login/logout audit trail
```

---

## 7. Security Architecture

### JWT Flow

```
1. Client  →  POST /api/v1/auth/login  →  API Gateway
2. Gateway  →  proxy  →  Auth Service
3. Auth Service: validate credentials, sign JWT with RSA private key
4. Returns: { accessToken (15min), refreshToken (7 days) }
5. Client stores tokens, sends Bearer token on every request
6. API Gateway validates JWT using RSA public key (no auth-service roundtrip)
7. Validated claims forwarded as headers: X-User-Id, X-User-Role, X-Org-Id
8. Backend services trust these headers (only gateway can set them)
```

### RBAC (Role-Based Access Control)

| Role          | Scope                                        |
| ------------- | -------------------------------------------- |
| `SUPER_ADMIN` | Full platform access, all organizations      |
| `ORG_ADMIN`   | Full access within one organization          |
| `MANAGER`     | Manage properties/tenants within org         |
| `TECHNICIAN`  | View and update maintenance requests         |
| `TENANT`      | View own lease, payments, submit maintenance |

### Service-to-Service Auth

Internal service calls (via Eureka) use a trusted header pattern: the API Gateway adds a `X-Gateway-Source: propertize-gateway` header to all proxied requests. Services reject requests missing this header unless they arrive on their direct port (for local dev).

---

## 8. Communication Patterns

### Synchronous (HTTP REST)

- **Frontend → API Gateway → Java services** — all user-facing requests
- **Propertize Core → Python services** — document upload, vendor matching, report generation, search reranking
- All calls wrapped with graceful fallback: if Python service unavailable, Java falls back to local implementation

### Asynchronous (Kafka)

- **Propertize Core → `screening.initiated` → Screening Worker** — rental application processing
- **Screening Worker → `screening.completed` → Propertize Core** — risk score result
- **Payment Worker → Kafka** — payment reminder events
- **Auth Service → `auth.events`** — audit trail

### Service Discovery (Eureka)

- Java services register with Eureka on startup
- API Gateway can route via `lb://service-name` (load-balanced) in addition to direct routes
- Eureka dashboard shows all registered instances and health status

---

## 9. One-Click Docker Operations

### First-Time Setup

```bash
# Clone and enter the repo
cd propertize-Services

# Build all images (Java: ~10-15 min, Python: ~5 min, frontend: ~5 min)
make build

# Start everything
make up
```

### Daily Commands

```bash
make up           # Start all 20+ services
make down         # Stop all (data preserved in volumes)
make health       # Check every service endpoint
make logs         # Tail all logs
make status       # Show container states and ports
```

### Selective Operations

```bash
make dev          # Infrastructure only (for local Java/npm dev)
make up-java      # Java services only (needs infra)
make up-python    # Python services only (needs infra)
make up-frontend  # Frontend only (needs gateway)

make logs-auth    # Auth service logs
make logs-python  # All Python service logs
make logs-infra   # DB + Kafka logs
```

### Database Access

```bash
make db-shell     # PostgreSQL psql shell
make mongo-shell  # MongoDB mongosh
make redis-cli    # Redis CLI
```

### Reset / Rebuild

```bash
make down-v       # Stop + delete all data volumes (destructive)
make clean-all    # Remove all containers + images + volumes
make clean-build  # Full wipe + rebuild + restart
```

### Open Management UIs

```bash
make open-ui      # Opens all UIs in browser
# Or individually:
open http://localhost:8761    # Eureka (admin/admin)
open http://localhost:8086    # Kafka UI
open http://localhost:8088    # Adminer (PostgreSQL)
open http://localhost:8089    # Mongo Express
open http://localhost:9001    # MinIO Console (propertize/propertize123)
open http://localhost:3000    # Frontend
```

### Service Startup Order

Docker automatically handles the dependency order via `depends_on` + `healthcheck`:

```
postgres, mongodb, redis, zookeeper
    → kafka
        → service-registry
            → auth-service, propertize, employee-service
                → api-gateway
                    → frontend

python-services (independent, start in parallel with Java services):
    postgres → report-service, vendor-matching, document-service, analytics-worker, payment-worker, screening-worker
    minio    → document-service
    kafka    → payment-worker, screening-worker
```

---

## 10. Environment Configuration

### Key Variables (docker-compose.yml sets all defaults)

| Variable                                              | Value in Docker                                    | Purpose                    |
| ----------------------------------------------------- | -------------------------------------------------- | -------------------------- |
| `SPRING_DATASOURCE_URL`                               | `jdbc:postgresql://postgres:5432/propertize_db`    | PostgreSQL host            |
| `SPRING_DATA_MONGODB_URI`                             | `mongodb://admin:...@mongodb:27017/...`            | MongoDB host               |
| `SPRING_DATA_REDIS_HOST`                              | `redis`                                            | Redis host                 |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`                      | `kafka:29092`                                      | Kafka internal listener    |
| `EUREKA_URL` / `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://admin:admin@service-registry:8761/eureka/` | Eureka                     |
| `AUTH_SERVICE_URL`                                    | `http://auth-service:8081`                         | Gateway → auth routing     |
| `PROPERTIZE_SERVICE_URL`                              | `http://propertize:8082`                           | Gateway → core routing     |
| `EMPLOYEE_SERVICE_URL`                                | `http://employee-service:8083`                     | Gateway → employee routing |
| `RSA_PRIVATE_KEY_PATH`                                | `/app/keys/private_key.pem`                        | JWT signing                |
| `RSA_PUBLIC_KEY_PATH`                                 | `/app/config/keys/public_key.pem`                  | JWT validation             |
| `PYTHON_DOCUMENT_SERVICE_URL`                         | `http://document-service:8092`                     | Python doc service         |
| `PYTHON_VENDOR_MATCHING_URL`                          | `http://vendor-matching:8091`                      | Python NLP matching        |
| `NEXT_PUBLIC_API_URL`                                 | `http://localhost:8080`                            | Browser API URL            |
| `API_URL`                                             | `http://api-gateway:8080`                          | Server-side API URL        |

### Local Development (without Docker)

When running services locally, all env vars default to `localhost`:

- PostgreSQL: `localhost:5432`
- MongoDB: `localhost:27017`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Eureka: `localhost:8761`

Start infrastructure with: `make dev` (runs only DB/Kafka/Redis containers, not app services)

---

## 11. Development Workflow

### Adding a New Feature

1. **Database change?** → Add Flyway migration in `propertize/src/main/resources/db/migration/`
2. **New entity?** → Add `@Entity` class, `@Repository`, DTO, `@RestController`, service
3. **New API route?** → Add to api-gateway's route predicates in `application-local.yml`
4. **New async process?** → Add Python worker or Kafka producer/consumer
5. **Frontend page?** → Add route in `propertize-front-end/src/app/(dashboard)/`

### Running Tests

```bash
# Java tests (from service directory)
cd propertize && ./mvnw test
cd auth-service && ./mvnw test

# Frontend tests
cd propertize-front-end && npm test

# Health check after deploy
make health
```

### Rebuilding a Single Service

```bash
# Rebuild and restart only the changed service
docker compose build propertize
docker compose up -d --no-deps propertize

# Or rebuild all Java services
make build-java && docker compose up -d service-registry auth-service propertize employee-service api-gateway
```

### Viewing Logs in Real-time

```bash
make logs                          # All services
make logs-propertize               # Core service only
docker compose logs -f --tail=100 auth-service api-gateway   # Multiple specific
```

---

_Architecture document maintained as of March 2026. Update this file when adding new services, changing ports, or modifying the data model._
