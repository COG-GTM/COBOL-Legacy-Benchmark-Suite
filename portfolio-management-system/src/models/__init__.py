"""
Investment Portfolio Management System - Data Models

This package contains SQLAlchemy ORM models and Pydantic data models
migrated from COBOL copybooks and DB2 table definitions.

SQLAlchemy ORM Models (database layer):
    - PortfolioMaster, InvestmentPosition, TransactionHistory
    - PositionHistory, ErrorLog, AuthFile, AuditLog, ReturnCode
    - VSAMPortfolioMaster, VSAMTransactionHistory, VSAMPositionHistory
    - BatchControl

Pydantic Data Models (copybook translations):
    - TransactionRecord (from TRNREC.cpy)
    - PositionRecord (from POSREC.cpy)
    - HistoryRecord (from HISTREC.cpy)
    - InquiryCommunication (from INQCOM.cpy)
    - DB2Request (from DB2REQ.cpy)
    - ErrorHandling (from ERRHND.cpy)
"""

from src.models.orm import (
    Base,
    PortfolioMaster,
    InvestmentPosition,
    TransactionHistoryORM,
    PositionHistory,
    ErrorLog,
    AuthFile,
    AuditLog,
    ReturnCode,
    VSAMPortfolioMaster,
    VSAMTransactionHistory,
    VSAMPositionHistory,
    BatchControl,
)
from src.models.transaction import TransactionRecord, TransactionType, TransactionStatus
from src.models.position import PositionRecord, PositionStatus
from src.models.history import HistoryRecord, HistoryRecordType, HistoryActionCode
from src.models.inquiry import InquiryCommunication, InquiryFunction
from src.models.db2_request import DB2Request, DB2RequestType
from src.models.error import ErrorHandling, ErrorSeverity, ErrorAction

__all__ = [
    # ORM Models
    "Base",
    "PortfolioMaster",
    "InvestmentPosition",
    "TransactionHistoryORM",
    "PositionHistory",
    "ErrorLog",
    "AuthFile",
    "AuditLog",
    "ReturnCode",
    "VSAMPortfolioMaster",
    "VSAMTransactionHistory",
    "VSAMPositionHistory",
    "BatchControl",
    # Pydantic Models (Copybook translations)
    "TransactionRecord",
    "TransactionType",
    "TransactionStatus",
    "PositionRecord",
    "PositionStatus",
    "HistoryRecord",
    "HistoryRecordType",
    "HistoryActionCode",
    "InquiryCommunication",
    "InquiryFunction",
    "DB2Request",
    "DB2RequestType",
    "ErrorHandling",
    "ErrorSeverity",
    "ErrorAction",
]
