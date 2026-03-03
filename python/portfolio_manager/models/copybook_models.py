"""Pydantic models translated from COBOL copybooks.

Each copybook in src/copybook/ is translated into corresponding
Python dataclasses or Pydantic models preserving the original
data structures, validation rules, and business constants.

Source copybooks:
  - src/copybook/common/POSREC.cpy   -> PositionRecord
  - src/copybook/common/TRNREC.cpy   -> TransactionRecord
  - src/copybook/common/HISTREC.cpy  -> HistoryRecord
  - src/copybook/common/PORTFLIO.cpy -> PortfolioRecord
  - src/copybook/common/AUDITLOG.cpy -> AuditRecord
  - src/copybook/common/COMMON.cpy   -> CommonDateTime, AuditFields, etc.
  - src/copybook/common/ERRHAND.cpy  -> ErrorCategories, ErrorMessage, etc.
  - src/copybook/common/PORTVAL.cpy  -> PortfolioValidation
  - src/copybook/common/RETHND.cpy   -> ReturnHandling
  - src/copybook/common/RTNCODE.cpy  -> ReturnCodeArea
  - src/copybook/batch/BCHCTL.cpy    -> BatchControlRecord
  - src/copybook/batch/BCHCON.cpy    -> BatchControlConstants
  - src/copybook/batch/CKPRST.cpy    -> CheckpointControl, CheckpointRecord
  - src/copybook/batch/PRCSEQ.cpy    -> ProcessSequenceRecord
  - src/copybook/db2/DBTBLS.cpy      -> (covered by SQLAlchemy models)
  - src/copybook/db2/DBPROC.cpy      -> DB2ErrorHandling
  - src/copybook/db2/SQLCA.cpy       -> SQLStatusCodes
  - src/copybook/online/INQCOM.cpy   -> InquiryCommArea
  - src/copybook/online/DB2REQ.cpy   -> DB2RequestArea
  - src/copybook/online/ERRHND.cpy   -> OnlineErrorHandling
"""

from __future__ import annotations

import enum
from datetime import date, datetime, time
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, Field

# ---------------------------------------------------------------------------
# Enumerations (from COBOL 88-level items)
# ---------------------------------------------------------------------------


class TransactionType(str, enum.Enum):
    """Transaction types from TRNREC.cpy / COMMON.cpy."""

    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(str, enum.Enum):
    """Transaction status from TRNREC.cpy."""

    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


class PositionStatus(str, enum.Enum):
    """Position status from POSREC.cpy."""

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


class HistoryRecordType(str, enum.Enum):
    """History record type from HISTREC.cpy."""

    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, enum.Enum):
    """History action code from HISTREC.cpy."""

    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


class StatusCode(str, enum.Enum):
    """General status codes from COMMON.cpy."""

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"
    SUSPENDED = "S"
    FAILED = "F"
    REVERSED = "R"


class CurrencyCode(str, enum.Enum):
    """Currency codes from COMMON.cpy."""

    USD = "USD"
    EUR = "EUR"
    GBP = "GBP"
    JPY = "JPY"
    CAD = "CAD"


class InquiryFunction(str, enum.Enum):
    """Online inquiry functions from INQCOM.cpy."""

    MENU = "MENU"
    PORTFOLIO = "INQP"
    HISTORY = "INQH"
    EXIT = "EXIT"


class ErrorSeverity(str, enum.Enum):
    """Error severity levels from ERRHND.cpy (online)."""

    FATAL = "F"
    WARNING = "W"
    INFO = "I"


class ErrorAction(str, enum.Enum):
    """Error action from ERRHND.cpy (online)."""

    RETURN = "R"
    CONTINUE = "C"
    ABEND = "A"


class ErrorType(str, enum.Enum):
    """Error type from RETHND.cpy."""

    VALIDATION = "V"
    PROCESSING = "P"
    DATABASE = "D"
    FILE = "F"
    SECURITY = "S"


class BatchStatus(str, enum.Enum):
    """Batch control status from BCHCTL.cpy."""

    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


class CheckpointPhase(str, enum.Enum):
    """Checkpoint phase from CKPRST.cpy."""

    INIT = "00"
    READ = "10"
    PROCESS = "20"
    UPDATE = "30"
    TERMINATE = "40"


