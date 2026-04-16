"""Reranking business logic using BM25Okapi."""
import sys, os, re, logging
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from shared.base_service import BaseService

DEFAULT_WEIGHTS = {
    "title": 3.0, "name": 3.0, "first_name": 3.0, "last_name": 3.0,
    "address": 2.0, "unit": 2.0, "city": 2.0,
    "description": 1.0, "notes": 1.0, "email": 1.5, "phone": 1.0,
}


def tokenize(text: str) -> list[str]:
    return re.findall(r"\w+", text.lower()) if text else []


class RerankerService(BaseService):

    def service_name(self) -> str:
        return "search-reranker"

    def rerank(self, query: str, results: list[dict],
               weights: dict[str, float] | None = None) -> list[dict]:
        if not results:
            return []

        w = {**DEFAULT_WEIGHTS, **(weights or {})}

        corpus = []
        for r in results:
            tokens: list[str] = []
            for field, value in r.get("fields", {}).items():
                if value:
                    tokens.extend(tokenize(value) * int(w.get(field, 1.0)))
            corpus.append(tokens if tokens else [""])

        try:
            from rank_bm25 import BM25Okapi
            bm25 = BM25Okapi(corpus)
            bm25_scores = bm25.get_scores(tokenize(query)).tolist()
        except ImportError:
            self.log.warning("rank-bm25 not installed — returning original order")
            bm25_scores = [r.get("initial_score", 0.0) for r in results]

        max_bm25 = max(bm25_scores) if max(bm25_scores) > 0 else 1.0
        norm = [s / max_bm25 for s in bm25_scores]

        reranked = []
        for i, r in enumerate(results):
            combined = norm[i] * 0.7 + r.get("initial_score", 0.0) * 0.3
            reranked.append({
                **r,
                "bm25_score": round(norm[i], 4),
                "final_score": round(combined, 4),
            })

        reranked.sort(key=lambda x: x["final_score"], reverse=True)
        self.log.info(f"Reranked {len(reranked)} results for query='{query}'")
        return reranked

