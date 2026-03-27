# Propertize Services — Project Conventions

## Architecture Overview

Propertize is a property-management platform built as a microservices monorepo. All services share a single Docker Compose stack and a single PostgreSQL database (`propertize_db`).

| Layer         | Services                                                                                               |
| ------------- | ------------------------------------------------------------------------------------------------------ |
| **Gateway**   | `api-gateway` (8080) — all external traffic enters here                                                |
| **Auth**      | `auth-service` (8081) — JWT issuance, RBAC engine                                                      |
| **Core Java** | `propertize` (8082), `employee-service` (8083), `payment-service` (8084), `payroll-service` (8085)     |
| **Python**    | `report-service` (8090), `vendor-matching` (8091), `document-service` (8092), `search-reranker` (8093) |
| **Workers**   | `analytics-worker`, `payment-worker`, `screening-worker` (Kafka/APScheduler, no ports)                 |
| **Frontend**  | `propertize-front-end` (3000) — Next.js 14                                                             |
| **Infra**     | PostgreSQL 16, MongoDB 7, Redis 7, Kafka + Zookeeper, MinIO                                            |

Service discovery: **Eureka** (`service-registry` on 8761). All Java services register automatically.

## Technology Stack

- **Java 21**, Spring Boot 3.5.10, Spring Cloud 2025.0.0, Maven 3.9+
- **Python 3.11**, FastAPI, Uvicorn, psycopg2 (no ORM — raw SQL + pandas)
- **Frontend**: Next.js 14 (App Router), TypeScript, Tailwind CSS, Zustand, React Query v5, NextAuth v5
- **Database**: PostgreSQL 16 (shared), MongoDB 7 (events/audit), Redis 7 (cache/sessions)
- **Messaging**: Kafka (event-driven async), APScheduler (cron workers)

## Java Service Conventions

### Package Structure

```
src/main/java/com/propertize/[service]/
├── controller/     # @RestController — thin, delegates to service layer
├── service/        # Business logic, @Transactional here
├── repository/     # JpaRepository<Entity, ID>
├── entity/         # @Entity classes; extend BaseEntity when available
│   ├── base/       # BaseEntity, AuditableEntity
│   └── embedded/   # @Embeddable value objects
├── dto/            # Separate *Request and *Response DTOs
├── config/         # @Configuration beans
├── filter/         # Servlet filters (auth header extraction)
├── enums/          # Domain enumerations
├── exception/      # Custom exceptions + @ControllerAdvice handlers
└── client/         # OpenFeign / RestTemplate inter-service clients
```

### Key Patterns

- **Authentication**: Gateway validates JWT → forwards `X-User-ID`, `X-User-Email`, `X-User-Roles` headers. Services read these headers via filters — never use `@AuthenticationPrincipal` directly.
- **Entity IDs**: Use `@GeneratedValue(strategy = GenerationType.IDENTITY)` with `Long` for auto-increment, or `UUID` for distributed IDs. Never mix types in FK relationships.
- **Embedded objects**: When an `@Embeddable` defines a column (e.g., `@Column(name = "ytd_deductions")`), the owning `@Entity` must NOT declare a field mapping to the same column name.
- **DTOs**: Always separate request and response objects. Use MapStruct (1.5+) for entity ↔ DTO mapping.
- **Profiles**: `application.yml` (local), `application-docker.yml` (container), `application-prod.yml`. Use `${ENV_VAR:default}` for externalization.
- **Transactions**: `@Transactional` belongs on the service layer, not controllers or repositories.
- **Caching**: Caffeine for in-process, Redis for distributed. Do NOT use ehcache (blocked HTTP repos in Maven 3.8+ Docker builds).
- **Resilience**: Resilience4j for circuit breakers, retries, rate limiting.
- **API docs**: SpringDoc 2.5+ auto-generates `/api-docs` and `/swagger-ui.html`.

### Hibernate / JPA Rules

