# Spring Profile Guide — Propertize Services

> One rule: **the profile name tells you WHERE infrastructure lives.**

---

## The 3 Profiles (and 1 for tests)

| Profile | Who sets it | Where infra lives | How to start |
|---------|------------|-------------------|-------------|
| `local` | You, manually | `localhost:*` (Docker infra on host ports) | `make dev` then `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` |
| `docker` | `docker-compose.yml` | Docker container hostnames (`postgres`, `redis`, …) | `docker compose up` |
| `prod` | CI / Kubernetes / ECS | External cloud services, all values from env vars | Deploy pipeline |
| `test` | Maven Surefire / JUnit | H2 in-memory / Testcontainers | `./mvnw test` |

> **Rule of thumb:** `local` = you're debugging code and running the JVM yourself. `docker` = containers talk to containers.

---

## File Layout — Every Service Follows This Pattern

```
src/main/resources/
├── application.yml              ← shared defaults (no hosts, no passwords)
├── application-local.yml        ← localhost:* — run JVM on your Mac
├── application-docker.yml       ← container DNS — used by docker-compose
├── application-prod.yml         ← ${ENV_VAR} only — no hardcoded values
└── (none for test — see src/test/resources/)

src/test/resources/
├── application.yml              ← test base
└── application-test.yml         ← H2 / Testcontainers overrides
```

**You should NEVER see:**
- `application-dev.yml` — ambiguous, deleted
- `application-qa.yml` — not an infra boundary, deleted
- `application-dev-mailhog.yml` — too narrow, deleted
- Hostnames like `postgres:5432` inside `application-local.yml`
- Hostnames like `localhost:5432` inside `application-docker.yml`

---

## How Each Profile Is Activated

### `local` — developer daily workflow
```bash
# Step 1 — start only infra (Postgres, Redis, Kafka, Mongo)
make dev          # runs docker-compose.infra.yml

# Step 2 — run each service you're working on
cd auth-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Or in IntelliJ: Edit Run Configuration → Active Profiles → local
```

### `docker` — full stack in containers
```bash
docker compose up --build          # docker-compose.yml sets SPRING_PROFILES_ACTIVE=docker
```
That's it. The `docker-compose.yml` injects the profile. The Dockerfile also has `ENV SPRING_PROFILES_ACTIVE=docker` as a fallback default.

### `prod` — CI/CD pipeline
Set via Kubernetes `env:` or ECS task definition:
```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: prod
  - name: DATABASE_URL
    valueFrom:
      secretKeyRef: { name: db-secret, key: url }
```
No hardcoded values exist in `application-prod.yml` — everything is `${ENV_VAR}` or `${ENV_VAR:default}`.

### `test` — unit & integration tests
Maven Surefire activates `test` automatically when it detects `src/test/resources/application-test.yml`.
```bash
./mvnw test                        # picks up application-test.yml automatically
./mvnw test -Dspring.profiles.active=test   # explicit override
```

---

## Infra Addresses Quick-Reference

| Service | `local` profile | `docker` profile | `prod` profile |
|---------|-----------------|-----------------|----------------|
| PostgreSQL | `localhost:5432` | `postgres:5432` | `${DATABASE_URL}` |
| MongoDB | `localhost:27017` | `mongodb:27017` | `${MONGO_URI}` |
| Redis | `localhost:6379` | `redis:6379` | `${REDIS_HOST}` |
| Kafka | `localhost:9092` | `kafka:29092` | `${KAFKA_BOOTSTRAP}` |
| Eureka | `localhost:8761` | `service-registry:8761` | `${EUREKA_URL}` |
| Auth Service | `localhost:8081` | `http://auth-service:8081` | `${AUTH_SERVICE_URL}` |
| Propertize | `localhost:8082` | `http://propertize:8082` | `${PROPERTIZE_URL}` |
| Employee | `localhost:8083` | `http://employee-service:8083` | `${EMPLOYEE_URL}` |

---

## What Lives Where: The Hard Rules

### `application.yml` (base / shared)
- Spring app name
- Actuator endpoints list
- Non-environment-specific Jackson, validation, MVC settings
- **No** hosts, ports, passwords

### `application-local.yml`
- All infra at `localhost:*`
- `logging.level` → DEBUG (verbose for dev)
- `app.security.bypass-platform-check: true`
- `tracing.enabled: false`
- **No** Docker DNS names like `postgres`, `redis`

### `application-docker.yml`
- All infra at Docker service names (`postgres`, `redis`, etc.)
- `logging.level` → INFO
- `app.security.bypass-platform-check: false`
- Passwords as `${ENV_VAR:docker-default}` — default matches docker-compose
- **No** `localhost`

### `application-prod.yml`
- Everything is `${ENV_VAR}` with **no** fallback defaults for secrets
- `hibernate.ddl-auto: validate` (never auto-create schema)
- `show-sql: false`
- `logging.level.root: WARN`
- **No** hardcoded passwords, URLs, or keys

---

## docker-compose.yml Philosophy

`docker-compose.yml` only sets:
1. `SPRING_PROFILES_ACTIVE: docker` — tells Spring which yml to load
2. **Actual secrets** as env vars (`REDIS_PASSWORD`, `SERVICE_API_KEY`) — values the docker profile yml reads via `${ENV_VAR:default}`

It does **NOT** override infra URLs with env vars anymore — those live entirely in `application-docker.yml`.

```yaml
# ✅ CORRECT — docker-compose.yml
environment:
  SPRING_PROFILES_ACTIVE: docker
  REDIS_PASSWORD: redis_secure_pass
  SERVICE_API_KEY: some-secret

# ❌ WRONG — repeating infra config as env vars
environment:
  SPRING_PROFILES_ACTIVE: local          # wrong profile for Docker
  SPRING_DATASOURCE_URL: jdbc:...        # this belongs in application-docker.yml
  SPRING_DATA_REDIS_HOST: redis          # this belongs in application-docker.yml
```

---

## RSA Key Paths by Profile

| Service | `local` | `docker` / `prod` |
|---------|---------|-------------------|
| auth-service | `auth-service/keys/private_key.pem` (relative) | `/app/keys/private_key.pem` (absolute in container) |
| propertize | `propertize/config/keys/public_key.pem` (relative) | `/app/config/keys/public_key.pem` (absolute) |
| api-gateway | `api-gateway/config/keys/public_key.pem` (relative) | `/app/config/keys/public_key.pem` (absolute) |
| employee-service | `employee-service/config/keys/public_key.pem` (relative) | `/app/config/keys/public_key.pem` (absolute) |

---

## FAQ

**Q: I changed a profile file, do I need to rebuild the Docker image?**  
Yes for `docker` profile — it's baked into the JAR. Run `docker compose up --build <service>`.

**Q: Can I override a single property without changing the yml?**  
Yes — env vars always win: `SPRING_DATASOURCE_URL=jdbc:... ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

**Q: What about `application-prod.yml` in the auth-service, it has `port: 8090` not `8081`. Bug?**  
Yes — `server.port: ${AUTH_SERVICE_PORT:8090}` should use `:8081`. But in prod the `AUTH_SERVICE_PORT` env var is set explicitly, so it's harmless. Fix it when touching that file next.

**Q: Where do I add a new environment-specific config key?**  
1. Add `key: ${ENV_VAR:local-default}` to `application.yml` (base)
2. Set a concrete localhost value in `application-local.yml`
3. Set a Docker container value in `application-docker.yml`
4. Set `key: ${ENV_VAR}` (no default) in `application-prod.yml`

---

*Last updated: March 2026*

