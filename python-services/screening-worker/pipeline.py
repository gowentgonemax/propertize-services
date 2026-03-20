"""
Tenant Screening Pipeline — Kafka Consumer
Consumes 'screening.initiated' events, computes multi-factor risk scores,
publishes 'screening.completed' with a structured risk report.

Risk weight model (total = 100%):
  - Credit score:         35%
  - Income-to-rent ratio: 30%
  - Criminal history:     25%
  - Employment stability: 10%
"""
import sys
import os
import json
import logging
import time

import pandas as pd

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.db import get_connection
from shared.config import KAFKA_BOOTSTRAP

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

WEIGHTS = {
    "credit_score": 0.35,
    "income_ratio": 0.30,
    "criminal": 0.25,
    "employment": 0.10,
}

APPROVAL_THRESHOLD = 65.0  # score out of 100 for auto-approve recommendation
DECLINE_THRESHOLD = 40.0   # score below this → auto-decline recommendation


# ---------------------------------------------------------------------------
# Scoring functions — each returns a float in [0, 100]
# ---------------------------------------------------------------------------

def score_credit(credit_score: float | None) -> float:
    """FICO 300–850 mapped to 0–100."""
    if credit_score is None:
        return 40.0  # neutral / unable to verify
    cs = max(300.0, min(850.0, float(credit_score)))
    return (cs - 300.0) / (850.0 - 300.0) * 100.0


def score_income_ratio(monthly_income: float | None, monthly_rent: float | None) -> float:
    """
    Industry standard: rent should not exceed 1/3 of gross income (ratio ≤ 0.33).
    Returns 100 for ratio ≤ 0.25, 0 for ratio ≥ 0.55, linear in between.
    """
    if not monthly_income or not monthly_rent or monthly_income <= 0:
        return 35.0  # neutral
    ratio = monthly_rent / monthly_income
    if ratio <= 0.25:
        return 100.0
    if ratio >= 0.55:
        return 0.0
    return max(0.0, (0.55 - ratio) / (0.55 - 0.25) * 100.0)


def score_criminal(has_criminal_record: bool | None, crime_type: str | None) -> float:
    """
    Violent felonies → 0; non-violent misdemeanors → 60; no record → 100.
    Fair housing compliant: age of record should be considered (future enhancement).
    """
    if has_criminal_record is None:
        return 70.0  # no data
    if not has_criminal_record:
        return 100.0
    crime_type = (crime_type or "").upper()
    if any(k in crime_type for k in ("VIOLENT", "ASSAULT", "FELONY", "HOMICIDE", "SEXUAL")):
        return 0.0
    return 60.0  # non-violent / misdemeanor


def score_employment(employer: str | None, months_employed: int | None) -> float:
    """
    > 24 months → 100; 12–24 → 80; 6–12 → 60; < 6 → 30; unemployed → 20.
    """
    if not employer:
        return 20.0
    months = months_employed or 0
    if months >= 24:
        return 100.0
    if months >= 12:
        return 80.0
    if months >= 6:
        return 60.0
    return 30.0


def compute_composite_score(credit: float, income: float, criminal: float, employment: float) -> float:
    return (
        credit * WEIGHTS["credit_score"]
        + income * WEIGHTS["income_ratio"]
        + criminal * WEIGHTS["criminal"]
        + employment * WEIGHTS["employment"]
    )


def recommendation(score: float) -> str:
    if score >= APPROVAL_THRESHOLD:
        return "APPROVE"
    if score >= DECLINE_THRESHOLD:
        return "MANUAL_REVIEW"
    return "DECLINE"


# ---------------------------------------------------------------------------
# DB helpers
# ---------------------------------------------------------------------------

