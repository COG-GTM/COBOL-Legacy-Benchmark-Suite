"""SQLAlchemy ORM models - converted from DB2 table definitions.

These models replace the DB2 tables defined in:
- POSHIST.sql
- ERRLOG.sql
- db2-definitions.sql
"""

from datetime import datetime

from sqlalchemy import (
    Boolean,
    Column,
    Date,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    Numeric,
    String,
    Text,
    Time,
)
from sqlalchemy.orm import DeclarativeBase, relationship


class Base(DeclarativeBase):
    """Base class for all ORM models."""

    pass


class PortfolioMaster(Base):
    """Portfolio Master table - from db2-definitions.sql PORTFOLIO_MASTER.

    Stores portfolio header information including client details and status.
    """

    __tablename__ = "portfolio_master"

    portfolio_id = Column(String(8), primary_key=True)
    account_type = Column(String(2), nullable=False)
    branch_id = Column(String(2), nullable=False)
    client_id = Column(String(10), nullable=False, index=True)
    portfolio_name = Column(String(50), nullable=False)
    currency_code = Column(String(3), nullable=False, default="USD")
    risk_level = Column(String(1), nullable=False, default="M")
    status = Column(String(1), nullable=False, default="A")
    open_date = Column(Date, nullable=False)
    close_date = Column(Date, nullable=True)
    last_maint_date = Column(DateTime, nullable=False, default=datetime.now)
    last_maint_user = Column(String(8), nullable=False)

    positions = relationship("InvestmentPosition", back_populates="portfolio")
    transactions = relationship("TransactionHistory", back_populates="portfolio")

    __table_args__ = (
        Index("idx_port_master_client", "client_id", "status"),
    )


class InvestmentPosition(Base):
    """Investment Positions table - from db2-definitions.sql INVESTMENT_POSITIONS.

    Stores current portfolio positions with quantity, cost basis, and market value.
    """

    __tablename__ = "investment_positions"

    portfolio_id = Column(
        String(8), ForeignKey("portfolio_master.portfolio_id"), primary_key=True
    )
    investment_id = Column(String(10), primary_key=True)
    position_date = Column(Date, primary_key=True)
    quantity = Column(Numeric(18, 4), nullable=False, default=0)
    cost_basis = Column(Numeric(18, 2), nullable=False, default=0)
    market_value = Column(Numeric(18, 2), nullable=False, default=0)
    currency_code = Column(String(3), nullable=False, default="USD")
    last_maint_date = Column(DateTime, nullable=False, default=datetime.now)
    last_maint_user = Column(String(8), nullable=False)

    portfolio = relationship("PortfolioMaster", back_populates="positions")

    __table_args__ = (
        Index("idx_positions_date", "position_date", "portfolio_id"),
    )


class TransactionHistory(Base):
    """Transaction History table - from db2-definitions.sql TRANSACTION_HISTORY.

    Stores all portfolio transactions (buy, sell, transfer, fee).
    """

    __tablename__ = "transaction_history"

    transaction_id = Column(String(20), primary_key=True)
    portfolio_id = Column(
        String(8), ForeignKey("portfolio_master.portfolio_id"), nullable=False
    )
    transaction_date = Column(Date, nullable=False)
    transaction_time = Column(Time, nullable=False)
    investment_id = Column(String(10), nullable=False)
    transaction_type = Column(String(2), nullable=False)
    quantity = Column(Numeric(18, 4), nullable=False)
    price = Column(Numeric(18, 4), nullable=False)
    amount = Column(Numeric(18, 2), nullable=False)
    currency_code = Column(String(3), nullable=False, default="USD")
    status = Column(String(1), nullable=False, default="P")
    process_date = Column(DateTime, nullable=False, default=datetime.now)
    process_user = Column(String(8), nullable=False)

    portfolio = relationship("PortfolioMaster", back_populates="transactions")

    __table_args__ = (
        Index("idx_trans_hist_port", "portfolio_id", "transaction_date"),
        Index("idx_trans_hist_date", "transaction_date", "portfolio_id"),
    )


class PositionHistory(Base):
    """Position History table - from POSHIST.sql.

    Stores historical position and transaction data for reporting.
    """

    __tablename__ = "poshist"

    account_no = Column(String(8), primary_key=True)
    portfolio_id = Column(String(10), primary_key=True)
    trans_date = Column(Date, primary_key=True)
    trans_time = Column(Time, primary_key=True)
    trans_type = Column(String(2), nullable=False)
    security_id = Column(String(12), nullable=False, index=True)
    quantity = Column(Numeric(15, 3), nullable=False)
    price = Column(Numeric(15, 3), nullable=False)
    amount = Column(Numeric(15, 2), nullable=False)
    fees = Column(Numeric(15, 2), nullable=False, default=0)
    total_amount = Column(Numeric(15, 2), nullable=False)
    cost_basis = Column(Numeric(15, 2), nullable=False)
    gain_loss = Column(Numeric(15, 2), nullable=False)
    process_date = Column(Date, nullable=False)
    process_time = Column(Time, nullable=False)
    program_id = Column(String(8), nullable=False)
    user_id = Column(String(8), nullable=False)
    audit_timestamp = Column(DateTime, nullable=False, default=datetime.now)

    __table_args__ = (
        Index("poshist_ix1", "security_id", "trans_date"),
        Index("poshist_ix2", "process_date", "program_id"),
    )


