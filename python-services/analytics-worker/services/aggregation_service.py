"""Analytics aggregation business logic."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime
from shared.base_service import BaseService
from repositories.analytics_repository import AnalyticsRepository


class AnalyticsAggregationService(BaseService):

    def __init__(self, repo: AnalyticsRepository | None = None):
        super().__init__()
        self.repo = repo or AnalyticsRepository()

    def service_name(self) -> str:
        return "analytics-worker"

    def initialise(self):
        self.repo.ensure_summary_table()
        self.log.info("Summary table and indexes ensured.")

    def aggregate(self):
        self.log.info("Starting analytics aggregation run")
        start = datetime.now()

        df = self.repo.fetch_unaggregated_events()
        if df.empty:
            self.log.info("No new events to aggregate.")
            return

        for _, row in df.iterrows():
            self.repo.upsert_summary(
                row["hour_bucket"], row["event_name"],
                row["event_type"], int(row["event_count"]),
            )

        self.repo.mark_events_aggregated()

        elapsed = (datetime.now() - start).total_seconds()
        self.log.info(f"Aggregation complete: {len(df)} groups in {elapsed:.2f}s")

