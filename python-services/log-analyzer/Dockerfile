FROM python:3.11-slim AS builder
WORKDIR /app

# System deps for chromadb + psycopg2
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential gcc libpq-dev curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM python:3.11-slim
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /root/.local /root/.local
ENV PATH="/root/.local/bin:$PATH"

# Shared modules
COPY ../shared ./shared

# Service code
COPY main.py .

# Cache dirs for sentence-transformers / HuggingFace
ENV HF_HOME=/tmp/.cache \
    TRANSFORMERS_CACHE=/tmp/.cache \
    XDG_CACHE_HOME=/tmp/.cache \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

EXPOSE 8094

HEALTHCHECK --interval=30s --timeout=15s --start-period=120s --retries=5 \
    CMD curl -f http://localhost:8094/health || exit 1

CMD ["python", "main.py"]

