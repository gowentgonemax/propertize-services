"""Screening data access."""
import sys, os, json
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.base_repository import BaseRepository


class ScreeningRepository(BaseRepository):

    def fetch_application(self, application_id: str) -> dict | None:
        df = self.query("""
            SELECT ra.id, ra.organization_id, ra.tenant_id, ra.monthly_income,
                   ra.employer_name, ra.months_at_employer, ra.credit_score,
                   ra.has_criminal_record, ra.criminal_offense_type, ra.status,
                   l.monthly_rent
            FROM rental_application ra LEFT JOIN lease l ON l.id = ra.lease_id
            WHERE ra.id = %s
        """, [application_id])
        return df.iloc[0].to_dict() if not df.empty else None

    def save_result(self, application_id: str, result: dict):
        rec = result["recommendation"]
        self.execute("""
            UPDATE rental_application
            SET screening_score = %s, screening_recommendation = %s,
                screening_breakdown = %s::jsonb,
                status = CASE WHEN %s = 'APPROVE' THEN 'SCREENING_PASSED'
                              WHEN %s = 'DECLINE' THEN 'SCREENING_FAILED'
                              ELSE 'UNDER_REVIEW' END,
                updated_at = NOW()
            WHERE id = %s
        """, (result["composite_score"], rec, json.dumps(result), rec, rec, application_id))

