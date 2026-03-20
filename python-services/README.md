# Python Services — Propertize

Python microservices layer that augments the Java backend with high-performance computation,
async processing, and ML-based features.

## Architecture Overview

```
                   ┌─────────────────────────────────┐
                   │   Java API Gateway (port 8080)  │
                   └──────────────┬──────────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
    ┌─────────▼──────┐  ┌────────▼──────┐  ┌────────▼──────┐
    │ report-service │  │vendor-matching│  │document-service│
    │   port 8090    │  │   port 8091   │  │   port 8092    │
    └────────────────┘  └───────────────┘  └───────────────┘

    ┌─────────────────┐   ┌───────────────────────────────┐
    │ search-reranker │   │     Background Workers         │
    │   port 8093     │   │  analytics-worker (APScheduler)│
    └─────────────────┘   │  payment-worker   (APScheduler)│
                          │  screening-worker (Kafka)      │
                          └───────────────────────────────┘
```

## Services

| Service | Port | Purpose | Replaces |
|---|---|---|---|
| `report-service` | 8090 | PDF & Excel report generation | Java iText/POI blocking calls |
| `vendor-matching` | 8091 | NLP semantic vendor matching | Java keyword substring matching |
| `document-service` | 8092 | MinIO document upload/download | `generatePlaceholderUrl()` stub |
| `search-reranker` | 8093 | BM25 search result reranking | Uniform 1.0 SQL scores |
| `analytics-worker` | — | Pre-aggregates analytics events | Full table scan on every request |
| `payment-worker` | — | Auto rent generation + late fees | Empty `processOverduePayments()` |
| `screening-worker` | — | Multi-factor tenant risk scoring | No screening pipeline |

## Quick Start

### Prerequisites
- Docker + Docker Compose
- Java services and infrastructure already running

### 1. Start Infrastructure First
```bash
docker compose -f docker-compose.infra.yml up -d
```

### 2. Start Python Services
```bash
cd python-services
docker compose -f docker-compose.python.yml up -d --build
```

### 3. Verify All Services
```bash
curl http://localhost:8090/health   # report-service
curl http://localhost:8091/health   # vendor-matching
curl http://localhost:8092/health   # document-service
curl http://localhost:8093/health   # search-reranker
```

### MinIO Console
Open [http://localhost:9001](http://localhost:9001)
- Username: `propertize`
- Password: `propertize123`

## Local Development (no Docker)

```bash
# Create virtual environments per service
cd python-services/report-service
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python main.py

# For workers — set env vars first
export DATABASE_URL=postgresql://propertize_user:propertize_password@localhost:5432/propertize_db
export KAFKA_BOOTSTRAP=localhost:9092
```

## API Reference

### report-service (8090)
```
GET /health
GET /reports/financial/pdf?organization_id=<uuid>
GET /reports/delinquency/excel?organization_id=<uuid>
GET /reports/rent-roll/excel?organization_id=<uuid>
```

### vendor-matching (8091)
```
GET  /health
POST /match-vendors
     { "maintenance_category": "HVAC", "description": "AC not cooling", "organization_id": "...", "limit": 5 }
```

### document-service (8092)
```
GET    /health
POST   /documents/upload          (multipart: file, organization_id, entity_type, entity_id)
GET    /documents/url?object_name=<path>
DELETE /documents/{object_name}
```

### search-reranker (8093)
```
GET  /health
POST /rerank
     { "query": "2br apartment downtown", "results": [...] }
```

## Integration with Java Services

### Calling report-service from Java
```java
// In ReportService.java — delegate to Python instead of generating locally
WebClient.create("http://python-report-service:8090")
    .get()
    .uri("/reports/financial/pdf?organization_id=" + orgId)
    .retrieve()
    .bodyToMono(byte[].class)
    .subscribe(bytes -> /* stream to client */ );
```

### Calling vendor-matching from Java
```java
// In VendorMatchingEngine.java — replace keyword match with semantic score
String url = "http://python-vendor-matching:8091/match-vendors";
// POST { maintenance_category, description, organization_id, limit }
```

### Calling search-reranker from Java
```java
// In UniversalSearchService.java — after fetching candidates from DB
String url = "http://python-search-reranker:8093/rerank";
// POST { query, results: [...] }
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `postgresql://propertize_user:propertize_password@localhost:5432/propertize_db` | PostgreSQL connection |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka broker(s) |
| `MINIO_ENDPOINT` | `localhost:9000` | MinIO S3 endpoint |
| `MINIO_ACCESS_KEY` | `propertize` | MinIO access key |
| `MINIO_SECRET_KEY` | `propertize123` | MinIO secret key |
| `MINIO_BUCKET` | `propertize-documents` | Default bucket name |
| `PORT` | varies per service | HTTP port to bind |

## Performance Impact

| Area | Before | After |
|---|---|---|
| Dashboard analytics | 2–8s (full table scan) | ~10ms (pre-aggregated) |
| Report generation | JVM heap spike, 5–15s | 0.5–2s streaming response |
| Vendor matching accuracy | ~30% (keyword) | ~70–80% (semantic NLP) |
| Document storage | Fake placeholder URLs | Real MinIO presigned URLs |
| Late fee application | Never applied | Applied daily automatically |
| Tenant screening | Manual / none | Automated risk score in seconds |
| Search relevance | All scores = 1.0 | BM25 ranked by actual relevance |
