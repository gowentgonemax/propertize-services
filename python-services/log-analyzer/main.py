"""
Log Analyzer Microservice — FastAPI + ChromaDB + SentenceTransformers
=======================================================================
RAG-powered log analysis for Propertize microservices platform.

Features:
  • Vector-based semantic log search (ChromaDB + all-MiniLM-L6-v2)
  • Background ingestion from PostgreSQL audit_log table (every 2 min)
  • Kafka consumer for real-time log streaming
  • Pattern analysis: error clustering, anomaly detection, frequency trends
  • Optional LLM synthesis (LLM_PROVIDER=none|openai|ollama)
  • REST API consumed by Java services and the Next.js frontend

Port: 8094  (LOG_ANALYZER_PORT env var)
"""

import sys
import os
import re
import json
import logging
import threading
import time
from collections import defaultdict
from datetime import datetime, timedelta
from typing import List, Optional, Dict, Any

import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import uvicorn

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.db import get_connection
from shared.config import DATABASE_URL, KAFKA_BOOTSTRAP, LOG_ANALYZER_PORT

# ─── Logging Setup ───────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [log-analyzer] %(message)s",
)
log = logging.getLogger(__name__)

# ─── FastAPI App ──────────────────────────────────────────────────────────────
app = FastAPI(
    title="Propertize Log Analyzer",
    description="RAG-powered semantic log analysis and anomaly detection",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── Config ───────────────────────────────────────────────────────────────────
CHROMA_PERSIST_DIR = os.getenv("CHROMA_PERSIST_DIR", "/tmp/chroma_data")
COLLECTION_NAME = "propertize_logs"
EMBED_MODEL_NAME = os.getenv("EMBED_MODEL", "all-MiniLM-L6-v2")
LLM_PROVIDER = os.getenv("LLM_PROVIDER", "none")        # none | openai | ollama
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2")
INGEST_INTERVAL_SECONDS = int(os.getenv("INGEST_INTERVAL_SECONDS", "120"))
MAX_DOCS_PER_INGEST = int(os.getenv("MAX_DOCS_PER_INGEST", "500"))
KAFKA_LOG_TOPIC = os.getenv("KAFKA_LOG_TOPIC", "service-logs")

# ─── Lazy-loaded Singletons ──────────────────────────────────────────────────
_embed_model = None
_chroma_client = None
_chroma_collection = None


def get_embed_model():
    global _embed_model
    if _embed_model is None:
        log.info("Loading SentenceTransformer model '%s'...", EMBED_MODEL_NAME)
        try:
            from sentence_transformers import SentenceTransformer
            _embed_model = SentenceTransformer(EMBED_MODEL_NAME)
            log.info("Embedding model loaded ✓")
        except ImportError:
            log.warning("sentence-transformers not installed — using keyword fallback")
            _embed_model = "keyword_fallback"
    return _embed_model


def get_chroma():
    global _chroma_client, _chroma_collection
    if _chroma_client is None:
        import chromadb
        os.makedirs(CHROMA_PERSIST_DIR, exist_ok=True)
        _chroma_client = chromadb.PersistentClient(path=CHROMA_PERSIST_DIR)
        _chroma_collection = _chroma_client.get_or_create_collection(
            name=COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"},
        )
        log.info("ChromaDB initialized at '%s' — collection: %s (%d docs)",
                 CHROMA_PERSIST_DIR, COLLECTION_NAME, _chroma_collection.count())
    return _chroma_client, _chroma_collection


def embed_texts(texts: List[str]) -> List[List[float]]:
    model = get_embed_model()
    if model == "keyword_fallback":
        # TF-like bag-of-words sparse vector (simple fallback)
        vocab = {}
        for text in texts:
            for w in text.lower().split():
                if w not in vocab:
                    vocab[w] = len(vocab)
        dim = max(len(vocab), 1)
        vecs = []
        for text in texts:
            v = [0.0] * dim
            for w in text.lower().split():
                if w in vocab:
                    v[vocab[w]] += 1.0
            norm = sum(x * x for x in v) ** 0.5 or 1.0
            vecs.append([x / norm for x in v])
        return vecs
    return model.encode(texts, show_progress_bar=False).tolist()


# ─── Pydantic Models ─────────────────────────────────────────────────────────

class LogEntry(BaseModel):
    id: Optional[str] = None
    service: str = "unknown"
    level: str = "INFO"          # TRACE|DEBUG|INFO|WARN|ERROR|FATAL
    message: str
    timestamp: Optional[str] = None
    correlation_id: Optional[str] = None
    trace_id: Optional[str] = None
    exception: Optional[str] = None
    thread: Optional[str] = None
    logger: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None


class IngestRequest(BaseModel):
    entries: List[LogEntry]
    source: str = "api"          # api | audit_log | kafka | file


class QueryRequest(BaseModel):
    query: str
    n_results: int = Field(default=10, ge=1, le=50)
    filters: Optional[Dict[str, str]] = None   # e.g. {"service": "propertize", "level": "ERROR"}
    time_range_hours: Optional[int] = None      # look back N hours
    include_analysis: bool = True


class AnalyzeRequest(BaseModel):
    pattern: str                # e.g. "JWT signature validation failed"
    service: Optional[str] = None
    look_back_hours: int = 24
    n_results: int = 20


class LogQueryResult(BaseModel):
    log_id: str
    service: str
    level: str
    message: str
    timestamp: Optional[str]
    correlation_id: Optional[str]
    exception: Optional[str]
    relevance_score: float
    metadata: Optional[Dict[str, Any]]


class AnalysisResult(BaseModel):
    query: str
    total_found: int
    results: List[LogQueryResult]
    pattern_analysis: Optional[Dict[str, Any]] = None
    llm_summary: Optional[str] = None


class PatternAnalysis(BaseModel):
    pattern: str
    occurrence_count: int
    services_affected: List[str]
    first_seen: Optional[str]
    last_seen: Optional[str]
    frequency_per_hour: float
    is_increasing: bool
    common_exceptions: List[str]
    suggested_investigation: str
    similar_patterns: List[str]


class LogStats(BaseModel):
    total_indexed: int
    by_service: Dict[str, int]
    by_level: Dict[str, int]
    error_rate_last_hour: float
    warn_rate_last_hour: float
    top_error_messages: List[Dict[str, Any]]
    services_with_errors: List[str]
    indexed_since: Optional[str]


# ─── Core Ingestion ──────────────────────────────────────────────────────────

def _build_doc_text(entry: LogEntry) -> str:
    """Compose a rich text representation of a log entry for embedding."""
    parts = [
        f"[{entry.level}]",
        f"service={entry.service}",
        entry.message,
    ]
    if entry.exception:
        # Include only first line of exception (class name + message)
        exc_first = entry.exception.split("\n")[0]
        parts.append(f"exception={exc_first}")
    if entry.logger:
        parts.append(f"logger={entry.logger}")
    return " | ".join(parts)


def ingest_entries(entries: List[LogEntry], source: str = "api") -> int:
    """Embed and store log entries in ChromaDB. Returns count of new docs."""
    if not entries:
        return 0

    _, col = get_chroma()

    doc_texts, doc_ids, doc_metas = [], [], []
    for e in entries:
        doc_id = e.id or f"{e.service}-{e.timestamp or datetime.utcnow().isoformat()}-{hash(e.message) & 0xFFFFFF:06x}"
        # Skip duplicates
        try:
            existing = col.get(ids=[doc_id])
            if existing["ids"]:
                continue
        except Exception:
            pass

        ts = e.timestamp or datetime.utcnow().isoformat()
        meta = {
            "service": e.service or "unknown",
            "level": (e.level or "INFO").upper(),
            "timestamp": ts,
            "correlation_id": e.correlation_id or "",
            "trace_id": e.trace_id or "",
            "has_exception": "true" if e.exception else "false",
            "source": source,
            "logger": e.logger or "",
        }
        if e.metadata:
            for k, v in e.metadata.items():
                if isinstance(v, (str, int, float, bool)):
                    meta[str(k)] = str(v)

        doc_texts.append(_build_doc_text(e))
        doc_ids.append(doc_id)
        doc_metas.append(meta)

    if not doc_texts:
        return 0

    # Batch embed
    BATCH = 64
    embeddings = []
    for i in range(0, len(doc_texts), BATCH):
        embeddings.extend(embed_texts(doc_texts[i : i + BATCH]))

    col.add(
        documents=doc_texts,
        embeddings=embeddings,
        ids=doc_ids,
        metadatas=doc_metas,
    )
    log.info("Ingested %d new log entries (source=%s)", len(doc_texts), source)
    return len(doc_texts)


# ─── Background: PostgreSQL audit_log polling ────────────────────────────────

_last_ingest_ts: Optional[str] = None


def _ingest_from_postgres():
    global _last_ingest_ts
    try:
        since = _last_ingest_ts or (
            datetime.utcnow() - timedelta(hours=24)
        ).isoformat()

        with get_connection() as conn:
            df = pd.read_sql("""
                SELECT
                    al.id::text,
                    COALESCE(al.service_name, 'propertize') AS service,
                    UPPER(COALESCE(al.status, 'INFO')) AS level,
                    COALESCE(al.action || ': ' || al.resource_type, al.action, 'audit') AS message,
                    al.created_at::text AS timestamp,
                    al.correlation_id,
                    al.user_id::text AS trace_id,
                    al.error_message AS exception
                FROM audit_log al
                WHERE al.created_at > %s
                ORDER BY al.created_at DESC
                LIMIT %s
            """, conn, params=[since, MAX_DOCS_PER_INGEST])

        if df.empty:
            return

        entries = [
            LogEntry(
                id=row["id"],
                service=row.get("service", "propertize"),
                level=_normalize_level(str(row.get("level", "INFO"))),
                message=str(row.get("message", "")),
                timestamp=str(row.get("timestamp", "")),
                correlation_id=str(row.get("correlation_id", "")) if row.get("correlation_id") else None,
                trace_id=str(row.get("trace_id", "")) if row.get("trace_id") else None,
                exception=str(row.get("exception", "")) if row.get("exception") else None,
            )
            for _, row in df.iterrows()
        ]

        count = ingest_entries(entries, source="audit_log")
        if count > 0:
            _last_ingest_ts = datetime.utcnow().isoformat()
            log.info("Auto-ingested %d audit log entries from PostgreSQL", count)

    except Exception as exc:
        log.warning("Postgres ingestion skipped: %s", exc)


def _normalize_level(raw: str) -> str:
    mapping = {
        "SUCCESS": "INFO", "FAILURE": "ERROR", "COMPLETED": "INFO",
        "PENDING": "INFO", "ERROR": "ERROR", "WARN": "WARN",
        "WARNING": "WARN", "INFO": "INFO", "DEBUG": "DEBUG",
        "TRACE": "TRACE", "FATAL": "FATAL",
    }
    return mapping.get(raw.upper(), "INFO")


def _start_background_ingestion():
    """Polls PostgreSQL every INGEST_INTERVAL_SECONDS in a daemon thread."""
    def loop():
        # Wait for startup before first poll
        time.sleep(30)
        while True:
            _ingest_from_postgres()
            time.sleep(INGEST_INTERVAL_SECONDS)

    t = threading.Thread(target=loop, daemon=True, name="log-ingest-worker")
    t.start()
    log.info("Background log ingestion started (interval=%ds)", INGEST_INTERVAL_SECONDS)


# ─── Kafka Consumer ───────────────────────────────────────────────────────────

def _start_kafka_consumer():
    """
    Subscribes to KAFKA_LOG_TOPIC and ingests log entries in real-time.
    Each message is expected to be a JSON object matching the LogEntry schema.
    Falls back gracefully if Kafka is unavailable so the service still starts.
    """
    if not KAFKA_BOOTSTRAP:
        log.info("KAFKA_BOOTSTRAP not set — Kafka consumer disabled")
        return

    def consumer_loop():
        # Delay startup to give Kafka time to become ready
        time.sleep(45)
        while True:
            try:
                from kafka import KafkaConsumer
                consumer = KafkaConsumer(
                    KAFKA_LOG_TOPIC,
                    bootstrap_servers=KAFKA_BOOTSTRAP.split(","),
                    group_id="log-analyzer-consumer",
                    auto_offset_reset="latest",
                    enable_auto_commit=True,
                    value_deserializer=lambda m: m.decode("utf-8", errors="replace"),
                    consumer_timeout_ms=5000,
                    # Reconnect automatically on broker restart
                    reconnect_backoff_max_ms=10_000,
                )
                log.info("Kafka consumer connected — topic=%s bootstrap=%s", KAFKA_LOG_TOPIC, KAFKA_BOOTSTRAP)

                while True:
                    try:
                        batch: list[LogEntry] = []
                        for message in consumer:
                            try:
                                payload = json.loads(message.value)
                                # Accept either a single LogEntry dict or a list
                                if isinstance(payload, list):
                                    for item in payload:
                                        batch.append(LogEntry(**item))
                                else:
                                    batch.append(LogEntry(**payload))
                                if len(batch) >= 50:
                                    break
                            except Exception as parse_err:
                                log.debug("Kafka message parse error: %s", parse_err)

                        if batch:
                            ingested = ingest_entries(batch, source="kafka")
                            log.debug("Kafka: ingested %d entries from topic %s", ingested, KAFKA_LOG_TOPIC)

                    except Exception as poll_err:
                        log.warning("Kafka poll error: %s", poll_err)
                        time.sleep(5)

            except ImportError:
                log.warning("kafka-python not installed — Kafka consumer disabled")
                return
            except Exception as connect_err:
                log.warning("Kafka consumer connection failed (%s) — retrying in 30s", connect_err)
                time.sleep(30)

    t = threading.Thread(target=consumer_loop, daemon=True, name="kafka-log-consumer")
    t.start()
    log.info("Kafka consumer thread started — topic=%s", KAFKA_LOG_TOPIC)


# ─── Pattern / Anomaly Analysis ──────────────────────────────────────────────

_KNOWN_PATTERNS = {
    r"JWT signature": "JWT key mismatch — verify RSA key sync across all services",
    r"Connection refused": "Service or DB unreachable — check Docker networking and health checks",
    r"NullPointerException": "Unhandled null — add null guards; check @NonNull annotations",
    r"could not execute statement": "DB constraint violation — check FK relations and nullable columns",
    r"HikariPool.*Connection is not available": "DB connection pool exhausted — increase max-pool-size or check for leaks",
    r"UnsupportedOperationException": "@Immutable entity update attempted — use native SQL instead",
    r"CircuitBreaker.*OPEN": "Circuit breaker tripped — downstream service is unhealthy",
    r"401|Unauthorized|forbidden|403": "Auth failure — verify JWT token, RBAC permissions, and header forwarding",
    r"OutOfMemoryError": "JVM heap exhausted — tune -Xmx or fix memory leak",
    r"StackOverflowError": "Infinite recursion — check circular service dependencies",
    r"duplicate key": "Unique constraint violation — check idempotency and upsert logic",
    r"SerializationException": "Redis cache deserialization failure — flush cache or update serializer",
    r"FlywayException|Migration": "Database migration failed — check Flyway migration SQL syntax",
    r"RequestHeaderFieldsTooLarge|431": "HTTP 431 — JWT/cookie headers too large; strip Cookie headers in gateway",
}


def _suggest_investigation(message: str, exception: Optional[str] = None) -> str:
    combined = f"{message} {exception or ''}".lower()
    for pattern, suggestion in _KNOWN_PATTERNS.items():
        if re.search(pattern, combined, re.IGNORECASE):
            return suggestion
    return "Review stack traces and correlation IDs in related log entries for root cause analysis"


def _extract_exception_class(text: str) -> Optional[str]:
    """Extract the first exception class name from a log message or stack trace."""
    match = re.search(
        r"((?:[A-Z]\w+\.)*[A-Z]\w*Exception|[A-Z]\w*Error)\b", text
    )
    return match.group(1) if match else None


def _analyze_results(query: str, results: List[LogQueryResult]) -> Dict[str, Any]:
    """Rule-based analysis of retrieved log entries."""
    if not results:
        return {"summary": "No similar log entries found."}

    # Service distribution
    service_counts: Dict[str, int] = defaultdict(int)
    level_counts: Dict[str, int] = defaultdict(int)
    timestamps = []
    exceptions: List[str] = []

    for r in results:
        service_counts[r.service] += 1
        level_counts[r.level] += 1
        if r.timestamp:
            try:
                timestamps.append(datetime.fromisoformat(r.timestamp.replace("Z", "+00:00")))
            except Exception:
                pass
        if r.exception:
            exc_cls = _extract_exception_class(r.exception)
            if exc_cls:
                exceptions.append(exc_cls)

    # Time distribution
    is_increasing = False
    if len(timestamps) >= 4:
        timestamps.sort()
        mid = len(timestamps) // 2
        first_half = timestamps[:mid]
        second_half = timestamps[mid:]
        if first_half and second_half:
            first_rate = len(first_half) / max(
                (first_half[-1] - first_half[0]).total_seconds() / 3600, 0.01
            )
            second_rate = len(second_half) / max(
                (second_half[-1] - second_half[0]).total_seconds() / 3600, 0.01
            )
            is_increasing = second_rate > first_rate * 1.5

    # Top exception classes
    exc_counts: Dict[str, int] = defaultdict(int)
    for e in exceptions:
        exc_counts[e] += 1
    top_exceptions = sorted(exc_counts, key=exc_counts.__getitem__, reverse=True)[:5]

    # Suggested investigation
    combined_text = " ".join(r.message for r in results[:5])
    suggestion = _suggest_investigation(combined_text)

    return {
        "total_matches": len(results),
        "services_affected": dict(service_counts),
        "level_distribution": dict(level_counts),
        "is_increasing": is_increasing,
        "time_span": {
            "first": timestamps[0].isoformat() if timestamps else None,
            "last": timestamps[-1].isoformat() if timestamps else None,
        },
        "common_exceptions": top_exceptions,
        "suggested_investigation": suggestion,
        "severity": "CRITICAL" if level_counts.get("FATAL", 0) > 0 or level_counts.get("ERROR", 0) > 5
        else "HIGH" if level_counts.get("ERROR", 0) > 0
        else "MEDIUM" if level_counts.get("WARN", 0) > 3
        else "LOW",
    }


# ─── Optional LLM Synthesis ──────────────────────────────────────────────────

def _llm_synthesize(query: str, context_entries: List[LogQueryResult]) -> Optional[str]:
    """Generate a natural language summary using configured LLM provider."""
    if LLM_PROVIDER == "none" or not context_entries:
        return None

    context_text = "\n".join(
        f"[{e.service}/{e.level}] {e.message}" + (f" | {e.exception.split(chr(10))[0]}" if e.exception else "")
        for e in context_entries[:8]
    )
    prompt = (
        f"You are a platform reliability engineer analyzing microservice logs.\n"
        f"Question: {query}\n\n"
        f"Relevant log entries:\n{context_text}\n\n"
        f"Provide a concise diagnosis (3-5 sentences): root cause, affected services, "
        f"and recommended fix."
    )

    try:
        if LLM_PROVIDER == "openai" and OPENAI_API_KEY:
            import openai
            client = openai.OpenAI(api_key=OPENAI_API_KEY)
            resp = client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[{"role": "user", "content": prompt}],
                max_tokens=300,
                temperature=0.2,
            )
            return resp.choices[0].message.content.strip()

        elif LLM_PROVIDER == "ollama":
            import httpx
            resp = httpx.post(
                f"{OLLAMA_URL}/api/generate",
                json={"model": OLLAMA_MODEL, "prompt": prompt, "stream": False},
                timeout=30.0,
            )
            if resp.status_code == 200:
                return resp.json().get("response", "").strip()

    except Exception as exc:
        log.warning("LLM synthesis failed (%s): %s", LLM_PROVIDER, exc)
    return None


