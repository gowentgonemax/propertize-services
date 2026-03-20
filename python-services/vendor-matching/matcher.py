"""
Vendor Matching Microservice — FastAPI + Sentence Transformers
Replaces the naive keyword substring matching in Java VendorMatchingEngine.java.

Uses semantic similarity (cosine distance of sentence embeddings) to match
maintenance request context against vendor service descriptions.
Model: all-MiniLM-L6-v2 (~80MB, fast inference, no GPU required)
"""
import sys
import os
import logging
from typing import List, Optional

import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.db import get_connection
from shared.config import VENDOR_MATCHING_PORT

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

app = FastAPI(title="Propertize Vendor Matching Service", version="1.0.0")

# Lazy-load the model on first request to avoid blocking startup
_model = None


def get_model():
    global _model
    if _model is None:
        log.info("Loading SentenceTransformer model (first request)...")
        try:
            from sentence_transformers import SentenceTransformer
            _model = SentenceTransformer("all-MiniLM-L6-v2")
            log.info("Model loaded successfully.")
        except ImportError:
            log.warning("sentence-transformers not installed — falling back to keyword matching")
            _model = "keyword_fallback"
    return _model


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
    match_type: str  # 'semantic' | 'keyword'
    reasoning: str


def keyword_score(query: str, services: str) -> float:
    """Fallback when sentence-transformers is unavailable."""
    if not services or not query:
        return 0.3
    query_words = set(query.lower().split())
    service_words = set(services.lower().split())
    overlap = query_words & service_words
    return min(1.0, len(overlap) / max(len(query_words), 1) * 1.5)


@app.post("/match-vendors", response_model=List[VendorScore])
def match_vendors(request: MatchRequest):
    """
    Find and rank vendors for a maintenance request.
    Uses semantic similarity if sentence-transformers is available,
    falls back to keyword matching otherwise.
    """
    log.info(f"Matching vendors for category={request.maintenance_category}, org={request.organization_id}")

    # Fetch vendors for the organization
    with get_connection() as conn:
        df = pd.read_sql("""
            SELECT
                v.id, v.name, v.services_offered, v.specializations,
                v.performance_rating, v.average_response_time_hours,
                v.is_active, v.organization_id
            FROM vendor v
            WHERE v.organization_id = %s
              AND v.is_active = TRUE
              AND v.deleted_at IS NULL
        """, conn, params=[request.organization_id])

    if df.empty:
        log.warning(f"No active vendors for org={request.organization_id}")
        return []

    query = f"{request.maintenance_category} {request.description or ''}".strip()
    model = get_model()
    scores = []

    if model == "keyword_fallback":
        match_type = "keyword"
        for _, vendor in df.iterrows():
            service_text = f"{vendor['services_offered'] or ''} {vendor['specializations'] or ''}"
            semantic_score = keyword_score(query, service_text)
            scores.append((vendor, semantic_score))
    else:
        match_type = "semantic"
        vendor_texts = [
            f"{row['name']} {row['services_offered'] or ''} {row['specializations'] or ''}"
            for _, row in df.iterrows()
        ]
        try:
            from sklearn.metrics.pairwise import cosine_similarity
            query_embedding = model.encode([query])
            vendor_embeddings = model.encode(vendor_texts)
            similarities = cosine_similarity(query_embedding, vendor_embeddings)[0]
        except ImportError:
            log.warning("scikit-learn not installed — using dot product fallback")
            query_embedding = model.encode([query])[0]
            vendor_embeddings_list = model.encode(vendor_texts)
            similarities = [
                float(np.dot(query_embedding, ve) /
                      (np.linalg.norm(query_embedding) * np.linalg.norm(ve) + 1e-9))
                for ve in vendor_embeddings_list
            ]

        scores = list(zip(df.itertuples(index=False), similarities))

    results = []
    for vendor_row, semantic_score in scores:
        # Combined score: semantic 50%, rating 30%, response time 20%
        rating_score = float(vendor_row.performance_rating or 3.0) / 5.0
        avg_hours = float(vendor_row.average_response_time_hours or 12)
        response_score = max(0.0, 1.0 - (avg_hours / 24.0))

        combined = (
            float(semantic_score) * 0.5 +
            rating_score * 0.3 +
            response_score * 0.2
        )

        if combined < request.min_score:
            continue

        results.append(VendorScore(
            vendor_id=str(vendor_row.id),
            vendor_name=vendor_row.name,
            score=round(combined, 3),
            match_type=match_type,
            reasoning=(
                f"semantic={float(semantic_score):.2f}, "
                f"rating={rating_score:.2f} ({vendor_row.performance_rating or 'N/A'}/5), "
                f"response={response_score:.2f} ({avg_hours:.0f}h avg)"
            ),
        ))

    results.sort(key=lambda x: x.score, reverse=True)
    top = results[: request.max_results]

    log.info(f"Matched {len(top)} vendors (from {len(df)} total) for org={request.organization_id}")
    return top


@app.get("/health")
def health():
    model_status = "loaded" if _model is not None and _model != "keyword_fallback" else (
        "fallback" if _model == "keyword_fallback" else "not_loaded"
    )
    return {"status": "UP", "model": model_status, "service": "vendor-matching"}


if __name__ == "__main__":
    # Pre-load model before accepting requests
    get_model()
    uvicorn.run(app, host="0.0.0.0", port=VENDOR_MATCHING_PORT)
