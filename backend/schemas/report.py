from pydantic import BaseModel
from datetime import datetime
from typing import Optional


class PositionReportItem(BaseModel):
    portfolio_id: str
    portfolio_name: str
    investment_id: str
    symbol: str
    name: str
    quantity: float
    cost_basis: float
    market_value: float
    gain_loss: float
    gain_loss_percent: float


class PositionReportResponse(BaseModel):
    report_date: datetime
    report_type: str = "DAILY_POSITION"
    total_portfolios: int
    total_positions: int
    total_market_value: float
    total_cost_basis: float
    total_gain_loss: float
    items: list[PositionReportItem]


class AuditEntry(BaseModel):
    timestamp: datetime
    program_id: str
    error_code: str
    account_number: Optional[str] = None
    portfolio_id: Optional[str] = None
    description: str
    severity: str


class AuditReportResponse(BaseModel):
    report_date: datetime
    report_type: str = "AUDIT"
    total_entries: int
    error_count: int
    warning_count: int
    entries: list[AuditEntry]


class StatisticsResponse(BaseModel):
    report_date: datetime
    total_portfolios: int
    active_portfolios: int
    total_positions: int
    total_transactions: int
    transactions_today: int
    total_market_value: float
    total_gain_loss: float
    avg_portfolio_value: float
    system_health: str = "HEALTHY"
