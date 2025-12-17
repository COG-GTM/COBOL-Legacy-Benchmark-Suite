"""
Data models for the COBOL to Python migration.

This module contains Python dataclasses and SQLAlchemy models that replace
the COBOL copybook data structures and VSAM/DB2 storage.
"""

from migration.python.models.transaction import (
    TransactionType,
    TransactionStatus,
    TransactionKey,
    TransactionData,
    TransactionAudit,
    TransactionRecord,
    ValidatedTransaction,
    ValidationError,
)

from migration.python.models.position import (
    PositionStatus,
    PositionKey,
    PositionData,
    PositionAudit,
    PositionRecord,
)

from migration.python.models.history import (
    HistoryRecordType,
    HistoryActionCode,
    HistoryKey,
    HistoryData,
    HistoryAudit,
    HistoryRecord,
)

from migration.python.models.batch_control import (
    BatchStatus,
    BatchPrerequisite,
    BatchControlKey,
    BatchProcessControl,
    BatchReturnInfo,
    BatchStatistics,
    BatchControlRecord,
)

__all__ = [
    # Transaction models
    'TransactionType',
    'TransactionStatus',
    'TransactionKey',
    'TransactionData',
    'TransactionAudit',
    'TransactionRecord',
    'ValidatedTransaction',
    'ValidationError',
    # Position models
    'PositionStatus',
    'PositionKey',
    'PositionData',
    'PositionAudit',
    'PositionRecord',
    # History models
    'HistoryRecordType',
    'HistoryActionCode',
    'HistoryKey',
    'HistoryData',
    'HistoryAudit',
    'HistoryRecord',
    # Batch control models
    'BatchStatus',
    'BatchPrerequisite',
    'BatchControlKey',
    'BatchProcessControl',
    'BatchReturnInfo',
    'BatchStatistics',
    'BatchControlRecord',
]
