"""
Document Processing Microservice — FastAPI (OOP refactored)
Thin routing layer; delegates to DocumentService.
"""
import sys, os, logging

from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Query
import uvicorn

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.config import DOCUMENT_SERVICE_PORT
from shared.logging_middleware import add_logging_middleware
from services.document_service import DocumentService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

app = FastAPI(title="Propertize Document Service", version="2.0.0")
add_logging_middleware(app, service_name="document-service")
service = DocumentService()


@app.post("/documents/upload")
async def upload_document(
    file: UploadFile = File(...),
    entity_type: str = Form(...),
    entity_id: str = Form(...),
    organization_id: str = Form(...),
    description: str = Form(default=""),
):
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="File is empty")
    try:
        return service.upload(
            content, file.filename or "document",
            file.content_type or "application/octet-stream",
            entity_type, entity_id, organization_id, description,
        )
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))


@app.get("/documents/url")
def get_signed_url(
    object_name: str = Query(...),
    expires_hours: int = Query(default=1, le=168),
):
    try:
        return service.get_signed_url(object_name, expires_hours)
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.delete("/documents/{object_name:path}")
def delete_document(object_name: str):
    try:
        return service.delete(object_name)
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.get("/health")
def health():
    return service.health_check()


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=DOCUMENT_SERVICE_PORT)
