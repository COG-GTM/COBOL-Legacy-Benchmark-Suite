from pydantic import BaseModel
from datetime import date, datetime
from typing import Optional


class PositionResponse(BaseModel):
    id: str
    portfolio_id: str
    investment_id: str
    symbol: str
    name: str
    position_date: date
    quantity: float
    cost_basis: float
    market_value: float
    current_price: float
    currency: str
    status: str
    gain_loss: float
    gain_loss_percent: float
    last_maint_user: str
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class PositionHistoryResponse(BaseModel):
    id: str
    portfolio_id: str
    investment_id: str
    record_date: date
    share_balance: float
    cost_basis: float
    market_value: float
    avg_cost: float
    event_type: str
    created_at: datetime

    class Config:
        from_attributes = True
