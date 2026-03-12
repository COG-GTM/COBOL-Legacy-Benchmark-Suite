"""Transaction record model translated from src/copybook/common/TRNREC.cpy."""

from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, field_validator

from models.enums import TransactionStatus, TransactionType


class TransactionRecord(BaseModel):
    """Transaction record (TRNREC.cpy TRANSACTION-RECORD).

    - quantity: PIC S9(11)V9(4) COMP-3 -> Decimal, 4 decimal places
    - price:    PIC S9(11)V9(4) COMP-3 -> Decimal, 4 decimal places
    - amount:   PIC S9(13)V9(2) COMP-3 -> Decimal, 2 decimal places
    """

    date: str
    time: str
    portfolio_id: str
    sequence_no: str
    investment_id: str
    trn_type: TransactionType
    quantity: Decimal
    price: Decimal
    amount: Decimal
    currency: str
    status: TransactionStatus
    process_date: datetime
    process_user: str

    @field_validator("date")
    @classmethod
    def validate_date(cls, v: str) -> str:
        """Transaction date must be YYYYMMDD (COBOL PIC X(08))."""
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Transaction date must be 8 numeric digits (YYYYMMDD)")
        return v

    @field_validator("time")
    @classmethod
    def validate_time(cls, v: str) -> str:
        """Transaction time must be HHMMSS (COBOL PIC X(06))."""
        if len(v) != 6 or not v.isdigit():
            raise ValueError("Transaction time must be 6 numeric digits (HHMMSS)")
        return v

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        """Portfolio ID max 8 characters (COBOL PIC X(08))."""
        if len(v) > 8:
            raise ValueError("Portfolio ID must not exceed 8 characters")
        return v

    @field_validator("sequence_no")
    @classmethod
    def validate_sequence_no(cls, v: str) -> str:
        """Sequence number max 6 characters (COBOL PIC X(06))."""
        if len(v) > 6:
            raise ValueError("Sequence number must not exceed 6 characters")
        return v

    @field_validator("investment_id")
    @classmethod
    def validate_investment_id(cls, v: str) -> str:
        """Investment ID max 10 characters (COBOL PIC X(10))."""
        if len(v) > 10:
            raise ValueError("Investment ID must not exceed 10 characters")
        return v

    @field_validator("currency")
    @classmethod
    def validate_currency(cls, v: str) -> str:
        """Currency code max 3 characters (COBOL PIC X(03))."""
        if len(v) > 3:
            raise ValueError("Currency code must not exceed 3 characters")
        return v

    @field_validator("process_user")
    @classmethod
    def validate_process_user(cls, v: str) -> str:
        """Process user max 8 characters (COBOL PIC X(08))."""
        if len(v) > 8:
            raise ValueError("Process user must not exceed 8 characters")
        return v
