"""Vendor data access."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.base_repository import BaseRepository


class VendorRepository(BaseRepository):

    def get_active_vendors(self, organization_id: str):
        return self.query("""
            SELECT v.id, v.name, v.services_offered, v.specializations,
                   v.performance_rating, v.average_response_time_hours,
                   v.is_active, v.organization_id
            FROM vendor v
            WHERE v.organization_id = %s AND v.is_active = TRUE AND v.deleted_at IS NULL
        """, [organization_id])

