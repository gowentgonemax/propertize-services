"""Financial data access — extracts SQL from the old monolithic main.py."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from shared.base_repository import BaseRepository


class FinancialRepository(BaseRepository):

    def get_income_by_type(self, org_id: str, start_date: str, end_date: str):
        return self.query("""
            SELECT payment_type, SUM(amount) as total, COUNT(*) as count
            FROM payment
            WHERE organization_id = %s
              AND payment_date BETWEEN %s AND %s
              AND status = 'COMPLETED' AND deleted_at IS NULL
            GROUP BY payment_type ORDER BY total DESC
        """, [org_id, start_date, end_date])

    def get_expenses_by_category(self, org_id: str, start_date: str, end_date: str):
        return self.query("""
            SELECT category, SUM(amount) as total, COUNT(*) as count
            FROM expense
            WHERE organization_id = %s
              AND expense_date BETWEEN %s AND %s
              AND deleted_at IS NULL
            GROUP BY category ORDER BY total DESC
        """, [org_id, start_date, end_date])

    def get_delinquency_data(self, org_id: str, as_of_date: str):
        return self.query("""
            SELECT
                t.first_name || ' ' || t.last_name AS tenant_name,
                t.email, t.phone_number AS phone,
                CONCAT_WS(', ', p.address_street, p.address_city, p.address_state) AS property_address,
                l.monthly_rent,
                SUM(CASE WHEN pay.status != 'COMPLETED' AND pay.due_date < %s
                         THEN pay.amount ELSE 0 END) AS amount_owed,
                MIN(CASE WHEN pay.status != 'COMPLETED' AND pay.due_date < %s
                         THEN pay.due_date END) AS oldest_due_date
            FROM tenant t
            JOIN lease l ON t.id = l.tenant_id
            JOIN property p ON l.property_id = p.id
            LEFT JOIN payment pay ON l.id = pay.lease_id
            WHERE l.status = 'ACTIVE' AND t.organization_id = %s AND t.deleted_at IS NULL
            GROUP BY t.id, t.first_name, t.last_name, t.email, t.phone_number,
                     p.address_street, p.address_city, p.address_state, l.monthly_rent
            HAVING SUM(CASE WHEN pay.status != 'COMPLETED' AND pay.due_date < %s
                            THEN pay.amount ELSE 0 END) > 0
            ORDER BY amount_owed DESC
        """, [as_of_date, as_of_date, org_id, as_of_date])

    def get_rent_roll(self, org_id: str):
        return self.query("""
            SELECT
                CONCAT_WS(' ', p.address_street, p.address_unit) AS unit,
                p.address_city, p.address_state, p.type AS property_type,
                p.status AS property_status,
                l.monthly_rent, l.start_date, l.end_date, l.status AS lease_status,
                t.first_name || ' ' || t.last_name AS tenant_name,
                t.email AS tenant_email, t.phone_number AS tenant_phone
            FROM property p
            LEFT JOIN lease l ON p.id = l.property_id AND l.status IN ('ACTIVE', 'PENDING')
            LEFT JOIN tenant t ON l.tenant_id = t.id
            WHERE p.organization_id = %s AND p.deleted_at IS NULL
            ORDER BY p.address_street, p.address_unit
        """, [org_id])

