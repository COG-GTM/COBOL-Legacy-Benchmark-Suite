from datetime import date
from decimal import Decimal

from pydantic import BaseModel, ConfigDict


class PositionRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    portfolio_id: str
    investment_id: str
    position_date: date
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    currency_code: str


class PositionList(BaseModel):
    items: list[PositionRead]
    total: int
