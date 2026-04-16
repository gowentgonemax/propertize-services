"""
Analytics Aggregation Worker (OOP refactored)
Thin entry point; delegates to AnalyticsAggregationService.
"""
import sys, os, logging
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from apscheduler.schedulers.blocking import BlockingScheduler
from services.aggregation_service import AnalyticsAggregationService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

service = AnalyticsAggregationService()

if __name__ == "__main__":
    service.initialise()
    service.aggregate()  # run once immediately

    scheduler = BlockingScheduler()
    scheduler.add_job(service.aggregate, "interval", minutes=5, id="analytics_aggregation")
    log.info("Scheduler started — aggregating every 5 minutes")
    scheduler.start()
