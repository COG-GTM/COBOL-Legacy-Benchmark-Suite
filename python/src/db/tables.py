"""
SQLAlchemy ORM table definitions translated from:
  - src/database/db2/db2-definitions.sql
  - src/database/db2/db2-indexes.sql
  - src/database/vsam/vsam-definitions.txt

COBOL COMP-3 decimal precision is preserved via Numeric(precision, scale).
COBOL PIC X(n) fields use String(n).
"""

from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import (
    Boolean,
    Date,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    Numeric,
    String,
    Text,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    """SQLAlchemy declarative base for all tables."""

    pass


# ---------------------------------------------------------------------------
# PORTFOLIO_MASTER  (from db2-definitions.sql + VSAM PORTMSTR)
# ---------------------------------------------------------------------------
class PortfolioMaster(Base):
    __tablename__ = "portfolio_master"

    portfolio_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    account_no: Mapped[str] = mapped_column(String(10), nullable=False, default="")
    account_type: Mapped[str] = mapped_column(String(2), nullable=False, default="IN")
    branch_id: Mapped[str] = mapped_column(String(2), nullable=False, default="")
    client_id: Mapped[str] = mapped_column(String(10), nullable=False, default="")
    portfolio_name: Mapped[str] = mapped_column(String(50), nullable=False, default="")
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    risk_level: Mapped[str] = mapped_column(String(1), nullable=False, default="M")

    # Client info (from PORTFLIO.cpy)
    client_name: Mapped[str] = mapped_column(String(30), nullable=False, default="")
    client_type: Mapped[str] = mapped_column(String(1), nullable=False, default="I")

    # Status and dates
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="A")
    open_date: Mapped[date] = mapped_column(Date, nullable=False)
    close_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    create_date: Mapped[date] = mapped_column(Date, nullable=False)

    # Financial - Numeric for COBOL COMP-3 precision: PIC S9(13)V99
    total_value: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0.00")
    )
    cash_balance: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0.00")
    )

    # Audit
    last_maint_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    last_trans_date: Mapped[date | None] = mapped_column(Date, nullable=True)

    # Relationships
    positions: Mapped[list["InvestmentPosition"]] = relationship(
        back_populates="portfolio", cascade="all, delete-orphan"
    )
    transactions: Mapped[list["TransactionHistory"]] = relationship(
        back_populates="portfolio", cascade="all, delete-orphan"
    )

    __table_args__ = (
        Index("idx_portfolio_client", "client_id"),
        Index("idx_portfolio_branch", "branch_id"),
        Index("idx_portfolio_status", "status"),
        Index("idx_portfolio_account", "account_type", "branch_id"),
    )


# ---------------------------------------------------------------------------
# INVESTMENT_POSITIONS  (from db2-definitions.sql + VSAM POSHIST)
# Composite PK: PORTFOLIO_ID + INVESTMENT_ID + POSITION_DATE
# ---------------------------------------------------------------------------
class InvestmentPosition(Base):
    __tablename__ = "investment_positions"

    portfolio_id: Mapped[str] = mapped_column(
        String(8), ForeignKey("portfolio_master.portfolio_id"), primary_key=True
    )
    investment_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    position_date: Mapped[date] = mapped_column(Date, primary_key=True)

    # Position data - Numeric for COBOL COMP-3: PIC S9(11)V9(4) and PIC S9(13)V9(2)
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(15, 4), nullable=False, default=Decimal("0.0000")
    )
    cost_basis: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0.00")
    )
    market_value: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0.00")
    )
    currency: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="A")

    # Audit
    last_maint_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False, default="")

    # Relationships
    portfolio: Mapped["PortfolioMaster"] = relationship(back_populates="positions")

    __table_args__ = (
        Index("idx_position_investment", "investment_id"),
        Index("idx_position_date", "position_date"),
        Index("idx_position_status", "status"),
    )


# ---------------------------------------------------------------------------
# TRANSACTION_HISTORY  (from db2-definitions.sql + VSAM TRANHIST)
# ---------------------------------------------------------------------------
class TransactionHistory(Base):
    __tablename__ = "transaction_history"

    transaction_id: Mapped[str] = mapped_column(String(20), primary_key=True)
    trn_date: Mapped[date] = mapped_column(Date, nullable=False)
    trn_time: Mapped[str] = mapped_column(String(6), nullable=False, default="000000")
    portfolio_id: Mapped[str] = mapped_column(String(8), ForeignKey("portfolio_master.portfolio_id"), nullable=False)
    sequence_no: Mapped[str] = mapped_column(String(6), nullable=False, default="000001")

    # Transaction data
    investment_id: Mapped[str] = mapped_column(String(10), nullable=False, default="")
    trn_type: Mapped[str] = mapped_column(String(2), nullable=False)
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(15, 4), nullable=False, default=Decimal("0.0000")
    )
    price: Mapped[Decimal] = mapped_column(
        Numeric(15, 4), nullable=False, default=Decimal("0.0000")
    )
    amount: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0.00")
    )
    currency: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="P")

    # Audit
    process_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    process_user: Mapped[str] = mapped_column(String(8), nullable=False, default="")

    # Relationships
    portfolio: Mapped["PortfolioMaster"] = relationship(back_populates="transactions")

    __table_args__ = (
        Index("idx_transaction_portfolio", "portfolio_id"),
        Index("idx_transaction_date", "trn_date"),
        Index("idx_transaction_type", "trn_type"),
        Index("idx_transaction_status", "status"),
        Index("idx_transaction_investment", "investment_id"),
    )


