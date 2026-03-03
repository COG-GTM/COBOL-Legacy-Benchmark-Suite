"""SQLAlchemy ORM table definitions.

Migrated from:
- src/database/db2/db2-definitions.sql (DB2 tables)
- src/database/vsam/vsam-definitions.txt (VSAM files)

All VSAM KSDS files are replaced with PostgreSQL tables using
composite primary keys matching the original key structures.
"""

from sqlalchemy import (
    CHAR,
    DECIMAL,
    DATE,
    INTEGER,
    TIMESTAMP,
    VARCHAR,
    Column,
    Index,
    MetaData,
    PrimaryKeyConstraint,
    Text,
)
from sqlalchemy.orm import DeclarativeBase

metadata = MetaData()


class Base(DeclarativeBase):
    """Base class for all ORM models."""

    metadata = metadata


# ---------------------------------------------------------------------------
# DB2 Table Migrations (from db2-definitions.sql)
# ---------------------------------------------------------------------------


class PortfolioMaster(Base):
    """PORTFOLIO_MASTER table - migrated from DB2.

    Original DB2 DDL: CREATE TABLE PORTFOLIO_MASTER (...)
    Also replaces VSAM PORTMSTR (KSDS, key=12 bytes: PORT-ID + ACCOUNT-NO).
    """

    __tablename__ = "portfolio_master"

    portfolio_id = Column(CHAR(8), nullable=False)
    account_type = Column(CHAR(2), nullable=False)
    branch_id = Column(CHAR(2), nullable=False)
    client_id = Column(CHAR(10), nullable=False)
    portfolio_name = Column(VARCHAR(50), nullable=False)
    currency_code = Column(CHAR(3), nullable=False)
    risk_level = Column(CHAR(1), nullable=False)
    status = Column(CHAR(1), nullable=False)
    open_date = Column(DATE, nullable=False)
    close_date = Column(DATE, nullable=True)
    last_maint_date = Column(TIMESTAMP, nullable=False)
    last_maint_user = Column(VARCHAR(8), nullable=False)

    __table_args__ = (PrimaryKeyConstraint("portfolio_id", name="pk_portfolio_master"),)


class InvestmentPositions(Base):
    """INVESTMENT_POSITIONS table - migrated from DB2.

    Original DB2 DDL: CREATE TABLE INVESTMENT_POSITIONS (...)
    Also replaces VSAM POSHIST (KSDS, key=18 bytes: portfolio+date+investment).
    Composite PK matches VSAM key structure.
    """

    __tablename__ = "investment_positions"

    portfolio_id = Column(CHAR(8), nullable=False)
    investment_id = Column(CHAR(10), nullable=False)
    position_date = Column(DATE, nullable=False)
    quantity = Column(DECIMAL(18, 4), nullable=False)
    cost_basis = Column(DECIMAL(18, 2), nullable=False)
    market_value = Column(DECIMAL(18, 2), nullable=False)
    currency_code = Column(CHAR(3), nullable=False)
    last_maint_date = Column(TIMESTAMP, nullable=False)
    last_maint_user = Column(VARCHAR(8), nullable=False)

    __table_args__ = (
        PrimaryKeyConstraint("portfolio_id", "investment_id", "position_date", name="pk_investment_positions"),
    )


