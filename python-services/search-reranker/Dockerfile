FROM python:3.11-slim
WORKDIR /app

COPY search-reranker/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY shared ./shared
COPY search-reranker/ .

EXPOSE 8093
CMD ["python", "reranker.py"]
