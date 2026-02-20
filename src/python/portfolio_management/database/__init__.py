"""Database schema definitions - migrated from COBOL DB2 SQL."""

from portfolio_management.database.schema import (
    POSITION_HISTORY_DDL,
    ERROR_LOG_DDL,
    AUDIT_LOG_DDL,
    ALL_DDL,
)

__all__ = [
    "POSITION_HISTORY_DDL",
    "ERROR_LOG_DDL",
    "AUDIT_LOG_DDL",
    "ALL_DDL",
]
