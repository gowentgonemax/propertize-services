"""
Analytics Aggregation Worker
Runs every 5 minutes via APScheduler.
Pre-aggregates analytics_events into analytics_summary so dashboard
queries are instant lookups instead of full table scans.
"""
import sys
import os
import logging
from datetime import datetime

import pandas as pd
from apscheduler.schedulers.blocking import BlockingScheduler

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.db import get_connection

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)


def ensure_summary_table():
    """Create analytics_summary table if it doesn't exist."""
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""
                CREATE TABLE IF NOT EXISTS analytics_summary (
                    id SERIAL PRIMARY KEY,
                    hour_bucket TIMESTAMPTZ NOT NULL,
                    event_name VARCHAR(255) NOT NULL,
                    event_type VARCHAR(100),
                    event_count BIGINT NOT NULL DEFAULT 0,
                    updated_at TIMESTAMPTZ DEFAULT NOW(),
                    UNIQUE (hour_bucket, event_name)
                );
                CREATE INDEX IF NOT EXISTS idx_analytics_summary_bucket
                    ON analytics_summary (hour_bucket DESC);

                ALTER TABLE analytics_events
                    ADD COLUMN IF NOT EXISTS aggregated BOOLEAN DEFAULT FALSE;
                CREATE INDEX IF NOT EXISTS idx_analytics_events_unagg
                    ON analytics_events (aggregated, created_at)
                    WHERE aggregated = FALSE;
            """)
    log.info("Summary table and indexes ensured.")


def aggregate_analytics():
    """
    Main aggregation job.
    Reads unaggregated events, groups by hour bucket + event_name,
    upserts into analytics_summary, then marks source rows as aggregated.
    """
    log.info("Starting analytics aggregation run")
    start = datetime.now()

    with get_connection() as conn:
        df = pd.read_sql("""
            SELECT
                event_name,
                event_type,
                DATE_TRUNC('hour', created_at) AS hour_bucket,
                COUNT(*) AS event_count
            FROM analytics_events
            WHERE aggregated = FALSE
            GROUP BY event_name, event_type, DATE_TRUNC('hour', created_at)
        """, conn)

        if df.empty:
            log.info("No new events to aggregate.")
            return

        with conn.cursor() as cur:
            for _, row in df.iterrows():
                cur.execute("""
                    INSERT INTO analytics_summary (hour_bucket, event_name, event_type, event_count, updated_at)
                    VALUES (%s, %s, %s, %s, NOW())
                    ON CONFLICT (hour_bucket, event_name) DO UPDATE
                    SET event_count = analytics_summary.event_count + EXCLUDED.event_count,
                        updated_at = NOW()
                """, (row['hour_bucket'], row['event_name'], row['event_type'], int(row['event_count'])))

            # Mark source rows as aggregated
            cur.execute("UPDATE analytics_events SET aggregated = TRUE WHERE aggregated = FALSE")

    elapsed = (datetime.now() - start).total_seconds()
    log.info(f"Aggregation complete: {len(df)} groups processed in {elapsed:.2f}s")


if __name__ == "__main__":
    ensure_summary_table()
    # Run once immediately on startup
    aggregate_analytics()

    scheduler = BlockingScheduler()
    scheduler.add_job(aggregate_analytics, "interval", minutes=5, id="analytics_aggregation")
    log.info("Scheduler started — aggregating every 5 minutes")
    scheduler.start()