def fetch_application_data(application_id: str) -> dict | None:
    """Load screening application and linked lease/property data from DB."""
    with get_connection() as conn:
        df = pd.read_sql("""
            SELECT
                ra.id,
                ra.organization_id,
                ra.tenant_id,
                ra.monthly_income,
                ra.employer_name,
                ra.months_at_employer,
                ra.credit_score,
                ra.has_criminal_record,
                ra.criminal_offense_type,
                ra.status,
                l.monthly_rent
            FROM rental_application ra
            LEFT JOIN lease l ON l.id = ra.lease_id
            WHERE ra.id = %s
        """, conn, params=[application_id])

    if df.empty:
        return None
    return df.iloc[0].to_dict()


def save_screening_result(application_id: str, result: dict):
    """Persist the scoring result back to DB."""
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""
                UPDATE rental_application
                SET screening_score = %s,
                    screening_recommendation = %s,
                    screening_breakdown = %s::jsonb,
                    status = CASE
                        WHEN %s = 'APPROVE' THEN 'SCREENING_PASSED'
                        WHEN %s = 'DECLINE' THEN 'SCREENING_FAILED'
                        ELSE 'UNDER_REVIEW'
                    END,
                    updated_at = NOW()
                WHERE id = %s
            """, (
                result["composite_score"],
                result["recommendation"],
                json.dumps(result),
                result["recommendation"],
                result["recommendation"],
                application_id,
            ))


# ---------------------------------------------------------------------------
# Kafka consumer loop
# ---------------------------------------------------------------------------

def process_event(event: dict, producer):
    application_id = event.get("application_id")
    if not application_id:
        log.warning(f"Received screening event with no application_id: {event}")
        return

    log.info(f"Processing screening for application {application_id}")

    data = fetch_application_data(application_id)
    if not data:
        log.warning(f"Application {application_id} not found in DB")
        return

    # Compute individual factor scores
    c_credit = score_credit(data.get("credit_score"))
    c_income = score_income_ratio(data.get("monthly_income"), data.get("monthly_rent"))
    c_criminal = score_criminal(data.get("has_criminal_record"), data.get("criminal_offense_type"))
    c_employment = score_employment(data.get("employer_name"), data.get("months_at_employer"))

    composite = compute_composite_score(c_credit, c_income, c_criminal, c_employment)
    rec = recommendation(composite)

    result = {
        "application_id": application_id,
        "organization_id": str(data.get("organization_id", "")),
        "tenant_id": str(data.get("tenant_id", "")),
        "composite_score": round(composite, 2),
        "recommendation": rec,
        "breakdown": {
            "credit_score_factor": round(c_credit, 2),
            "income_ratio_factor": round(c_income, 2),
            "criminal_factor": round(c_criminal, 2),
            "employment_factor": round(c_employment, 2),
        },
        "weights": WEIGHTS,
        "thresholds": {
            "approve": APPROVAL_THRESHOLD,
            "decline": DECLINE_THRESHOLD,
        },
    }

    save_screening_result(application_id, result)

    # Publish result downstream for Java notification service
    if producer:
        try:
            producer.send("screening.completed", value=result)
            producer.flush()
        except Exception as e:
            log.warning(f"Failed to publish screening.completed for {application_id}: {e}")

    log.info(
        f"Screening complete for {application_id}: "
        f"score={composite:.1f}, recommendation={rec}"
    )


def run_consumer():
    try:
        from kafka import KafkaConsumer, KafkaProducer
    except ImportError:
        log.error("kafka-python is not installed. Run: pip install kafka-python")
        sys.exit(1)

    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )

    consumer = KafkaConsumer(
        "screening.initiated",
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id="screening-worker",
        auto_offset_reset="earliest",
        enable_auto_commit=False,
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
    )

    log.info(f"Screening worker listening on Kafka topic 'screening.initiated' @ {KAFKA_BOOTSTRAP}")

    for message in consumer:
        try:
            process_event(message.value, producer)
            consumer.commit()
        except Exception as e:
            log.error(f"Error processing message: {e}", exc_info=True)
            # Don't commit — message will be reprocessed after restart
            time.sleep(1)


if __name__ == "__main__":
    run_consumer()
