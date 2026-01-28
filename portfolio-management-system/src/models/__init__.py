"""
Data models package.
Contains SQLAlchemy ORM models and Pydantic schemas migrated from COBOL copybooks.
"""

from src.models.transaction import (
    TransactionRecord,
    TransactionType,
    TransactionStatus,
    TransactionKey,
    TransactionData,
    TransactionCreate,
    TransactionResponse,
    TransactionSummary,
)

from src.models.position import (
    PositionRecord,
    PositionStatus,
    PositionKey,
    PositionData,
    PositionCreate,
    PositionResponse,
    PositionSummary,
    PortfolioPositions,
)

from src.models.history import (
    HistoryRecord,
    HistoryRecordType,
    HistoryActionCode,
    HistoryKey,
    HistoryData,
    HistoryCreate,
    HistoryResponse,
    HistoryQuery,
    HistoryListResponse,
)

from src.models.inquiry import (
    InquiryFunction,
    InquiryResponseCode,
    InquiryRequest,
    InquiryResponse,
    PositionInquiryRequest,
    HistoryInquiryRequest,
    MenuOption,
    MainMenuResponse,
    SessionContext,
)

from src.models.db2_request import (
    DB2RequestType,
    DB2ResponseCode,
    SQLErrorCategory,
    DB2ErrorInfo,
    DB2Request,
    DB2Response,
    ConnectionPoolStatus,
    RecoveryRequest,
    RecoveryResponse,
    CommitStatistics,
)

from src.models.error import (
    ErrorSeverity,
    ErrorAction,
    ReturnCode,
    ErrorCategory,
    ErrorLogRecord,
    ErrorTrace,
    ErrorHandling,
    ErrorLogCreate,
    ErrorLogResponse,
    ErrorSummary,
    VSAMStatusCode,
)

__all__ = [
    # Transaction models
    "TransactionRecord",
    "TransactionType",
    "TransactionStatus",
    "TransactionKey",
    "TransactionData",
    "TransactionCreate",
    "TransactionResponse",
    "TransactionSummary",
    # Position models
    "PositionRecord",
    "PositionStatus",
    "PositionKey",
    "PositionData",
    "PositionCreate",
    "PositionResponse",
    "PositionSummary",
    "PortfolioPositions",
    # History models
    "HistoryRecord",
    "HistoryRecordType",
    "HistoryActionCode",
    "HistoryKey",
    "HistoryData",
    "HistoryCreate",
    "HistoryResponse",
    "HistoryQuery",
    "HistoryListResponse",
    # Inquiry models
    "InquiryFunction",
    "InquiryResponseCode",
    "InquiryRequest",
    "InquiryResponse",
    "PositionInquiryRequest",
    "HistoryInquiryRequest",
    "MenuOption",
    "MainMenuResponse",
    "SessionContext",
    # DB2 request models
    "DB2RequestType",
    "DB2ResponseCode",
    "SQLErrorCategory",
    "DB2ErrorInfo",
    "DB2Request",
    "DB2Response",
    "ConnectionPoolStatus",
    "RecoveryRequest",
    "RecoveryResponse",
    "CommitStatistics",
    # Error models
    "ErrorSeverity",
    "ErrorAction",
    "ReturnCode",
    "ErrorCategory",
    "ErrorLogRecord",
    "ErrorTrace",
    "ErrorHandling",
    "ErrorLogCreate",
    "ErrorLogResponse",
    "ErrorSummary",
    "VSAMStatusCode",
]