- Set `spring.jpa.hibernate.ddl-auto: update` for dev/docker, `validate` for prod.
- When adding `@Embedded` fields, verify no duplicate column names between the embeddable and the entity.
- Repository query methods must reference actual entity field names (Java property names, not column names). Validate with `./mvnw compile`.
- Avoid `dependency:go-offline` in Dockerfiles — it breaks on transitive deps with blocked HTTP repos. Use a single `mvn package -DskipTests` stage instead.

### Lombok

All Java services use Lombok 1.18+. Standard annotations:

- `@Getter @Setter` on entities (not `@Data` — avoids equals/hashCode issues with JPA)
- `@RequiredArgsConstructor` on services and controllers for constructor injection
- `@Builder` on DTOs
- `@Slf4j` for logging

## Python Service Conventions

### Structure

```
python-services/[service-name]/
├── main.py              # FastAPI app, routes, startup
├── Dockerfile           # FROM python:3.11-slim, multi-stage
├── requirements.txt     # pip deps
└── shared/              # Symlinked shared modules (db.py, config.py)
```

### Patterns

- **No ORM** — use raw SQL via `psycopg2` and `pandas.read_sql()`.
- **Connection pooling** via shared `db.py` module.
- **Config**: Read `DATABASE_URL` and `PORT` from environment variables.
- **Health endpoint**: Every service must expose `GET /health` returning `{"status": "ok"}`.
- **Cache dirs**: Set `HF_HOME`, `TRANSFORMERS_CACHE`, `XDG_CACHE_HOME` to `/tmp/.cache` in Docker for ML model downloads.

## Frontend Conventions (Next.js)

### Project Layout

```
propertize-front-end/src/
├── app/                  # Next.js App Router (pages, layouts)
│   ├── (dashboard)/      # Dashboard route group
│   └── (platform)/       # Platform route group
├── components/
│   ├── ui/               # Reusable design-system components
│   ├── layout/           # CommonDashboardLayout, Sidebar, Header
│   ├── common/           # ErrorBoundary, ProtectedRoute
│   └── [feature]/        # Feature-specific components
├── services/             # API integration (*.service.ts)
├── stores/               # Zustand stores
├── hooks/                # Custom React hooks
├── config/               # Constants, API base URL
├── types/                # TypeScript interfaces/types
└── providers/            # Context providers (auth, theme, query)
```

### Key Rules

- **`CommonDashboardLayout`** requires `activeTab` prop (string). Every dashboard page must pass it: `<CommonDashboardLayout activeTab="payroll">`.
- **API services**: One file per domain in `src/services/` (e.g., `payroll.service.ts`). Use the shared `httpClient` for all requests.
- **State**: Zustand for client state, React Query for server/async state. No Redux.
- **Styling**: Tailwind CSS utility classes. No CSS modules or styled-components.
- **Auth**: NextAuth v5. Protected routes wrap with `<ProtectedRoute>`.
- **Icons**: `react-icons` (prefer `Fa*` from `react-icons/fa`). Remove unused icon imports — Next.js build treats them as errors.
- **TypeScript**: Strict mode. No `any` types. All component props must have interfaces.

## Docker & Infrastructure

### Dockerfile Pattern (Java)