# ─── API Routes ──────────────────────────────────────────────────────────────

@app.on_event("startup")
def startup_event():
    """Warm up ChromaDB and start background workers."""
    try:
        get_chroma()
    except Exception as e:
        log.warning("ChromaDB warmup failed: %s", e)
    _start_background_ingestion()
    _start_kafka_consumer()


@app.post("/logs/ingest", summary="Ingest log entries")
def ingest_logs(req: IngestRequest):
    """
    Batch-ingest log entries into the vector store.
    Called by Java services, Kafka consumers, or file-based importers.
    """
    count = ingest_entries(req.entries, source=req.source)
    _, col = get_chroma()
    return {
        "ingested": count,
        "skipped": len(req.entries) - count,
        "total_indexed": col.count(),
    }


@app.post("/logs/query", response_model=AnalysisResult, summary="Semantic log search + analysis")
def query_logs(req: QueryRequest):
    """
    Semantic search over indexed logs. Optionally returns pattern analysis
    and LLM-generated summary.
    """
    _, col = get_chroma()
    if col.count() == 0:
        return AnalysisResult(query=req.query, total_found=0, results=[],
                              pattern_analysis={"note": "No logs indexed yet. Submit logs via /logs/ingest or wait for auto-ingestion."})

    # Build ChromaDB where clause
    where: Optional[Dict] = None
    if req.filters:
        conditions = []
        for k, v in req.filters.items():
            conditions.append({k: {"$eq": v}})
        where = {"$and": conditions} if len(conditions) > 1 else conditions[0]

    # Embed query
    query_vec = embed_texts([req.query])[0]

    # Retrieve
    try:
        raw = col.query(
            query_embeddings=[query_vec],
            n_results=min(req.n_results, col.count()),
            where=where,
            include=["documents", "metadatas", "distances"],
        )
    except Exception as exc:
        log.error("ChromaDB query failed: %s", exc)
        raise HTTPException(status_code=500, detail=f"Vector search error: {exc}")

    ids = raw.get("ids", [[]])[0]
    docs = raw.get("documents", [[]])[0]
    metas = raw.get("metadatas", [[]])[0]
    dists = raw.get("distances", [[]])[0]

    results = []
    for i, doc_id in enumerate(ids):
        meta = metas[i] if i < len(metas) else {}
        dist = dists[i] if i < len(dists) else 1.0
        relevance = round(1.0 - float(dist), 4)  # cosine similarity

        # Apply time filter
        if req.time_range_hours and meta.get("timestamp"):
            try:
                ts = datetime.fromisoformat(meta["timestamp"].replace("Z", "+00:00").replace(" ", "T"))
                cutoff = datetime.utcnow().replace(tzinfo=ts.tzinfo) - timedelta(hours=req.time_range_hours)
                if ts < cutoff:
                    continue
            except Exception:
                pass

        doc_text = docs[i] if i < len(docs) else ""
        # Extract exception from doc text
        exception_match = re.search(r"exception=(.+?)(?:\s*\|.*)?$", doc_text)
        exception_text = exception_match.group(1) if exception_match else meta.get("exception")

        results.append(LogQueryResult(
            log_id=doc_id,
            service=meta.get("service", "unknown"),
            level=meta.get("level", "INFO"),
            message=doc_text,
            timestamp=meta.get("timestamp"),
            correlation_id=meta.get("correlation_id") or None,
            exception=exception_text,
            relevance_score=relevance,
            metadata={k: v for k, v in meta.items()
                       if k not in {"service", "level", "timestamp", "correlation_id", "trace_id", "has_exception"}},
        ))

    pattern_analysis = _analyze_results(req.query, results) if req.include_analysis else None
    llm_summary = _llm_synthesize(req.query, results) if req.include_analysis else None

    return AnalysisResult(
        query=req.query,
        total_found=len(results),
        results=results,
        pattern_analysis=pattern_analysis,
        llm_summary=llm_summary,
    )


