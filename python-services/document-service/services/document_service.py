"""Document processing business logic — upload, PII detection, URL generation."""
import sys, os, re, uuid, hashlib, logging
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import timedelta
from shared.base_service import BaseService
from repositories.minio_repository import MinioRepository


class PiiDetector:
    """Encapsulates PII regex scanning."""

    PATTERNS = {
        "ssn": re.compile(r"\b\d{3}-\d{2}-\d{4}\b"),
        "credit_card": re.compile(r"\b\d{4}[- ]?\d{4}[- ]?\d{4}[- ]?\d{4}\b"),
        "bank_account": re.compile(r"\b\d{8,17}\b"),
    }

    def detect(self, text: str) -> list[str]:
        return [pii_type for pii_type, pat in self.PATTERNS.items() if pat.search(text)]


class PdfExtractor:
    """Extracts text from PDF bytes using PyMuPDF."""

    def __init__(self):
        self.log = logging.getLogger(self.__class__.__name__)

    def extract(self, content: bytes) -> str:
        try:
            import fitz
            doc = fitz.open(stream=content, filetype="pdf")
            text = " ".join(page.get_text() for page in doc)
            doc.close()
            return text
        except ImportError:
            self.log.warning("PyMuPDF not installed — PDF extraction skipped")
            return ""
        except Exception as e:
            self.log.warning(f"PDF extraction failed: {e}")
            return ""


class DocumentService(BaseService):

    def __init__(
        self,
        repo: MinioRepository | None = None,
        pii: PiiDetector | None = None,
        pdf: PdfExtractor | None = None,
    ):
        super().__init__()
        self.repo = repo or MinioRepository()
        self.pii = pii or PiiDetector()
        self.pdf = pdf or PdfExtractor()

    def service_name(self) -> str:
        return "document-service"

    def upload(
        self, content: bytes, filename: str, content_type: str,
        entity_type: str, entity_id: str, organization_id: str,
        description: str = "",
    ) -> dict:
        file_hash = hashlib.sha256(content).hexdigest()
        safe_name = re.sub(r"[^\w.\-]", "_", filename or "document")
        object_name = f"{organization_id}/{entity_type}/{entity_id}/{uuid.uuid4()}/{safe_name}"

        self.repo.upload(object_name, content, content_type or "application/octet-stream")

        extracted_text = ""
        pii_detected: list[str] = []
        if filename and filename.lower().endswith(".pdf"):
            extracted_text = self.pdf.extract(content)
            if extracted_text:
                pii_detected = self.pii.detect(extracted_text)

        try:
            file_url = self.repo.presigned_url(object_name, timedelta(days=7))
        except Exception:
            file_url = f"minio://propertize-documents/{object_name}"

        return {
            "file_url": file_url,
            "object_name": object_name,
            "file_hash": file_hash,
            "file_size": len(content),
            "content_type": content_type,
            "extracted_text_preview": extracted_text[:500] if extracted_text else None,
            "extracted_text_length": len(extracted_text),
            "pii_detected": pii_detected,
            "pii_warning": len(pii_detected) > 0,
            "description": description,
        }

    def get_signed_url(self, object_name: str, expires_hours: int = 1) -> dict:
        url = self.repo.presigned_url(object_name, timedelta(hours=expires_hours))
        return {"url": url, "expires_in_hours": expires_hours}

    def delete(self, object_name: str) -> dict:
        self.repo.delete(object_name)
        self.log.info(f"Deleted document: {object_name}")
        return {"deleted": True, "object_name": object_name}

    def health_check(self) -> dict:
        minio_up = self.repo.is_healthy()
        return {
            "status": "UP" if minio_up else "DEGRADED",
            "minio": "UP" if minio_up else "DOWN",
        }

