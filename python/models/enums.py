"""Enums and constants translated from src/copybook/common/COMMON.cpy."""

from enum import Enum, IntEnum


class ReturnCode(IntEnum):
    """Standard return codes from COBOL RETURN-CODES / ERR-RETURN-CODES."""

    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    CRITICAL = 16


class PortfolioStatus(str, Enum):
    """Portfolio status codes from COMMON.cpy STATUS-CODES and PORTFLIO.cpy."""

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"
    SUSPENDED = "S"


class TransactionType(str, Enum):
    """Transaction type codes from COMMON.cpy TRANSACTION-TYPES."""

    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(str, Enum):
    """Transaction status codes from TRNREC.cpy TRN-STATUS."""

    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


class PositionStatus(str, Enum):
    """Position status codes from POSREC.cpy POS-STATUS."""

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


class ClientType(str, Enum):
    """Client type codes from PORTFLIO.cpy PORT-CLIENT-TYPE."""

    INDIVIDUAL = "I"
    CORPORATE = "C"
    TRUST = "T"


class Currency(str, Enum):
    """Currency codes from COMMON.cpy CURRENCY-CODES."""

    USD = "USD"
    EUR = "EUR"
    GBP = "GBP"
    JPY = "JPY"
    CAD = "CAD"


class AuditType(str, Enum):
    """Audit type codes from AUDITLOG.cpy AUD-TYPE."""

    TRANSACTION = "TRAN"
    USER_ACTION = "USER"
    SYSTEM_EVENT = "SYST"


class AuditAction(str, Enum):
    """Audit action codes from AUDITLOG.cpy AUD-ACTION."""

    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    INQUIRE = "INQUIRE"
    LOGIN = "LOGIN"
    LOGOUT = "LOGOUT"
    STARTUP = "STARTUP"
    SHUTDOWN = "SHUTDOWN"


class AuditStatus(str, Enum):
    """Audit status codes from AUDITLOG.cpy AUD-STATUS."""

    SUCCESS = "SUCC"
    FAILURE = "FAIL"
    WARNING = "WARN"


class InvestmentType(str, Enum):
    """Investment type codes used across the system."""

    STOCK = "STK"
    BOND = "BND"
    MONEY_MARKET = "MMF"
    ETF = "ETF"


class DeleteReason(str, Enum):
    """Reason codes for portfolio deletion."""

    CLOSED = "01"
    TRANSFERRED = "02"
    REQUESTED = "03"


class ErrorCategory(str, Enum):
    """Error categories from ERRHAND.cpy ERR-CATEGORIES."""

    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


class BatchStatus(str, Enum):
    """Batch process status from BCHCTL.cpy / BCHCON.cpy."""

    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


class CheckpointStatus(str, Enum):
    """Checkpoint status from CKPRST.cpy CK-STATUS."""

    INITIAL = "I"
    ACTIVE = "A"
    COMPLETE = "C"
    FAILED = "F"
    RESTARTED = "R"


class CheckpointPhase(str, Enum):
    """Checkpoint processing phase from CKPRST.cpy CK-PHASE."""

    INIT = "00"
    READ = "10"
    PROCESS = "20"
    UPDATE = "30"
    TERMINATE = "40"


class ProcessType(str, Enum):
    """Batch process types from BCHCON.cpy BCT-PROC-TYPES."""

    INITIAL = "INI"
    UPDATE = "UPD"
    REPORT = "RPT"
    CLEANUP = "CLN"


class DependencyType(str, Enum):
    """Dependency types from BCHCON.cpy BCT-DEP-TYPES."""

    REQUIRED = "R"
    OPTIONAL = "O"
    EXCLUSIVE = "X"


class PrcseqDependencyType(str, Enum):
    """Dependency types from PRCSEQ.cpy PSR-DEP-TYPE."""

    HARD = "H"
    SOFT = "S"


class ProcessSequenceType(str, Enum):
    """Process sequence types from PRCSEQ.cpy PSR-TYPE."""

    INIT = "INI"
    PROCESS = "PRC"
    REPORT = "RPT"
    TERMINATE = "TRM"


class ScheduleFrequency(str, Enum):
    """Schedule frequency from PRCSEQ.cpy PSR-FREQ."""

    DAILY = "D"
    WEEKLY = "W"
    MONTHLY = "M"


class HistoryRecordType(str, Enum):
    """History record types from HISTREC.cpy HIST-RECORD-TYPE."""

    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    """History action codes from HISTREC.cpy HIST-ACTION-CODE."""

    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


class RestartMode(str, Enum):
    """Checkpoint restart mode from CKPRST.cpy CK-RESTART-MODE."""

    NORMAL = "N"
    RESTART = "R"
    RECOVER = "C"


class ErrorLogType(str, Enum):
    """Error log types from DBTBLS.cpy EL-ERROR-TYPE."""

    SYSTEM = "S"
    APPLICATION = "A"
    DATA = "D"


class ErrorLogSeverity(IntEnum):
    """Error log severity from DBTBLS.cpy EL-ERROR-SEVERITY."""

    INFO = 1
    WARNING = 2
    ERROR = 3
    SEVERE = 4
