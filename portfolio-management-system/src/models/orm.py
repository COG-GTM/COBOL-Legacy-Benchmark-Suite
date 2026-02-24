"""
SQLAlchemy ORM Models for Investment Portfolio Management System.

These models represent all database tables migrated from DB2 and VSAM.
Each model maps to a PostgreSQL table created by the migration scripts
in the migrations/ directory.

COBOL Data Type Mapping Reference:
    CHAR(n)              -> String(n)
    VARCHAR(n)           -> String(n)
    DECIMAL(p,s)         -> Numeric(p,s)
    DATE                 -> Date
    TIME                 -> Time
    TIMESTAMP            -> DateTime(timezone=True)
    INTEGER              -> Integer
"""

from datetime import date, datetime, time

from sqlalchemy import (
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


# =====================================================================
# DB2 Table Models (from db2-definitions.sql and DBTBLS copybook)
# =====================================================================


class PortfolioMaster(Base):
    """Portfolio master records.

    Source: DB2 PORTFOLIO_MASTER table (db2-definitions.sql)
    """

    __tablename__ = "portfolio_master"

    portfolio_id: str = Column(String(8), primary_key=True)
    account_type: str = Column(String(2), nullable=False)
    branch_id: str = Column(String(2), nullable=False)
    client_id: str = Column(String(10), nullable=False)
    portfolio_name: str = Column(String(50), nullable=False)
    currency_code: str = Column(String(3), nullable=False)
    risk_level: str = Column(String(1), nullable=False)
    status: str = Column(String(1), nullable=False)
    open_date: date = Column(Date, nullable=False)
    close_date: date = Column(Date, nullable=True)
    last_maint_date: datetime = Column(DateTime(timezone=True), nullable=False)
    last_maint_user: str = Column(String(8), nullable=False)

    # Relationships
    positions = relationship("InvestmentPosition", back_populates="portfolio")
    transactions = relationship("TransactionHistoryORM", back_populates="portfolio")

    __table_args__ = (
        Index("idx_port_master_client", "client_id", "status"),
    )

    def __repr__(self) -> str:
        return (
            f"<PortfolioMaster(portfolio_id={self.portfolio_id!r}, "
            f"name={self.portfolio_name!r}, status={self.status!r})>"
        )


class InvestmentPosition(Base):
    """Investment position records.

    Source: DB2 INVESTMENT_POSITIONS table (db2-definitions.sql)
    COBOL numeric types:
        QUANTITY     -> DECIMAL(18,4) from PIC S9(11)V9(4) COMP-3
        COST_BASIS   -> DECIMAL(18,2) from PIC S9(13)V9(2) COMP-3
        MARKET_VALUE -> DECIMAL(18,2) from PIC S9(13)V9(2) COMP-3
    """

    __tablename__ = "investment_positions"

    portfolio_id: str = Column(
        String(8), ForeignKey("portfolio_master.portfolio_id"), primary_key=True
    )
    investment_id: str = Column(String(10), primary_key=True)
    position_date: date = Column(Date, primary_key=True)
    quantity = Column(Numeric(18, 4), nullable=False)
    cost_basis = Column(Numeric(18, 2), nullable=False)
    market_value = Column(Numeric(18, 2), nullable=False)
    currency_code: str = Column(String(3), nullable=False)
    last_maint_date: datetime = Column(DateTime(timezone=True), nullable=False)
    last_maint_user: str = Column(String(8), nullable=False)

    # Relationships
    portfolio = relationship("PortfolioMaster", back_populates="positions")

    __table_args__ = (
        Index("idx_positions_date", "position_date", "portfolio_id"),
    )

    def __repr__(self) -> str:
        return (
            f"<InvestmentPosition(portfolio={self.portfolio_id!r}, "
            f"investment={self.investment_id!r}, date={self.position_date})>"
        )


class TransactionHistoryORM(Base):
    """Transaction history records.

    Source: DB2 TRANSACTION_HISTORY table (db2-definitions.sql)
    Transaction types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
    Status codes: P=Processed, F=Failed, R=Reversed
    Transaction ID format: YYYYMMDDHHMMSS + 6-digit sequence
    """

    __tablename__ = "transaction_history"

    transaction_id: str = Column(String(20), primary_key=True)
    portfolio_id: str = Column(
        String(8), ForeignKey("portfolio_master.portfolio_id"), nullable=False
    )
    transaction_date: date = Column(Date, nullable=False)
    transaction_time: time = Column(Time, nullable=False)
    investment_id: str = Column(String(10), nullable=False)
    transaction_type: str = Column(String(2), nullable=False)
    quantity = Column(Numeric(18, 4), nullable=False)
    price = Column(Numeric(18, 4), nullable=False)
    amount = Column(Numeric(18, 2), nullable=False)
    currency_code: str = Column(String(3), nullable=False)
    status: str = Column(String(1), nullable=False)
    process_date: datetime = Column(DateTime(timezone=True), nullable=False)
    process_user: str = Column(String(8), nullable=False)

    # Relationships
    portfolio = relationship("PortfolioMaster", back_populates="transactions")

    __table_args__ = (
        Index("idx_trans_hist_port", "portfolio_id", "transaction_date"),
        Index("idx_trans_hist_date", "transaction_date", "portfolio_id"),
    )

    def __repr__(self) -> str:
        return (
            f"<TransactionHistory(id={self.transaction_id!r}, "
            f"type={self.transaction_type!r}, status={self.status!r})>"
        )


class PositionHistory(Base):
    """Position history records loaded from VSAM to DB2.

    Source: DB2 POSHIST table (DBTBLS copybook)
    Used by: HISTLD00 (batch loader), INQHIST (online inquiry)
    """

    __tablename__ = "position_history"

    account_no: str = Column(String(8), primary_key=True)
    portfolio_id: str = Column(String(10), primary_key=True)
    trans_date: date = Column(Date, primary_key=True)
    trans_time: time = Column(Time, primary_key=True)
    trans_type: str = Column(String(2), nullable=False)
    security_id: str = Column(String(12), nullable=False)
    quantity = Column(Numeric(15, 3), nullable=False)
    price = Column(Numeric(15, 3), nullable=False)
    amount = Column(Numeric(18, 2), nullable=False)
    fees = Column(Numeric(18, 2), nullable=False, default=0)
    total_amount = Column(Numeric(18, 2), nullable=False)
    cost_basis = Column(Numeric(18, 2), nullable=False, default=0)
    gain_loss = Column(Numeric(18, 2), nullable=False, default=0)
    process_date: date = Column(Date, nullable=False)
    process_time: time = Column(Time, nullable=False)
    program_id: str = Column(String(8), nullable=False)
    user_id: str = Column(String(8), nullable=False)
    audit_timestamp: datetime = Column(DateTime(timezone=True), nullable=False)

    __table_args__ = (
        Index("idx_poshist_portfolio", "portfolio_id", "trans_date"),
    )

    def __repr__(self) -> str:
        return (
            f"<PositionHistory(account={self.account_no!r}, "
            f"portfolio={self.portfolio_id!r}, date={self.trans_date})>"
        )


class ErrorLog(Base):
    """Application error log.

    Source: DB2 ERRLOG table (DBTBLS copybook)
    Error types: S=System, A=Application, D=Data
    Severity: 1=Info, 2=Warning, 3=Error, 4=Fatal
    """

    __tablename__ = "error_log"

    error_timestamp: datetime = Column(
        DateTime(timezone=True), primary_key=True
    )
    program_id: str = Column(String(8), primary_key=True)
    error_type: str = Column(String(1), nullable=False)
    error_severity: int = Column(Integer, nullable=False)
    error_code: str = Column(String(8), nullable=False)
    error_message: str = Column(String(200), nullable=False)
    process_date: date = Column(Date, nullable=False)
    process_time: time = Column(Time, nullable=False)
    user_id: str = Column(String(8), nullable=False)
    additional_info: str = Column(String(500), nullable=True)

    __table_args__ = (
        Index("idx_errlog_program", "program_id", "error_timestamp"),
        Index("idx_errlog_severity", "error_severity", "error_timestamp"),
    )

    def __repr__(self) -> str:
        return (
            f"<ErrorLog(program={self.program_id!r}, "
            f"code={self.error_code!r}, severity={self.error_severity})>"
        )


class AuthFile(Base):
    """User authorization records.

    Source: DB2 AUTHFILE (inferred from SECMGR program)
    """

    __tablename__ = "auth_file"

    user_id: str = Column(String(8), primary_key=True)
    resource_name: str = Column(String(50), primary_key=True)
    access_type: str = Column(String(10), nullable=False)
    granted_date: datetime = Column(DateTime(timezone=True), nullable=False)
    granted_by: str = Column(String(8), nullable=False)
    expiry_date: date = Column(Date, nullable=True)
    status: str = Column(String(1), nullable=False, default="A")

    def __repr__(self) -> str:
        return (
            f"<AuthFile(user={self.user_id!r}, "
            f"resource={self.resource_name!r}, access={self.access_type!r})>"
        )


class AuditLog(Base):
    """Security and operational audit trail.

    Source: DB2 AUDITLOG (AUDITLOG copybook + SECMGR program)
    Audit types: TRAN=Transaction, USER=User, SYST=System
    Actions: CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN
    Status: SUCC=Success, FAIL=Failure, WARN=Warning
    """

    __tablename__ = "audit_log"

    audit_timestamp: datetime = Column(
        DateTime(timezone=True), primary_key=True
    )
    system_id: str = Column(String(8), nullable=False)
    user_id: str = Column(String(8), primary_key=True)
    terminal_id: str = Column(String(8), nullable=True)
    trans_id: str = Column(String(4), nullable=True)
    program_id: str = Column(String(8), primary_key=True)
    audit_type: str = Column(String(4), nullable=False)
    action_code: str = Column(String(8), nullable=False)
    status: str = Column(String(4), nullable=False)
    portfolio_id: str = Column(String(8), nullable=True)
    account_no: str = Column(String(10), nullable=True)
    before_image: str = Column(Text, nullable=True)
    after_image: str = Column(Text, nullable=True)
    message: str = Column(String(200), nullable=True)

    __table_args__ = (
        Index("idx_audit_user", "user_id", "audit_timestamp"),
    )

    def __repr__(self) -> str:
        return (
            f"<AuditLog(user={self.user_id!r}, "
            f"action={self.action_code!r}, status={self.status!r})>"
        )


class ReturnCode(Base):
    """Return code logging.

    Source: DB2 RTNCODES table (RTNCODES.sql)
    Status codes: S=Success, W=Warning, E=Error, F=Fatal
    """

    __tablename__ = "return_codes"

    logged_at: datetime = Column(DateTime(timezone=True), primary_key=True)
    program_id: str = Column(String(8), primary_key=True)
    return_code: int = Column(Integer, nullable=False)
    highest_code: int = Column(Integer, nullable=False)
    status_code: str = Column(String(1), nullable=False)
    message_text: str = Column(String(80), nullable=True)

    __table_args__ = (
        Index("idx_rtncodes_program", "program_id", "logged_at"),
        Index("idx_rtncodes_status", "status_code", "logged_at"),
    )

    def __repr__(self) -> str:
        return (
            f"<ReturnCode(program={self.program_id!r}, "
            f"code={self.return_code}, status={self.status_code!r})>"
        )


# =====================================================================
# VSAM File Models (from vsam-definitions.txt)
# =====================================================================


class VSAMPortfolioMaster(Base):
    """Portfolio master records from VSAM PORTMSTR file.

    Source: VSAM KSDS, Record Length: 400, Key Length: 12
    Key: Portfolio ID (8) + Account Type (2) + Branch ID (2)
    Copybook: PORTFLIO
    """

    __tablename__ = "vsam_portfolio_master"

    portfolio_id: str = Column(String(8), primary_key=True)
    account_type: str = Column(String(2), primary_key=True)
    branch_id: str = Column(String(2), primary_key=True)
    account_no: str = Column(String(10), nullable=False)
    client_name: str = Column(String(30), nullable=False)
    client_type: str = Column(String(1), nullable=False)
    create_date: date = Column(Date, nullable=False)
    last_maint_date: datetime = Column(DateTime(timezone=True), nullable=False)
    status: str = Column(String(1), nullable=False, default="A")
    total_value = Column(Numeric(15, 2), nullable=False, default=0)
    cash_balance = Column(Numeric(15, 2), nullable=False, default=0)
    last_user: str = Column(String(8), nullable=True)
    last_trans_id: str = Column(String(20), nullable=True)

    __table_args__ = (
        Index("idx_vsam_portmstr_account", "account_no"),
        Index("idx_vsam_portmstr_client", "client_name"),
        Index("idx_vsam_portmstr_status", "status"),
    )

    def __repr__(self) -> str:
        return (
            f"<VSAMPortfolioMaster(portfolio={self.portfolio_id!r}, "
            f"client={self.client_name!r})>"
        )


class VSAMTransactionHistory(Base):
    """Transaction history records from VSAM TRANHIST file.

    Source: VSAM KSDS, Record Length: 300, Key Length: 20
    Key: Trans Date (8) + Trans Time (6) + Portfolio ID (8) + Seq No (6)
    Copybook: TRNREC
    """

    __tablename__ = "vsam_transaction_history"

    transaction_date: str = Column(String(8), primary_key=True)
    transaction_time: str = Column(String(6), primary_key=True)
    portfolio_id: str = Column(String(8), primary_key=True)
    sequence_no: str = Column(String(6), primary_key=True)
    investment_id: str = Column(String(10), nullable=False)
    transaction_type: str = Column(String(2), nullable=False)
    quantity = Column(Numeric(15, 4), nullable=False)
    price = Column(Numeric(15, 4), nullable=False)
    amount = Column(Numeric(15, 2), nullable=False)
    currency_code: str = Column(String(3), nullable=False, default="USD")
    status: str = Column(String(1), nullable=False, default="P")
    process_date: str = Column(String(26), nullable=True)
    process_user: str = Column(String(8), nullable=True)

    __table_args__ = (
        Index("idx_vsam_tranhist_portfolio", "portfolio_id", "transaction_date"),
        Index("idx_vsam_tranhist_type", "transaction_type", "transaction_date"),
        Index("idx_vsam_tranhist_status", "status"),
    )

    def __repr__(self) -> str:
        return (
            f"<VSAMTransactionHistory(date={self.transaction_date!r}, "
            f"portfolio={self.portfolio_id!r}, type={self.transaction_type!r})>"
        )


class VSAMPositionHistory(Base):
    """Position history records from VSAM POSHIST file.

    Source: VSAM KSDS, Record Length: 350, Key Length: 18
    Key: Portfolio ID (8) + Position Date (8) + Investment ID (10)
    Copybook: POSREC
    """

    __tablename__ = "vsam_position_history"

    portfolio_id: str = Column(String(8), primary_key=True)
    position_date: str = Column(String(8), primary_key=True)
    investment_id: str = Column(String(10), primary_key=True)
    quantity = Column(Numeric(15, 4), nullable=False, default=0)
    cost_basis = Column(Numeric(15, 2), nullable=False, default=0)
    market_value = Column(Numeric(15, 2), nullable=False, default=0)
    currency_code: str = Column(String(3), nullable=False, default="USD")
    status: str = Column(String(1), nullable=False, default="A")
    last_maint_date: str = Column(String(26), nullable=True)
    last_maint_user: str = Column(String(8), nullable=True)

    __table_args__ = (
        Index("idx_vsam_poshist_date", "position_date", "portfolio_id"),
    )

    def __repr__(self) -> str:
        return (
            f"<VSAMPositionHistory(portfolio={self.portfolio_id!r}, "
            f"date={self.position_date!r}, investment={self.investment_id!r})>"
        )


class BatchControl(Base):
    """Batch job control records.

    Source: VSAM BATCH-CONTROL-FILE used by BCHCTL00
    Status codes: P=Pending, R=Running, C=Complete, F=Failed, A=Aborted
    """

    __tablename__ = "batch_control"

    batch_key: str = Column(String(20), primary_key=True)
    job_name: str = Column(String(8), nullable=False)
    step_name: str = Column(String(8), nullable=True)
    status: str = Column(String(1), nullable=False, default="P")
    start_timestamp: datetime = Column(DateTime(timezone=True), nullable=True)
    end_timestamp: datetime = Column(DateTime(timezone=True), nullable=True)
    records_read: int = Column(Integer, nullable=False, default=0)
    records_written: int = Column(Integer, nullable=False, default=0)
    records_error: int = Column(Integer, nullable=False, default=0)
    return_code: int = Column(Integer, nullable=False, default=0)
    last_checkpoint: datetime = Column(DateTime(timezone=True), nullable=True)
    restart_info: str = Column(String(200), nullable=True)

    def __repr__(self) -> str:
        return (
            f"<BatchControl(key={self.batch_key!r}, "
            f"job={self.job_name!r}, status={self.status!r})>"
        )