class CheckpointStatus(str, enum.Enum):
    """Checkpoint status from CKPRST.cpy."""

    INITIAL = "I"
    ACTIVE = "A"
    COMPLETE = "C"
    FAILED = "F"
    RESTARTED = "R"


class CheckpointMode(str, enum.Enum):
    """Checkpoint restart mode from CKPRST.cpy."""

    NORMAL = "N"
    RESTART = "R"
    RECOVER = "C"


class AuditType(str, enum.Enum):
    """Audit record type from AUDITLOG.cpy."""

    TRANSACTION = "TRAN"
    USER_ACTION = "USER"
    SYSTEM_EVENT = "SYST"


class AuditAction(str, enum.Enum):
    """Audit action from AUDITLOG.cpy."""

    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    INQUIRE = "INQUIRE"
    LOGIN = "LOGIN"
    LOGOUT = "LOGOUT"
    STARTUP = "STARTUP"
    SHUTDOWN = "SHUTDOWN"


class AuditStatus(str, enum.Enum):
    """Audit status from AUDITLOG.cpy."""

    SUCCESS = "SUCC"
    FAILURE = "FAIL"
    WARNING = "WARN"


class ProcessType(str, enum.Enum):
    """Process type from PRCSEQ.cpy."""

    INIT = "INI"
    PROCESS = "PRC"
    REPORT = "RPT"
    TERMINATE = "TRM"


class ProcessFrequency(str, enum.Enum):
    """Process frequency from PRCSEQ.cpy."""

    DAILY = "D"
    WEEKLY = "W"
    MONTHLY = "M"


class DependencyType(str, enum.Enum):
    """Dependency type from PRCSEQ.cpy / BCHCON.cpy."""

    HARD = "H"
    SOFT = "S"
    REQUIRED = "R"
    OPTIONAL = "O"
    EXCLUSIVE = "X"


class ReturnCodeStatus(str, enum.Enum):
    """Return code status from RTNCODE.cpy."""

    SUCCESS = "S"
    WARNING = "W"
    ERROR = "E"
    SEVERE = "F"


# ---------------------------------------------------------------------------
# COMMON.cpy  ->  Return codes, status codes, datetime, error handling, audit
# ---------------------------------------------------------------------------


class ReturnCodes(BaseModel):
    """Standard return codes from COMMON.cpy."""

    SUCCESS: int = 0
    WARNING: int = 4
    ERROR: int = 8
    SEVERE: int = 12
    CRITICAL: int = 16


class CommonDateTime(BaseModel):
    """Common date/time fields from COMMON.cpy."""

    current_date: Optional[date] = None
    current_time: Optional[time] = None


class ErrorHandlingArea(BaseModel):
    """Common error handling from COMMON.cpy."""

    error_code: str = Field(default="", max_length=4)
    error_module: str = Field(default="", max_length=8)
    error_routine: str = Field(default="", max_length=8)
    error_message: str = Field(default="", max_length=80)


class AuditFields(BaseModel):
    """Common audit fields from COMMON.cpy."""

    timestamp: Optional[datetime] = None
    user: str = Field(default="", max_length=8)
    terminal: str = Field(default="", max_length=8)
    program: str = Field(default="", max_length=8)


# ---------------------------------------------------------------------------
# POSREC.cpy  ->  PositionRecord
# ---------------------------------------------------------------------------


class PositionRecord(BaseModel):
    """Position record from POSREC.cpy.

    VSAM KSDS key: portfolio_id + position_date + investment_id
    """

    # Key fields
    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    position_date: str = Field(max_length=8, description="Position date YYYYMMDD")
    investment_id: str = Field(max_length=10, description="Investment identifier")

    # Data fields
    quantity: Decimal = Field(
        default=Decimal("0"), decimal_places=4, description="Holding quantity"
    )
    cost_basis: Decimal = Field(
        default=Decimal("0"), decimal_places=2, description="Total cost basis"
    )
    market_value: Decimal = Field(
        default=Decimal("0"), decimal_places=2, description="Current market value"
    )
    currency: CurrencyCode = CurrencyCode.USD
    status: PositionStatus = PositionStatus.ACTIVE

    # Audit fields
    last_maint_date: Optional[datetime] = None
    last_maint_user: str = Field(default="", max_length=8)


