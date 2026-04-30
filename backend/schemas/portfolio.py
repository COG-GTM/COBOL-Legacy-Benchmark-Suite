from pydantic import BaseModel, Field
from typing import Optional
from datetime import date, datetime


class PortfolioCreate(BaseModel):
    portfolio_id: str = Field(..., min_length=8, max_length=8)
    account_number: str = Field(..., min_length=1, max_length=10)
    client_name: str = Field(..., min_length=1, max_length=50)
    client_type: str = Field(default="I", pattern=r"^[ICT]$")
    portfolio_name: str = Field(default="", max_length=50)
    currency_code: str = Field(default="USD", min_length=3, max_length=3)
    risk_level: str = Field(default="M", pattern=r"^[LMH]$")


class PortfolioUpdate(BaseModel):
    client_name: Optional[str] = Field(None, max_length=50)
    portfolio_name: Optional[str] = Field(None, max_length=50)
    risk_level: Optional[str] = Field(None, pattern=r"^[LMH]$")
    status: Optional[str] = Field(None, pattern=r"^[ACS]$")


class PositionSummary(BaseModel):
    investment_id: str
    symbol: str
    name: str
    quantity: float
    cost_basis: float
    current_price: float
    market_value: float
    gain_loss: float
    gain_loss_percent: float
    status: str

    class Config:
        from_attributes = True


class PortfolioResponse(BaseModel):
    portfolio_id: str
    account_number: str
    client_name: str
    client_type: str
    portfolio_name: str
    currency_code: str
    risk_level: str
    status: str
    total_value: float
    cash_balance: float
    open_date: date
    close_date: Optional[date] = None
    last_maint_user: str
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class PortfolioDetailResponse(PortfolioResponse):
    positions: list[PositionSummary] = []
    total_gain_loss: float = 0
    total_gain_loss_percent: float = 0
    position_count: int = 0


class PortfolioListResponse(BaseModel):
    portfolios: list[PortfolioResponse]
    total: int
