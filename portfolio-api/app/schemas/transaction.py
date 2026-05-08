from datetime import date, datetime, time
from decimal import Decimal

from pydantic import BaseModel, ConfigDict

from app.schemas.constants import CurrencyCode, TransactionType


class TransactionCreate(BaseModel):
    portfolio_id: str
    transaction_date: date
    investment_id: str
    transaction_type: TransactionType
    quantity: Decimal
    price: Decimal
    amount: Decimal
    currency_code: CurrencyCode


class TransactionRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    transaction_id: str
    portfolio_id: str
    transaction_date: date
    transaction_time: time
    investment_id: str
    transaction_type: str
    quantity: Decimal
    price: Decimal
    amount: Decimal
    currency_code: str
    status: str
    process_date: datetime
    process_user: str
