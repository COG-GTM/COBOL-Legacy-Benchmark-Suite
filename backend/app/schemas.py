"""Pydantic schemas for request/response validation.

Replaces COBOL LINKAGE SECTION and COMMAREA data structures
used for inter-program communication in CICS.
"""

from datetime import date, datetime

from pydantic import BaseModel, Field


# --- Portfolio Schemas ---

class PositionOut(BaseModel):
    id: int
    portfolio_id: str
    investment_id: str
    investment_name: str
    position_date: date
    quantity: float
    cost_basis: float
    market_value: float
    gain_loss: float = 0
    gain_loss_pct: float = 0
    currency_code: str
    status: str
    last_maint_date: datetime

    model_config = {"from_attributes": True}


class TransactionOut(BaseModel):
    id: int
    transaction_id: str
    portfolio_id: str
    investment_id: str
    transaction_date: date
    transaction_type: str
    quantity: float
    price: float
    amount: float
    currency_code: str
    status: str
    process_date: datetime

    model_config = {"from_attributes": True}


class PortfolioSummary(BaseModel):
    id: int
    portfolio_id: str
    account_no: str
    client_name: str
    client_type: str
    currency_code: str
    risk_level: str
    status: str
    total_value: float
    cash_balance: float
    open_date: date
    close_date: date | None
    last_maint_date: datetime
    position_count: int = 0
    total_market_value: float = 0
    total_cost_basis: float = 0
    total_gain_loss: float = 0
    total_gain_loss_pct: float = 0

    model_config = {"from_attributes": True}


class PortfolioDetail(PortfolioSummary):
    positions: list[PositionOut] = []
    recent_transactions: list[TransactionOut] = []


class PortfolioCreate(BaseModel):
    portfolio_id: str = Field(min_length=1, max_length=8)
    account_no: str = Field(min_length=1, max_length=10)
    client_name: str = Field(min_length=1, max_length=50)
    client_type: str = Field(default="I", pattern=r"^[ICT]$")
    currency_code: str = Field(default="USD", max_length=3)
    risk_level: str = Field(default="M", pattern=r"^[LMHA]$")
    cash_balance: float = Field(default=0, ge=0)


class PortfolioUpdate(BaseModel):
    client_name: str | None = None
    status: str | None = Field(default=None, pattern=r"^[ACS]$")
    risk_level: str | None = Field(default=None, pattern=r"^[LMHA]$")
    cash_balance: float | None = None


# --- Dashboard / Report Schemas ---

class DashboardStats(BaseModel):
    total_portfolios: int
    active_portfolios: int
    total_market_value: float
    total_cost_basis: float
    total_gain_loss: float
    total_gain_loss_pct: float
    total_positions: int
    total_transactions: int
    recent_transactions: list[TransactionOut]
    portfolio_breakdown: list[dict]
    status_breakdown: list[dict]
    top_performers: list[dict]


class TransactionCreate(BaseModel):
    portfolio_id: str = Field(min_length=1, max_length=8)
    investment_id: str = Field(min_length=1, max_length=10)
    transaction_type: str = Field(pattern=r"^(BU|SL|TR|FE)$")
    quantity: float = Field(gt=0)
    price: float = Field(gt=0)
