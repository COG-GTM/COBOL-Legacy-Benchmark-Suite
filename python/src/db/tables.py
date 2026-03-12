"""SQLAlchemy 2.0 ORM models for the Investment Portfolio Management System.

Translated from:
  - src/database/db2/db2-definitions.sql   (PORTFOLIO_MASTER, INVESTMENT_POSITIONS,
                                             TRANSACTION_HISTORY)
  - src/database/db2/POSHIST.sql           (POSHIST position history)
  - src/database/db2/ERRLOG.sql            (ERRLOG error log)
  - src/database/db2/RTNCODES.sql          (RTNCODES return codes)
  - src/database/vsam/vsam-definitions.txt (PORTMSTR, TRANHIST, POSHIST KSDS files)
  - src/copybook/db2/DBTBLS.cpy           (COBOL PIC clause references)

Column types follow COBOL PIC clause mappings:
  PIC S9(13)V9(2) -> Numeric(15, 2)
  PIC S9(12)V9(3) -> Numeric(15, 3)
  PIC S9(4) COMP  -> Integer
  PIC X(n)        -> String(n)
"""

from __future__ import annotations

import datetime
from decimal import Decimal

from sqlalchemy import (
    Date,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    Numeric,
    String,
    Time,
)
from sqlalchemy.orm import (
    DeclarativeBase,
    Mapped,
    mapped_column,
    relationship,
)


class Base(DeclarativeBase):
    """Declarative base for all ORM models."""

    pass


# =====================================================================
# PORTFOLIO_MASTER
# Source: db2-definitions.sql lines 10-24
# VSAM: PORTMSTR (KSDS, key = portfolio_id + account_type + branch_id)
# =====================================================================
class PortfolioMaster(Base):
    """Portfolio master table.

    Primary key: PORTFOLIO_ID (CHAR(8))
    VSAM KSDS key structure: portfolio_id(8) + account_type(2) + branch_id(2)
    """

    __tablename__ = "portfolio_master"

    portfolio_id: Mapped[str] = mapped_column(
        String(8), primary_key=True, comment="Portfolio identifier"
    )
    account_type: Mapped[str] = mapped_column(
        String(2), nullable=False, comment="Account type code"
    )
    branch_id: Mapped[str] = mapped_column(
        String(2), nullable=False, comment="Branch identifier"
    )
    client_id: Mapped[str] = mapped_column(
        String(10), nullable=False, comment="Client identifier"
    )
    portfolio_name: Mapped[str] = mapped_column(
        String(50), nullable=False, comment="Portfolio display name"
    )
    currency_code: Mapped[str] = mapped_column(
        String(3), nullable=False, comment="ISO currency code"
    )
    risk_level: Mapped[str] = mapped_column(
        String(1), nullable=False, comment="Risk level code"
    )
    status: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="Status: A=Active, C=Closed, S=Suspended",
    )
    open_date: Mapped[datetime.date] = mapped_column(
        Date, nullable=False, comment="Portfolio open date"
    )
    close_date: Mapped[datetime.date | None] = mapped_column(
        Date, nullable=True, comment="Portfolio close date"
    )
    last_maint_date: Mapped[datetime.datetime] = mapped_column(
        DateTime, nullable=False, comment="Last maintenance timestamp"
    )
    last_maint_user: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Last maintenance user ID"
    )

    # Relationships
    positions: Mapped[list[InvestmentPosition]] = relationship(
        back_populates="portfolio", cascade="all, delete-orphan"
    )
    transactions: Mapped[list[TransactionHistory]] = relationship(
        back_populates="portfolio", cascade="all, delete-orphan"
    )

    # Indexes matching DB2 definitions and VSAM key structure
    __table_args__ = (
        # db2-definitions.sql: IDX_PORT_MASTER_CLIENT ON (CLIENT_ID, STATUS)
        Index("ix_portfolio_master_client", "client_id", "status"),
        # VSAM PORTMSTR KSDS key: portfolio_id + account_type + branch_id
        Index(
            "ix_portfolio_master_vsam_key",
            "portfolio_id",
            "account_type",
            "branch_id",
            unique=True,
        ),
        # Index for branch-based lookups
        Index("ix_portfolio_master_branch", "branch_id", "status"),
    )

    def __repr__(self) -> str:
        return (
            f"PortfolioMaster(portfolio_id={self.portfolio_id!r}, "
            f"client_id={self.client_id!r}, status={self.status!r})"
        )


