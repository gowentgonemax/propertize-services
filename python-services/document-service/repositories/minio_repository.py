"""MinIO storage access — encapsulates S3-compatible object operations."""
import io, logging
from typing import Optional
from minio import Minio
from minio.error import S3Error
from datetime import timedelta

from shared.config import (
    MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY,
    MINIO_SECURE, MINIO_BUCKET,
)


class MinioRepository:
    """Encapsulates all MinIO (S3) operations."""

    def __init__(self):
        self.log = logging.getLogger(self.__class__.__name__)
        self._client: Optional[Minio] = None

    def _get_client(self) -> Minio:
        if self._client is None:
            self._client = Minio(
                MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
                secret_key=MINIO_SECRET_KEY, secure=MINIO_SECURE,
            )
            if not self._client.bucket_exists(MINIO_BUCKET):
                self._client.make_bucket(MINIO_BUCKET)
                self.log.info(f"Created MinIO bucket: {MINIO_BUCKET}")
        return self._client

    def upload(self, object_name: str, data: bytes, content_type: str) -> None:
        self._get_client().put_object(
            MINIO_BUCKET, object_name, io.BytesIO(data),
            length=len(data), content_type=content_type,
        )

    def presigned_url(self, object_name: str, expires: timedelta) -> str:
        return self._get_client().presigned_get_object(
            MINIO_BUCKET, object_name, expires=expires,
        )

    def delete(self, object_name: str) -> None:
        self._get_client().remove_object(MINIO_BUCKET, object_name)

    def is_healthy(self) -> bool:
        try:
            self._get_client().list_buckets()
            return True
        except Exception:
            return False

