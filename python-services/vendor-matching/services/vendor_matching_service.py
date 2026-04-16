"""Vendor matching business logic with Strategy pattern."""
import sys, os, logging
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from shared.base_service import BaseService
from repositories.vendor_repository import VendorRepository
from strategies.matching import MatchingStrategy, SemanticStrategy, KeywordStrategy

log = logging.getLogger(__name__)


class ModelManager:
    """Lazy-loads the ML model; returns the appropriate MatchingStrategy."""

    def __init__(self):
        self._model = None
        self._strategy: MatchingStrategy | None = None

    def get_strategy(self) -> MatchingStrategy:
        if self._strategy is not None:
            return self._strategy

        log.info("Loading SentenceTransformer model (first request)...")
        try:
            from sentence_transformers import SentenceTransformer
            self._model = SentenceTransformer("all-MiniLM-L6-v2")
            self._strategy = SemanticStrategy(self._model)
            log.info("Model loaded — using SemanticStrategy.")
        except ImportError:
            log.warning("sentence-transformers not installed — using KeywordStrategy")
            self._strategy = KeywordStrategy()
        return self._strategy

    @property
    def status(self) -> str:
        if self._strategy is None:
            return "not_loaded"
        return self._strategy.match_type()


class VendorMatchingService(BaseService):

    def __init__(
        self,
        repo: VendorRepository | None = None,
        model_mgr: ModelManager | None = None,
    ):
        super().__init__()
        self.repo = repo or VendorRepository()
        self.model_mgr = model_mgr or ModelManager()

    def service_name(self) -> str:
        return "vendor-matching"

    def match_vendors(
        self,
        maintenance_category: str,
        description: str,
        organization_id: str,
        max_results: int = 5,
        min_score: float = 0.3,
    ) -> list[dict]:
        df = self.repo.get_active_vendors(organization_id)
        if df.empty:
            self.log.warning(f"No active vendors for org={organization_id}")
            return []

        query = f"{maintenance_category} {description or ''}".strip()
        strategy = self.model_mgr.get_strategy()

        vendor_texts = [
            f"{row['name']} {row['services_offered'] or ''} {row['specializations'] or ''}"
            for _, row in df.iterrows()
        ]
        raw_scores = strategy.score(query, vendor_texts)

        results = []
        for i, (_, vendor) in enumerate(df.iterrows()):
            semantic_score = raw_scores[i]
            rating_score = float(vendor.performance_rating or 3.0) / 5.0
            avg_hours = float(vendor.average_response_time_hours or 12)
            response_score = max(0.0, 1.0 - (avg_hours / 24.0))

            combined = semantic_score * 0.5 + rating_score * 0.3 + response_score * 0.2
            if combined < min_score:
                continue

            results.append({
                "vendor_id": str(vendor.id),
                "vendor_name": vendor["name"],
                "score": round(combined, 3),
                "match_type": strategy.match_type(),
                "reasoning": (
                    f"semantic={semantic_score:.2f}, "
                    f"rating={rating_score:.2f} ({vendor.performance_rating or 'N/A'}/5), "
                    f"response={response_score:.2f} ({avg_hours:.0f}h avg)"
                ),
            })

        results.sort(key=lambda x: x["score"], reverse=True)
        top = results[:max_results]
        self.log.info(f"Matched {len(top)} vendors (from {len(df)}) for org={organization_id}")
        return top

    def health_check(self) -> dict:
        return {
            "status": "UP",
            "model": self.model_mgr.status,
            "service": self.service_name(),
        }

