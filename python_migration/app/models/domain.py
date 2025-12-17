"""
Domain models (Pydantic) migrated from COBOL copybooks.
These models represent the business data structures.

Copybook to Python Type Mappings:
- PIC X(n) -> str
- PIC 9(n) -> int
- PIC S9(n)V9(m) COMP-3 -> Decimal
- PIC X(8) date -> date
- PIC X(26) timestamp -> datetime
"""

from datetime import date, datetime, time
from decimal import Decimal
from enum import Enum

from pydantic import BaseModel, Field, field_validator


class TransactionType(str, Enum):
    """Transaction types from TRNREC.cpy"""
    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(str, Enum):
    """Transaction status from TRNREC.cpy"""
    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


class PositionStatus(str, Enum):
    """Position status from POSREC.cpy"""
    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


class PortfolioStatus(str, Enum):
    """Portfolio status from PORTFLIO.cpy"""
    ACTIVE = "A"
    CLOSED = "C"
    SUSPENDED = "S"


class ClientType(str, Enum):
    """Client type from PORTFLIO.cpy"""
    INDIVIDUAL = "I"
    CORPORATE = "C"
    TRUST = "T"


class HistoryRecordType(str, Enum):
    """History record type from HISTREC.cpy"""
    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    """History action code from HISTREC.cpy"""
    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


class AuditType(str, Enum):
    """Audit type from AUDITLOG.cpy"""
    TRANSACTION = "TRAN"
    USER_ACTION = "USER"
    SYSTEM_EVENT = "SYST"


class AuditAction(str, Enum):
    """Audit action from AUDITLOG.cpy"""
    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    INQUIRE = "INQUIRE"
    LOGIN = "LOGIN"
    LOGOUT = "LOGOUT"
    STARTUP = "STARTUP"
    SHUTDOWN = "SHUTDOWN"


class AuditStatus(str, Enum):
    """Audit status from AUDITLOG.cpy"""
    SUCCESS = "SUCC"
    FAILURE = "FAIL"
    WARNING = "WARN"


class ErrorSeverity(int, Enum):
    """Error severity from ERRHAND.cpy and RETHND.cpy"""
    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    CRITICAL = 16


class ErrorCategory(str, Enum):
    """Error category from ERRHAND.cpy"""
    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


class ErrorType(str, Enum):
    """Error type from RETHND.cpy"""
    VALIDATION = "V"
    PROCESSING = "P"
    DATABASE = "D"
    FILE = "F"
    SECURITY = "S"


class BatchStatus(str, Enum):
    """Batch status from BCHCTL.cpy"""
    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


class CheckpointStatus(str, Enum):
    """Checkpoint status from CKPRST.cpy"""
    INITIAL = "I"
    ACTIVE = "A"
    COMPLETE = "C"
    FAILED = "F"
    RESTARTED = "R"


class CheckpointPhase(str, Enum):
    """Checkpoint phase from CKPRST.cpy"""
    INIT = "00"
    READ = "10"
    PROCESS = "20"
    UPDATE = "30"
    TERMINATE = "40"


class TransactionRecord(BaseModel):
    """
    Transaction Record - migrated from TRNREC.cpy
    Represents a financial transaction in the portfolio system.
    """
    transaction_date: date = Field(..., description="Transaction date (YYYYMMDD)")
    transaction_time: time = Field(..., description="Transaction time (HHMMSS)")
    portfolio_id: str = Field(..., max_length=8, description="Portfolio identifier")
    sequence_no: str = Field(..., max_length=6, description="Sequence number")
    investment_id: str = Field(..., max_length=10, description="Investment identifier")
    transaction_type: TransactionType = Field(..., description="BU=Buy, SL=Sell, TR=Transfer, FE=Fee")
    quantity: Decimal = Field(..., decimal_places=4, description="Transaction quantity")
    price: Decimal = Field(..., decimal_places=4, description="Transaction price")
    amount: Decimal = Field(..., decimal_places=2, description="Transaction amount")
    currency: str = Field(default="USD", max_length=3, description="Currency code")
    status: TransactionStatus = Field(default=TransactionStatus.PENDING, description="Transaction status")
    process_date: datetime | None = Field(None, description="Processing timestamp")
    process_user: str | None = Field(None, max_length=8, description="Processing user ID")

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        if not v or len(v.strip()) == 0:
            raise ValueError("Portfolio ID cannot be empty")
        return v.strip().upper()

    @field_validator("amount")
    @classmethod
    def validate_amount(cls, v: Decimal) -> Decimal:
        max_amount = Decimal("9999999999999.99")
        min_amount = Decimal("-9999999999999.99")
        if v < min_amount or v > max_amount:
            raise ValueError(f"Amount must be between {min_amount} and {max_amount}")
        return v

    class Config:
        json_encoders = {
            Decimal: str,
            date: lambda v: v.isoformat(),
            time: lambda v: v.isoformat(),
            datetime: lambda v: v.isoformat(),
        }


