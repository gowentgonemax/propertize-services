FROM python:3.11-slim
WORKDIR /app

COPY screening-worker/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY shared ./shared
COPY screening-worker/ .


# Security: run as non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
RUN chown -R appuser:appgroup /app
USER appuser

CMD ["python", "pipeline.py"]
