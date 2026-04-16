# RAG & Vector Log Analysis — Integration Guide

## Overview

Propertize integrates a **Retrieval-Augmented Generation (RAG) log analysis system** via a dedicated Python
microservice (`log-analyzer`) that runs alongside the existing Python services layer.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Log Sources                               │
│   PostgreSQL audit_log  ·  Kafka service-logs  ·  POST /ingest  │
└─────────────────────────┬───────────────────────────────────────┘
                          │ (auto-poll every 2 min)
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│              log-analyzer  (port 8094)                           │
│                                                                  │
│   ① Embedding          sentence-transformers/all-MiniLM-L6-v2   │
│   ② Vector Store       ChromaDB (persistent Docker volume)       │
│   ③ Pattern Analysis   Rule-based clustering & anomaly detection │
│   ④ LLM Synthesis      Configurable: none / openai / ollama      │
│                                                                  │
│   Endpoints:                                                     │
│     POST /logs/ingest          → store log entries               │
│     POST /logs/query           → semantic search + analysis      │
│     POST /logs/analyze         → deep pattern analysis           │
│     GET  /logs/stats           → index statistics                │
│     GET  /logs/recent-errors   → recent ERROR/WARN entries       │
│     POST /logs/bulk-ingest-from-db  → manual DB sync trigger     │
│     GET  /health               → service health                  │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼ via gateway /api/v1/log-analysis/**
┌─────────────────────────────────────────────────────────────────┐
│                  Frontend — Log Analyzer Page                    │
│   /dashboard/log-analyzer                                        │
│   Tabs: Semantic Search · Recent Errors · Pattern Analysis · Stats │
└─────────────────────────────────────────────────────────────────┘
```

## Port Assignment

| Service           | Port  |
|-------------------|-------|
| report-service    | 8090  |
| vendor-matching   | 8091  |
| document-service  | 8092  |
| search-reranker   | 8093  |
| **log-analyzer**  | **8094** |

## How RAG Works Here

1. **Ingestion**: Log entries (from audit_log table, Kafka, or direct API POST) are converted to a
   rich text representation: `[LEVEL] service=X | message | exception=Y | logger=Z`, then
   embedded using sentence-transformers into 384-dimensional vectors and stored in ChromaDB.

2. **Retrieval**: A semantic query (natural language or error snippet) is embedded with the same
   model, and the top-K most similar log vectors are retrieved via cosine similarity.

3. **Augmented Generation**: The retrieved log entries are analysed with rule-based pattern
   detection (frequency trends, service correlation, exception classification, known-issue matching).
   Optionally, the context is sent to an LLM for a natural-language diagnostic summary.

## Enabling LLM Synthesis

By default `LLM_PROVIDER=none` — only semantic search + rule-based analysis runs (no API keys needed).

### OpenAI (GPT-4o-mini)
```yaml
# docker-compose.yml — log-analyzer environment:
LLM_PROVIDER: openai
OPENAI_API_KEY: sk-...
```

### Ollama (local model, free)
```bash
# Run Ollama on the host
ollama pull llama3.2
ollama serve   # listens on :11434
```
```yaml
# docker-compose.yml — log-analyzer environment:
LLM_PROVIDER: ollama
OLLAMA_URL: http://host.docker.internal:11434
OLLAMA_MODEL: llama3.2
```

## API Gateway Route

Requests to `/api/v1/log-analysis/**` are proxied by the gateway to `log-analyzer:8094/logs/**`:

```yaml
- id: log-analyzer
  uri: http://log-analyzer:8094
  predicates:
    - Path=/api/v1/log-analysis/**
  filters:
    - RewritePath=/api/v1/log-analysis/(?<segment>.*), /logs/${segment}
```

## Frontend Usage

Navigate to **Dashboard → Management → Log Analyzer** (requires `PLATFORM_*` or `ORGANIZATION_OWNER/ADMIN` role).

### Semantic Search
Ask natural-language questions about your system:
- *"Why are JWT validations failing?"*
- *"What is causing HTTP 431 errors?"*
- *"Show me database connection pool issues"*

Results include:
- Relevance-ranked log entries
- Affected services + level distribution
- Trend direction (increasing/stable)
- Suggested investigation approach
- LLM-generated diagnosis (if configured)

### Pattern Analysis
Enter a specific error keyword or exception:
- *"HikariPool"*, *"JWT signature"*, *"NullPointerException"*

Get:
- Occurrence count over last 24h
- Frequency per hour (and trend direction)
- First/last seen timestamps
- Exception class breakdown
- Fix suggestion from built-in knowledge base

### Statistics
Real-time index stats:
- Total logs indexed
- Distribution by service and log level
- Error/warn rates in last hour
- Services currently experiencing errors

### Manual Sync
The `Sync Logs` button calls `POST /logs/bulk-ingest-from-db` to immediately pull the latest
entries from PostgreSQL `audit_log` (auto-sync runs every 2 minutes in the background).

## Known Error Pattern Knowledge Base

The service ships with a built-in knowledge base for common patterns:

| Pattern | Suggestion |
|---------|-----------|
| `JWT signature` | JWT key mismatch — verify RSA key sync |
| `Connection refused` | Service or DB unreachable — check health checks |
| `HikariPool` | DB connection pool exhausted — increase max-pool-size |
| `CircuitBreaker.*OPEN` | Downstream service unhealthy |
| `SerializationException` | Redis cache deserialization — flush cache |
| `FlywayException` | Database migration failed — check SQL syntax |
| `431` / `RequestHeaderFieldsTooLarge` | Headers too large — strip Cookie headers |
| `OutOfMemoryError` | JVM heap exhausted — tune -Xmx |

To extend the knowledge base, edit `_KNOWN_PATTERNS` dict in `log-analyzer/main.py`.

## ChromaDB Persistence

Vector data persists in a named Docker volume `log-analyzer-chroma` mounted at `/tmp/chroma_data`.
Data survives container restarts. To reset the index:

```bash
curl -X DELETE "http://localhost:8094/logs/reset?confirm=yes"
```

## Running Locally (without Docker)

```bash
cd python-services/log-analyzer
pip install -r requirements.txt
DATABASE_URL=postgresql://ravishah@localhost:5432/propertize_db \
LOG_ANALYZER_PORT=8094 \
python main.py
```

## Java Integration (optional)

To push real-time logs from Java services directly to the analyzer, add a logback appender or
use the existing `PythonServiceClient` pattern. Example:

```java
// In any @Service, inject PythonLogAnalyzerClient (extend existing pattern):
pythonLogAnalyzerClient.ingestLog(LogEntry.builder()
    .service("propertize")
    .level("ERROR")
    .message(exception.getMessage())
    .exception(ExceptionUtils.getStackTrace(exception))
    .correlationId(MDC.get("correlationId"))
    .build());
```