# ---------------------------------------------------------------------------
# TRNREC.cpy  ->  TransactionRecord
# ---------------------------------------------------------------------------


class TransactionRecord(BaseModel):
    """Transaction record from TRNREC.cpy.

    Key: trn_date + trn_time + portfolio_id + sequence_no
    """

    # Key fields
    trn_date: str = Field(max_length=8, description="Transaction date YYYYMMDD")
    trn_time: str = Field(max_length=6, description="Transaction time HHMMSS")
    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    sequence_no: str = Field(max_length=6, description="Sequence number")

    # Data fields
    investment_id: str = Field(max_length=10, description="Investment identifier")
    transaction_type: TransactionType = TransactionType.BUY
    quantity: Decimal = Field(default=Decimal("0"), decimal_places=4)
    price: Decimal = Field(default=Decimal("0"), decimal_places=4)
    amount: Decimal = Field(default=Decimal("0"), decimal_places=2)
    currency: CurrencyCode = CurrencyCode.USD
    status: TransactionStatus = TransactionStatus.PENDING

    # Audit
    process_date: Optional[datetime] = None
    process_user: str = Field(default="", max_length=8)


# ---------------------------------------------------------------------------
# HISTREC.cpy  ->  HistoryRecord
# ---------------------------------------------------------------------------


class HistoryRecord(BaseModel):
    """History record from HISTREC.cpy.

    Key: portfolio_id + hist_date + hist_time + seq_no
    """

    # Key fields
    portfolio_id: str = Field(max_length=8)
    hist_date: str = Field(max_length=8, description="History date YYYYMMDD")
    hist_time: str = Field(max_length=6, description="History time HHMMSS")
    seq_no: str = Field(max_length=4, description="Sequence number")

    # Data fields
    record_type: HistoryRecordType = HistoryRecordType.TRANSACTION
    action_code: HistoryActionCode = HistoryActionCode.ADD
    before_image: str = Field(default="", max_length=400)
    after_image: str = Field(default="", max_length=400)
    reason_code: str = Field(default="", max_length=4)

    # Audit
    process_date: Optional[datetime] = None
    process_user: str = Field(default="", max_length=8)


# ---------------------------------------------------------------------------
# PORTFLIO.cpy  ->  PortfolioRecord
# ---------------------------------------------------------------------------


class PortfolioRecord(BaseModel):
    """Portfolio master record from PORTFLIO.cpy.

    Key: port_id + account_no
    """

    # Key
    port_id: str = Field(max_length=8)
    account_no: str = Field(max_length=10)

    # Client info
    client_name: str = Field(default="", max_length=30)
    client_type: str = Field(
        default="I", max_length=1, description="I=Individual, C=Corporate, T=Trust"
    )

    # Portfolio info
    create_date: Optional[int] = None  # YYYYMMDD numeric
    last_maint: Optional[int] = None
    status: StatusCode = StatusCode.ACTIVE

    # Financial info
    total_value: Decimal = Field(default=Decimal("0"), decimal_places=2)
    cash_balance: Decimal = Field(default=Decimal("0"), decimal_places=2)

    # Audit
    last_user: str = Field(default="", max_length=8)
    last_trans: Optional[int] = None


# ---------------------------------------------------------------------------
# AUDITLOG.cpy  ->  AuditRecord
# ---------------------------------------------------------------------------


class AuditRecord(BaseModel):
    """Audit trail record from AUDITLOG.cpy."""

    # Header
    timestamp: Optional[datetime] = None
    system_id: str = Field(default="", max_length=8)
    user_id: str = Field(default="", max_length=8)
    program: str = Field(default="", max_length=8)
    terminal: str = Field(default="", max_length=8)

    # Type & action
    audit_type: AuditType = AuditType.SYSTEM_EVENT
    action: AuditAction = AuditAction.INQUIRE
    status: AuditStatus = AuditStatus.SUCCESS

    # Key info
    portfolio_id: str = Field(default="", max_length=8)
    account_no: str = Field(default="", max_length=10)

    # Images
    before_image: str = Field(default="", max_length=100)
    after_image: str = Field(default="", max_length=100)
    message: str = Field(default="", max_length=100)


# ---------------------------------------------------------------------------
# ERRHAND.cpy  ->  ErrorCategories, ErrorReturnCodes, ErrorMessage, etc.
# ---------------------------------------------------------------------------


