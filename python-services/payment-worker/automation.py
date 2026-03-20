"""
Payment Automation Worker — APScheduler
Handles recurring payment generation and late fee application.
Fills the gap in Java PaymentSchedulerService which logs but never acts.
Publishes Kafka events so Java NotificationService sends emails.
"""
import sys
import os
import json
import logging
from datetime import date, timedelta
from decimal import Decimal

import pandas as pd
from apscheduler.schedulers.blocking import BlockingScheduler

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.db import get_connection
from shared.config import KAFKA_BOOTSTRAP

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

# Configurable — could be loaded from DB per organization
LATE_FEE_PERCENT = Decimal("0.05")  # 5% of monthly rent
GRACE_PERIOD_DAYS = 5
REMINDER_DAYS_BEFORE = 3


def get_kafka_producer():
    try:
        from kafka import KafkaProducer
        return KafkaProducer(
            bootstrap_servers=KAFKA_BOOTSTRAP,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        )
    except ImportError:
        log.warning("kafka-python not installed — Kafka events will be skipped")
        return None
    except Exception as e:
        log.warning(f"Kafka unavailable: {e} — events will be skipped")
        return None


def ensure_payment_columns():
    """Add late_fee_applied column if not present."""
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""
                ALTER TABLE payment
                    ADD COLUMN IF NOT EXISTS late_fee_applied BOOLEAN DEFAULT FALSE,
                    ADD COLUMN IF NOT EXISTS late_fee_amount NUMERIC(12,2) DEFAULT 0;
            """)
    log.info("Payment table columns ensured.")


def generate_monthly_rent_payments():
    """
    For every ACTIVE lease with payment_due_day configured,
    auto-generate a PENDING payment record for the upcoming month
    if one doesn't already exist.
    """
    today = date.today()
    # Generate for the month starting 7 days from now (gives time for tenants to see it)
    target_date = today + timedelta(days=7)
    first_of_target_month = target_date.replace(day=1)

    log.info(f"Generating rent payments for month starting {first_of_target_month}")

    with get_connection() as conn:
        df = pd.read_sql("""
            SELECT
                l.id AS lease_id,
                l.tenant_id,
                l.monthly_rent,
                COALESCE(l.payment_due_day, 1) AS payment_due_day,
                l.organization_id,
                p.id AS property_id
            FROM lease l
            JOIN property p ON l.property_id = p.id
            WHERE l.status = 'ACTIVE'
              AND l.deleted_at IS NULL
              AND p.deleted_at IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM payment pay
                  WHERE pay.lease_id = l.id
                    AND DATE_TRUNC('month', pay.due_date) = DATE_TRUNC('month', %s::date)
                    AND pay.deleted_at IS NULL
              )
        """, conn, params=[first_of_target_month])

        if df.empty:
            log.info("No new rent payments to generate this run.")
            return

        with conn.cursor() as cur:
            for _, lease in df.iterrows():
                due_day = min(int(lease["payment_due_day"]), 28)
                due_date = first_of_target_month.replace(day=due_day)

                cur.execute("""
                    INSERT INTO payment
                        (id, lease_id, tenant_id, amount, status, payment_type,
                         due_date, organization_id, late_fee_applied, created_at, updated_at)
                    VALUES
                        (gen_random_uuid(), %s, %s, %s, 'PENDING', 'RENT',
                         %s, %s, FALSE, NOW(), NOW())
                """, (
                    str(lease["lease_id"]),
                    str(lease["tenant_id"]),
                    float(lease["monthly_rent"]),
                    due_date,
                    str(lease["organization_id"]),
                ))

        log.info(f"Generated {len(df)} rent payment records for {first_of_target_month}")


def apply_late_fees():
    """
    For PENDING payments overdue beyond the grace period,
    apply a late fee and publish a Kafka notification event.
    """
    cutoff_date = date.today() - timedelta(days=GRACE_PERIOD_DAYS)
    log.info(f"Applying late fees for payments overdue before {cutoff_date}")

    producer = get_kafka_producer()

    with get_connection() as conn:
        df = pd.read_sql("""
            SELECT
                pay.id, pay.amount, pay.lease_id, pay.tenant_id,
                pay.organization_id, pay.due_date,
                l.monthly_rent
            FROM payment pay
            JOIN lease l ON pay.lease_id = l.id
            WHERE pay.status = 'PENDING'
              AND pay.due_date < %s
              AND pay.late_fee_applied = FALSE
              AND pay.deleted_at IS NULL
        """, conn, params=[cutoff_date])

        if df.empty:
            log.info("No overdue payments require late fees this run.")
            return

        with conn.cursor() as cur:
            for _, payment in df.iterrows():
                late_fee = Decimal(str(payment["monthly_rent"])) * LATE_FEE_PERCENT

                cur.execute("""
                    UPDATE payment
                    SET late_fee_applied = TRUE,
                        late_fee_amount = %s,
                        amount = amount + %s,
                        updated_at = NOW()
                    WHERE id = %s
                """, (float(late_fee), float(late_fee), payment["id"]))

                # Publish Kafka event for Java notification service
                if producer:
                    try:
                        producer.send("payment.late_fee_applied", value={
                            "payment_id": str(payment["id"]),
                            "tenant_id": str(payment["tenant_id"]),
                            "organization_id": str(payment["organization_id"]),
                            "late_fee": float(late_fee),
                            "total_amount": float(payment["amount"]) + float(late_fee),
                            "due_date": str(payment["due_date"]),
                        })
                    except Exception as e:
                        log.warning(f"Failed to publish Kafka event for payment {payment['id']}: {e}")

    if producer:
        producer.flush()

    log.info(f"Applied late fees to {len(df)} payments")


def send_upcoming_reminders():
    """
    Send payment reminders for payments due in REMINDER_DAYS_BEFORE days.
    Publishes Kafka event — Java handles the actual email/SMS send.
    """
    reminder_date = date.today() + timedelta(days=REMINDER_DAYS_BEFORE)
    log.info(f"Sending reminders for payments due on {reminder_date}")

    producer = get_kafka_producer()
    if not producer:
        return

    with get_connection() as conn:
        df = pd.read_sql("""
            SELECT
                pay.id, pay.amount, pay.tenant_id, pay.organization_id, pay.due_date,
                t.email AS tenant_email, t.first_name
            FROM payment pay
            JOIN tenant t ON pay.tenant_id = t.id
            WHERE pay.status = 'PENDING'
              AND pay.due_date = %s
              AND pay.deleted_at IS NULL
        """, conn, params=[reminder_date])

    for _, payment in df.iterrows():
        try:
            producer.send("payment.reminder_due", value={
                "payment_id": str(payment["id"]),
                "tenant_id": str(payment["tenant_id"]),
                "tenant_email": payment["tenant_email"],
                "tenant_first_name": payment["first_name"],
                "organization_id": str(payment["organization_id"]),
                "amount": float(payment["amount"]),
                "due_date": str(payment["due_date"]),
            })
        except Exception as e:
            log.warning(f"Failed to send reminder event for payment {payment['id']}: {e}")

    producer.flush()
    log.info(f"Sent {len(df)} payment reminder events")


if __name__ == "__main__":
    ensure_payment_columns()

    scheduler = BlockingScheduler()

    # Generate next month's rent at 01:00 on the 20th of each month
    scheduler.add_job(generate_monthly_rent_payments, "cron",
                      day=20, hour=1, minute=0, id="generate_rent_payments")

    # Apply late fees daily at 02:00
    scheduler.add_job(apply_late_fees, "cron",
                      hour=2, minute=0, id="apply_late_fees")

    # Send reminders daily at 09:00
    scheduler.add_job(send_upcoming_reminders, "cron",
                      hour=9, minute=0, id="send_reminders")

    log.info("Payment worker started. Jobs: generate (monthly), late_fees (daily 02:00), reminders (daily 09:00)")
    scheduler.start()
