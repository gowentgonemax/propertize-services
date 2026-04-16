"""
Report Microservice — FastAPI (OOP refactored)
Thin routing layer; delegates to ReportService for business logic.
"""
import sys, os, logging
from datetime import date

from fastapi import FastAPI, Query, HTTPException
from fastapi.responses import StreamingResponse
import uvicorn

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.config import REPORT_SERVICE_PORT
from shared.logging_middleware import add_logging_middleware
from services.report_service import ReportService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

app = FastAPI(title="Propertize Report Service", version="2.0.0")
add_logging_middleware(app, service_name="report-service")
report_service = ReportService()


@app.get("/reports/financial/pdf")
def financial_report_pdf(
    org_id: str = Query(...),
    start_date: str = Query(...),
    end_date: str = Query(...),
):
    try:
        buffer = report_service.financial_report_pdf(org_id, start_date, end_date)
        return StreamingResponse(
            buffer, media_type="application/pdf",
            headers={"Content-Disposition":
                      f"attachment; filename=financial-report-{start_date}-{end_date}.pdf"},
        )
    except Exception as e:
        log.error(f"Financial PDF generation failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/reports/delinquency/excel")
def delinquency_report_excel(
    org_id: str = Query(...),
    as_of_date: str = Query(default=str(date.today())),
):
    try:
        buffer = report_service.delinquency_report_excel(org_id, as_of_date)
        return StreamingResponse(
            buffer,
            media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            headers={"Content-Disposition":
                      f"attachment; filename=delinquency-report-{as_of_date}.xlsx"},
        )
    except Exception as e:
        log.error(f"Delinquency Excel generation failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/reports/rent-roll/excel")
def rent_roll_excel(
    org_id: str = Query(...),
    as_of_date: str = Query(default=str(date.today())),
):
    try:
        buffer = report_service.rent_roll_excel(org_id, as_of_date)
        return StreamingResponse(
            buffer,
            media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            headers={"Content-Disposition":
                      f"attachment; filename=rent-roll-{as_of_date}.xlsx"},
        )
    except Exception as e:
        log.error(f"Rent roll Excel generation failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
def health():
    return report_service.health_check()


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=REPORT_SERVICE_PORT)
