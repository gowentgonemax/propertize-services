"""
Search Reranker Microservice — FastAPI (OOP refactored)
Thin routing layer; delegates to RerankerService.
"""
import sys, os, logging
from typing import List, Dict, Any, Optional

from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.config import SEARCH_RERANKER_PORT
from shared.logging_middleware import add_logging_middleware
from services.reranker_service import RerankerService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

app = FastAPI(title="Propertize Search Reranker", version="2.0.0")
add_logging_middleware(app, service_name="search-reranker")
service = RerankerService()


class SearchResult(BaseModel):
    id: str
    type: str
    fields: Dict[str, Optional[str]]
    initial_score: float = 0.0


class RerankRequest(BaseModel):
    query: str
    results: List[SearchResult]
    weights: Optional[Dict[str, float]] = None


class RerankResponse(BaseModel):
    query: str
    total: int
    results: List[Dict[str, Any]]


@app.post("/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest):
    results_dicts = [r.dict() for r in request.results]
    reranked = service.rerank(request.query, results_dicts, request.weights)
    return RerankResponse(query=request.query, total=len(reranked), results=reranked)


@app.get("/health")
def health():
    return service.health_check()


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=SEARCH_RERANKER_PORT)
