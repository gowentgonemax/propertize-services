"""
Shared configuration for all Python microservices.
Reads from environment variables with safe defaults for local development.
"""
import os

# PostgreSQL
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "5432"))
DB_NAME = os.getenv("DB_NAME", "propertize_db")
DB_USER = os.getenv("DB_USER", "ravishah")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
)

# Kafka
KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP", "localhost:9092")

# MinIO
MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "propertize")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "propertize_secret")
MINIO_SECURE = os.getenv("MINIO_SECURE", "false").lower() == "true"
MINIO_BUCKET = os.getenv("MINIO_BUCKET", "propertize-documents")

# Service ports
REPORT_SERVICE_PORT = int(os.getenv("REPORT_SERVICE_PORT", "8090"))
VENDOR_MATCHING_PORT = int(os.getenv("VENDOR_MATCHING_PORT", "8091"))
DOCUMENT_SERVICE_PORT = int(os.getenv("DOCUMENT_SERVICE_PORT", "8092"))
SEARCH_RERANKER_PORT = int(os.getenv("SEARCH_RERANKER_PORT", "8093"))
