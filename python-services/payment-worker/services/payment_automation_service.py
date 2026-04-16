"""Payment automation business logic — rent generation, late fees, reminders."""
import sys, os, json, logging
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import date, timedelta
from decimal import Decimal

from shared.base_service import BaseService
from shared.config import KAFKA_BOOTSTRAP
from repositories.payment_repository import PaymentRepository

LATE_FEE_PERCENT = Decimal("0.05")
GRACE_PERIOD_DAYS = 5
REMINDER_DAYS_BEFORE = 3


class KafkaPublisher:
    """Encapsulates Kafka producer lifecycle."""

    def __init__(self):
        self.log = logging.getLogger(self.__class__.__name__)
        self._producer = None

    def _get_producer(self):
        if self._producer is not None:
            return self._producer
        try:
            from kafka import KafkaProducer
            self._producer = KafkaProducer(
                bootstrap_servers=KAFKA_BOOTSTRAP,
                value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            )
        except (ImportError, Exception) as e:
            self.log.warning(f"Kafka unavailable: {e}")
        return self._producer

    def send(self, topic: str, value: dict):
        p = self._get_producer()
        if p:
            try:
                p.send(topic, value=value)
            except Exception as e:
                self.log.warning(f"Kafka send failed on {topic}: {e}")

    def flush(self):
        p = self._get_producer()
        if p:
            p.flush()


class PaymentAutomationService(BaseService):

    def __init__(
        self,
        repo: PaymentRepository | None = None,
        publisher: KafkaPublisher | None = None,
    ):
        super().__init__()
        self.repo = repo or PaymentRepository()
        self.publisher = publisher or KafkaPublisher()

    def service_name(self) -> str:
        return "payment-worker"

    def initialise(self):
        self.repo.ensure_columns()
        self.log.info("Payment table columns ensured.")

    def generate_monthly_rent_payments(self):
        today = date.today()
        target = today + timedelta(days=7)
        first_of_month = target.replace(day=1)
        self.log.info(f"Generating rent payments for {first_of_month}")

        df = self.repo.get_active_leases_without_payment(first_of_month)
        if df.empty:
            self.log.info("No new rent payments to generate.")
            return

        for _, lease in df.iterrows():
            due_day = min(int(lease["payment_due_day"]), 28)
            due_date = first_of_month.replace(day=due_day)
            self.repo.insert_pending_payment(
                lease["lease_id"], lease["tenant_id"],
                lease["monthly_rent"], due_date, lease["organization_id"],
            )
        self.log.info(f"Generated {len(df)} rent payment records for {first_of_month}")

    def apply_late_fees(self):
        cutoff = date.today() - timedelta(days=GRACE_PERIOD_DAYS)
        self.log.info(f"Applying late fees for payments overdue before {cutoff}")

        df = self.repo.get_overdue_payments(cutoff)
        if df.empty:
            self.log.info("No overdue payments require late fees.")
            return

        for _, pay in df.iterrows():
            late_fee = float(Decimal(str(pay["monthly_rent"])) * LATE_FEE_PERCENT)
            self.repo.apply_late_fee(pay["id"], late_fee)
            self.publisher.send("payment.late_fee_applied", {
                "payment_id": str(pay["id"]),
                "tenant_id": str(pay["tenant_id"]),
                "organization_id": str(pay["organization_id"]),
                "late_fee": late_fee,
                "total_amount": float(pay["amount"]) + late_fee,
                "due_date": str(pay["due_date"]),
            })

        self.publisher.flush()
        self.log.info(f"Applied late fees to {len(df)} payments")

    def send_upcoming_reminders(self):
        reminder_date = date.today() + timedelta(days=REMINDER_DAYS_BEFORE)
        self.log.info(f"Sending reminders for payments due on {reminder_date}")

        df = self.repo.get_upcoming_payments(reminder_date)
        for _, pay in df.iterrows():
            self.publisher.send("payment.reminder_due", {
                "payment_id": str(pay["id"]),
                "tenant_id": str(pay["tenant_id"]),
                "tenant_email": pay["tenant_email"],
                "tenant_first_name": pay["first_name"],
                "organization_id": str(pay["organization_id"]),
                "amount": float(pay["amount"]),
                "due_date": str(pay["due_date"]),
            })

        self.publisher.flush()
        self.log.info(f"Sent {len(df)} payment reminder events")

