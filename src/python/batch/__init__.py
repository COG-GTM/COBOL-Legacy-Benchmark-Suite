"""
Investment Portfolio Management System - Python Batch Processing Layer

This module provides Python implementations of the COBOL batch processing programs:
- TRNVAL00: Transaction Validation
- POSUPD00: Position Updates
- HISTLD00: History Loading

The implementation includes:
- Checkpoint/restart framework for reliability
- Apache Airflow DAG for workflow orchestration
- PostgreSQL database integration
"""

__version__ = "1.0.0"

from .models import (
    TransactionRecord,
    PositionRecord,
    HistoryRecord,
    BatchControlRecord,
    CheckpointControl,
)
from .processors import (
    TransactionValidator,
    PositionUpdater,
    HistoryLoader,
    BatchProcessor,
    ProcessingResult,
)
from .checkpoint import CheckpointManager, CheckpointStorage
from .database import DatabaseConnection, PositionHistory, ErrorLog

__all__ = [
    "TransactionRecord",
    "PositionRecord",
    "HistoryRecord",
    "BatchControlRecord",
    "CheckpointControl",
    "TransactionValidator",
    "PositionUpdater",
    "HistoryLoader",
    "BatchProcessor",
    "ProcessingResult",
    "CheckpointManager",
    "CheckpointStorage",
    "DatabaseConnection",
    "PositionHistory",
    "ErrorLog",
]
