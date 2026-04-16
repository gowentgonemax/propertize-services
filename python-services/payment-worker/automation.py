"""
Payment Automation Worker (OOP refactored)
Thin entry point; delegates to PaymentAutomationService.
"""
import sys, os, logging
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from apscheduler.schedulers.blocking import BlockingScheduler
from services.payment_automation_service import PaymentAutomationService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

service = PaymentAutomationService()

if __name__ == "__main__":
    service.initialise()

    scheduler = BlockingScheduler()
    scheduler.add_job(service.generate_monthly_rent_payments, "cron",
                      day=20, hour=1, minute=0, id="generate_rent_payments")
    scheduler.add_job(service.apply_late_fees, "cron",
                      hour=2, minute=0, id="apply_late_fees")
    scheduler.add_job(service.send_upcoming_reminders, "cron",
                      hour=9, minute=0, id="send_reminders")

    log.info("Payment worker started. Jobs: generate (monthly), late_fees (daily 02:00), reminders (daily 09:00)")
    scheduler.start()