class ErrorCategories(BaseModel):
    """Error categories from ERRHAND.cpy."""

    VSAM: str = "VS"
    VALIDATION: str = "VL"
    PROCESSING: str = "PR"
    SYSTEM: str = "SY"


class ErrorReturnCodes(BaseModel):
    """Standard return codes from ERRHAND.cpy."""

    SUCCESS: int = 0
    WARNING: int = 4
    ERROR: int = 8
    SEVERE: int = 12
    TERMINAL: int = 16


class ErrorMessage(BaseModel):
    """Error message structure from ERRHAND.cpy."""

    err_date: str = Field(default="", max_length=10)
    err_time: str = Field(default="", max_length=8)
    program: str = Field(default="", max_length=8)
    category: str = Field(default="", max_length=2)
    code: str = Field(default="", max_length=4)
    severity: int = 0
    text: str = Field(default="", max_length=80)
    details: str = Field(default="", max_length=256)


class VSAMStatusCodes(BaseModel):
    """VSAM file status codes from ERRHAND.cpy."""

    SUCCESS: str = "00"
    DUPKEY: str = "22"
    NOTFND: str = "23"
    EOF: str = "10"


# ---------------------------------------------------------------------------
# PORTVAL.cpy  ->  PortfolioValidation
# ---------------------------------------------------------------------------


class PortfolioValidation(BaseModel):
    """Portfolio validation rules from PORTVAL.cpy."""

    MIN_AMOUNT: Decimal = Decimal("-9999999999999.99")
    MAX_AMOUNT: Decimal = Decimal("9999999999999.99")
    ID_PREFIX: str = "PORT"

    # Validation return codes
    VAL_SUCCESS: int = 0
    VAL_INVALID_ID: int = 1
    VAL_INVALID_ACCT: int = 2
    VAL_INVALID_TYPE: int = 3
    VAL_INVALID_AMT: int = 4


# ---------------------------------------------------------------------------
# RETHND.cpy  ->  ReturnHandling
# ---------------------------------------------------------------------------


class ReturnHandling(BaseModel):
    """Return code handling from RETHND.cpy."""

    return_code: int = 0
    reason_code: int = 0
    module_id: str = Field(default="", max_length=8)
    function_id: str = Field(default="", max_length=8)

    # Error location
    program_name: str = Field(default="", max_length=8)
    paragraph_name: str = Field(default="", max_length=8)
    error_routine: str = Field(default="", max_length=8)

    # Error info
    error_type: Optional[ErrorType] = None
    error_code: str = Field(default="", max_length=4)
    error_text: str = Field(default="", max_length=80)

    # System info
    system_code: str = Field(default="", max_length=4)
    system_msg: str = Field(default="", max_length=80)

    # Action
    action_flag: str = Field(default="C", max_length=1)
    retry_count: int = 0
    max_retries: int = 3


# Standard error codes from RETHND.cpy
STANDARD_ERROR_CODES = {
    "E001": "Invalid data",
    "E002": "Not found",
    "E003": "Duplicate",
    "E004": "File error",
    "E005": "Database error",
    "E006": "Security error",
    "E007": "Processing error",
    "E008": "Validation error",
    "E009": "Version error",
    "E010": "Timeout error",
}


# ---------------------------------------------------------------------------
# RTNCODE.cpy  ->  ReturnCodeArea
# ---------------------------------------------------------------------------


class ReturnCodeArea(BaseModel):
    """Return code management from RTNCODE.cpy."""

    request_type: str = Field(
        default="I", max_length=1, description="I=Init, S=Set, G=Get, L=Log, A=Analyze"
    )
    program_id: str = Field(default="", max_length=8)

    current_code: int = 0
    highest_code: int = 0
    new_code: int = 0
    status: ReturnCodeStatus = ReturnCodeStatus.SUCCESS
    message: str = Field(default="", max_length=80)
    response_code: int = 0

    # Analysis data
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    total_codes: int = 0
    max_code: int = 0
    min_code: int = 0

    # Return data
    return_value: int = 0
    highest_return: int = 0
    return_status: str = Field(default="", max_length=1)


# ---------------------------------------------------------------------------
# BCHCTL.cpy  ->  BatchControlRecord
# ---------------------------------------------------------------------------