class PositionRecord(BaseModel):
    """
    Position Record - migrated from POSREC.cpy
    Represents a portfolio position (holdings).
    """
    portfolio_id: str = Field(..., max_length=8, description="Portfolio identifier")
    position_date: date = Field(..., description="Position date (YYYYMMDD)")
    investment_id: str = Field(..., max_length=10, description="Investment identifier")
    quantity: Decimal = Field(..., decimal_places=4, description="Holding quantity")
    cost_basis: Decimal = Field(..., decimal_places=2, description="Total cost basis")
    market_value: Decimal = Field(..., decimal_places=2, description="Current market value")
    currency: str = Field(default="USD", max_length=3, description="Currency code")
    status: PositionStatus = Field(default=PositionStatus.ACTIVE, description="Position status")
    last_maint_date: datetime | None = Field(None, description="Last maintenance timestamp")
    last_maint_user: str | None = Field(None, max_length=8, description="Last maintenance user")

    class Config:
        json_encoders = {
            Decimal: str,
            date: lambda v: v.isoformat(),
            datetime: lambda v: v.isoformat(),
        }


class PortfolioRecord(BaseModel):
    """
    Portfolio Record - migrated from PORTFLIO.cpy
    Represents a portfolio master record.
    """
    portfolio_id: str = Field(..., max_length=8, description="Portfolio identifier")
    account_no: str = Field(..., max_length=10, description="Account number")
    client_name: str = Field(..., max_length=30, description="Client name")
    client_type: ClientType = Field(default=ClientType.INDIVIDUAL, description="Client type")
    create_date: date = Field(..., description="Portfolio creation date")
    last_maint_date: date = Field(..., description="Last maintenance date")
    status: PortfolioStatus = Field(default=PortfolioStatus.ACTIVE, description="Portfolio status")
    total_value: Decimal = Field(default=Decimal("0.00"), decimal_places=2, description="Total portfolio value")
    cash_balance: Decimal = Field(default=Decimal("0.00"), decimal_places=2, description="Cash balance")
    last_user: str | None = Field(None, max_length=8, description="Last user ID")
    last_trans_date: date | None = Field(None, description="Last transaction date")

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id_format(cls, v: str) -> str:
        v = v.strip().upper()
        if not v.startswith("PORT"):
            raise ValueError("Portfolio ID must start with 'PORT'")
        return v

    class Config:
        json_encoders = {
            Decimal: str,
            date: lambda v: v.isoformat(),
        }


class HistoryRecord(BaseModel):
    """
    History Record - migrated from HISTREC.cpy
    Represents a change history record for audit trail.
    """
    portfolio_id: str = Field(..., max_length=8, description="Portfolio identifier")
    history_date: date = Field(..., description="History date (YYYYMMDD)")
    history_time: time = Field(..., description="History time (HHMMSS)")
    sequence_no: str = Field(..., max_length=4, description="Sequence number")
    record_type: HistoryRecordType = Field(..., description="PT=Portfolio, PS=Position, TR=Transaction")
    action_code: HistoryActionCode = Field(..., description="A=Add, C=Change, D=Delete")
    before_image: str | None = Field(None, max_length=400, description="Record before change")
    after_image: str | None = Field(None, max_length=400, description="Record after change")
    reason_code: str | None = Field(None, max_length=4, description="Reason for change")
    process_date: datetime | None = Field(None, description="Processing timestamp")
    process_user: str | None = Field(None, max_length=8, description="Processing user ID")

    class Config:
        json_encoders = {
            date: lambda v: v.isoformat(),
            time: lambda v: v.isoformat(),
            datetime: lambda v: v.isoformat(),
        }


class AuditRecord(BaseModel):
    """
    Audit Record - migrated from AUDITLOG.cpy
    Represents an audit trail entry.
    """
    timestamp: datetime = Field(..., description="Audit timestamp")
    system_id: str = Field(..., max_length=8, description="System identifier")
    user_id: str = Field(..., max_length=8, description="User identifier")
    program: str = Field(..., max_length=8, description="Program name")
    terminal: str | None = Field(None, max_length=8, description="Terminal ID")
    audit_type: AuditType = Field(..., description="Audit type")
    action: AuditAction = Field(..., description="Audit action")
    status: AuditStatus = Field(..., description="Audit status")
    portfolio_id: str | None = Field(None, max_length=8, description="Portfolio ID")
    account_no: str | None = Field(None, max_length=10, description="Account number")
    before_image: str | None = Field(None, max_length=100, description="Before image")
    after_image: str | None = Field(None, max_length=100, description="After image")
    message: str | None = Field(None, max_length=100, description="Audit message")

    class Config:
        json_encoders = {
            datetime: lambda v: v.isoformat(),
        }