# =====================================================================
# INVESTMENT_POSITIONS
# Source: db2-definitions.sql lines 29-41
# VSAM: POSHIST (KSDS, key = portfolio_id + position_date + investment_id)
# Copybook: DBTBLS.cpy PIC clauses for decimal precision
# =====================================================================
class InvestmentPosition(Base):
    """Investment positions table.

    Composite primary key: (PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE)
    VSAM KSDS key structure: portfolio_id(8) + position_date(8) + investment_id(10)
    """

    __tablename__ = "investment_positions"

    portfolio_id: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("portfolio_master.portfolio_id"),
        primary_key=True,
        comment="Portfolio identifier (FK to portfolio_master)",
    )
    investment_id: Mapped[str] = mapped_column(
        String(10), primary_key=True, comment="Investment/security identifier"
    )
    position_date: Mapped[datetime.date] = mapped_column(
        Date, primary_key=True, comment="Position date (YYYYMMDD)"
    )
    # PIC S9(14)V9(4) -> DECIMAL(18,4)
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(18, 4), nullable=False, comment="Position quantity"
    )
    # PIC S9(16)V9(2) -> DECIMAL(18,2)
    cost_basis: Mapped[Decimal] = mapped_column(
        Numeric(18, 2), nullable=False, comment="Cost basis amount"
    )
    market_value: Mapped[Decimal] = mapped_column(
        Numeric(18, 2), nullable=False, comment="Current market value"
    )
    currency_code: Mapped[str] = mapped_column(
        String(3), nullable=False, comment="ISO currency code"
    )
    last_maint_date: Mapped[datetime.datetime] = mapped_column(
        DateTime, nullable=False, comment="Last maintenance timestamp"
    )
    last_maint_user: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Last maintenance user ID"
    )

    # Relationships
    portfolio: Mapped[PortfolioMaster] = relationship(back_populates="positions")

    # Indexes matching DB2 definitions and VSAM key structure
    __table_args__ = (
        # db2-definitions.sql: IDX_POSITIONS_DATE ON (POSITION_DATE, PORTFOLIO_ID)
        Index("ix_positions_date", "position_date", "portfolio_id"),
        # VSAM POSHIST KSDS key: portfolio_id + position_date + investment_id
        Index(
            "ix_positions_vsam_key",
            "portfolio_id",
            "position_date",
            "investment_id",
            unique=True,
        ),
    )

    def __repr__(self) -> str:
        return (
            f"InvestmentPosition(portfolio_id={self.portfolio_id!r}, "
            f"investment_id={self.investment_id!r}, "
            f"position_date={self.position_date!r})"
        )


# =====================================================================
# TRANSACTION_HISTORY
# Source: db2-definitions.sql lines 46-62
# VSAM: TRANHIST (KSDS, key = date + time + portfolio_id + sequence)
# =====================================================================
class TransactionHistory(Base):
    """Transaction history table.

    Primary key: TRANSACTION_ID (CHAR(20))
    Format: YYYYMMDDHHMMSS + 6-digit sequence
    VSAM KSDS key structure: date(8) + time(6) + portfolio_id(8) + seq(6)
    """

    __tablename__ = "transaction_history"

    transaction_id: Mapped[str] = mapped_column(
        String(20),
        primary_key=True,
        comment="Transaction ID (YYYYMMDDHHMMSS + 6-digit seq)",
    )
    portfolio_id: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("portfolio_master.portfolio_id"),
        nullable=False,
        comment="Portfolio identifier (FK to portfolio_master)",
    )
    transaction_date: Mapped[datetime.date] = mapped_column(
        Date, nullable=False, comment="Transaction date"
    )
    transaction_time: Mapped[datetime.time] = mapped_column(
        Time, nullable=False, comment="Transaction time"
    )
    investment_id: Mapped[str] = mapped_column(
        String(10), nullable=False, comment="Investment/security identifier"
    )
    transaction_type: Mapped[str] = mapped_column(
        String(2),
        nullable=False,
        comment="Type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee",
    )
    # PIC S9(14)V9(4) -> DECIMAL(18,4)
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(18, 4), nullable=False, comment="Transaction quantity"
    )
    price: Mapped[Decimal] = mapped_column(
        Numeric(18, 4), nullable=False, comment="Transaction price"
    )
    # PIC S9(16)V9(2) -> DECIMAL(18,2)
    amount: Mapped[Decimal] = mapped_column(
        Numeric(18, 2), nullable=False, comment="Transaction amount"
    )
    currency_code: Mapped[str] = mapped_column(
        String(3), nullable=False, comment="ISO currency code"
    )
    status: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="Status: P=Processed, F=Failed, R=Reversed",
    )
    process_date: Mapped[datetime.datetime] = mapped_column(
        DateTime, nullable=False, comment="Processing timestamp"
    )
    process_user: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Processing user ID"
    )

    # Relationships
    portfolio: Mapped[PortfolioMaster] = relationship(back_populates="transactions")

    # Indexes matching DB2 definitions and VSAM key structure
    __table_args__ = (
        # db2-definitions.sql: IDX_TRANS_HIST_PORT ON (PORTFOLIO_ID, TRANSACTION_DATE)
        Index("ix_trans_hist_port", "portfolio_id", "transaction_date"),
        # db2-definitions.sql: IDX_TRANS_HIST_DATE ON (TRANSACTION_DATE, PORTFOLIO_ID)
        Index("ix_trans_hist_date", "transaction_date", "portfolio_id"),
        # VSAM TRANHIST KSDS key: date + time + portfolio_id + seq (covered by
        # transaction_id format YYYYMMDDHHMMSS + seq, plus portfolio_id)
        Index(
            "ix_trans_hist_vsam_key",
            "transaction_date",
            "transaction_time",
            "portfolio_id",
        ),
    )

    def __repr__(self) -> str:
        return (
            f"TransactionHistory(transaction_id={self.transaction_id!r}, "
            f"portfolio_id={self.portfolio_id!r}, "
            f"transaction_type={self.transaction_type!r})"
        )


