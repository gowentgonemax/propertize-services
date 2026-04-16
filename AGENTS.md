# Propertize Services — AI Agent Guide

> **⚠️ MANDATORY:** Read `CLAUDE.md` (repo root) FIRST. It contains the complete coding rules,
> architecture constraints, and banned patterns all AI agents must follow.
> No code generation until `CLAUDE.md` has been read.

## Architecture at a Glance
Multi-service monorepo sharing one PostgreSQL database (`propertize_db`). All external traffic enters through `api-gateway` (8080), which validates JWTs and forwards user context headers downstream. Service discovery via Eureka (`service-registry:8761`).

| Layer | Services & Ports |
|---|---|
| Gateway | `api-gateway` 8080 |
| Auth/RBAC | `auth-service` 8081 |
| Core Java | `propertize` 8082, `employee-service` 8083, `payment-service` 8084, `payroll-service` 8085 |
| Python | `report-service` 8090, `vendor-matching` 8091, `document-service` 8092, `search-reranker` 8093 |
| Workers | `analytics-worker`, `payment-worker`, `screening-worker` (Kafka/APScheduler, no ports) |
| Frontend | `propertize-front-end` 3000 (Next.js 14 App Router) |

## Authentication & Authorization
- Gateway validates JWT → strips the `Cookie` header (prevents HTTP 431) → forwards `X-User-Id`, `X-Username`, `X-Email`, `X-Organization-Id`, `X-Roles`, `X-Permissions`, `X-Primary-Role` to downstream services.
- Each Java service reads these in `TrustedGatewayHeaderFilter`. **Never use `@AuthenticationPrincipal`.**
- RBAC decisions route to `POST /api/v1/auth/authorize`. The gateway enforces roles via `rbac.yml`; individual services may call the auth-service for fine-grained checks.

## Developer Workflow
```bash
# Hybrid local dev (infra in Docker, services on JVM)
make dev                                               # starts postgres, redis, kafka, mongo, eureka
cd <service> && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Full Docker stack
make up          # start everything  |  make down  # stop
make rebuild     # down → build → up
make health      # check all endpoints
make logs-<svc>  # e.g. make logs-auth, make logs-gateway
make db-shell    # psql -U dbuser -d propertize_db

# Python services only
cd python-services && docker compose -f docker-compose.python.yml up -d --build

# Frontend
cd propertize-front-end && npm install && npm run dev
npx tsc --noEmit   # type-check without building
```

**Spring profiles:** `local` (you run the JVM, Docker infra on host ports) | `docker` (all containers, set by `docker-compose.yml`) | `prod` (all from env vars) | `test` (H2, automatic with `./mvnw test`).

## Java Conventions
- **Package root:** `com.propertize.[service]/` (e.g. `com.propertize.payroll`). Sub-packages: `controller/ service/ repository/ entity/ dto/ config/ filter/ enums/ exception/ client/`.
- **Entities:** extend `BaseEntity` (id, timestamps) or `AuditableEntity` (adds `createdBy`, `updatedBy`, soft-delete via `deletedAt`). Use `@Getter @Setter` — **not `@Data`** (breaks JPA equals/hashCode). `@GeneratedValue(IDENTITY)` with `Long` for auto-increment.
- **DTOs:** always separate `*Request` and `*Response` classes. **Responses are nested** — e.g. `PropertyResponseDTO` has sections `basicInfo`, `financial`, `amenities`. Use `ApiResponse<T>` wrapper (each service has its own copy in `dto/common/`).
- **Lombok:** `@RequiredArgsConstructor` for injection, `@Builder` on DTOs, `@Slf4j` for logging.
- **`@Transactional`** belongs only on the service layer, never on controllers or repositories.
- **Dockerfiles:** single `mvn package -DskipTests` stage. **Never add `mvn dependency:go-offline`** — it breaks transitive deps in Maven 3.8+ Docker builds.

## Shared Modules
- **`propertize-commons`** (`com.propertize.commons`): canonical Kafka topic constants (`KafkaTopics.EMPLOYEE_EVENTS`, `PAYMENT_EVENTS`, `NOTIFICATION_EVENTS`, `AUDIT_EVENTS`, `ANALYTICS_EVENTS`) and shared enums for cross-service types. Always use `KafkaTopics.*` constants — hardcoding topic strings caused past silent mismatches.

## Kafka Event Flows
| Topic | Publisher | Consumer |
|---|---|---|
| `employee-events` | `employee-service` | `payroll-service` (upsert employee cache) |
| `user-events` | `auth-service` | `propertize` (user profile sync) |
| `payment-events` | `payment-service` | `propertize` (audit, notifications) |
| `audit-events` | any service | MongoDB audit sink |
| `analytics-events` | `propertize` | `analytics-worker` |

## Python Services
- No ORM — raw SQL via `psycopg2` and `pandas.read_sql()`. Shared utilities symlinked from `python-services/shared/`.
- Every service exposes `GET /health → {"status": "ok"}`.
- ML services (`vendor-matching`, `search-reranker`) require `HF_HOME=/tmp/.cache` in Docker for model downloads.

## Frontend Conventions
- All API calls go through the singleton `httpClient` (`src/services/httpClient.ts`). All backend endpoints **must** start with `/api/v1/` — the client throws on non-conforming URLs and auto-fixes legacy `/v1/` prefixes.
- `CommonDashboardLayout` requires `activeTab` prop (string) on every dashboard page — omitting it is a build error.
- Zustand (`src/stores/`) for client state; React Query for server state. No Redux.
- Unused icon imports from `react-icons` are TypeScript strict-mode build errors — always clean them up.

## API Gateway Routing
Routes in `api-gateway/src/main/resources/application.yml` are **order-sensitive**: specific paths (auth, payment, employee, payroll) come before the `propertize-catchall` (`/api/v1/**`). When adding a new service, insert its route **above** the catchall. `StripPrefix=0` on all routes — paths are forwarded verbatim.

## Common Pitfalls
1. `@Embedded` column names must not duplicate a field already on the owning `@Entity` — Hibernate throws `DuplicateMappingException`.
2. JPQL repository queries use **Java field names**, not column names.
3. UUID entity fields require UUID parameters in repository methods, not `String`.
4. Cookie headers are stripped at the gateway — never rely on cookies in downstream Java services.
5. `X-Organization-Id` header is required for RBAC-protected endpoints; omitting it causes 403 errors.