```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S propertize && adduser -S propertize -G propertize
COPY --from=build /app/target/*-service-*.jar app.jar
RUN chown -R propertize:propertize /app
USER propertize
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE [port]
HEALTHCHECK --interval=20s --timeout=10s --start-period=90s --retries=5 \
    CMD wget -qO- http://localhost:[port]/actuator/health || exit 1
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

Do NOT include `RUN mvn dependency:go-offline` — it fails on transitive deps with HTTP-only Maven repos blocked since Maven 3.8+.

### Docker Compose

- All services on network `propertize-net`.
- Java services depend on `postgres` (healthy), `service-registry` (healthy), `auth-service` (healthy).
- `api-gateway` depends on all backend services.
- Environment variables use `${VAR:-default}` syntax; secrets with `${VAR:?}` fail-hard.
- `.env` file at repo root holds credentials (never commit real keys).

### Makefile Targets

| Command                   | Purpose                        |
| ------------------------- | ------------------------------ |
| `make up`                 | Start all services             |
| `make down`               | Stop (keep volumes)            |
| `make build`              | Build all images in parallel   |
| `make build-java`         | Build only Java service images |
| `make rebuild`            | down → build → up              |
| `make logs`               | Tail all logs                  |
| `make logs-[service]`     | Tail specific service logs     |
| `make ps` / `make status` | Container status               |
| `make health`             | Verbose health checks          |
| `make db-shell`           | PostgreSQL CLI                 |

## API Routing

All external requests go through the gateway at `localhost:8080`. Routes are defined in `api-gateway/src/main/resources/application.yml`.

- Pattern: `/api/v1/[resource]` → routed to the owning service via Eureka service name
- Gateway strips no prefix by default (`StripPrefix=0`)
- Auth endpoints: `/auth/**` → auth-service
- Payroll: `/api/v1/clients/*/payroll/**`, `/api/v1/timesheets/**`, etc. → payroll-service
- Python: `/api/v1/reports/**` → report-service, etc.

## Database Rules

- **Single PostgreSQL instance** shared by all services. Each service manages its own tables via Hibernate DDL or Flyway.
- **Flyway** migrations in auth-service: `src/main/resources/db/migration/V*.sql`.
- **Schema changes**: When modifying entities, always verify column types match across FK relationships (e.g., don't FK a `varchar` column to a `uuid` primary key).
- **Naming**: Tables use `snake_case`, entity fields use `camelCase`. Hibernate's implicit naming strategy handles conversion.

## Common Pitfalls

1. **Embedded column duplication**: If an `@Embeddable` maps column `X`, the parent `@Entity` must not also have a field mapping to column `X`. Hibernate will throw `DuplicateMappingException`.
2. **Repository query validation**: JPQL queries reference Java field names, not database column names. `lb.availableHours` works only if `LeaveBalanceEntity` has a field named `availableHours`.
3. **UUID vs String type mismatches**: If an entity field is `UUID`, the repository method parameter must also be `UUID`, not `String`.
4. **Maven HTTP blocking**: Maven 3.8+ blocks HTTP-only repos inside Docker. Exclude deps that pull from `http://maven.java.net` or remove them entirely.
5. **Unused imports in Next.js**: TypeScript strict mode + Next.js build treats unused imports as build errors. Always clean up.
6. **`activeTab` prop**: Every page using `CommonDashboardLayout` must pass the `activeTab` string prop or the build fails.
7. **Python model cache**: ML services (vendor-matching, search-reranker) need writable cache dirs. Set `HF_HOME=/tmp/.cache` in Docker environment.

## Git Workflow

- Feature branches off `main`
- PR required for merge
- CI runs on push (see `propertize-front-end/.github/workflows/ci-cd.yml`)
- Do not commit: `.env` (secrets), `target/` (build output), `node_modules/`, `__pycache__/`, `*.jar`

## Reference Documentation

Detailed guides live in `docs/`:

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — system design, data flows
- [AUTHENTICATION_GUIDE.md](docs/AUTHENTICATION_GUIDE.md) — JWT, RBAC, token lifecycle
- [LOCAL_DEVELOPMENT_GUIDE.md](docs/LOCAL_DEVELOPMENT_GUIDE.md) — local setup, debugging
- [DOCKER_MANAGEMENT.md](docs/DOCKER_MANAGEMENT.md) — Docker Compose usage
- [SERVICE_DEPENDENCIES.md](docs/SERVICE_DEPENDENCIES.md) — inter-service topology
- [RBAC_V6_ENGINE_GUIDE.md](docs/RBAC_V6_ENGINE_GUIDE.md) — role-based access control
- [HOW_TO_RUN.md](docs/HOW_TO_RUN.md) — step-by-step launch guide
