FROM python:3.11-slim
WORKDIR /app

COPY vendor-matching/requirements.txt .
# sentence-transformers downloads model on first run; cache it at build time
RUN pip install --no-cache-dir -r requirements.txt && \
    python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"

COPY shared ./shared
COPY vendor-matching/ .

EXPOSE 8091

# Security: run as non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
RUN chown -R appuser:appgroup /app
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8091/health')" || exit 1

CMD ["python", "matcher.py"]
