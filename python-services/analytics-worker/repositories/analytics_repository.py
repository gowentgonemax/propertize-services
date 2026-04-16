"""Analytics data access."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.base_repository import BaseRepository


class AnalyticsRepository(BaseRepository):

    def ensure_summary_table(self):
        self.execute("""
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

    def fetch_unaggregated_events(self):
        return self.query("""
            SELECT event_name, event_type,
                   DATE_TRUNC('hour', created_at) AS hour_bucket,
                   COUNT(*) AS event_count
            FROM analytics_events
            WHERE aggregated = FALSE
            GROUP BY event_name, event_type, DATE_TRUNC('hour', created_at)
        """)

    def upsert_summary(self, hour_bucket, event_name, event_type, event_count: int):
        self.execute("""
            INSERT INTO analytics_summary (hour_bucket, event_name, event_type, event_count, updated_at)
            VALUES (%s, %s, %s, %s, NOW())
            ON CONFLICT (hour_bucket, event_name) DO UPDATE
            SET event_count = analytics_summary.event_count + EXCLUDED.event_count,
                updated_at = NOW()
        """, (hour_bucket, event_name, event_type, event_count))

    def mark_events_aggregated(self):
        self.execute("UPDATE analytics_events SET aggregated = TRUE WHERE aggregated = FALSE")

