"""Payment data access for the payment-worker."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.base_repository import BaseRepository


class PaymentRepository(BaseRepository):

    def ensure_columns(self):
        self.execute("""
            ALTER TABLE payment
                ADD COLUMN IF NOT EXISTS late_fee_applied BOOLEAN DEFAULT FALSE,
                ADD COLUMN IF NOT EXISTS late_fee_amount NUMERIC(12,2) DEFAULT 0;
        """)

    def get_active_leases_without_payment(self, first_of_month):
        return self.query("""
            SELECT l.id AS lease_id, l.tenant_id, l.monthly_rent,
                   COALESCE(l.payment_due_day, 1) AS payment_due_day,
                   l.organization_id, p.id AS property_id
            FROM lease l JOIN property p ON l.property_id = p.id
            WHERE l.status = 'ACTIVE' AND l.deleted_at IS NULL AND p.deleted_at IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM payment pay
                  WHERE pay.lease_id = l.id
                    AND DATE_TRUNC('month', pay.due_date) = DATE_TRUNC('month', %s::date)
                    AND pay.deleted_at IS NULL
              )
        """, [first_of_month])

    def insert_pending_payment(self, lease_id, tenant_id, amount, due_date, org_id):
        self.execute("""
            INSERT INTO payment
                (id, lease_id, tenant_id, amount, status, payment_type,
                 due_date, organization_id, late_fee_applied, created_at, updated_at)
            VALUES (gen_random_uuid(), %s, %s, %s, 'PENDING', 'RENT', %s, %s, FALSE, NOW(), NOW())
        """, (str(lease_id), str(tenant_id), float(amount), due_date, str(org_id)))

    def get_overdue_payments(self, cutoff_date):
        return self.query("""
            SELECT pay.id, pay.amount, pay.lease_id, pay.tenant_id,
                   pay.organization_id, pay.due_date, l.monthly_rent
            FROM payment pay JOIN lease l ON pay.lease_id = l.id
            WHERE pay.status = 'PENDING' AND pay.due_date < %s
              AND pay.late_fee_applied = FALSE AND pay.deleted_at IS NULL
        """, [cutoff_date])

    def apply_late_fee(self, payment_id, late_fee: float):
        self.execute("""
            UPDATE payment
            SET late_fee_applied = TRUE, late_fee_amount = %s,
                amount = amount + %s, updated_at = NOW()
            WHERE id = %s
        """, (late_fee, late_fee, payment_id))

    def get_upcoming_payments(self, reminder_date):
        return self.query("""
            SELECT pay.id, pay.amount, pay.tenant_id, pay.organization_id, pay.due_date,
                   t.email AS tenant_email, t.first_name
            FROM payment pay JOIN tenant t ON pay.tenant_id = t.id
            WHERE pay.status = 'PENDING' AND pay.due_date = %s AND pay.deleted_at IS NULL
        """, [reminder_date])

