"""SQLAlchemy ORM models derived from src/database/db2/db2-definitions.sql.

Tables:
- PortfolioMaster   (PORTFOLIO_MASTER)
- InvestmentPositions (INVESTMENT_POSITIONS)
- TransactionHistory (TRANSACTION_HISTORY)
- ErrorLog          (ERRLOG)          — from ERRLOG.sql / ERRHNDL.cbl / DB2ERR.cbl
- AuditLog          (AUDITLOG)        — from SECMGR.cbl
- AuthFile          (AUTHFILE)        — from SECMGR.cbl authorization checks
"""

from datetime import date, datetime, time
from decimal import Decimal

from sqlalchemy import (
    CHAR,
    TIMESTAMP,
    VARCHAR,
    Date,
    ForeignKeyConstraint,
    Index,
    Integer,
    Numeric,
    Time,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    """Declarative base for all ORM models."""


# -------------------------------------------------------------------------
# PORTFOLIO_MASTER
# -------------------------------------------------------------------------


class PortfolioMaster(Base):
    """Portfolio master table (db2-definitions.sql PORTFOLIO_MASTER)."""

    __tablename__ = "portfolio_master"

    portfolio_id: Mapped[str] = mapped_column(CHAR(8), primary_key=True)
    account_type: Mapped[str] = mapped_column(CHAR(2), nullable=False)
    branch_id: Mapped[str] = mapped_column(CHAR(2), nullable=False)
    client_id: Mapped[str] = mapped_column(CHAR(10), nullable=False)
    portfolio_name: Mapped[str] = mapped_column(VARCHAR(50), nullable=False)
    currency_code: Mapped[str] = mapped_column(CHAR(3), nullable=False)
    risk_level: Mapped[str] = mapped_column(CHAR(1), nullable=False)
    status: Mapped[str] = mapped_column(CHAR(1), nullable=False)
    open_date: Mapped[date] = mapped_column(Date, nullable=False)
    close_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    last_maint_date: Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False)
    last_maint_user: Mapped[str] = mapped_column(VARCHAR(8), nullable=False)

    # Relationships
    positions: Mapped[list["InvestmentPositions"]] = relationship(back_populates="portfolio")
    transactions: Mapped[list["TransactionHistory"]] = relationship(
        back_populates="portfolio"
    )

    __table_args__ = (
        Index("idx_port_master_client", "client_id", "status"),
    )


# -------------------------------------------------------------------------
# INVESTMENT_POSITIONS
# -------------------------------------------------------------------------


class InvestmentPositions(Base):
    """Investment positions table (db2-definitions.sql INVESTMENT_POSITIONS)."""

    __tablename__ = "investment_positions"

    portfolio_id: Mapped[str] = mapped_column(CHAR(8), primary_key=True)
    investment_id: Mapped[str] = mapped_column(CHAR(10), primary_key=True)
    position_date: Mapped[date] = mapped_column(Date, primary_key=True)
    quantity: Mapped[Decimal] = mapped_column(Numeric(18, 4), nullable=False)
    cost_basis: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    market_value: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    currency_code: Mapped[str] = mapped_column(CHAR(3), nullable=False)
    last_maint_date: Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False)
    last_maint_user: Mapped[str] = mapped_column(VARCHAR(8), nullable=False)

    # Relationship
    portfolio: Mapped["PortfolioMaster"] = relationship(back_populates="positions")

    __table_args__ = (
        ForeignKeyConstraint(["portfolio_id"], ["portfolio_master.portfolio_id"]),
        Index("idx_positions_date", "position_date", "portfolio_id"),
    )


# -------------------------------------------------------------------------
# TRANSACTION_HISTORY
# -------------------------------------------------------------------------


class TransactionHistory(Base):
    """Transaction history table (db2-definitions.sql TRANSACTION_HISTORY)."""

    __tablename__ = "transaction_history"

    transaction_id: Mapped[str] = mapped_column(CHAR(20), primary_key=True)
    portfolio_id: Mapped[str] = mapped_column(CHAR(8), nullable=False)
    transaction_date: Mapped[date] = mapped_column(Date, nullable=False)
    transaction_time: Mapped[time] = mapped_column(Time, nullable=False)
    investment_id: Mapped[str] = mapped_column(CHAR(10), nullable=False)
    transaction_type: Mapped[str] = mapped_column(CHAR(2), nullable=False)
    quantity: Mapped[Decimal] = mapped_column(Numeric(18, 4), nullable=False)
    price: Mapped[Decimal] = mapped_column(Numeric(18, 4), nullable=False)
    amount: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    currency_code: Mapped[str] = mapped_column(CHAR(3), nullable=False)
    status: Mapped[str] = mapped_column(CHAR(1), nullable=False)
    process_date: Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False)
    process_user: Mapped[str] = mapped_column(VARCHAR(8), nullable=False)

    # Relationship
    portfolio: Mapped["PortfolioMaster"] = relationship(back_populates="transactions")

    __table_args__ = (
        ForeignKeyConstraint(["portfolio_id"], ["portfolio_master.portfolio_id"]),
        Index("idx_trans_hist_port", "portfolio_id", "transaction_date"),
        Index("idx_trans_hist_date", "transaction_date", "portfolio_id"),
    )


