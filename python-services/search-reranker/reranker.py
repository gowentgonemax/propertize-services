"""
Search Reranker Microservice — FastAPI + BM25
Reranks initial SQL search results by relevance using BM25Okapi.
Called by Java UniversalSearchService after initial DB fetch.
Improves tenant/property search quality without adding DB load.
"""
import sys
import os
import re
import logging
from typing import List, Dict, Any, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.config import SEARCH_RERANKER_PORT

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

app = FastAPI(title="Propertize Search Reranker", version="1.0.0")


def tokenize(text: str) -> List[str]:
    """Simple whitespace + alphanumeric tokenizer."""
    return re.findall(r"\w+", text.lower()) if text else []


class SearchResult(BaseModel):
    id: str
    type: str                                   # 'property' | 'tenant' | 'invoice' etc.
    fields: Dict[str, Optional[str]]            # searchable text fields
    initial_score: float = 0.0                 # score from SQL (similarity function)


class RerankRequest(BaseModel):
    query: str
    results: List[SearchResult]
    weights: Optional[Dict[str, float]] = None  # field name → weight multiplier


class RerankResponse(BaseModel):
    query: str
    total: int
    results: List[Dict[str, Any]]


@app.post("/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest):
    """
    BM25Okapi reranking of search candidates.

    Field weighting strategy (configurable via request.weights):
    - title / name: 3× (highest priority)
    - address / unit: 2×
    - description / notes: 1×

    Combined score: 70% BM25 + 30% initial SQL score.
    """
    if not request.results:
        return RerankResponse(query=request.query, total=0, results=[])

    # Resolve field weights
    default_weights = {
        "title": 3.0, "name": 3.0, "first_name": 3.0, "last_name": 3.0,
        "address": 2.0, "unit": 2.0, "city": 2.0,
        "description": 1.0, "notes": 1.0, "email": 1.5, "phone": 1.0,
    }
    weights = {**default_weights, **(request.weights or {})}

    # Build weighted corpus — repeat tokens proportional to field weight
    corpus = []
    for result in request.results:
        tokens = []
        for field, value in result.fields.items():
            if value:
                field_weight = int(weights.get(field, 1.0))
                tokens.extend(tokenize(value) * field_weight)
        corpus.append(tokens if tokens else [""])

    # BM25 scoring
    try:
        from rank_bm25 import BM25Okapi
        bm25 = BM25Okapi(corpus)
        query_tokens = tokenize(request.query)
        bm25_scores = bm25.get_scores(query_tokens)
    except ImportError:
        log.warning("rank-bm25 not installed — returning results in original order")
        bm25_scores = [r.initial_score for r in request.results]

    # Normalise BM25 scores to [0, 1]
    max_bm25 = max(bm25_scores) if max(bm25_scores) > 0 else 1.0
    normalised_bm25 = [s / max_bm25 for s in bm25_scores]

    # Combine scores
    reranked = []
    for i, result in enumerate(request.results):
        combined_score = normalised_bm25[i] * 0.7 + result.initial_score * 0.3
        reranked.append({
            **result.dict(),
            "bm25_score": round(float(normalised_bm25[i]), 4),
            "final_score": round(combined_score, 4),
        })

    reranked.sort(key=lambda x: x["final_score"], reverse=True)

    log.info(f"Reranked {len(reranked)} results for query='{request.query}'")
    return RerankResponse(query=request.query, total=len(reranked), results=reranked)


@app.get("/health")
def health():
    return {"status": "UP", "service": "search-reranker"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=SEARCH_RERANKER_PORT)
