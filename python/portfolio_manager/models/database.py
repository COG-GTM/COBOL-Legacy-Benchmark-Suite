"""SQLAlchemy ORM models for PostgreSQL.

Replaces:
- VSAM KSDS files (PORTMSTR, TRANHIST, POSHIST) -> PostgreSQL tables
- DB2 tables (from src/database/db2/) -> PostgreSQL tables

Source references:
  - src/database/db2/db2-definitions.sql
    (PORTFOLIO_MASTER, INVESTMENT_POSITIONS, TRANSACTION_HISTORY)
  - src/database/db2/POSHIST.sql          (POSHIST)
  - src/database/db2/ERRLOG.sql           (ERRLOG)
  - src/database/db2/RTNCODES.sql         (RTNCODES)
  - src/database/vsam/vsam-definitions.txt (PORTMSTR, TRANHIST, POSHIST VSAM)
  - src/copybook/common/AUDITLOG.cpy      (AUDITLOG)
"""

from __future__ import annotations

from datetime import date, datetime, time
from decimal import Decimal

from sqlalchemy import (
    CheckConstraint,
    Date,
    DateTime,
    Index,
    Integer,
    Numeric,
    String,
    Text,
    Time,
    func,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    """SQLAlchemy declarative base for all ORM models."""

    pass


# ---------------------------------------------------------------------------
# PORTFOLIO_MASTER  (from db2-definitions.sql lines 10-24)
# Replaces VSAM KSDS PORTMSTR (key: PORTFOLIO_ID)
# ---------------------------------------------------------------------------


class PortfolioMaster(Base):
    """Portfolio master table.

    Migrated from DB2 PORTFOLIO_MASTER + VSAM PORTMSTR.
    """

    __tablename__ = "portfolio_master"

    portfolio_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    account_type: Mapped[str] = mapped_column(String(2), nullable=False)
    branch_id: Mapped[str] = mapped_column(String(2), nullable=False)
    client_id: Mapped[str] = mapped_column(String(10), nullable=False)
    portfolio_name: Mapped[str] = mapped_column(String(50), nullable=False)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    risk_level: Mapped[str] = mapped_column(String(1), nullable=False, default="M")
    status: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        default="A",
        comment="A=Active, C=Closed, S=Suspended",
    )
    open_date: Mapped[date] = mapped_column(Date, nullable=False)
    close_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    last_maint_date: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now(), onupdate=func.now()
    )
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False)

    __table_args__ = (
        Index("idx_port_master_client", "client_id", "status"),
        CheckConstraint("status IN ('A', 'C', 'S')", name="ck_portfolio_status"),
    )

    def __repr__(self) -> str:
        return (
            f"<PortfolioMaster(portfolio_id={self.portfolio_id!r}, "
            f"name={self.portfolio_name!r})>"
        )


# ---------------------------------------------------------------------------
# INVESTMENT_POSITIONS  (from db2-definitions.sql lines 29-41)
# Replaces VSAM KSDS POSHIST
# (Position History VSAM, key: PORTFOLIO_ID + POSITION_DATE + INVESTMENT_ID)
# ---------------------------------------------------------------------------


class InvestmentPosition(Base):
    """Investment positions table.

    Migrated from DB2 INVESTMENT_POSITIONS.
    Composite PK: (portfolio_id, investment_id, position_date).
    """

    __tablename__ = "investment_positions"

    portfolio_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    investment_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    position_date: Mapped[date] = mapped_column(Date, primary_key=True)
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(18, 4), nullable=False, default=Decimal("0")
    )
    cost_basis: Mapped[Decimal] = mapped_column(
        Numeric(18, 2), nullable=False, default=Decimal("0")
    )
    market_value: Mapped[Decimal] = mapped_column(
        Numeric(18, 2), nullable=False, default=Decimal("0")
    )
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    last_maint_date: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now(), onupdate=func.now()
    )
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False)

    __table_args__ = (
        Index("idx_positions_date", "position_date", "portfolio_id"),
    )

    def __repr__(self) -> str:
        return (
            f"<InvestmentPosition(portfolio={self.portfolio_id!r}, "
            f"investment={self.investment_id!r}, date={self.position_date})>"
        )