# =====================================================================
# POSHIST (Position History)
# Source: POSHIST.sql
# Copybook: DBTBLS.cpy POSHIST-RECORD
# =====================================================================
class PositionHistory(Base):
    """Position history table — stores all portfolio transaction history.

    Composite primary key: (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
    Mapped from POSHIST.sql and DBTBLS.cpy POSHIST-RECORD.

    Decimal precision from DBTBLS.cpy:
      PH-QUANTITY    PIC S9(12)V9(3) -> Numeric(15, 3)
      PH-PRICE       PIC S9(12)V9(3) -> Numeric(15, 3)
      PH-AMOUNT      PIC S9(13)V9(2) -> Numeric(15, 2)
      PH-FEES        PIC S9(13)V9(2) -> Numeric(15, 2)
    """

    __tablename__ = "poshist"

    # PK columns: ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME
    account_no: Mapped[str] = mapped_column(
        String(8), primary_key=True, comment="Account number"
    )
    portfolio_id: Mapped[str] = mapped_column(
        String(10), primary_key=True, comment="Portfolio identifier"
    )
    trans_date: Mapped[datetime.date] = mapped_column(
        Date, primary_key=True, comment="Transaction date"
    )
    trans_time: Mapped[datetime.time] = mapped_column(
        Time, primary_key=True, comment="Transaction time"
    )
    trans_type: Mapped[str] = mapped_column(
        String(2),
        nullable=False,
        comment="Transaction type: BU=Buy, SL=Sell, TR=Transfer",
    )
    security_id: Mapped[str] = mapped_column(
        String(12), nullable=False, comment="Security identifier"
    )
    # PIC S9(12)V9(3) -> Numeric(15, 3)
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(15, 3), nullable=False, comment="Transaction quantity"
    )
    price: Mapped[Decimal] = mapped_column(
        Numeric(15, 3), nullable=False, comment="Transaction price"
    )
    # PIC S9(13)V9(2) -> Numeric(15, 2)
    amount: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, comment="Transaction amount"
    )
    fees: Mapped[Decimal] = mapped_column(
        Numeric(15, 2),
        nullable=False,
        default=Decimal("0"),
        server_default="0",
        comment="Transaction fees",
    )
    total_amount: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, comment="Total amount including fees"
    )
    cost_basis: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, comment="Cost basis amount"
    )
    gain_loss: Mapped[Decimal] = mapped_column(
        Numeric(15, 2), nullable=False, comment="Realized gain/loss amount"
    )
    process_date: Mapped[datetime.date] = mapped_column(
        Date, nullable=False, comment="Processing date"
    )
    process_time: Mapped[datetime.time] = mapped_column(
        Time, nullable=False, comment="Processing time"
    )
    program_id: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Processing program ID"
    )
    user_id: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Processing user ID"
    )
    audit_timestamp: Mapped[datetime.datetime] = mapped_column(
        DateTime, nullable=False, comment="Audit trail timestamp"
    )

    # Indexes matching POSHIST.sql secondary indexes
    __table_args__ = (
        # POSHIST_IX1: (SECURITY_ID, TRANS_DATE)
        Index("ix_poshist_security", "security_id", "trans_date"),
        # POSHIST_IX2: (PROCESS_DATE, PROGRAM_ID)
        Index("ix_poshist_process", "process_date", "program_id"),
    )

    def __repr__(self) -> str:
        return (
            f"PositionHistory(account_no={self.account_no!r}, "
            f"portfolio_id={self.portfolio_id!r}, "
            f"trans_date={self.trans_date!r})"
        )