# -------------------------------------------------------------------------
# ERRLOG  (from ERRLOG.sql, used by ERRHNDL.cbl and DB2ERR.cbl)
# -------------------------------------------------------------------------


class ErrorLog(Base):
    """Error logging table (ERRLOG.sql)."""

    __tablename__ = "errlog"

    error_timestamp: Mapped[datetime] = mapped_column(TIMESTAMP, primary_key=True)
    program_id: Mapped[str] = mapped_column(CHAR(8), primary_key=True)
    error_type: Mapped[str] = mapped_column(CHAR(1), nullable=False)
    error_severity: Mapped[int] = mapped_column(Integer, nullable=False)
    error_code: Mapped[str] = mapped_column(CHAR(8), nullable=False)
    error_message: Mapped[str] = mapped_column(VARCHAR(200), nullable=False)
    process_date: Mapped[date] = mapped_column(Date, nullable=False)
    process_time: Mapped[time] = mapped_column(Time, nullable=False)
    user_id: Mapped[str] = mapped_column(CHAR(8), nullable=False)
    additional_info: Mapped[str | None] = mapped_column(VARCHAR(500), nullable=True)

    __table_args__ = (
        Index("errlog_ix1", "process_date", "error_severity"),
    )


# -------------------------------------------------------------------------
# AUDITLOG  (used by SECMGR.cbl for audit trail)
# -------------------------------------------------------------------------


class AuditLog(Base):
    """Audit log table used by SECMGR.cbl for security audit trails."""

    __tablename__ = "auditlog"

    audit_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    audit_timestamp: Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False)
    system_id: Mapped[str] = mapped_column(CHAR(8), nullable=False)
    user_id: Mapped[str] = mapped_column(CHAR(8), nullable=False)
    program: Mapped[str] = mapped_column(CHAR(8), nullable=False)
    terminal: Mapped[str] = mapped_column(CHAR(8), nullable=False)
    audit_type: Mapped[str] = mapped_column(CHAR(4), nullable=False)
    action: Mapped[str] = mapped_column(CHAR(8), nullable=False)
    status: Mapped[str] = mapped_column(CHAR(4), nullable=False)
    portfolio_id: Mapped[str | None] = mapped_column(CHAR(8), nullable=True)
    account_no: Mapped[str | None] = mapped_column(CHAR(10), nullable=True)
    before_image: Mapped[str | None] = mapped_column(VARCHAR(100), nullable=True)
    after_image: Mapped[str | None] = mapped_column(VARCHAR(100), nullable=True)
    message: Mapped[str | None] = mapped_column(VARCHAR(100), nullable=True)

    __table_args__ = (
        Index("idx_auditlog_user", "user_id", "audit_timestamp"),
        Index("idx_auditlog_portfolio", "portfolio_id", "audit_timestamp"),
    )


# -------------------------------------------------------------------------
# AUTHFILE  (used by SECMGR.cbl for authorization checks)
# -------------------------------------------------------------------------


class AuthFile(Base):
    """Authorization file used by SECMGR.cbl for access control."""

    __tablename__ = "authfile"

    user_id: Mapped[str] = mapped_column(CHAR(8), primary_key=True)
    password_hash: Mapped[str] = mapped_column(VARCHAR(128), nullable=False)
    user_name: Mapped[str] = mapped_column(VARCHAR(50), nullable=False)
    access_level: Mapped[int] = mapped_column(Integer, nullable=False)
    status: Mapped[str] = mapped_column(CHAR(1), nullable=False)
    last_login: Mapped[datetime | None] = mapped_column(TIMESTAMP, nullable=True)
    failed_attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    created_date: Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False)
    last_maint_date: Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False)
    last_maint_user: Mapped[str] = mapped_column(VARCHAR(8), nullable=False)

    __table_args__ = (
        Index("idx_authfile_status", "status", "user_id"),
    )