# ---------------------------------------------------------------------------
# MARKET_DATA  (from MKTDATA.cpy concepts)
# ---------------------------------------------------------------------------
class MarketData(Base):
    __tablename__ = "market_data"

    investment_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    price_date: Mapped[date] = mapped_column(Date, primary_key=True)
    current_price: Mapped[Decimal] = mapped_column(
        Numeric(15, 4), nullable=False, default=Decimal("0.0000")
    )
    previous_close: Mapped[Decimal] = mapped_column(
        Numeric(15, 4), nullable=False, default=Decimal("0.0000")
    )
    day_high: Mapped[Decimal] = mapped_column(
        Numeric(15, 4), nullable=False, default=Decimal("0.0000")
    )
    day_low: Mapped[Decimal] = mapped_column(
        Numeric(15, 4), nullable=False, default=Decimal("0.0000")
    )
    volume: Mapped[Decimal] = mapped_column(
        Numeric(15, 0), nullable=False, default=Decimal("0")
    )
    currency: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    last_update: Mapped[datetime] = mapped_column(DateTime, nullable=False)


# ---------------------------------------------------------------------------
# AUDIT_LOG  (from AUDITLOG.cpy)
# ---------------------------------------------------------------------------
class AuditLog(Base):
    __tablename__ = "audit_log"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    system_id: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    user_id: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    program: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    terminal: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    audit_type: Mapped[str] = mapped_column(String(4), nullable=False, default="TRAN")
    action: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    status: Mapped[str] = mapped_column(String(4), nullable=False, default="SUCC")
    portfolio_id: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    account_no: Mapped[str] = mapped_column(String(10), nullable=False, default="")
    before_image: Mapped[str] = mapped_column(String(100), nullable=False, default="")
    after_image: Mapped[str] = mapped_column(String(100), nullable=False, default="")
    message: Mapped[str] = mapped_column(String(100), nullable=False, default="")

    __table_args__ = (
        Index("idx_audit_timestamp", "timestamp"),
        Index("idx_audit_user", "user_id"),
        Index("idx_audit_portfolio", "portfolio_id"),
    )


# ---------------------------------------------------------------------------
# ERROR_LOG  (from ERRLOG.cpy)
# ---------------------------------------------------------------------------
class ErrorLog(Base):
    __tablename__ = "error_log"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    system_id: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    program: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    paragraph: Mapped[str] = mapped_column(String(30), nullable=False, default="")
    severity: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    category: Mapped[str] = mapped_column(String(2), nullable=False, default="PR")
    error_code: Mapped[str] = mapped_column(String(4), nullable=False, default="")
    message: Mapped[str] = mapped_column(String(80), nullable=False, default="")
    sqlcode: Mapped[int | None] = mapped_column(Integer, nullable=True)
    sqlstate: Mapped[str] = mapped_column(String(5), nullable=False, default="")
    error_data: Mapped[str] = mapped_column(Text, nullable=False, default="")

    __table_args__ = (
        Index("idx_error_timestamp", "timestamp"),
        Index("idx_error_severity", "severity"),
        Index("idx_error_program", "program"),
    )


# ---------------------------------------------------------------------------
# BATCH_CONTROL  (from BCHCTL.cpy / BCHCON.cpy)
# ---------------------------------------------------------------------------
class BatchControl(Base):
    __tablename__ = "batch_control"

    batch_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    batch_name: Mapped[str] = mapped_column(String(30), nullable=False, default="")
    batch_status: Mapped[str] = mapped_column(String(1), nullable=False, default="R")
    schedule_date: Mapped[date] = mapped_column(Date, nullable=False)
    process_type: Mapped[str] = mapped_column(String(3), nullable=False, default="INI")
    max_restarts: Mapped[int] = mapped_column(Integer, nullable=False, default=3)
    restart_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    last_run_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    last_run_rc: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    __table_args__ = (
        Index("idx_batch_status", "batch_status"),
        Index("idx_batch_schedule", "schedule_date"),
    )


# ---------------------------------------------------------------------------
# CHECKPOINT  (from CKPRST.cpy)
# ---------------------------------------------------------------------------
class Checkpoint(Base):
    __tablename__ = "checkpoint"

    checkpoint_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    batch_id: Mapped[str] = mapped_column(String(8), nullable=False)
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="I")
    phase: Mapped[str] = mapped_column(String(2), nullable=False, default="00")
    save_date: Mapped[date] = mapped_column(Date, nullable=False)
    save_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_key: Mapped[str] = mapped_column(String(50), nullable=False, default="")
    records_at_checkpoint: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    commit_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    restart_data: Mapped[str] = mapped_column(String(200), nullable=False, default="")
    total_amount: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0.00")
    )

    __table_args__ = (
        Index("idx_checkpoint_batch", "batch_id"),
        Index("idx_checkpoint_status", "status"),
    )


# ---------------------------------------------------------------------------
# USER_SECURITY  (from SECPARM.cpy / USRDATA.cpy)
# ---------------------------------------------------------------------------
class UserSecurity(Base):
    __tablename__ = "user_security"

    user_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    user_name: Mapped[str] = mapped_column(String(30), nullable=False, default="")
    password_hash: Mapped[str] = mapped_column(String(64), nullable=False, default="")
    department: Mapped[str] = mapped_column(String(20), nullable=False, default="")
    branch_id: Mapped[str] = mapped_column(String(2), nullable=False, default="")
    role: Mapped[str] = mapped_column(String(10), nullable=False, default="")
    email: Mapped[str] = mapped_column(String(50), nullable=False, default="")
    phone: Mapped[str] = mapped_column(String(15), nullable=False, default="")
    security_level: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    access_portfolio: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    access_transaction: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    access_inquiry: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    access_admin: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    access_batch: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    locked: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    failed_attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    last_login: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_modified: Mapped[datetime] = mapped_column(DateTime, nullable=False)
