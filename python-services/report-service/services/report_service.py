"""Report generation service — business logic layer for report-service."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import io
import pandas as pd
from shared.base_service import BaseService
from shared.renderers import BaseRenderer, PdfRenderer, ExcelRenderer
from repositories.financial_repository import FinancialRepository


class ReportService(BaseService):

    def __init__(self, repo: FinancialRepository | None = None):
        super().__init__()
        self.repo = repo or FinancialRepository()
        self._pdf = PdfRenderer()
        self._excel = ExcelRenderer()

    def service_name(self) -> str:
        return "report-service"

    # ── Financial Report (PDF) ─────────────────────────────────
    def financial_report_pdf(self, org_id: str, start_date: str, end_date: str) -> io.BytesIO:
        income_df = self.repo.get_income_by_type(org_id, start_date, end_date)
        expense_df = self.repo.get_expenses_by_category(org_id, start_date, end_date)

        # Format totals for PDF
        if not income_df.empty:
            income_df["total"] = income_df["total"].apply(lambda v: f"${v:,.2f}")
            income_df["count"] = income_df["count"].astype(str)
        if not expense_df.empty:
            expense_df["total"] = expense_df["total"].apply(lambda v: f"${v:,.2f}")
            expense_df["count"] = expense_df["count"].astype(str)

        sections = [
            {"heading": "Income Summary", "dataframe": income_df,
             "columns": ["payment_type", "count", "total"]},
            {"heading": "Expense Summary", "dataframe": expense_df,
             "columns": ["category", "count", "total"]},
        ]
        return self._pdf.render(
            sections, title=f"Financial Report: {start_date} to {end_date}"
        )

    # ── Delinquency Report (Excel) ─────────────────────────────
    def delinquency_report_excel(self, org_id: str, as_of_date: str) -> io.BytesIO:
        df = self.repo.get_delinquency_data(org_id, as_of_date)

        if not df.empty:
            df["oldest_due_date"] = pd.to_datetime(df["oldest_due_date"])
            df["days_overdue"] = (
                (pd.Timestamp(as_of_date) - df["oldest_due_date"]).dt.days.fillna(0).astype(int)
            )
            df["aging_bucket"] = pd.cut(
                df["days_overdue"],
                bins=[-1, 30, 60, 90, float("inf")],
                labels=["0-30 days", "31-60 days", "61-90 days", "90+ days"],
            )

        detail_df = df[["tenant_name", "email", "phone", "property_address",
                         "monthly_rent", "amount_owed", "days_overdue",
                         "aging_bucket"]] if not df.empty else pd.DataFrame()

        summary_df = (
            df.groupby("aging_bucket", observed=True)["amount_owed"]
            .agg(["sum", "count"])
            .rename(columns={"sum": "Total Amount", "count": "Tenant Count"})
            .reset_index()
        ) if not df.empty else pd.DataFrame()

        sections = [
            {"heading": "Delinquency Detail", "dataframe": detail_df,
             "sheet_name": "Delinquency Detail"},
            {"heading": "Aging Summary", "dataframe": summary_df,
             "sheet_name": "Aging Summary"},
        ]
        return self._excel.render(sections, title="Delinquency Report")

    # ── Rent Roll (Excel) ──────────────────────────────────────
    def rent_roll_excel(self, org_id: str, as_of_date: str) -> io.BytesIO:
        df = self.repo.get_rent_roll(org_id)

        summary_data = {
            "Total Units": [len(df)],
            "Occupied": [(df["lease_status"] == "ACTIVE").sum() if not df.empty else 0],
            "Available": [(df["property_status"] == "AVAILABLE").sum() if not df.empty else 0],
            "Total Monthly Rent": [
                df.loc[df["lease_status"] == "ACTIVE", "monthly_rent"].sum()
                if not df.empty else 0
            ],
        }

        sections = [
            {"heading": "Rent Roll", "dataframe": df, "sheet_name": "Rent Roll"},
            {"heading": "Summary", "dataframe": pd.DataFrame(summary_data),
             "sheet_name": "Summary"},
        ]
        return self._excel.render(sections, title="Rent Roll")

