"""
Matching strategies — Strategy pattern for vendor scoring.
SemanticStrategy uses sentence-transformer embeddings.
KeywordStrategy is a fallback using token overlap.
"""
import logging
from abc import ABC, abstractmethod

import numpy as np

log = logging.getLogger(__name__)


class MatchingStrategy(ABC):
    """Abstract matching strategy (Strategy interface)."""

    @abstractmethod
    def score(self, query: str, vendor_texts: list[str]) -> list[float]:
        """Return a similarity score in [0, 1] for each vendor text."""
        ...

    @abstractmethod
    def match_type(self) -> str:
        ...


class SemanticStrategy(MatchingStrategy):
    """Cosine similarity via sentence-transformer embeddings."""

    def __init__(self, model):
        self._model = model

    def score(self, query: str, vendor_texts: list[str]) -> list[float]:
        try:
            from sklearn.metrics.pairwise import cosine_similarity
            q_emb = self._model.encode([query])
            v_embs = self._model.encode(vendor_texts)
            return cosine_similarity(q_emb, v_embs)[0].tolist()
        except ImportError:
            log.warning("scikit-learn unavailable — using dot-product fallback")
            q_emb = self._model.encode([query])[0]
            v_embs = self._model.encode(vendor_texts)
            return [
                float(np.dot(q_emb, ve) /
                      (np.linalg.norm(q_emb) * np.linalg.norm(ve) + 1e-9))
                for ve in v_embs
            ]

    def match_type(self) -> str:
        return "semantic"


class KeywordStrategy(MatchingStrategy):
    """Token-overlap fallback when ML model is unavailable."""

    def score(self, query: str, vendor_texts: list[str]) -> list[float]:
        query_words = set(query.lower().split())
        results: list[float] = []
        for text in vendor_texts:
            words = set(text.lower().split())
            overlap = query_words & words
            results.append(min(1.0, len(overlap) / max(len(query_words), 1) * 1.5))
        return results

    def match_type(self) -> str:
        return "keyword"

