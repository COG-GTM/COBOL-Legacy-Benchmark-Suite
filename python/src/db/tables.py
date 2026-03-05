"""
SQLAlchemy ORM table definitions translated from:
- db2-definitions.sql (PORTFOLIO_MASTER, INVESTMENT_POSITIONS, TRANSACTION_HISTORY)
- db2-indexes.sql (index definitions)
- vsam-definitions.txt (VSAM KSDS files → relational tables)

All DECIMAL columns match COBOL COMP-3 PIC clauses exactly.
"""

from datetime import date, datetime

from sqlalchemy import (
    Boolean,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    Numeric,
    String,
    Text,
    func,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    pass


class PortfolioMaster(Base):
    """
    Translates DB2 PORTFOLIO_MASTER table and VSAM PORTMSTR KSDS.
    VSAM key: portfolio_id + account_type + branch_id
    """

    __tablename__ = "portfolio_master"

    portfolio_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    account_type: Mapped[str] = mapped_column(String(2), nullable=False, default="IN")
    branch_id: Mapped[str] = mapped_column(String(2), nullable=False, default="00")
    client_id: Mapped[str] = mapped_column(String(10), nullable=False)
    portfolio_name: Mapped[str] = mapped_column(String(50), nullable=False)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    risk_level: Mapped[str] = mapped_column(String(1), nullable=False, default="M")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="A")
    client_name: Mapped[str] = mapped_column(String(30), nullable=False, default="")
    client_type: Mapped[str] = mapped_column(String(1), nullable=False, default="I")
    total_value: Mapped[Numeric] = mapped_column(
        Numeric(15, 2), nullable=False, default=0
    )
    cash_balance: Mapped[Numeric] = mapped_column(
        Numeric(15, 2), nullable=False, default=0
    )
    account_number: Mapped[str] = mapped_column(String(10), nullable=False, default="")
    open_date: Mapped[date] = mapped_column(nullable=False, default=func.current_date())
    close_date: Mapped[date | None] = mapped_column(nullable=True)
    last_maint_date: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=func.now()
    )
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False, default="SYSTEM")

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
        Index("idx_portfolio_vsam_key", "portfolio_id", "account_type", "branch_id", unique=True),
    )


class InvestmentPosition(Base):
    """
    Translates DB2 INVESTMENT_POSITIONS table and VSAM POSHIST KSDS.
    Composite PK: portfolio_id + investment_id + position_date
    VSAM key: portfolio + date + investment
    """

    __tablename__ = "investment_positions"

    portfolio_id: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("portfolio_master.portfolio_id"),
        primary_key=True,
    )
    investment_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    position_date: Mapped[date] = mapped_column(primary_key=True)

    # PIC S9(11)V9(4) COMP-3 → Numeric(18,4)
    quantity: Mapped[Numeric] = mapped_column(
        Numeric(18, 4), nullable=False, default=0
    )
    # PIC S9(13)V9(2) COMP-3 → Numeric(18,2)
    cost_basis: Mapped[Numeric] = mapped_column(
        Numeric(18, 2), nullable=False, default=0
    )
    market_value: Mapped[Numeric] = mapped_column(
        Numeric(18, 2), nullable=False, default=0
    )
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="A")
    last_maint_date: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=func.now()
    )
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False, default="SYSTEM")

    # Relationship
    portfolio: Mapped["PortfolioMaster"] = relationship(back_populates="positions")

    __table_args__ = (
        Index("idx_position_portfolio", "portfolio_id"),
        Index("idx_position_date", "position_date"),
        Index("idx_position_investment", "investment_id"),
    )