class TransactionHistory(Base):
    """TRANSACTION_HISTORY table - migrated from DB2.

    Original DB2 DDL: CREATE TABLE TRANSACTION_HISTORY (...)
    Also replaces VSAM TRANHIST (KSDS, key=20 bytes).
    """

    __tablename__ = "transaction_history"

    portfolio_id = Column(CHAR(8), nullable=False)
    investment_id = Column(CHAR(10), nullable=False)
    trans_date = Column(DATE, nullable=False)
    trans_time = Column(CHAR(6), nullable=False)
    trans_type = Column(CHAR(2), nullable=False)
    quantity = Column(DECIMAL(18, 4), nullable=False)
    price = Column(DECIMAL(18, 4), nullable=False)
    amount = Column(DECIMAL(18, 2), nullable=False)
    fees = Column(DECIMAL(18, 2), nullable=False, default=0)
    total_amount = Column(DECIMAL(18, 2), nullable=False)
    cost_basis = Column(DECIMAL(18, 2), nullable=False, default=0)
    gain_loss = Column(DECIMAL(18, 2), nullable=False, default=0)
    currency_code = Column(CHAR(3), nullable=False)
    status = Column(CHAR(1), nullable=False)
    last_maint_date = Column(TIMESTAMP, nullable=False)
    last_maint_user = Column(VARCHAR(8), nullable=False)

    __table_args__ = (
        PrimaryKeyConstraint(
            "portfolio_id", "investment_id", "trans_date", "trans_time", name="pk_transaction_history"
        ),
        Index("idx_trans_portfolio", "portfolio_id"),
        Index("idx_trans_date", "trans_date"),
    )


class PositionHistory(Base):
    """POSHIST table - DB2 table loaded by HISTLD00.

    Maps to the DB2 INSERT in HISTLD00 2200-LOAD-TO-DB2.
    """

    __tablename__ = "poshist"

    account_no = Column(CHAR(10), nullable=False)
    portfolio_id = Column(CHAR(8), nullable=False)
    trans_date = Column(DATE, nullable=False)
    trans_time = Column(CHAR(8), nullable=False)
    trans_type = Column(CHAR(4), nullable=False)
    security_id = Column(CHAR(10), nullable=False)
    quantity = Column(DECIMAL(18, 4), nullable=False)
    price = Column(DECIMAL(18, 4), nullable=False)
    amount = Column(DECIMAL(18, 2), nullable=False)
    fees = Column(DECIMAL(18, 2), nullable=False, default=0)
    total_amount = Column(DECIMAL(18, 2), nullable=False, default=0)
    cost_basis = Column(DECIMAL(18, 2), nullable=False, default=0)
    gain_loss = Column(DECIMAL(18, 2), nullable=False, default=0)

    __table_args__ = (
        PrimaryKeyConstraint("account_no", "portfolio_id", "trans_date", "trans_time", name="pk_poshist"),
    )


class ErrorLog(Base):
    """ERRLOG table - used by ERRHNDL and DB2ERR for error logging.

    Maps to the DB2 INSERT in ERRHNDL P200-LOG-ERROR and
    DB2ERR 1200-INSERT-ERROR.
    """

    __tablename__ = "errlog"

    error_timestamp = Column(TIMESTAMP, nullable=False)
    program_id = Column(CHAR(8), nullable=False)
    error_type = Column(CHAR(1), nullable=True, default="D")
    error_code = Column(VARCHAR(30), nullable=True)
    error_severity = Column(INTEGER, nullable=True, default=1)
    error_message = Column(VARCHAR(80), nullable=True)
    process_date = Column(CHAR(10), nullable=True)
    process_time = Column(CHAR(8), nullable=True)
    user_id = Column(VARCHAR(8), nullable=True)
    additional_info = Column(VARCHAR(100), nullable=True)
    paragraph = Column(VARCHAR(30), nullable=True)
    sqlcode = Column(INTEGER, nullable=True, default=0)
    cics_resp = Column(INTEGER, nullable=True, default=0)
    severity_char = Column(CHAR(1), nullable=True)
    log_message = Column(VARCHAR(80), nullable=True)
    trace_id = Column(VARCHAR(16), nullable=True)

    __table_args__ = (
        PrimaryKeyConstraint("error_timestamp", "program_id", name="pk_errlog"),
        Index("idx_errlog_program", "program_id"),
    )