class ErrorLog(Base):
    """Error Log table - from ERRLOG.sql.

    Stores application errors and warnings for troubleshooting.
    """

    __tablename__ = "errlog"

    id = Column(Integer, primary_key=True, autoincrement=True)
    error_timestamp = Column(DateTime, nullable=False, default=datetime.now)
    program_id = Column(String(8), nullable=False, index=True)
    error_type = Column(String(1), nullable=False)
    error_severity = Column(Integer, nullable=False)
    error_code = Column(String(8), nullable=False)
    error_message = Column(String(200), nullable=False)
    process_date = Column(Date, nullable=False)
    process_time = Column(Time, nullable=False)
    user_id = Column(String(8), nullable=False)
    additional_info = Column(Text, nullable=True)

    __table_args__ = (
        Index("errlog_ix1", "process_date", "error_severity"),
    )


class AuthFile(Base):
    """Authorization File table - for user access control.

    Replaces RACF-based authorization with database-backed access control.
    """

    __tablename__ = "authfile"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(8), nullable=False, index=True)
    resource = Column(String(8), nullable=False)
    access_type = Column(String(8), nullable=False)
    granted_date = Column(DateTime, nullable=False, default=datetime.now)
    granted_by = Column(String(8), nullable=False)
    expiry_date = Column(Date, nullable=True)
    is_active = Column(Boolean, nullable=False, default=True)

    __table_args__ = (
        Index("authfile_ix1", "user_id", "resource", "access_type"),
    )


class AuditLog(Base):
    """Audit Log table - for security and compliance tracking.

    Stores all user actions and system events for audit purposes.
    """

    __tablename__ = "auditlog"

    id = Column(Integer, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime, nullable=False, default=datetime.now)
    user_id = Column(String(8), nullable=False, index=True)
    terminal_id = Column(String(8), nullable=True)
    trans_id = Column(String(8), nullable=True)
    program = Column(String(8), nullable=False)
    access_type = Column(String(8), nullable=False)
    portfolio_id = Column(String(8), nullable=True)
    account_no = Column(String(10), nullable=True)
    action_status = Column(String(4), nullable=False, default="SUCC")
    before_image = Column(Text, nullable=True)
    after_image = Column(Text, nullable=True)
    message = Column(String(100), nullable=True)

    __table_args__ = (
        Index("auditlog_ix1", "timestamp", "user_id"),
        Index("auditlog_ix2", "portfolio_id", "timestamp"),
    )


class BatchControl(Base):
    """Batch Control table - for job scheduling and checkpoint/restart.

    Stores batch job control records for managing job dependencies
    and checkpoint/restart functionality.
    """

    __tablename__ = "batch_control"

    job_name = Column(String(8), primary_key=True)
    process_date = Column(String(8), primary_key=True)
    sequence_no = Column(Integer, primary_key=True)
    status = Column(String(1), nullable=False, default="R")
    step_name = Column(String(8), nullable=True)
    program_name = Column(String(8), nullable=True)
    start_time = Column(String(8), nullable=True)
    end_time = Column(String(8), nullable=True)
    prereq_count = Column(Integer, nullable=False, default=0)
    return_code = Column(Integer, nullable=False, default=0)
    error_desc = Column(String(80), nullable=True)
    restart_count = Column(Integer, nullable=False, default=0)
    attempt_ts = Column(DateTime, nullable=True)
    complete_ts = Column(DateTime, nullable=True)
    records_read = Column(Integer, nullable=False, default=0)
    records_written = Column(Integer, nullable=False, default=0)

    __table_args__ = (
        Index("batch_control_ix1", "process_date", "status"),
    )


class ProcessSequence(Base):
    """Process Sequence table - for defining batch job sequences.

    Stores process sequence definitions and dependencies.
    """

    __tablename__ = "process_sequence"

    process_id = Column(String(8), primary_key=True)
    process_date = Column(String(8), primary_key=True)
    sequence_type = Column(String(3), nullable=False)
    sequence_order = Column(Integer, nullable=False)
    description = Column(String(50), nullable=True)
    is_restartable = Column(Boolean, nullable=False, default=True)
    max_restarts = Column(Integer, nullable=False, default=3)
    dep_count = Column(Integer, nullable=False, default=0)

    __table_args__ = (
        Index("process_seq_ix1", "sequence_type", "sequence_order"),
    )


class ProcessDependency(Base):
    """Process Dependency table - for defining job dependencies.

    Stores dependencies between batch processes.
    """

    __tablename__ = "process_dependency"

    id = Column(Integer, primary_key=True, autoincrement=True)
    process_id = Column(String(8), nullable=False)
    process_date = Column(String(8), nullable=False)
    dep_process_id = Column(String(8), nullable=False)
    dep_sequence = Column(Integer, nullable=False)
    dep_max_rc = Column(Integer, nullable=False, default=4)
    is_hard_dependency = Column(Boolean, nullable=False, default=True)

    __table_args__ = (
        Index("process_dep_ix1", "process_id", "process_date"),
    )


class User(Base):
    """User table - for authentication.

    Stores user credentials and profile information.
    """

    __tablename__ = "users"

    user_id = Column(String(8), primary_key=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(100), nullable=True)
    email = Column(String(100), nullable=True)
    is_active = Column(Boolean, nullable=False, default=True)
    is_admin = Column(Boolean, nullable=False, default=False)
    created_date = Column(DateTime, nullable=False, default=datetime.now)
    last_login = Column(DateTime, nullable=True)
    failed_attempts = Column(Integer, nullable=False, default=0)
    locked_until = Column(DateTime, nullable=True)