class PrerequisiteJob(BaseModel):
    """Prerequisite job entry from BCHCTL.cpy."""

    prereq_name: str = Field(max_length=8)
    prereq_seq: int = 0
    prereq_rc: int = 0


class BatchControlRecord(BaseModel):
    """Batch control record from BCHCTL.cpy.

    Key: job_name + process_date + sequence_no
    """

    # Key
    job_name: str = Field(max_length=8)
    process_date: str = Field(max_length=8, description="YYYYMMDD")
    sequence_no: int = 0

    # Status
    status: BatchStatus = BatchStatus.READY

    # Process control
    step_name: str = Field(default="", max_length=8)
    program_name: str = Field(default="", max_length=8)
    start_time: str = Field(default="", max_length=8)
    end_time: str = Field(default="", max_length=8)

    # Dependencies (max 10 prereqs)
    prereq_count: int = 0
    prereq_jobs: list[PrerequisiteJob] = Field(default_factory=list)

    # Return info
    return_code: int = 0
    error_desc: str = Field(default="", max_length=80)

    # Statistics
    restart_count: int = 0
    attempt_ts: Optional[datetime] = None
    complete_ts: Optional[datetime] = None


# ---------------------------------------------------------------------------
# BCHCON.cpy  ->  BatchControlConstants
# ---------------------------------------------------------------------------


class BatchControlConstants(BaseModel):
    """Batch control constants from BCHCON.cpy."""

    # Return code thresholds
    RC_SUCCESS: int = 0
    RC_WARNING: int = 4
    RC_ERROR: int = 8
    RC_SEVERE: int = 12
    RC_CRITICAL: int = 16

    # Process control values
    MAX_PREREQ: int = 10
    MAX_RESTARTS: int = 3
    WAIT_INTERVAL: int = 300  # seconds
    MAX_WAIT_TIME: int = 3600  # seconds

    # Process types
    TYPE_INITIAL: str = "INI"
    TYPE_UPDATE: str = "UPD"
    TYPE_REPORT: str = "RPT"
    TYPE_CLEANUP: str = "CLN"

    # Standard messages
    MSG_STARTING: str = "Process starting..."
    MSG_COMPLETE: str = "Process completed successfully"
    MSG_FAILED: str = "Process failed - check errors"
    MSG_WAITING: str = "Waiting for prerequisites"


# ---------------------------------------------------------------------------
# CKPRST.cpy  ->  CheckpointControl, CheckpointRecord
# ---------------------------------------------------------------------------


class FileStatus(BaseModel):
    """File status entry within checkpoint (CKPRST.cpy)."""

    file_name: str = Field(default="", max_length=8)
    file_pos: str = Field(default="", max_length=50)
    file_status: str = Field(default="00", max_length=2)


class CheckpointControl(BaseModel):
    """Checkpoint/restart control structure from CKPRST.cpy."""

    # Header
    program_id: str = Field(max_length=8)
    run_date: str = Field(default="", max_length=8)
    run_time: str = Field(default="", max_length=6)
    status: CheckpointStatus = CheckpointStatus.INITIAL

    # Counters
    records_read: int = 0
    records_processed: int = 0
    records_error: int = 0
    restart_count: int = 0

    # Position
    last_key: str = Field(default="", max_length=50)
    last_time: Optional[datetime] = None
    phase: CheckpointPhase = CheckpointPhase.INIT

    # Resources (up to 5 files)
    file_statuses: list[FileStatus] = Field(default_factory=list)

    # Control info
    commit_freq: int = 1000
    max_errors: int = 100
    max_restarts: int = 3
    restart_mode: CheckpointMode = CheckpointMode.NORMAL


class CheckpointRecord(BaseModel):
    """Checkpoint VSAM file record from CKPRST.cpy."""

    program_id: str = Field(max_length=8)
    run_date: str = Field(max_length=8)
    data: str = Field(default="", max_length=400)


# ---------------------------------------------------------------------------
# PRCSEQ.cpy  ->  ProcessSequenceRecord
# ---------------------------------------------------------------------------


class DependencyEntry(BaseModel):
    """Dependency entry from PRCSEQ.cpy."""

    dep_id: str = Field(max_length=8)
    dep_type: DependencyType = DependencyType.HARD
    dep_rc: int = 0


