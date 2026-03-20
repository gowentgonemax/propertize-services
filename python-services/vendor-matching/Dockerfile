FROM python:3.11-slim
WORKDIR /app

COPY requirements.txt .
# sentence-transformers downloads model on first run; cache it at build time
RUN pip install --no-cache-dir -r requirements.txt && \
    python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"

COPY ../shared ./shared
COPY . .

EXPOSE 8091
CMD ["python", "matcher.py"]
