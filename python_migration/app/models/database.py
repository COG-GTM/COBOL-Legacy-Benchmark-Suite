"""
SQLAlchemy ORM models for the Portfolio Management System.
These models map to PostgreSQL tables, replacing VSAM files and DB2 tables.

Database Schema Mappings:
- PORTMSTR (VSAM) -> portfolio_master
- POSFILE (VSAM) -> position_master
- TRANHIST (VSAM) -> transaction_history
- POSHIST (DB2) -> position_history
- ERRLOG (DB2) -> error_log
- AUDITLOG (DB2) -> audit_log
- AUTHFILE (DB2) -> auth_file
- BCHCTL (VSAM) -> batch_control
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
    UniqueConstraint,
)
from sqlalchemy.orm import DeclarativeBase, relationship


class Base(DeclarativeBase):
    """Base class for all SQLAlchemy models."""
    pass


class PortfolioMaster(Base):
    """
    Portfolio Master table - migrated from PORTMSTR VSAM and PORTFOLIO_MASTER DB2 table.
    Stores portfolio information.
    """
    __tablename__ = "portfolio_master"

    portfolio_id = Column(String(8), primary_key=True, comment="Portfolio identifier")
    account_type = Column(String(2), nullable=False, comment="Account type code")
    branch_id = Column(String(2), nullable=False, comment="Branch identifier")
    client_id = Column(String(10), nullable=False, comment="Client identifier")
    portfolio_name = Column(String(50), nullable=False, comment="Portfolio name")
    client_name = Column(String(30), comment="Client name")
    client_type = Column(String(1), default="I", comment="I=Individual, C=Corporate, T=Trust")
    currency_code = Column(String(3), nullable=False, default="USD", comment="Currency code")
    risk_level = Column(String(1), comment="Risk level")
    status = Column(String(1), nullable=False, default="A", comment="A=Active, C=Closed, S=Suspended")
    total_value = Column(Numeric(15, 2), default=0, comment="Total portfolio value")
    cash_balance = Column(Numeric(15, 2), default=0, comment="Cash balance")
    open_date = Column(Date, nullable=False, comment="Portfolio open date")
    close_date = Column(Date, comment="Portfolio close date")
    last_maint_date = Column(DateTime, nullable=False, default=datetime.utcnow, comment="Last maintenance timestamp")
    last_maint_user = Column(String(8), nullable=False, comment="Last maintenance user")
    last_trans_date = Column(Date, comment="Last transaction date")

    positions = relationship("PositionMaster", back_populates="portfolio")
    transactions = relationship("TransactionHistory", back_populates="portfolio")
    position_history = relationship("PositionHistory", back_populates="portfolio")

    __table_args__ = (
        Index("idx_portfolio_client", "client_id", "status"),
    )


class PositionMaster(Base):
    """
    Position Master table - migrated from POSFILE VSAM and INVESTMENT_POSITIONS DB2 table.
    Stores current portfolio positions (holdings).
    """
    __tablename__ = "position_master"

    id = Column(Integer, primary_key=True, autoincrement=True)
    portfolio_id = Column(String(8), ForeignKey("portfolio_master.portfolio_id"), nullable=False)
    investment_id = Column(String(10), nullable=False, comment="Investment/Security identifier")
    position_date = Column(Date, nullable=False, comment="Position date")
    quantity = Column(Numeric(18, 4), nullable=False, comment="Holding quantity")
    cost_basis = Column(Numeric(18, 2), nullable=False, comment="Total cost basis")
    market_value = Column(Numeric(18, 2), nullable=False, comment="Current market value")
    currency_code = Column(String(3), nullable=False, default="USD", comment="Currency code")
    status = Column(String(1), nullable=False, default="A", comment="A=Active, C=Closed, P=Pending")
    last_maint_date = Column(DateTime, nullable=False, default=datetime.utcnow)
    last_maint_user = Column(String(8), nullable=False)

    portfolio = relationship("PortfolioMaster", back_populates="positions")

    __table_args__ = (
        UniqueConstraint("portfolio_id", "investment_id", "position_date", name="uq_position"),
        Index("idx_position_date", "position_date", "portfolio_id"),
    )


class TransactionHistory(Base):
    """
    Transaction History table - migrated from TRANHIST VSAM and TRANSACTION_HISTORY DB2 table.
    Stores all financial transactions.
    """
    __tablename__ = "transaction_history"

    transaction_id = Column(String(20), primary_key=True, comment="Transaction identifier")
    portfolio_id = Column(String(8), ForeignKey("portfolio_master.portfolio_id"), nullable=False)
    transaction_date = Column(Date, nullable=False, comment="Transaction date")
    transaction_time = Column(Time, nullable=False, comment="Transaction time")
    investment_id = Column(String(10), nullable=False, comment="Investment/Security identifier")
    transaction_type = Column(String(2), nullable=False, comment="BU=Buy, SL=Sell, TR=Transfer, FE=Fee")
    quantity = Column(Numeric(18, 4), nullable=False, comment="Transaction quantity")
    price = Column(Numeric(18, 4), nullable=False, comment="Transaction price")
    amount = Column(Numeric(18, 2), nullable=False, comment="Transaction amount")
    fees = Column(Numeric(18, 2), default=0, comment="Transaction fees")
    total_amount = Column(Numeric(18, 2), nullable=False, comment="Total amount including fees")
    currency_code = Column(String(3), nullable=False, default="USD")
    status = Column(String(1), nullable=False, default="P", comment="P=Pending, D=Done, F=Failed, R=Reversed")
    process_date = Column(DateTime, nullable=False, default=datetime.utcnow)
    process_user = Column(String(8), nullable=False)

    portfolio = relationship("PortfolioMaster", back_populates="transactions")

    __table_args__ = (
        Index("idx_trans_portfolio", "portfolio_id", "transaction_date"),
        Index("idx_trans_date", "transaction_date", "portfolio_id"),
    )


class PositionHistory(Base):
    """
    Position History table - migrated from POSHIST DB2 table.
    Stores historical position and transaction data for reporting.
    """
    __tablename__ = "position_history"

    id = Column(Integer, primary_key=True, autoincrement=True)
    account_no = Column(String(8), nullable=False, comment="Account number")
    portfolio_id = Column(String(10), ForeignKey("portfolio_master.portfolio_id"), nullable=False)
    trans_date = Column(Date, nullable=False, comment="Transaction date")
    trans_time = Column(Time, nullable=False, comment="Transaction time")
    trans_type = Column(String(2), nullable=False, comment="Transaction type")
    security_id = Column(String(12), nullable=False, comment="Security identifier")
    quantity = Column(Numeric(15, 3), nullable=False)
    price = Column(Numeric(15, 3), nullable=False)
    amount = Column(Numeric(15, 2), nullable=False)
    fees = Column(Numeric(15, 2), default=0)
    total_amount = Column(Numeric(15, 2), nullable=False)
    cost_basis = Column(Numeric(15, 2), nullable=False)
    gain_loss = Column(Numeric(15, 2), nullable=False)
    process_date = Column(Date, nullable=False)
    process_time = Column(Time, nullable=False)
    program_id = Column(String(8), nullable=False)
    user_id = Column(String(8), nullable=False)
    audit_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)

    portfolio = relationship("PortfolioMaster", back_populates="position_history")

    __table_args__ = (
        UniqueConstraint("account_no", "portfolio_id", "trans_date", "trans_time", name="uq_poshist"),
        Index("idx_poshist_security", "security_id", "trans_date"),
        Index("idx_poshist_process", "process_date", "program_id"),
    )


class ErrorLog(Base):
    """
    Error Log table - migrated from ERRLOG DB2 table.
    Stores application errors and warnings.
    """
    __tablename__ = "error_log"

    id = Column(Integer, primary_key=True, autoincrement=True)
    error_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    program_id = Column(String(8), nullable=False, comment="Program identifier")
    paragraph_name = Column(String(30), comment="COBOL paragraph name / Python function")
    error_type = Column(String(1), nullable=False, comment="S=System, A=Application, D=Data")
    error_severity = Column(Integer, nullable=False, comment="1=Info, 2=Warning, 3=Error, 4=Severe")
    error_code = Column(String(8), nullable=False, comment="Error code")
    error_message = Column(String(200), nullable=False, comment="Error message")
    sqlcode = Column(Integer, comment="SQL error code if applicable")
    cics_resp = Column(Integer, comment="CICS response code (legacy)")
    process_date = Column(Date, nullable=False)
    process_time = Column(Time, nullable=False)
    user_id = Column(String(8), nullable=False)
    trace_id = Column(String(16), comment="Trace identifier for correlation")
    additional_info = Column(Text, comment="Additional error details")

    __table_args__ = (
        Index("idx_errlog_date", "process_date", "error_severity"),
    )


class AuditLog(Base):
    """
    Audit Log table - migrated from AUDITLOG copybook.
    Stores audit trail entries.
    """
    __tablename__ = "audit_log"

    id = Column(Integer, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    system_id = Column(String(8), nullable=False, comment="System identifier")
    user_id = Column(String(8), nullable=False, comment="User identifier")
    program = Column(String(8), nullable=False, comment="Program name")
    terminal = Column(String(8), comment="Terminal ID (legacy)")
    audit_type = Column(String(4), nullable=False, comment="TRAN, USER, SYST")
    action = Column(String(8), nullable=False, comment="CREATE, UPDATE, DELETE, INQUIRE, etc.")
    status = Column(String(4), nullable=False, comment="SUCC, FAIL, WARN")
    portfolio_id = Column(String(8), comment="Portfolio ID if applicable")
    account_no = Column(String(10), comment="Account number if applicable")
    before_image = Column(String(100), comment="Record before change")
    after_image = Column(String(100), comment="Record after change")
    message = Column(String(100), comment="Audit message")

    __table_args__ = (
        Index("idx_audit_user", "user_id", "timestamp"),
        Index("idx_audit_portfolio", "portfolio_id", "timestamp"),
    )


class AuthFile(Base):
    """
    Authorization File table - migrated from AUTHFILE referenced in SECMGR.cbl.
    Stores user authorization records.
    """
    __tablename__ = "auth_file"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(8), nullable=False, comment="User identifier")
    resource = Column(String(8), nullable=False, comment="Resource name")
    access_type = Column(String(8), nullable=False, comment="Access type (READ, WRITE, etc.)")
    granted_date = Column(DateTime, nullable=False, default=datetime.utcnow)
    granted_by = Column(String(8), nullable=False)
    expiry_date = Column(DateTime, comment="Authorization expiry date")
    is_active = Column(Boolean, default=True)

    __table_args__ = (
        UniqueConstraint("user_id", "resource", "access_type", name="uq_auth"),
        Index("idx_auth_user", "user_id", "is_active"),
    )


class BatchControl(Base):
    """
    Batch Control table - migrated from BCHCTL VSAM file.
    Stores batch job control and checkpoint information.
    """
    __tablename__ = "batch_control"

    id = Column(Integer, primary_key=True, autoincrement=True)
    job_name = Column(String(8), nullable=False, comment="Job name")
    process_date = Column(Date, nullable=False, comment="Process date")
    sequence_no = Column(Integer, nullable=False, comment="Sequence number")
    status = Column(String(1), nullable=False, default="R", comment="R=Ready, A=Active, W=Waiting, D=Done, E=Error")
    step_name = Column(String(8), comment="Current step name")
    program_name = Column(String(8), comment="Program name")
    start_time = Column(Time, comment="Start time")
    end_time = Column(Time, comment="End time")
    return_code = Column(Integer, default=0, comment="Return code")
    error_desc = Column(String(80), comment="Error description")
    records_read = Column(Integer, default=0, comment="Records read count")
    records_written = Column(Integer, default=0, comment="Records written count")
    records_error = Column(Integer, default=0, comment="Error records count")
    restart_count = Column(Integer, default=0, comment="Restart count")
    last_key = Column(String(50), comment="Last processed key for restart")
    attempt_timestamp = Column(DateTime, comment="Last attempt timestamp")
    complete_timestamp = Column(DateTime, comment="Completion timestamp")

    __table_args__ = (
        UniqueConstraint("job_name", "process_date", "sequence_no", name="uq_batch"),
        Index("idx_batch_status", "status", "process_date"),
    )


class User(Base):
    """
    User table for authentication.
    Replaces CICS security with JWT-based authentication.
    """
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(8), unique=True, nullable=False, comment="User identifier")
    username = Column(String(50), unique=True, nullable=False)
    email = Column(String(100), unique=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(100))
    is_active = Column(Boolean, default=True)
    is_superuser = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    last_login = Column(DateTime)

    __table_args__ = (
        Index("idx_user_active", "is_active"),
    )