@app.post("/logs/analyze", response_model=PatternAnalysis, summary="Deep pattern analysis")
def analyze_pattern(req: AnalyzeRequest):
    """
    Deep analysis of a specific error pattern or keyword.
    Returns frequency, affected services, exceptions, trend direction, and fix suggestions.
    """
    _, col = get_chroma()
    if col.count() == 0:
        raise HTTPException(status_code=404, detail="No logs indexed yet")

    query_vec = embed_texts([req.pattern])[0]
    where: Optional[Dict] = None
    if req.service:
        where = {"service": {"$eq": req.service}}

    try:
        raw = col.query(
            query_embeddings=[query_vec],
            n_results=min(req.n_results, col.count()),
            where=where,
            include=["documents", "metadatas", "distances"],
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

    ids = raw.get("ids", [[]])[0]
    metas = raw.get("metadatas", [[]])[0]
    docs = raw.get("documents", [[]])[0]
    dists = raw.get("distances", [[]])[0]

    # Filter by time range
    cutoff = datetime.utcnow() - timedelta(hours=req.look_back_hours)
    filtered_metas = []
    filtered_docs = []
    for i, meta in enumerate(metas):
        if meta.get("timestamp"):
            try:
                ts_str = meta["timestamp"].replace("Z", "+00:00").replace(" ", "T")
                ts = datetime.fromisoformat(ts_str).replace(tzinfo=None)
                if ts >= cutoff:
                    filtered_metas.append(meta)
                    filtered_docs.append(docs[i] if i < len(docs) else "")
            except Exception:
                filtered_metas.append(meta)
                filtered_docs.append(docs[i] if i < len(docs) else "")
        else:
            filtered_metas.append(meta)
            filtered_docs.append(docs[i] if i < len(docs) else "")

    # Compute stats
    services: Dict[str, int] = defaultdict(int)
    timestamps = []
    exceptions: List[str] = []
    similar_patterns = set()

    for meta, doc in zip(filtered_metas, filtered_docs):
        services[meta.get("service", "unknown")] += 1
        if meta.get("timestamp"):
            try:
                ts = datetime.fromisoformat(
                    meta["timestamp"].replace("Z", "+00:00").replace(" ", "T")
                ).replace(tzinfo=None)
                timestamps.append(ts)
            except Exception:
                pass
        exc_cls = _extract_exception_class(doc)
        if exc_cls:
            exceptions.append(exc_cls)
        # Extract short pattern snippet
        snippet = doc[:60].split("|")[0].strip()
        if snippet and snippet != req.pattern[:60]:
            similar_patterns.add(snippet)

    timestamps.sort()
    is_increasing = False
    freq_per_hour = 0.0
    if timestamps:
        span_hours = max(
            (timestamps[-1] - timestamps[0]).total_seconds() / 3600,
            0.1,
        )
        freq_per_hour = round(len(timestamps) / span_hours, 2)
        if len(timestamps) >= 4:
            mid = len(timestamps) // 2
            h1 = (timestamps[mid - 1] - timestamps[0]).total_seconds() / 3600 or 0.1
            h2 = (timestamps[-1] - timestamps[mid]).total_seconds() / 3600 or 0.1
            is_increasing = (mid / h2) > ((mid / h1) * 1.5)

    # De-dup exceptions
    exc_unique = list(dict.fromkeys(exceptions))[:8]

    suggestion = _suggest_investigation(req.pattern)

    return PatternAnalysis(
        pattern=req.pattern,
        occurrence_count=len(filtered_metas),
        services_affected=list(services.keys()),
        first_seen=timestamps[0].isoformat() if timestamps else None,
        last_seen=timestamps[-1].isoformat() if timestamps else None,
        frequency_per_hour=freq_per_hour,
        is_increasing=is_increasing,
        common_exceptions=exc_unique,
        suggested_investigation=suggestion,
        similar_patterns=list(similar_patterns)[:5],
    )


@app.get("/logs/stats", response_model=LogStats, summary="Log index statistics")
def log_stats():
    """Return summary statistics about indexed logs."""
    _, col = get_chroma()
    total = col.count()

    if total == 0:
        return LogStats(
            total_indexed=0,
            by_service={}, by_level={},
            error_rate_last_hour=0.0, warn_rate_last_hour=0.0,
            top_error_messages=[], services_with_errors=[], indexed_since=None,
        )

    # Get all metadata (paginated if large)
    try:
        # ChromaDB: get without embeddings for speed
        all_data = col.get(include=["metadatas"], limit=5000)
        metas = all_data.get("metadatas", [])
    except Exception as exc:
        log.warning("Stats fetch error: %s", exc)
        metas = []

    by_service: Dict[str, int] = defaultdict(int)
    by_level: Dict[str, int] = defaultdict(int)
    errors_by_service: Dict[str, int] = defaultdict(int)   # per-service error/fatal count
    error_messages: Dict[str, int] = defaultdict(int)
    cutoff = datetime.utcnow() - timedelta(hours=1)
    errors_last_hour = 0
    warns_last_hour = 0
    indexed_since = None

    for meta in metas:
        svc = meta.get("service", "unknown")
        lvl = meta.get("level", "INFO")
        by_service[svc] += 1
        by_level[lvl] += 1
        if lvl in ("ERROR", "FATAL"):
            errors_by_service[svc] += 1
        ts_str = meta.get("timestamp", "")
        if ts_str:
            try:
                ts = datetime.fromisoformat(ts_str.replace("Z", "+00:00").replace(" ", "T")).replace(tzinfo=None)
                if indexed_since is None or ts < indexed_since:
                    indexed_since = ts
                if ts >= cutoff:
                    if lvl in ("ERROR", "FATAL"):
                        errors_last_hour += 1
                    elif lvl == "WARN":
                        warns_last_hour += 1
            except Exception:
                pass

    services_with_errors = [s for s, cnt in errors_by_service.items() if cnt > 0]

    top_errors = sorted(
        [{"message": k, "count": v} for k, v in error_messages.items()],
        key=lambda x: x["count"], reverse=True
    )[:10]

    return LogStats(
        total_indexed=total,
        by_service=dict(by_service),
        by_level=dict(by_level),
        error_rate_last_hour=round(errors_last_hour / max(1.0, 1.0), 2),
        warn_rate_last_hour=round(warns_last_hour / max(1.0, 1.0), 2),
        top_error_messages=top_errors,
        services_with_errors=services_with_errors,
        indexed_since=indexed_since.isoformat() if indexed_since else None,
    )


@app.get("/logs/recent-errors", summary="Recent error/warn entries")
def recent_errors(
    limit: int = 20,
    service: Optional[str] = None,
    level: Optional[str] = "ERROR",
):
    """Return recent error/warn entries sorted by timestamp desc."""
    _, col = get_chroma()
    if col.count() == 0:
        return []

    where: Dict = {"level": {"$in": ["ERROR", "FATAL", "WARN"]}}
    if service:
        where = {"$and": [where, {"service": {"$eq": service}}]}
    if level and level.upper() != "ALL":
        lvl = level.upper()
        where = {"level": {"$eq": lvl}}
        if service:
            where = {"$and": [where, {"service": {"$eq": service}}]}

    try:
        data = col.get(
            where=where,
            include=["documents", "metadatas"],
            limit=min(limit * 3, 200),  # overfetch then sort
        )
    except Exception as exc:
        log.warning("recent-errors fetch error: %s", exc)
        return []

    ids = data.get("ids", [])
    metas = data.get("metadatas", [])
    docs = data.get("documents", [])

    entries = []
    for i, doc_id in enumerate(ids):
        meta = metas[i] if i < len(metas) else {}
        doc = docs[i] if i < len(docs) else ""
        entries.append({
            "id": doc_id,
            "service": meta.get("service", "unknown"),
            "level": meta.get("level", "?"),
            "message": doc,
            "timestamp": meta.get("timestamp"),
            "correlation_id": meta.get("correlation_id") or None,
            "has_exception": meta.get("has_exception") == "true",
        })

    # Sort by timestamp desc
    def sort_key(e):
        try:
            return datetime.fromisoformat(
                (e.get("timestamp") or "2000-01-01T00:00:00").replace("Z", "+00:00").replace(" ", "T")
            )
        except Exception:
            return datetime.min

    entries.sort(key=sort_key, reverse=True)
    return entries[:limit]


@app.post("/logs/bulk-ingest-from-db", summary="Manual trigger: ingest from PostgreSQL audit_log")
def manual_ingest():
    """Manually trigger PostgreSQL audit_log ingestion (no auth required — internal use)."""
    _ingest_from_postgres()
    _, col = get_chroma()
    return {"status": "triggered", "total_indexed": col.count()}


@app.delete("/logs/reset", summary="Clear all indexed logs (DANGEROUS)")
def reset_index(confirm: str = ""):
    """Delete entire log vector index. Requires ?confirm=yes."""
    if confirm != "yes":
        raise HTTPException(status_code=400, detail="Pass ?confirm=yes to reset the index")
    global _chroma_client, _chroma_collection
    _, col = get_chroma()
    count_before = col.count()
    _chroma_client.delete_collection(COLLECTION_NAME)
    _chroma_collection = _chroma_client.get_or_create_collection(
        name=COLLECTION_NAME,
        metadata={"hnsw:space": "cosine"},
    )
    return {"cleared": count_before, "total_indexed": 0}


@app.get("/health")
def health():
    model_status = "loaded" if _embed_model is not None and _embed_model != "keyword_fallback" else (
        "fallback" if _embed_model == "keyword_fallback" else "not_loaded"
    )
    try:
        _, col = get_chroma()
        chroma_status = "ok"
        indexed = col.count()
    except Exception as e:
        chroma_status = f"error: {e}"
        indexed = -1

    return {
        "status": "UP",
        "service": "log-analyzer",
        "vector_db": chroma_status,
        "indexed_documents": indexed,
        "embedding_model": model_status,
        "llm_provider": LLM_PROVIDER,
    }


# ─── Entry Point ─────────────────────────────────────────────────────────────

if __name__ == "__main__":
    # Pre-load model before accepting requests
    get_embed_model()
    get_chroma()
    uvicorn.run(app, host="0.0.0.0", port=LOG_ANALYZER_PORT)

