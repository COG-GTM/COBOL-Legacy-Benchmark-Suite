"""
Constants translated from COBOL copybooks: COMMON.cpy, ERRHAND.cpy, RTNCODE.cpy, BCHCON.cpy, PORTVAL.cpy.

Maps COBOL level-88 condition values, return codes, and business constants to Python.
"""

from decimal import Decimal
from enum import IntEnum, StrEnum


# ---------------------------------------------------------------------------
# Return Codes  (from COMMON.cpy / ERRHAND.cpy / RTNCODE.cpy)
# COBOL convention: 0=OK, 4=warning, 8=error, 12=severe, 16=fatal
# ---------------------------------------------------------------------------
class ReturnCode(IntEnum):
    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    CRITICAL = 16


# ---------------------------------------------------------------------------
# Portfolio Status  (from PORTFLIO.cpy level-88)
# ---------------------------------------------------------------------------
class PortfolioStatus(StrEnum):
    ACTIVE = "A"
    CLOSED = "C"
    SUSPENDED = "S"
    PENDING = "P"


# ---------------------------------------------------------------------------
# Client Type  (from PORTFLIO.cpy level-88)
# ---------------------------------------------------------------------------
class ClientType(StrEnum):
    INDIVIDUAL = "I"
    CORPORATE = "C"
    TRUST = "T"


# ---------------------------------------------------------------------------
# Transaction Type  (from TRNREC.cpy / COMMON.cpy level-88)
# ---------------------------------------------------------------------------
class TransactionType(StrEnum):
    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


# ---------------------------------------------------------------------------
# Transaction Status  (from TRNREC.cpy level-88)
# ---------------------------------------------------------------------------
class TransactionStatus(StrEnum):
    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


# ---------------------------------------------------------------------------
# Position Status  (from POSREC.cpy level-88)
# ---------------------------------------------------------------------------
class PositionStatus(StrEnum):
    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


# ---------------------------------------------------------------------------
# Account Type  (from db2-definitions.sql)
# ---------------------------------------------------------------------------
class AccountType(StrEnum):
    INDIVIDUAL = "IN"
    JOINT = "JT"
    CORPORATE = "CO"
    TRUST = "TR"
    RETIREMENT = "RT"


# ---------------------------------------------------------------------------
# Risk Level  (from db2-definitions.sql)
# ---------------------------------------------------------------------------
class RiskLevel(StrEnum):
    LOW = "L"
    MEDIUM = "M"
    HIGH = "H"


# ---------------------------------------------------------------------------
# Audit Type  (from AUDITLOG.cpy level-88)
# ---------------------------------------------------------------------------
class AuditType(StrEnum):
    TRANSACTION = "TRAN"
    USER_ACTION = "USER"
    SYSTEM_EVENT = "SYST"


# ---------------------------------------------------------------------------
# Audit Action  (from AUDITLOG.cpy level-88)
# ---------------------------------------------------------------------------
class AuditAction(StrEnum):
    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    INQUIRE = "INQUIRE"
    LOGIN = "LOGIN"
    LOGOUT = "LOGOUT"
    STARTUP = "STARTUP"
    SHUTDOWN = "SHUTDOWN"


# ---------------------------------------------------------------------------
# Audit Status  (from AUDITLOG.cpy level-88)
# ---------------------------------------------------------------------------
class AuditStatus(StrEnum):
    SUCCESS = "SUCC"
    FAILURE = "FAIL"
    WARNING = "WARN"


# ---------------------------------------------------------------------------
# Batch Control Status  (from BCHCTL.cpy / BCHCON.cpy level-88)
# ---------------------------------------------------------------------------
class BatchStatus(StrEnum):
    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


# ---------------------------------------------------------------------------
# Batch Process Type  (from BCHCON.cpy)
# ---------------------------------------------------------------------------
class BatchProcessType(StrEnum):
    INITIAL = "INI"
    UPDATE = "UPD"
    REPORT = "RPT"
    CLEANUP = "CLN"


# ---------------------------------------------------------------------------
# Batch Dependency Type  (from BCHCON.cpy / PRCSEQ.cpy)
# ---------------------------------------------------------------------------
class DependencyType(StrEnum):
    REQUIRED = "R"
    OPTIONAL = "O"
    EXCLUSIVE = "X"
    HARD = "H"
    SOFT = "S"


# ---------------------------------------------------------------------------
# Process Frequency  (from PRCSEQ.cpy)
# ---------------------------------------------------------------------------
class ProcessFrequency(StrEnum):
    DAILY = "D"
    WEEKLY = "W"
    MONTHLY = "M"


# ---------------------------------------------------------------------------
# Checkpoint Status  (from CKPRST.cpy level-88)
# ---------------------------------------------------------------------------
class CheckpointStatus(StrEnum):
    INITIAL = "I"
    ACTIVE = "A"
    COMPLETE = "C"
    FAILED = "F"
    RESTARTED = "R"


# ---------------------------------------------------------------------------
# Checkpoint Phase  (from CKPRST.cpy level-88)
# ---------------------------------------------------------------------------
class CheckpointPhase(StrEnum):
    INIT = "00"
    READ = "10"
    PROCESS = "20"
    UPDATE = "30"
    TERMINATE = "40"


# ---------------------------------------------------------------------------
# History Record Type  (from HISTREC.cpy level-88)
# ---------------------------------------------------------------------------
class HistoryRecordType(StrEnum):
    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


# ---------------------------------------------------------------------------
# History Action Code  (from HISTREC.cpy level-88)
# ---------------------------------------------------------------------------
class HistoryActionCode(StrEnum):
    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


