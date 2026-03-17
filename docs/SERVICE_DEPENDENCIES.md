# Service Startup Order & Dependencies

This document explains the startup sequence and dependencies of all services in the Propertize platform.

## Startup Sequence

```
Level 1: Infrastructure (No Dependencies)
├── PostgreSQL
├── MongoDB
├── Redis
└── Zookeeper
    └── Level 2: Message Broker
        └── Kafka
            └── Kafka UI

Level 3: Service Discovery
└── Service Registry (Eureka)

Level 4: Core Services
├── Auth Service (depends on: postgres, redis, eureka)
└── Level 5: Business Services
    ├── Propertize Service (depends on: postgres, mongodb, redis, kafka, eureka, auth)
    └── Employee Service (depends on: postgres, redis, eureka, auth)

Level 6: Gateway Layer
└── API Gateway (depends on: eureka, auth, propertize, employee)

Level 7: Frontend
└── Wagecraft Frontend (depends on: api-gateway)

Level 8: Management UIs (Optional)
├── Adminer (depends on: postgres)
└── Mongo Express (depends on: mongodb)
```

## Health Check Wait Times

| Service          | Start Period | Check Interval | Purpose                |
| ---------------- | ------------ | -------------- | ---------------------- |
| PostgreSQL       | -            | 10s            | Database ready         |
| MongoDB          | -            | 10s            | Database ready         |
| Redis            | -            | 10s            | Cache ready            |
| Kafka            | -            | 30s            | Message broker ready   |
| Service Registry | 60s          | 30s            | Discovery server ready |
| Auth Service     | 90s          | 30s            | Authentication ready   |
| Propertize       | 120s         | 30s            | Main service ready     |
| Employee Service | 90s          | 30s            | Employee service ready |
| API Gateway      | 90s          | 30s            | Gateway routing ready  |
| Frontend         | 10s          | 30s            | UI accessible          |

## Dependency Graph

```mermaid
graph TD
    PG[PostgreSQL] --> AUTH[Auth Service]
    PG --> PROP[Propertize Service]
    PG --> EMP[Employee Service]

    MONGO[MongoDB] --> PROP
    REDIS[Redis] --> AUTH
    REDIS --> PROP
    REDIS --> EMP

    ZK[Zookeeper] --> KAFKA[Kafka]
    KAFKA --> PROP

    REG[Service Registry] --> AUTH
    REG --> PROP
    REG --> EMP
    REG --> GW[API Gateway]

    AUTH --> PROP
    AUTH --> EMP
    AUTH --> GW

    PROP --> GW
    EMP --> GW

    GW --> FE[Frontend]
```

## Critical Paths

### Path 1: User Authentication

```
Frontend → API Gateway → Auth Service → PostgreSQL
                                    └→ Redis
```

### Path 2: Property Management

```
Frontend → API Gateway → Propertize Service → PostgreSQL
                                          └→ MongoDB
                                          └→ Kafka
                                          └→ Auth Service
```

### Path 3: Employee Management

```
Frontend → API Gateway → Employee Service → PostgreSQL
                                        └→ Auth Service
```

## Startup Time Estimate

- **Minimal (Infrastructure + Registry)**: ~30 seconds
- **Core Services (+ Auth)**: ~90 seconds
- **Full Platform**: ~2-3 minutes
- **First Build**: ~5-10 minutes (depends on internet speed)

## Service Restart Impact

| Service          | Impact     | Affected Services             |
| ---------------- | ---------- | ----------------------------- |
| PostgreSQL       | **HIGH**   | Auth, Propertize, Employee    |
| MongoDB          | **MEDIUM** | Propertize                    |
| Redis            | **MEDIUM** | Auth, Propertize, Employee    |
| Kafka            | **MEDIUM** | Propertize (async features)   |
| Service Registry | **HIGH**   | All microservices             |
| Auth Service     | **HIGH**   | Propertize, Employee, Gateway |
| Propertize       | **LOW**    | Only property features        |
| Employee         | **LOW**    | Only employee features        |
| API Gateway      | **HIGH**   | Frontend (all requests)       |
| Frontend         | **LOW**    | Only UI                       |

## Resource Requirements

### Minimum

- **RAM**: 8 GB
- **CPU**: 4 cores
- **Disk**: 20 GB

### Recommended

- **RAM**: 16 GB
- **CPU**: 8 cores
- **Disk**: 50 GB

### Per Service Memory Allocation

| Service           | Memory Limit | Notes                           |
| ----------------- | ------------ | ------------------------------- |
| PostgreSQL        | 512 MB       | Can increase for large datasets |
| MongoDB           | 512 MB       | Can increase for large datasets |
| Redis             | 256 MB       | Adjust based on cache size      |
| Kafka             | 1 GB         | Message buffer                  |
| Each Java Service | 1 GB         | -XX:MaxRAMPercentage=75.0       |
| Frontend (Nginx)  | 128 MB       | Static files                    |

## Network Communication

All services communicate via the `propertize-network` bridge network:

- **Internal Communication**: Service name DNS resolution
- **External Access**: Port mapping to host
- **Security**: Isolated from other Docker networks

## Scaling Recommendations

### Horizontal Scaling (Multiple Instances)

```yaml
# docker-compose.override.yml
services:
  propertize:
    deploy:
      replicas: 3

  employee-service:
    deploy:
      replicas: 2
```

### Vertical Scaling (More Resources)

```yaml
# docker-compose.override.yml
services:
  propertize:
    mem_limit: 2g
    cpus: 2.0
```

## Disaster Recovery

### Backup Important Data

```bash
# PostgreSQL
docker exec propertize-postgres pg_dump -U propertize_user propertize_db > backup.sql

# MongoDB
docker exec propertize-mongodb mongodump --uri="mongodb://admin:password@localhost:27017" --out=/backup

# Redis
docker exec propertize-redis redis-cli --rdb /data/dump.rdb SAVE
```

### Recovery Order

1. Start infrastructure (postgres, mongodb, redis)
2. Restore databases
3. Start service registry
4. Start application services
5. Start gateway and frontend

---

**Last Updated**: February 2026
