from pydantic import BaseModel, Field
from datetime import date, time, datetime
from typing import Optional


class TransactionCreate(BaseModel):
    portfolio_id: str = Field(..., min_length=8, max_length=8)
    investment_id: str = Field(..., min_length=1, max_length=10)
    transaction_type: str = Field(..., pattern=r"^(BU|SL|TR|FE)$")
    quantity: float = Field(..., gt=0)
    price: float = Field(..., gt=0)
    currency: str = Field(default="USD", min_length=3, max_length=3)


class TransactionStatusUpdate(BaseModel):
    status: str = Field(..., pattern=r"^[PDFR]$")


class TransactionResponse(BaseModel):
    transaction_id: str
    portfolio_id: str
    investment_id: str
    transaction_date: date
    transaction_time: time
    sequence_no: str
    transaction_type: str
    quantity: float
    price: float
    amount: float
    currency: str
    status: str
    process_date: Optional[datetime] = None
    process_user: str
    created_at: datetime

    class Config:
        from_attributes = True


class TransactionListResponse(BaseModel):
    transactions: list[TransactionResponse]
    total: int