# ---------------------------------------------------------------------------
# Error Categories  (from ERRHAND.cpy)
# ---------------------------------------------------------------------------
class ErrorCategory(StrEnum):
    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


# ---------------------------------------------------------------------------
# Standard Error Codes  (from RETHND.cpy)
# ---------------------------------------------------------------------------
class StandardErrorCode(StrEnum):
    INVALID_DATA = "E001"
    NOT_FOUND = "E002"
    DUPLICATE = "E003"
    FILE_ERROR = "E004"
    DB_ERROR = "E005"
    SECURITY = "E006"
    PROCESSING = "E007"
    VALIDATION = "E008"
    VERSION = "E009"
    TIMEOUT = "E010"


# ---------------------------------------------------------------------------
# Investment Type  (from PORTVALD.cbl validation)
# ---------------------------------------------------------------------------
class InvestmentType(StrEnum):
    STOCK = "STK"
    BOND = "BND"
    MONEY_MARKET = "MMF"
    ETF = "ETF"


# ---------------------------------------------------------------------------
# Currency Codes  (from COMMON.cpy)
# ---------------------------------------------------------------------------
class CurrencyCode(StrEnum):
    USD = "USD"
    EUR = "EUR"
    GBP = "GBP"
    JPY = "JPY"
    CAD = "CAD"


# ---------------------------------------------------------------------------
# Deletion Reason Codes  (from PORTDEL.cbl)
# ---------------------------------------------------------------------------
class DeletionReason(StrEnum):
    CLOSED = "01"
    TRANSFERRED = "02"
    REQUESTED = "03"


# ---------------------------------------------------------------------------
# DB2 SQL Error Categories  (from DB2ERR.cbl)
# ---------------------------------------------------------------------------
SQLCODE_DEADLOCK = -911
SQLCODE_TIMEOUT = -913
SQLCODE_CONNECTION_ERROR = -30081
SQLCODE_DUPLICATE_KEY = -803
SQLCODE_NOT_FOUND = 100


# ---------------------------------------------------------------------------
# Validation Constants  (from PORTVAL.cpy)
# ---------------------------------------------------------------------------
PORTFOLIO_ID_PREFIX = "PORT"
MIN_AMOUNT = Decimal("-9999999999999.99")
MAX_AMOUNT = Decimal("9999999999999.99")


# ---------------------------------------------------------------------------
# Batch Control Constants  (from BCHCON.cpy)
# ---------------------------------------------------------------------------
MAX_PREREQUISITES = 10
MAX_RESTARTS = 3
WAIT_INTERVAL_SECONDS = 300
MAX_WAIT_TIME_SECONDS = 3600
COMMIT_THRESHOLD = 1000
MAX_BATCH_ERRORS = 100


# ---------------------------------------------------------------------------
# DB2 Connection Constants  (from DB2CONN.cbl / DB2ONLN.cbl)
# ---------------------------------------------------------------------------
MAX_DB2_RETRIES = 3
MAX_DB2_CONNECTIONS = 100
RETRY_INTERVAL_SECONDS = 2


# ---------------------------------------------------------------------------
# Standard Batch Sequences  (from PRCSEQ.cpy)
# ---------------------------------------------------------------------------
SEQUENCE_START_OF_DAY = ["INITDAY", "CKPCLR", "DATEVAL"]
SEQUENCE_MAIN_PROCESS = ["TRNVAL00", "POSUPD00", "HISTLD00"]
SEQUENCE_END_OF_DAY = ["RPTGEN00", "BCKLOD00", "ENDDAY"]


# ---------------------------------------------------------------------------
# Maintenance Functions  (from UTLMNT00.cbl)
# ---------------------------------------------------------------------------
class MaintenanceFunction(StrEnum):
    ARCHIVE = "ARCHIVE"
    CLEANUP = "CLEANUP"
    REORG = "REORG"
    ANALYZE = "ANALYZE"


# ---------------------------------------------------------------------------
# Monitor Resource Types  (from UTLMON00.cbl)
# ---------------------------------------------------------------------------
class MonitorResourceType(StrEnum):
    CPU = "CPU"
    MEMORY = "MEMORY"
    DASD = "DASD"
    DB2 = "DB2"


# ---------------------------------------------------------------------------
# Monitor Alert Levels  (from UTLMON00.cbl)
# ---------------------------------------------------------------------------
class AlertLevel(StrEnum):
    INFO = "INFO"
    WARNING = "WARNING"
    CRITICAL = "CRITICAL"


# ---------------------------------------------------------------------------
# Validation Types  (from UTLVAL00.cbl)
# ---------------------------------------------------------------------------
class ValidationType(StrEnum):
    INTEGRITY = "INTEGRITY"
    XREF = "XREF"
    FORMAT = "FORMAT"
    BALANCE = "BALANCE"


# ---------------------------------------------------------------------------
# DB2 Error Severity Mapping  (from DB2ERR.cbl 1100-SET-SEVERITY)
# ---------------------------------------------------------------------------
DB2_SEVERITY_MAP: dict[int, int] = {
    SQLCODE_DEADLOCK: 2,
    SQLCODE_TIMEOUT: 2,
    SQLCODE_CONNECTION_ERROR: 4,
    SQLCODE_DUPLICATE_KEY: 1,
    SQLCODE_NOT_FOUND: 1,
}

# Whether a given SQLCODE should trigger retry
DB2_RETRYABLE_CODES: set[int] = {SQLCODE_DEADLOCK, SQLCODE_TIMEOUT}