class ProcessSequenceRecord(BaseModel):
    """Process sequence definition from PRCSEQ.cpy."""

    # Key
    process_id: str = Field(max_length=8)
    version: int = 1

    # Data
    description: str = Field(default="", max_length=30)
    process_type: ProcessType = ProcessType.PROCESS

    # Timing
    frequency: ProcessFrequency = ProcessFrequency.DAILY
    start_time: int = 0  # HHMM
    max_time: int = 0  # HHMM

    # Dependencies (max 10)
    dep_count: int = 0
    dependencies: list[DependencyEntry] = Field(default_factory=list)

    # Control
    program: str = Field(default="", max_length=8)
    parm: str = Field(default="", max_length=50)
    max_rc: int = 0
    restartable: bool = True

    # Schedule
    active_days: str = Field(default="YYYYYNN", max_length=7)
    month_end: bool = False
    holiday_run: bool = False

    # Recovery
    recovery_pgm: str = Field(default="", max_length=8)
    recovery_parm: str = Field(default="", max_length=50)
    error_limit: int = 0

    # Audit
    create_date: str = Field(default="", max_length=10)
    create_user: str = Field(default="", max_length=8)
    update_date: str = Field(default="", max_length=10)
    update_user: str = Field(default="", max_length=8)


# Standard process sequences from PRCSEQ.cpy
STANDARD_SEQUENCES = {
    "start_of_day": ["INITDAY", "CKPCLR", "DATEVAL"],
    "main_process": ["TRNVAL00", "POSUPD00", "HISTLD00"],
    "end_of_day": ["RPTGEN00", "BCKLOD00", "ENDDAY"],
}


# ---------------------------------------------------------------------------
# DBPROC.cpy  ->  DB2ErrorHandling
# ---------------------------------------------------------------------------


class DB2ErrorHandling(BaseModel):
    """DB2 standard procedures / error handling from DBPROC.cpy."""

    sqlcode_text: str = Field(default="", max_length=6)
    state: str = Field(default="", max_length=5)
    error_text: str = Field(default="", max_length=70)
    save_status: str = Field(default="", max_length=5)
    retry_count: int = 0
    max_retries: int = 3
    retry_wait: int = 100  # milliseconds


# SQL status codes from SQLCA.cpy
SQL_STATUS_CODES = {
    "SUCCESS": "00000",
    "NOT_FOUND": "02000",
    "DUP_KEY": "23505",
    "DEADLOCK": "40001",
    "TIMEOUT": "40003",
    "CONNECTION_ERROR": "08001",
    "DB_ERROR": "58004",
}


# ---------------------------------------------------------------------------
# INQCOM.cpy  ->  InquiryCommArea
# ---------------------------------------------------------------------------


class InquiryCommArea(BaseModel):
    """Online inquiry communication area from INQCOM.cpy."""

    function: InquiryFunction = InquiryFunction.MENU
    account_no: str = Field(default="", max_length=10)
    response_code: int = 0
    error_msg: str = Field(default="", max_length=80)


# ---------------------------------------------------------------------------
# DB2REQ.cpy  ->  DB2RequestArea
# ---------------------------------------------------------------------------


class DB2RequestArea(BaseModel):
    """DB2 request area from DB2REQ.cpy."""

    request_type: str = Field(
        default="C", max_length=1, description="C=Connect, D=Disconnect, S=Status"
    )
    response_code: int = 0
    connection_token: str = Field(default="", max_length=16)
    sqlcode: int = 0
    error_msg: str = Field(default="", max_length=80)


# ---------------------------------------------------------------------------
# ERRHND.cpy (online)  ->  OnlineErrorHandling
# ---------------------------------------------------------------------------


class OnlineErrorHandling(BaseModel):
    """Online error handling from src/copybook/online/ERRHND.cpy."""

    program: str = Field(default="", max_length=8)
    paragraph: str = Field(default="", max_length=30)
    sqlcode: int = 0
    cics_resp: int = 0
    cics_resp2: int = 0
    severity: ErrorSeverity = ErrorSeverity.INFO
    message: str = Field(default="", max_length=80)
    action: ErrorAction = ErrorAction.CONTINUE
    trace_id: str = Field(default="", max_length=16)
    timestamp: Optional[datetime] = None
