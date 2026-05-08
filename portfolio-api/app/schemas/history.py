from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict


class HistoryRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    transaction_id: str
    portfolio_id: str
    transaction_date: date
    investment_id: str
    transaction_type: str
    quantity: Decimal
    price: Decimal
    amount: Decimal
    currency_code: str
    status: str
    process_date: datetime
    process_user: str


class HistoryPage(BaseModel):
    items: list[HistoryRead]
    total: int
    page: int
    size: int
