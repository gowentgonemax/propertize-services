"""Screening business logic with pluggable scoring factors."""
import sys, os, json, logging
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from shared.base_service import BaseService
from shared.config import KAFKA_BOOTSTRAP
from repositories.screening_repository import ScreeningRepository
from scoring.factors import ScoringFactor, DEFAULT_FACTORS

APPROVAL_THRESHOLD = 65.0
DECLINE_THRESHOLD = 40.0


class ScreeningService(BaseService):

    def __init__(
        self,
        repo: ScreeningRepository | None = None,
        factors: list[ScoringFactor] | None = None,
    ):
        super().__init__()
        self.repo = repo or ScreeningRepository()
        self.factors = factors or DEFAULT_FACTORS

    def service_name(self) -> str:
        return "screening-worker"

    def process(self, application_id: str) -> dict | None:
        data = self.repo.fetch_application(application_id)
        if not data:
            self.log.warning(f"Application {application_id} not found")
            return None

        breakdown = {}
        composite = 0.0
        for factor in self.factors:
            s = factor.score(data)
            breakdown[f"{factor.name()}_factor"] = round(s, 2)
            composite += s * factor.weight()

        rec = (
            "APPROVE" if composite >= APPROVAL_THRESHOLD
            else "MANUAL_REVIEW" if composite >= DECLINE_THRESHOLD
            else "DECLINE"
        )

        result = {
            "application_id": application_id,
            "organization_id": str(data.get("organization_id", "")),
            "tenant_id": str(data.get("tenant_id", "")),
            "composite_score": round(composite, 2),
            "recommendation": rec,
            "breakdown": breakdown,
            "weights": {f.name(): f.weight() for f in self.factors},
            "thresholds": {"approve": APPROVAL_THRESHOLD, "decline": DECLINE_THRESHOLD},
        }

        self.repo.save_result(application_id, result)
        self.log.info(f"Screening complete: {application_id} score={composite:.1f} rec={rec}")
        return result