# =====================================================================
# ERRLOG (Error Log)
# Source: ERRLOG.sql
# Copybook: DBTBLS.cpy ERRLOG-RECORD
# =====================================================================
class ErrorLog(Base):
    """Error logging table — stores application errors and warnings.

    Composite primary key: (ERROR_TIMESTAMP, PROGRAM_ID)
    Mapped from ERRLOG.sql and DBTBLS.cpy ERRLOG-RECORD.

    Error types: S=System, A=Application, D=Data
    Severity levels: 1=Info, 2=Warning, 3=Error, 4=Severe
    """

    __tablename__ = "errlog"

    error_timestamp: Mapped[datetime.datetime] = mapped_column(
        DateTime, primary_key=True, comment="Error occurrence timestamp"
    )
    program_id: Mapped[str] = mapped_column(
        String(8), primary_key=True, comment="Program that raised the error"
    )
    error_type: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="Error type: S=System, A=Application, D=Data",
    )
    # PIC S9(4) COMP -> Integer
    error_severity: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        comment="Severity: 1=Info, 2=Warning, 3=Error, 4=Severe",
    )
    error_code: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Error code identifier"
    )
    error_message: Mapped[str] = mapped_column(
        String(200), nullable=False, comment="Error description message"
    )
    process_date: Mapped[datetime.date] = mapped_column(
        Date, nullable=False, comment="Processing date"
    )
    process_time: Mapped[datetime.time] = mapped_column(
        Time, nullable=False, comment="Processing time"
    )
    user_id: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="User ID at time of error"
    )
    additional_info: Mapped[str | None] = mapped_column(
        String(500), nullable=True, comment="Additional diagnostic information"
    )

    # Indexes matching ERRLOG.sql secondary indexes
    __table_args__ = (
        # ERRLOG_IX1: (PROCESS_DATE, ERROR_SEVERITY DESC)
        Index("ix_errlog_process_date", "process_date", "error_severity"),
    )

    def __repr__(self) -> str:
        return (
            f"ErrorLog(error_timestamp={self.error_timestamp!r}, "
            f"program_id={self.program_id!r}, "
            f"error_code={self.error_code!r})"
        )


# =====================================================================
# RTNCODES (Return Codes)
# Source: RTNCODES.sql
# =====================================================================
class ReturnCode(Base):
    """Return code logging table.

    Composite primary key: (TIMESTAMP, PROGRAM_ID)
    Mapped from RTNCODES.sql.
    """

    __tablename__ = "rtncodes"

    timestamp: Mapped[datetime.datetime] = mapped_column(
        DateTime, primary_key=True, comment="Return code timestamp"
    )
    program_id: Mapped[str] = mapped_column(
        String(8), primary_key=True, comment="Program identifier"
    )
    return_code: Mapped[int] = mapped_column(
        Integer, nullable=False, comment="Program return code"
    )
    highest_code: Mapped[int] = mapped_column(
        Integer, nullable=False, comment="Highest return code in run"
    )
    status_code: Mapped[str] = mapped_column(
        String(1), nullable=False, comment="Status code"
    )
    message_text: Mapped[str | None] = mapped_column(
        String(80), nullable=True, comment="Descriptive message"
    )

    # Indexes matching RTNCODES.sql
    __table_args__ = (
        # RTNCODES_PRG_IDX: (PROGRAM_ID, TIMESTAMP)
        Index("ix_rtncodes_program", "program_id", "timestamp"),
        # RTNCODES_STS_IDX: (STATUS_CODE, TIMESTAMP)
        Index("ix_rtncodes_status", "status_code", "timestamp"),
    )

    def __repr__(self) -> str:
        return (
            f"ReturnCode(timestamp={self.timestamp!r}, "
            f"program_id={self.program_id!r}, "
            f"return_code={self.return_code!r})"
        )