class TransactionHistory(Base):
    """
    Translates DB2 TRANSACTION_HISTORY table and VSAM TRANHIST KSDS.
    PK: transaction_id (auto-increment)
    VSAM key: date + time + portfolio + sequence
    """

    __tablename__ = "transaction_history"

    transaction_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    portfolio_id: Mapped[str] = mapped_column(
        String(8), ForeignKey("portfolio_master.portfolio_id"), nullable=False
    )
    investment_id: Mapped[str] = mapped_column(String(10), nullable=False)
    trn_date: Mapped[date] = mapped_column(nullable=False)
    trn_time: Mapped[str] = mapped_column(String(6), nullable=False, default="000000")
    sequence_no: Mapped[str] = mapped_column(String(6), nullable=False, default="000001")

    trn_type: Mapped[str] = mapped_column(String(2), nullable=False)
    quantity: Mapped[Numeric] = mapped_column(Numeric(18, 4), nullable=False, default=0)
    price: Mapped[Numeric] = mapped_column(Numeric(18, 4), nullable=False, default=0)
    amount: Mapped[Numeric] = mapped_column(Numeric(18, 2), nullable=False, default=0)
    fees: Mapped[Numeric] = mapped_column(Numeric(18, 2), nullable=False, default=0)
    total_amount: Mapped[Numeric] = mapped_column(Numeric(18, 2), nullable=False, default=0)
    cost_basis: Mapped[Numeric] = mapped_column(Numeric(18, 2), nullable=False, default=0)
    gain_loss: Mapped[Numeric] = mapped_column(Numeric(18, 2), nullable=False, default=0)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="P")
    process_date: Mapped[date | None] = mapped_column(nullable=True)
    process_user: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=func.now())

    # Relationship
    portfolio: Mapped["PortfolioMaster"] = relationship(back_populates="transactions")

    __table_args__ = (
        Index("idx_trn_portfolio", "portfolio_id"),
        Index("idx_trn_date", "trn_date"),
        Index("idx_trn_type", "trn_type"),
        Index("idx_trn_vsam_key", "trn_date", "trn_time", "portfolio_id", "sequence_no"),
    )


class AuditLog(Base):
    """Translates AUDITLOG DB2 table and VSAM audit file."""

    __tablename__ = "audit_log"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=func.now())
    system_id: Mapped[str] = mapped_column(String(8), nullable=False, default="SYSTEM")
    user_id: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    program: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    terminal: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    audit_type: Mapped[str] = mapped_column(String(4), nullable=False)
    action: Mapped[str] = mapped_column(String(8), nullable=False)
    status: Mapped[str] = mapped_column(String(4), nullable=False, default="SUCC")
    key_info: Mapped[str] = mapped_column(String(50), nullable=False, default="")
    before_image: Mapped[str] = mapped_column(Text, nullable=False, default="")
    after_image: Mapped[str] = mapped_column(Text, nullable=False, default="")
    message: Mapped[str] = mapped_column(Text, nullable=False, default="")

    __table_args__ = (
        Index("idx_audit_timestamp", "timestamp"),
        Index("idx_audit_user", "user_id"),
        Index("idx_audit_type", "audit_type"),
    )


class ErrorLog(Base):
    """Translates ERRLOG DB2 table and VSAM error log file."""

    __tablename__ = "error_log"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=func.now())
    program: Mapped[str] = mapped_column(String(8), nullable=False)
    category: Mapped[str] = mapped_column(String(2), nullable=False)
    error_code: Mapped[str] = mapped_column(String(4), nullable=False)
    severity: Mapped[int] = mapped_column(Integer, nullable=False)
    error_text: Mapped[str] = mapped_column(String(80), nullable=False)
    error_details: Mapped[str] = mapped_column(Text, nullable=False, default="")

    __table_args__ = (
        Index("idx_error_timestamp", "timestamp"),
        Index("idx_error_severity", "severity"),
    )


class BatchControl(Base):
    """Translates batch control VSAM file to relational table."""

    __tablename__ = "batch_control"

    job_name: Mapped[str] = mapped_column(String(8), primary_key=True)
    process_date: Mapped[str] = mapped_column(String(8), primary_key=True)
    sequence_no: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="R")
    return_code: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    start_time: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    end_time: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    records_read: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    records_written: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    error_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    restart_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    max_restarts: Mapped[int] = mapped_column(Integer, nullable=False, default=3)
    error_desc: Mapped[str] = mapped_column(String(80), nullable=False, default="")
    attempt_ts: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class ProcessSequence(Base):
    """Translates process sequence VSAM file."""

    __tablename__ = "process_sequence"

    process_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    sequence_type: Mapped[str] = mapped_column(String(3), primary_key=True)
    sequence_no: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    description: Mapped[str] = mapped_column(String(40), nullable=False, default="")
    restartable: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)


class UserAuth(Base):
    """Translates AUTHFILE DB2 table for SECMGR authorization checks."""

    __tablename__ = "user_auth"

    user_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    resource: Mapped[str] = mapped_column(String(8), primary_key=True)
    access_type: Mapped[str] = mapped_column(String(8), primary_key=True)
    granted: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
