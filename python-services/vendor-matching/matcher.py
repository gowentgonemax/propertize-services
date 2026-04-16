"""
Vendor Matching Microservice — FastAPI (OOP refactored)
Thin routing layer; delegates to VendorMatchingService.
"""
import sys, os, logging
from typing import List, Optional

from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.config import VENDOR_MATCHING_PORT
from shared.logging_middleware import add_logging_middleware
from services.vendor_matching_service import VendorMatchingService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

app = FastAPI(title="Propertize Vendor Matching Service", version="2.0.0")
add_logging_middleware(app, service_name="vendor-matching")
service = VendorMatchingService()


class MatchRequest(BaseModel):
    maintenance_category: str
    description: Optional[str] = ""
    organization_id: str
    max_results: int = 5
    min_score: float = 0.3


class VendorScore(BaseModel):
    vendor_id: str
    vendor_name: str
    score: float
    match_type: str
    reasoning: str


@app.post("/match-vendors", response_model=List[VendorScore])
def match_vendors(request: MatchRequest):
    return service.match_vendors(
        maintenance_category=request.maintenance_category,
        description=request.description or "",
        organization_id=request.organization_id,
        max_results=request.max_results,
        min_score=request.min_score,
    )


@app.get("/health")
def health():
    return service.health_check()


if __name__ == "__main__":
    service.model_mgr.get_strategy()  # Pre-load model
    uvicorn.run(app, host="0.0.0.0", port=VENDOR_MATCHING_PORT)
