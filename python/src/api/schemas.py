"""
API request/response schemas translated from BMS map definitions in INQMAP.bms.

The BMS map fields become API request/response fields:
  MENMAP -> Menu/navigation schemas
  POSMAP -> Position inquiry schemas
  HISMAP -> History inquiry schemas
  ERRMAP -> Error response schemas
"""

from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, Field

from src.common.constants import (
    AccountType,
    ClientType,
    CurrencyCode,
    RiskLevel,
    TransactionType,
)


# ---------------------------------------------------------------------------
# Portfolio Schemas (from POSMAP BMS fields)
# ---------------------------------------------------------------------------
class PortfolioCreateRequest(BaseModel):
    """Request to create a new portfolio."""

    portfolio_id: str = Field(max_length=8, description="Portfolio ID (PORT + 4 digits)")
    account_no: str = Field(default="", max_length=10, description="Account number")
    account_type: AccountType = Field(default=AccountType.INDIVIDUAL)
    branch_id: str = Field(default="", max_length=2)
    client_id: str = Field(default="", max_length=10)
    portfolio_name: str = Field(default="", max_length=50)
    currency_code: CurrencyCode = Field(default=CurrencyCode.USD)
    risk_level: RiskLevel = Field(default=RiskLevel.MEDIUM)
    client_name: str = Field(default="", max_length=30)
    client_type: ClientType = Field(default=ClientType.INDIVIDUAL)
    open_date: date = Field(default_factory=date.today)
    cash_balance: Decimal = Field(default=Decimal("0.00"))


class PortfolioUpdateRequest(BaseModel):
    """Request to update a portfolio."""

    portfolio_name: str | None = Field(default=None, max_length=50)
    currency_code: str | None = Field(default=None, max_length=3)
    risk_level: str | None = Field(default=None, max_length=1)
    status: str | None = Field(default=None, max_length=1)
    client_name: str | None = Field(default=None, max_length=30)
    cash_balance: Decimal | None = Field(default=None)


class PortfolioResponse(BaseModel):
    """Portfolio response (from POSMAP output fields)."""

    portfolio_id: str
    account_no: str
    account_type: str
    branch_id: str
    client_id: str
    portfolio_name: str
    currency_code: str
    risk_level: str
    client_name: str
    client_type: str
    status: str
    open_date: date
    close_date: date | None
    total_value: Decimal
    cash_balance: Decimal
    last_maint_date: datetime
    last_maint_user: str

    model_config = {"from_attributes": True}


class PortfolioListResponse(BaseModel):
    """Paginated list of portfolios."""

    items: list[PortfolioResponse]
    total: int
    offset: int
    limit: int


# ---------------------------------------------------------------------------
# Position Schemas (from POSMAP BMS fields)
# ---------------------------------------------------------------------------
class PositionResponse(BaseModel):
    """Position response."""

    portfolio_id: str
    investment_id: str
    position_date: date
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    gain_loss: Decimal
    currency: str
    status: str

    model_config = {"from_attributes": True}


class PortfolioPositionsResponse(BaseModel):
    """All positions for a portfolio."""

    portfolio_id: str
    positions: list[PositionResponse]
    total_market_value: Decimal
    total_cost_basis: Decimal
    total_gain_loss: Decimal


# ---------------------------------------------------------------------------
# Transaction Schemas (from HISMAP BMS fields)
# ---------------------------------------------------------------------------
class TransactionCreateRequest(BaseModel):
    """Request to create a transaction."""

    investment_id: str = Field(default="", max_length=10)
    trn_type: TransactionType
    quantity: Decimal = Field(gt=0)
    price: Decimal = Field(ge=0)
    amount: Decimal


class TransactionResponse(BaseModel):
    """Transaction response."""

    transaction_id: str
    trn_date: date
    trn_time: str
    portfolio_id: str
    sequence_no: str
    investment_id: str
    trn_type: str
    quantity: Decimal
    price: Decimal
    amount: Decimal
    currency: str
    status: str
    process_date: datetime
    process_user: str

    model_config = {"from_attributes": True}


class TransactionListResponse(BaseModel):
    """List of transactions."""

    items: list[TransactionResponse]
    total: int


# ---------------------------------------------------------------------------
# Batch Schemas
# ---------------------------------------------------------------------------
class BatchRunRequest(BaseModel):
    """Request to trigger batch processing."""

    process_date: date = Field(default_factory=date.today)
    full_cycle: bool = Field(default=False)
    step: str | None = Field(default=None)
    restart: bool = Field(default=False)


class BatchStatusResponse(BaseModel):
    """Batch status response."""

    batch_id: str
    status: str
    start_time: datetime | None
    end_time: datetime | None
    records_read: int
    records_processed: int
    records_rejected: int
    error_count: int
    return_code: int


# ---------------------------------------------------------------------------
# Error Schema (from ERRMAP BMS fields)
# ---------------------------------------------------------------------------
class ErrorResponse(BaseModel):
    """Error response (from ERRMAP output fields)."""

    error_code: str
    message: str
    severity: int
    detail: str = ""


# ---------------------------------------------------------------------------
# Health Check
# ---------------------------------------------------------------------------
class HealthResponse(BaseModel):
    """Health check response."""

    status: str = "healthy"
    version: str = "1.0.0"
    database: str = "connected"
    timestamp: datetime = Field(default_factory=datetime.now)
