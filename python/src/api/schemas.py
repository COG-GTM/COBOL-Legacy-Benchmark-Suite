"""
API request/response Pydantic models.
Translated from BMS map definitions in INQMAP.bms / INQSET.bms.
Map fields become API request/response fields.
"""

from typing import Optional

from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# Portfolio schemas (from POSMAP BMS fields)
# ---------------------------------------------------------------------------
class PortfolioCreateRequest(BaseModel):
    portfolio_id: str = Field(max_length=8)
    client_id: str = Field(max_length=10)
    client_name: str = Field(max_length=30)
    portfolio_name: str = Field(max_length=50, default="")
    account_type: str = Field(max_length=2, default="IN")
    branch_id: str = Field(max_length=2, default="00")
    currency_code: str = Field(max_length=3, default="USD")
    risk_level: str = Field(max_length=1, default="M")
    client_type: str = Field(max_length=1, default="I")


class PortfolioUpdateRequest(BaseModel):
    portfolio_name: Optional[str] = Field(default=None, max_length=50)
    currency_code: Optional[str] = Field(default=None, max_length=3)
    risk_level: Optional[str] = Field(default=None, max_length=1)
    status: Optional[str] = Field(default=None, max_length=1)
    client_name: Optional[str] = Field(default=None, max_length=30)
    branch_id: Optional[str] = Field(default=None, max_length=2)


class PortfolioResponse(BaseModel):
    portfolio_id: str
    client_id: str
    client_name: str
    client_type: str
    portfolio_name: str
    account_type: str
    branch_id: str
    currency_code: str
    risk_level: str
    status: str
    total_value: str  # Decimal as string for JSON safety
    cash_balance: str
    open_date: Optional[str] = None
    close_date: Optional[str] = None
    last_maint_date: Optional[str] = None
    last_maint_user: str = ""

    model_config = {"from_attributes": True}


class PortfolioListResponse(BaseModel):
    portfolios: list[PortfolioResponse]
    total: int


# ---------------------------------------------------------------------------
# Position schemas (from POSMAP BMS: FUNDOUT, UNITOUT, COSTOUT, VALOUT)
# ---------------------------------------------------------------------------
class PositionResponse(BaseModel):
    portfolio_id: str
    investment_id: str  # FUNDOUT
    position_date: str
    quantity: str  # UNITOUT — Decimal as string
    cost_basis: str  # COSTOUT
    market_value: str  # VALOUT
    currency: str
    status: str

    model_config = {"from_attributes": True}


class PositionListResponse(BaseModel):
    portfolio_id: str
    positions: list[PositionResponse]
    total: int


# ---------------------------------------------------------------------------
# Transaction schemas (from HISMAP BMS: Date, Type, Units, Price, Amount)
# ---------------------------------------------------------------------------
class TransactionCreateRequest(BaseModel):
    portfolio_id: str = Field(max_length=8)
    investment_id: str = Field(max_length=10)
    trn_type: str = Field(max_length=2)
    quantity: str  # Decimal as string
    price: str  # Decimal as string
    currency: str = Field(max_length=3, default="USD")


class TransactionResponse(BaseModel):
    transaction_id: Optional[int] = None
    portfolio_id: str
    investment_id: str
    trn_date: str
    trn_time: str
    trn_type: str
    quantity: str
    price: str
    amount: str
    currency: str
    status: str
    process_date: Optional[str] = None
    fees: str = "0.00"
    gain_loss: str = "0.00"

    model_config = {"from_attributes": True}


class TransactionListResponse(BaseModel):
    portfolio_id: str
    transactions: list[TransactionResponse]
    total: int


# ---------------------------------------------------------------------------
# Batch schemas
# ---------------------------------------------------------------------------
class BatchRunRequest(BaseModel):
    process_date: Optional[str] = None
    step: Optional[str] = None


class BatchStatusResponse(BaseModel):
    job_name: str
    process_date: str
    status: str
    return_code: int
    records_read: int
    records_written: int
    error_count: int
    start_time: Optional[str] = None
    end_time: Optional[str] = None


# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------
class HealthResponse(BaseModel):
    status: str
    database: str
    timestamp: str


# ---------------------------------------------------------------------------
# Error response (from ERRMAP BMS: ERRCOUT, ERRDOUT)
# ---------------------------------------------------------------------------
class ErrorResponse(BaseModel):
    error_code: str  # ERRCOUT
    detail: str  # ERRDOUT