class ErrorMessage(BaseModel):
    """
    Error Message - migrated from ERRHAND.cpy
    Represents an error message structure.
    """
    error_date: date = Field(..., description="Error date")
    error_time: time = Field(..., description="Error time")
    program: str = Field(..., max_length=8, description="Program name")
    category: ErrorCategory = Field(..., description="Error category")
    code: str = Field(..., max_length=4, description="Error code")
    severity: ErrorSeverity = Field(..., description="Error severity")
    text: str = Field(..., max_length=80, description="Error text")
    details: str | None = Field(None, max_length=256, description="Error details")

    class Config:
        json_encoders = {
            date: lambda v: v.isoformat(),
            time: lambda v: v.isoformat(),
        }


class BatchControlRecord(BaseModel):
    """
    Batch Control Record - migrated from BCHCTL.cpy
    Represents a batch job control record.
    """
    job_name: str = Field(..., max_length=8, description="Job name")
    process_date: date = Field(..., description="Process date")
    sequence_no: int = Field(..., ge=0, le=9999, description="Sequence number")
    status: BatchStatus = Field(default=BatchStatus.READY, description="Batch status")
    step_name: str | None = Field(None, max_length=8, description="Current step name")
    program_name: str | None = Field(None, max_length=8, description="Program name")
    start_time: time | None = Field(None, description="Start time")
    end_time: time | None = Field(None, description="End time")
    return_code: int = Field(default=0, description="Return code")
    error_desc: str | None = Field(None, max_length=80, description="Error description")
    restart_count: int = Field(default=0, ge=0, le=99, description="Restart count")
    attempt_timestamp: datetime | None = Field(None, description="Last attempt timestamp")
    complete_timestamp: datetime | None = Field(None, description="Completion timestamp")

    class Config:
        json_encoders = {
            date: lambda v: v.isoformat(),
            time: lambda v: v.isoformat(),
            datetime: lambda v: v.isoformat(),
        }


class CheckpointControl(BaseModel):
    """
    Checkpoint Control - migrated from CKPRST.cpy
    Represents checkpoint/restart control for batch processing.
    """
    program_id: str = Field(..., max_length=8, description="Program identifier")
    run_date: date = Field(..., description="Run date")
    run_time: time = Field(..., description="Run time")
    status: CheckpointStatus = Field(default=CheckpointStatus.INITIAL, description="Checkpoint status")
    records_read: int = Field(default=0, ge=0, description="Records read count")
    records_processed: int = Field(default=0, ge=0, description="Records processed count")
    records_error: int = Field(default=0, ge=0, description="Error records count")
    restart_count: int = Field(default=0, ge=0, le=99, description="Restart count")
    last_key: str | None = Field(None, max_length=50, description="Last processed key")
    last_time: datetime | None = Field(None, description="Last checkpoint time")
    phase: CheckpointPhase = Field(default=CheckpointPhase.INIT, description="Current phase")
    commit_frequency: int = Field(default=1000, ge=1, description="Commit frequency")
    max_errors: int = Field(default=100, ge=1, description="Maximum errors allowed")
    max_restarts: int = Field(default=3, ge=1, description="Maximum restarts allowed")

    class Config:
        json_encoders = {
            date: lambda v: v.isoformat(),
            time: lambda v: v.isoformat(),
            datetime: lambda v: v.isoformat(),
        }


class InquiryRequest(BaseModel):
    """
    Inquiry Request - migrated from INQCOM.cpy
    Represents an online inquiry request.
    """
    function: str = Field(..., max_length=4, description="Function code (MENU, INQP, INQH, EXIT)")
    account_no: str | None = Field(None, max_length=10, description="Account number")

    @field_validator("function")
    @classmethod
    def validate_function(cls, v: str) -> str:
        valid_functions = ["MENU", "INQP", "INQH", "EXIT"]
        v = v.strip().upper()
        if v not in valid_functions:
            raise ValueError(f"Function must be one of: {valid_functions}")
        return v


class InquiryResponse(BaseModel):
    """
    Inquiry Response - migrated from INQCOM.cpy
    Represents an online inquiry response.
    """
    response_code: int = Field(default=0, description="Response code")
    error_message: str | None = Field(None, max_length=80, description="Error message")
    data: dict | None = Field(None, description="Response data")
