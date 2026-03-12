"""Database layer for the Investment Portfolio Management System.

Migrated from COBOL DB2/VSAM definitions to SQLAlchemy 2.0 ORM models.
Provides engine configuration, session management, and repository pattern
for CRUD operations on portfolio, position, transaction, and audit data.
"""

from python.src.db.engine import get_engine, dispose_engine
from python.src.db.session import get_session, transactional
from python.src.db.tables import (
    Base,
    PortfolioMaster,
    InvestmentPosition,
    TransactionHistory,
    PositionHistory,
    ErrorLog,
    ReturnCode,
)

__all__ = [
    "Base",
    "PortfolioMaster",
    "InvestmentPosition",
    "TransactionHistory",
    "PositionHistory",
    "ErrorLog",
    "ReturnCode",
    "get_engine",
    "dispose_engine",
    "get_session",
    "transactional",
]
