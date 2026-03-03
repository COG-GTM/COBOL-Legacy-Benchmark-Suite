"""
Python Pydantic v2 models translated from COBOL copybook record definitions.

Source repository: COG-GTM/COBOL-Legacy-Benchmark-Suite
Copybook directory: src/copybook/
"""

# Common models
from .audit_log import AuditHeader, AuditKeyInfo, AuditRecord
from .common import (
    AuditFields,
    CommonDatetime,
    CurrencyCodes,
    CurrentDate,
    CurrentTime,
    ErrorHandling,
    ReturnCodes,
    StatusCodes,
    TransactionTypes,
)
from .error_handling import (
    ErrorCategories,
    ErrorMessage,
    ErrorReturnCodes,
    ErrorTimestamp,
    VsamMessages,
    VsamStatuses,
)
from .history import HistoryAudit, HistoryData, HistoryKey, HistoryRecord
from .portfolio import (
    PortfolioAuditInfo,
    PortfolioClientInfo,
    PortfolioFinancialInfo,
    PortfolioInfo,
    PortfolioKey,
    PortfolioRecord,
)
from .portfolio_validation import (
    ValidationConstants,
    ValidationErrorMessages,
    ValidationReturnCodes,
    ValidationWorkAreas,
)
from .position import PositionAudit, PositionData, PositionKey, PositionRecord
from .return_code import (
    ReturnCodeAnalysisData,
    ReturnCodeArea,
    ReturnCodesArea,
    ReturnData,
)
from .return_handling import (
    ErrorInfo,
    ErrorLocation,
    ReturnActions,
    ReturnDetails,
    ReturnHandling,
    ReturnStatus,
    StandardErrorCodes,
    SystemInfo,
)
from .transaction import (
    TransactionAudit,
    TransactionData,
    TransactionKey,
    TransactionRecord,
)

# Sub-package re-exports
from . import batch, db2, online

__all__ = [
    # Common - transaction.py (TRNREC)
    "TransactionAudit",
    "TransactionData",
    "TransactionKey",
    "TransactionRecord",
    # Common - position.py (POSREC)
    "PositionAudit",
    "PositionData",
    "PositionKey",
    "PositionRecord",
    # Common - history.py (HISTREC)
    "HistoryAudit",
    "HistoryData",
    "HistoryKey",
    "HistoryRecord",
    # Common - audit_log.py (AUDITLOG)
    "AuditHeader",
    "AuditKeyInfo",
    "AuditRecord",
    # Common - common.py (COMMON)
    "AuditFields",
    "CommonDatetime",
    "CurrencyCodes",
    "CurrentDate",
    "CurrentTime",
    "ErrorHandling",
    "ReturnCodes",
    "StatusCodes",
    "TransactionTypes",
    # Common - error_handling.py (ERRHAND)
    "ErrorCategories",
    "ErrorMessage",
    "ErrorReturnCodes",
    "ErrorTimestamp",
    "VsamMessages",
    "VsamStatuses",
    # Common - portfolio.py (PORTFLIO)
    "PortfolioAuditInfo",
    "PortfolioClientInfo",
    "PortfolioFinancialInfo",
    "PortfolioInfo",
    "PortfolioKey",
    "PortfolioRecord",
    # Common - portfolio_validation.py (PORTVAL)
    "ValidationConstants",
    "ValidationErrorMessages",
    "ValidationReturnCodes",
    "ValidationWorkAreas",
    # Common - return_handling.py (RETHND)
    "ErrorInfo",
    "ErrorLocation",
    "ReturnActions",
    "ReturnDetails",
    "ReturnHandling",
    "ReturnStatus",
    "StandardErrorCodes",
    "SystemInfo",
    # Common - return_code.py (RTNCODE)
    "ReturnCodeAnalysisData",
    "ReturnCodeArea",
    "ReturnCodesArea",
    "ReturnData",
    # Sub-packages
    "batch",
    "db2",
    "online",
]
