"""
Database module for the COBOL to Python migration.

This module provides SQLAlchemy ORM models and database management utilities
that replace the VSAM file and DB2 table operations from the original COBOL system.
"""

from migration.python.database.orm_models import (
    Base,
    PortfolioMaster,
    TransactionHistory,
    PositionHistory,
    BatchControl,
    ErrorLog,
)

from migration.python.database.session import (
    DatabaseManager,
    get_session,
    init_database,
)

__all__ = [
    # ORM Models
    'Base',
    'PortfolioMaster',
    'TransactionHistory',
    'PositionHistory',
    'BatchControl',
    'ErrorLog',
    # Session management
    'DatabaseManager',
    'get_session',
    'init_database',
]
