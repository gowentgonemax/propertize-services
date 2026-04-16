"""
Scoring factors — Strategy pattern for risk assessment.
Each factor returns a score in [0, 100].
"""
from abc import ABC, abstractmethod


class ScoringFactor(ABC):
    """Abstract scoring factor (Strategy interface)."""

    @abstractmethod
    def name(self) -> str: ...

    @abstractmethod
    def weight(self) -> float: ...

    @abstractmethod
    def score(self, data: dict) -> float: ...


class CreditScoreFactor(ScoringFactor):
    def name(self) -> str: return "credit_score"
    def weight(self) -> float: return 0.35

    def score(self, data: dict) -> float:
        cs = data.get("credit_score")
        if cs is None:
            return 40.0
        cs = max(300.0, min(850.0, float(cs)))
        return (cs - 300.0) / (850.0 - 300.0) * 100.0


class IncomeRatioFactor(ScoringFactor):
    def name(self) -> str: return "income_ratio"
    def weight(self) -> float: return 0.30

    def score(self, data: dict) -> float:
        income = data.get("monthly_income")
        rent = data.get("monthly_rent")
        if not income or not rent or float(income) <= 0:
            return 35.0
        ratio = float(rent) / float(income)
        if ratio <= 0.25: return 100.0
        if ratio >= 0.55: return 0.0
        return max(0.0, (0.55 - ratio) / (0.55 - 0.25) * 100.0)


class CriminalFactor(ScoringFactor):
    def name(self) -> str: return "criminal"
    def weight(self) -> float: return 0.25

    def score(self, data: dict) -> float:
        has_record = data.get("has_criminal_record")
        if has_record is None:
            return 70.0
        if not has_record:
            return 100.0
        crime = (data.get("criminal_offense_type") or "").upper()
        if any(k in crime for k in ("VIOLENT", "ASSAULT", "FELONY", "HOMICIDE", "SEXUAL")):
            return 0.0
        return 60.0


class EmploymentFactor(ScoringFactor):
    def name(self) -> str: return "employment"
    def weight(self) -> float: return 0.10

    def score(self, data: dict) -> float:
        if not data.get("employer_name"):
            return 20.0
        months = data.get("months_at_employer") or 0
        if months >= 24: return 100.0
        if months >= 12: return 80.0
        if months >= 6: return 60.0
        return 30.0


# Default factor chain — add new factors by appending to this list
DEFAULT_FACTORS: list[ScoringFactor] = [
    CreditScoreFactor(),
    IncomeRatioFactor(),
    CriminalFactor(),
    EmploymentFactor(),
]