# ---------------------------------------------------------------------------
# TRANSACTION_HISTORY  (from db2-definitions.sql lines 46-62)
# Replaces VSAM KSDS TRANHIST (key: PORTFOLIO_ID + TXN_DATE + SEQ)
# ---------------------------------------------------------------------------


class TransactionHistory(Base):
    """Transaction history table.

    Migrated from DB2 TRANSACTION_HISTORY + VSAM TRANHIST.
    """

    __tablename__ = "transaction_history"

    transaction_id: Mapped[str] = mapped_column(String(20), primary_key=True)
    portfolio_id: Mapped[str] = mapped_column(String(8), nullable=False)
    transaction_date: Mapped[date] = mapped_column(Date, nullable=False)
    transaction_time: Mapped[time] = mapped_column(Time, nullable=False)
    investment_id: Mapped[str] = mapped_column(String(10), nullable=False)
    transaction_type: Mapped[str] = mapped_column(
        String(2),
        nullable=False,
        comment="BU=Buy, SL=Sell, TR=Transfer, FE=Fee",
    )
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(18, 4), nullable=False, default=Decimal("0")
    )
    price: Mapped[Decimal] = mapped_column(
        Numeric(18, 4), nullable=False, default=Decimal("0")
    )
    amount: Mapped[Decimal] = mapped_column(
        Numeric(18, 2), nullable=False, default=Decimal("0")
    )
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        default="P",
        comment="P=Processed, F=Failed, R=Reversed",
    )
    process_date: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now()
    )
    process_user: Mapped[str] = mapped_column(String(8), nullable=False)

    __table_args__ = (
        Index("idx_trans_hist_port", "portfolio_id", "transaction_date"),
        Index("idx_trans_hist_date", "transaction_date", "portfolio_id"),
        CheckConstraint(
            "transaction_type IN ('BU', 'SL', 'TR', 'FE')",
            name="ck_trans_type",
        ),
    )

    def __repr__(self) -> str:
        return f"<TransactionHistory(id={self.transaction_id!r}, type={self.transaction_type!r})>"


# ---------------------------------------------------------------------------
# POSHIST  (from POSHIST.sql)
# Position History DB2 table - stores all portfolio transaction history
# ---------------------------------------------------------------------------


class PositionHistory(Base):
    """Position history table.

    Migrated from DB2 POSHIST (src/database/db2/POSHIST.sql).
    Composite PK: (account_no, portfolio_id, trans_date, trans_time).
    """

    __tablename__ = "poshist"

    account_no: Mapped[str] = mapped_column(String(8), primary_key=True)
    portfolio_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    trans_date: Mapped[date] = mapped_column(Date, primary_key=True)
    trans_time: Mapped[time] = mapped_column(Time, primary_key=True)
    trans_type: Mapped[str] = mapped_column(
        String(2),
        nullable=False,
        comment="BU=Buy, SL=Sell, TR=Transfer",
    )
    security_id: Mapped[str] = mapped_column(String(12), nullable=False)
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(15, 3), nullable=False, default=Decimal("0")
    )
    price: Mapped[Decimal] = mapped_column(
        Numeric(15, 3), nullable=False, default=Decimal("0")
    )
    amount: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0")
    )
    fees: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0")
    )
    total_amount: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0")
    )
    cost_basis: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0")
    )
    gain_loss: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, default=Decimal("0")
    )
    process_date: Mapped[date] = mapped_column(Date, nullable=False)
    process_time: Mapped[time] = mapped_column(Time, nullable=False)
    program_id: Mapped[str] = mapped_column(String(8), nullable=False)
    user_id: Mapped[str] = mapped_column(String(8), nullable=False)
    audit_timestamp: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now()
    )

    __table_args__ = (
        Index("poshist_ix1", "security_id", "trans_date"),
        Index("poshist_ix2", "process_date", "program_id"),
    )

    def __repr__(self) -> str:
        return (
            f"<PositionHistory(account={self.account_no!r}, "
            f"portfolio={self.portfolio_id!r}, date={self.trans_date})>"
        )


# ---------------------------------------------------------------------------
# ERRLOG  (from ERRLOG.sql)
# ---------------------------------------------------------------------------


