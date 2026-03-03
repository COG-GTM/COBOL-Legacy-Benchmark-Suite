"""
VSAM File Definitions → PostgreSQL/SQLAlchemy Models.

This package provides SQLAlchemy models equivalent to the VSAM KSDS files
defined in the COBOL Legacy Benchmark Suite, along with a data access
layer that mirrors VSAM file operations (READ, WRITE, REWRITE, DELETE).

Models:
    - PortfolioMaster:      VSAM PORTMSTR (Portfolio Master file)
    - TransactionHistory:   VSAM TRANHIST (Transaction History file)
    - PositionHistory:      VSAM POSHIST  (Position History file)
    - AuditHistory:         Audit trail file (from HISTREC.cpy)

Data Access:
    - VSAMDataAccess:       Generic VSAM-like CRUD operations
    - VSAMStatus:           VSAM file status codes
    - VSAMError:            Exception for VSAM operation failures
"""

from .audit_history import AuditHistory
from .base import Base
from .data_access import (
    VSAMCursor,
    VSAMDataAccess,
    VSAMError,
    VSAMStatus,
    create_audit_history_dao,
    create_portfolio_master_dao,
    create_position_history_dao,
    create_transaction_history_dao,
)
from .position_history import PositionHistory
from .position_master import PortfolioMaster
from .transaction_file import TransactionHistory

__all__ = [
    # Base
    "Base",
    # Models
    "PortfolioMaster",
    "TransactionHistory",
    "PositionHistory",
    "AuditHistory",
    # Data access
    "VSAMDataAccess",
    "VSAMCursor",
    "VSAMStatus",
    "VSAMError",
    # Factory functions
    "create_portfolio_master_dao",
    "create_transaction_history_dao",
    "create_position_history_dao",
    "create_audit_history_dao",
]
