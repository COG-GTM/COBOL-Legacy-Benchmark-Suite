"""
Constants translated from COBOL copybooks: COMMON.cpy, RTNCODE.cpy, BCHCON.cpy.

Maps COBOL level-88 condition values and return code conventions to Python.
"""

from enum import IntEnum, StrEnum


# ---------------------------------------------------------------------------
# Return codes  (from RTNCODE.cpy / RETHND.cpy)
# ---------------------------------------------------------------------------
class ReturnCode(IntEnum):
    """COBOL return-code convention: 0/4/8/12/16."""

    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    FATAL = 16


# ---------------------------------------------------------------------------
# Portfolio status  (from PORTFLIO.cpy level-88)
# ---------------------------------------------------------------------------
class PortfolioStatus(StrEnum):
    ACTIVE = "A"
    CLOSED = "C"
    SUSPENDED = "S"
    PENDING = "P"


# ---------------------------------------------------------------------------
# Client type  (from PORTFLIO.cpy level-88)
# ---------------------------------------------------------------------------
class ClientType(StrEnum):
    INDIVIDUAL = "I"
    CORPORATE = "C"
    TRUST = "T"


# ---------------------------------------------------------------------------
# Account type  (from COMMON.cpy / PORTKEY.cpy)
# ---------------------------------------------------------------------------
class AccountType(StrEnum):
    INDIVIDUAL = "IN"
    JOINT = "JT"
    CORPORATE = "CO"
    TRUST = "TR"
    RETIREMENT = "RT"


# ---------------------------------------------------------------------------
# Risk level  (from COMMON.cpy)
# ---------------------------------------------------------------------------
class RiskLevel(StrEnum):
    LOW = "L"
    MEDIUM = "M"
    HIGH = "H"
    AGGRESSIVE = "A"


# ---------------------------------------------------------------------------
# Transaction type  (from TRNREC.cpy level-88)
# ---------------------------------------------------------------------------
class TransactionType(StrEnum):
    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


# ---------------------------------------------------------------------------
# Transaction status  (from TRNREC.cpy level-88)
# ---------------------------------------------------------------------------
class TransactionStatus(StrEnum):
    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


# ---------------------------------------------------------------------------
# Position status  (from POSREC.cpy level-88)
# ---------------------------------------------------------------------------
class PositionStatus(StrEnum):
    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


# ---------------------------------------------------------------------------
# Audit type  (from AUDITLOG.cpy level-88)
# ---------------------------------------------------------------------------
class AuditType(StrEnum):
    TRANSACTION = "TRAN"
    USER = "USER"
    SYSTEM = "SYST"


# ---------------------------------------------------------------------------
# Audit action  (from AUDITLOG.cpy level-88)
# ---------------------------------------------------------------------------
class AuditAction(StrEnum):
    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    INQUIRY = "INQUIR"
    LOGIN = "LOGIN"
    LOGOUT = "LOGOUT"


# ---------------------------------------------------------------------------
# Audit status  (from AUDITLOG.cpy level-88)
# ---------------------------------------------------------------------------
class AuditStatus(StrEnum):
    SUCCESS = "SUCC"
    FAILURE = "FAIL"
    WARNING = "WARN"


# ---------------------------------------------------------------------------
# Batch status  (from BCHCTL.cpy / BCHCON.cpy level-88)
# ---------------------------------------------------------------------------
class BatchStatus(StrEnum):
    READY = "R"
    ACTIVE = "A"
    DONE = "D"
    ERROR = "E"
    SUSPENDED = "S"


# ---------------------------------------------------------------------------
# Recovery mode  (from RCVPRC00.cbl)
# ---------------------------------------------------------------------------
class RecoveryMode(StrEnum):
    PROCESS = "P"
    SEQUENCE = "S"
    ALL = "A"


# ---------------------------------------------------------------------------
# Recovery action  (from RCVPRC00.cbl)
# ---------------------------------------------------------------------------
class RecoveryAction(StrEnum):
    RESTART = "R"
    BYPASS = "B"
    TERMINATE = "T"


# ---------------------------------------------------------------------------
# Currency codes  (from COMMON.cpy)
# ---------------------------------------------------------------------------
VALID_CURRENCIES = frozenset({"USD", "EUR", "GBP", "JPY", "CAD"})

# ---------------------------------------------------------------------------
# Error categories  (from ERRHAND.cpy)
# ---------------------------------------------------------------------------
class ErrorCategory(StrEnum):
    DATABASE = "DB"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SECURITY = "SC"
    SYSTEM = "SY"


# ---------------------------------------------------------------------------
# Batch function codes  (from BCHCTL00.cbl linkage)
# ---------------------------------------------------------------------------
class BatchFunction(StrEnum):
    INIT = "INIT"
    CHECK = "CHEK"
    UPDATE = "UPDT"
    TERMINATE = "TERM"


# ---------------------------------------------------------------------------
# Sequencer function codes  (from PRCSEQ00.cbl linkage)
# ---------------------------------------------------------------------------
class SequencerFunction(StrEnum):
    INIT = "INIT"
    NEXT = "NEXT"
    STATUS = "STAT"
    TERMINATE = "TERM"


# ---------------------------------------------------------------------------
# Security request types  (from SECMGR.cbl)
# ---------------------------------------------------------------------------
class SecurityRequestType(StrEnum):
    VALIDATE = "V"
    AUTHORIZE = "A"
    AUDIT = "L"


# ---------------------------------------------------------------------------
# Maintenance functions  (from UTLMNT00.cbl)
# ---------------------------------------------------------------------------
class MaintenanceFunction(StrEnum):
    ARCHIVE = "ARCHIVE"
    CLEANUP = "CLEANUP"
    REORG = "REORG"
    ANALYZE = "ANALYZE"


# ---------------------------------------------------------------------------
# Monitoring resource types  (from UTLMON00.cbl)
# ---------------------------------------------------------------------------
class ResourceType(StrEnum):
    CPU = "CPU"
    MEMORY = "MEMORY"
    DASD = "DASD"
    DB2 = "DB2"


# ---------------------------------------------------------------------------
# Alert levels  (from UTLMON00.cbl)
# ---------------------------------------------------------------------------
class AlertLevel(StrEnum):
    INFO = "INFO"
    WARNING = "WARNING"
    CRITICAL = "CRITICAL"


# ---------------------------------------------------------------------------
# Validation types  (from UTLVAL00.cbl)
# ---------------------------------------------------------------------------
class ValidationType(StrEnum):
    INTEGRITY = "INTEGRITY"
    XREF = "XREF"
    FORMAT = "FORMAT"
    BALANCE = "BALANCE"


# ---------------------------------------------------------------------------
# Commit threshold for batch operations  (from HISTLD00.cbl)
# ---------------------------------------------------------------------------
BATCH_COMMIT_THRESHOLD = 1000

# Maximum batch error count before abort  (from HISTLD00.cbl)
MAX_BATCH_ERRORS = 100

# Maximum connection retries  (from DB2CONN.cbl)
MAX_CONNECTION_RETRIES = 3

# Delete reason codes  (from PORTDEL.cbl)
class DeleteReason(StrEnum):
    CLIENT_REQUEST = "CR"
    ACCOUNT_CLOSURE = "AC"
    REGULATORY = "RG"
    ADMINISTRATIVE = "AD"