class ErrorLog(Base):
    """Error logging table.

    Migrated from DB2 ERRLOG (src/database/db2/ERRLOG.sql).
    Composite PK: (error_timestamp, program_id).
    """

    __tablename__ = "errlog"

    error_timestamp: Mapped[datetime] = mapped_column(DateTime, primary_key=True)
    program_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    error_type: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="S=System, A=Application, D=Data",
    )
    error_severity: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        comment="1=Info, 2=Warning, 3=Error, 4=Severe",
    )
    error_code: Mapped[str] = mapped_column(String(8), nullable=False)
    error_message: Mapped[str] = mapped_column(String(200), nullable=False)
    process_date: Mapped[date] = mapped_column(Date, nullable=False)
    process_time: Mapped[time] = mapped_column(Time, nullable=False)
    user_id: Mapped[str] = mapped_column(String(8), nullable=False)
    additional_info: Mapped[str | None] = mapped_column(Text, nullable=True)

    __table_args__ = (
        Index("errlog_ix1", "process_date", error_severity.desc()),
    )

    def __repr__(self) -> str:
        return (
            f"<ErrorLog(ts={self.error_timestamp}, program={self.program_id!r}, "
            f"code={self.error_code!r})>"
        )


# ---------------------------------------------------------------------------
# AUDITLOG  (from AUDITLOG.cpy — no DB2 DDL in original, but logically stored)
# ---------------------------------------------------------------------------


class AuditLog(Base):
    """Audit log table.

    Migrated from the AUDITLOG.cpy structure. In the original system this
    was a VSAM sequential file; in the Python migration it is a PostgreSQL table.
    """

    __tablename__ = "audit_log"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    timestamp: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now()
    )
    system_id: Mapped[str] = mapped_column(String(8), nullable=False, default="PYMIG")
    user_id: Mapped[str] = mapped_column(String(8), nullable=False)
    program: Mapped[str] = mapped_column(String(8), nullable=False)
    terminal: Mapped[str] = mapped_column(String(8), nullable=False, default="")
    audit_type: Mapped[str] = mapped_column(
        String(4),
        nullable=False,
        comment="TRAN=Transaction, USER=User Action, SYST=System Event",
    )
    action: Mapped[str] = mapped_column(
        String(8),
        nullable=False,
        comment="CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN",
    )
    status: Mapped[str] = mapped_column(
        String(4),
        nullable=False,
        default="SUCC",
        comment="SUCC, FAIL, WARN",
    )
    portfolio_id: Mapped[str | None] = mapped_column(String(8), nullable=True)
    account_no: Mapped[str | None] = mapped_column(String(10), nullable=True)
    before_image: Mapped[str | None] = mapped_column(Text, nullable=True)
    after_image: Mapped[str | None] = mapped_column(Text, nullable=True)
    message: Mapped[str | None] = mapped_column(String(100), nullable=True)

    __table_args__ = (
        Index("idx_audit_log_ts", "timestamp"),
        Index("idx_audit_log_user", "user_id", "timestamp"),
    )

    def __repr__(self) -> str:
        return f"<AuditLog(id={self.id}, type={self.audit_type!r}, action={self.action!r})>"


# ---------------------------------------------------------------------------
# RTNCODES  (from RTNCODES.sql)
# ---------------------------------------------------------------------------


class ReturnCodeLog(Base):
    """Return code logging table.

    Migrated from DB2 RTNCODES (src/database/db2/RTNCODES.sql).
    """

    __tablename__ = "rtncodes"

    timestamp: Mapped[datetime] = mapped_column(DateTime, primary_key=True)
    program_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    return_code: Mapped[int] = mapped_column(Integer, nullable=False)
    highest_code: Mapped[int] = mapped_column(Integer, nullable=False)
    status_code: Mapped[str] = mapped_column(String(1), nullable=False)
    message_text: Mapped[str | None] = mapped_column(String(80), nullable=True)

    __table_args__ = (
        Index("rtncodes_prg_idx", "program_id", "timestamp"),
        Index("rtncodes_sts_idx", "status_code", "timestamp"),
    )

    def __repr__(self) -> str:
        return (
            f"<ReturnCodeLog(program={self.program_id!r}, rc={self.return_code})>"
        )