class AuditLog(Base):
    """AUDITLOG table - used by SECMGR P300-LOG-ACCESS and AUDPROC.

    Maps to the DB2 INSERT in SECMGR P300-LOG-ACCESS.
    """

    __tablename__ = "auditlog"

    audit_timestamp = Column(TIMESTAMP, nullable=False)
    user_id = Column(VARCHAR(8), nullable=False)
    terminal_id = Column(VARCHAR(4), nullable=True)
    trans_id = Column(VARCHAR(4), nullable=True)
    program = Column(VARCHAR(8), nullable=True)
    access_type = Column(VARCHAR(8), nullable=True)
    portfolio_id = Column(CHAR(8), nullable=True)
    account_no = Column(CHAR(10), nullable=True)
    action = Column(VARCHAR(8), nullable=True)
    status = Column(VARCHAR(4), nullable=True)
    before_image = Column(Text, nullable=True)
    after_image = Column(Text, nullable=True)
    message = Column(VARCHAR(100), nullable=True)

    __table_args__ = (
        PrimaryKeyConstraint("audit_timestamp", "user_id", name="pk_auditlog"),
        Index("idx_auditlog_user", "user_id"),
    )


class AuthFile(Base):
    """AUTHFILE table - used by SECMGR P200-CHECK-AUTH for access control.

    Maps to the DB2 SELECT in SECMGR P200-CHECK-AUTH.
    """

    __tablename__ = "authfile"

    user_id = Column(VARCHAR(8), nullable=False)
    resource = Column(VARCHAR(8), nullable=False)
    access_type = Column(VARCHAR(8), nullable=False)

    __table_args__ = (PrimaryKeyConstraint("user_id", "resource", "access_type", name="pk_authfile"),)


class ReturnCodes(Base):
    """RTNCODES table - used by RTNCDE00 for return code logging.

    Maps to the DB2 INSERT in RTNCDE00 P400-LOG-RETURN-CODE.
    """

    __tablename__ = "rtncodes"

    log_timestamp = Column(TIMESTAMP, nullable=False)
    program_id = Column(CHAR(8), nullable=False)
    return_code = Column(INTEGER, nullable=False)
    highest_code = Column(INTEGER, nullable=False)
    status_code = Column(CHAR(1), nullable=False)
    message_text = Column(VARCHAR(80), nullable=True)

    __table_args__ = (
        PrimaryKeyConstraint("log_timestamp", "program_id", name="pk_rtncodes"),
        Index("idx_rtncodes_program", "program_id"),
    )


# ---------------------------------------------------------------------------
# Job State Table (replaces checkpoint/restart framework)
# ---------------------------------------------------------------------------


class JobState(Base):
    """Job state tracking table - replaces COBOL checkpoint/restart framework.

    Replaces VSAM BCHCTL file and CKPRST checkpoint file with a database
    table for tracking batch job state, progress, and restart capability.
    """

    __tablename__ = "job_state"

    job_name = Column(VARCHAR(8), nullable=False)
    process_date = Column(CHAR(8), nullable=False)
    sequence_no = Column(INTEGER, nullable=False, default=0)
    status = Column(CHAR(1), nullable=False, default="R")
    step_name = Column(VARCHAR(8), nullable=True)
    program_name = Column(VARCHAR(8), nullable=True)
    start_time = Column(TIMESTAMP, nullable=True)
    end_time = Column(TIMESTAMP, nullable=True)
    return_code = Column(INTEGER, nullable=True, default=0)
    records_read = Column(INTEGER, nullable=True, default=0)
    records_processed = Column(INTEGER, nullable=True, default=0)
    records_error = Column(INTEGER, nullable=True, default=0)
    last_key = Column(VARCHAR(50), nullable=True)
    phase = Column(CHAR(2), nullable=True, default="00")
    restart_count = Column(INTEGER, nullable=True, default=0)
    error_desc = Column(VARCHAR(256), nullable=True)

    __table_args__ = (
        PrimaryKeyConstraint("job_name", "process_date", "sequence_no", name="pk_job_state"),
    )
