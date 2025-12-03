"""
SQLAlchemy Database Models

Corresponds to DB2 tables defined in DBTBLS.cpy:
- POSHIST (Position History)
- ERRLOG (Error Log)

Additional tables for batch control:
- BATCH_CONTROL
- CHECKPOINT
"""

from datetime import datetime
from decimal import Decimal
from typing import Optional

from sqlalchemy import (
    Column,
    DateTime,
    Integer,
    Numeric,
    String,
    Text,
    Index,
    UniqueConstraint,
)
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class PositionHistory(Base):
    """
    Position History table.
    
    Corresponds to POSHIST in DBTBLS.cpy and DB2 table definition.
    Stores historical position and transaction data for reporting.
    """
    __tablename__ = "poshist"

    id = Column(Integer, primary_key=True, autoincrement=True)
    account_no = Column(String(8), nullable=False, index=True)
    portfolio_id = Column(String(10), nullable=False, index=True)
    trans_date = Column(String(10), nullable=False)
    trans_time = Column(String(8), nullable=False)
    trans_type = Column(String(2), nullable=False)
    security_id = Column(String(12), nullable=False)
    quantity = Column(Numeric(15, 3), nullable=False, default=Decimal("0"))
    price = Column(Numeric(15, 3), nullable=False, default=Decimal("0"))
    amount = Column(Numeric(15, 2), nullable=False, default=Decimal("0"))
    fees = Column(Numeric(15, 2), nullable=False, default=Decimal("0"))
    total_amount = Column(Numeric(15, 2), nullable=False, default=Decimal("0"))
    cost_basis = Column(Numeric(15, 2), nullable=False, default=Decimal("0"))
    gain_loss = Column(Numeric(15, 2), nullable=False, default=Decimal("0"))
    process_date = Column(String(10), nullable=False)
    process_time = Column(String(8), nullable=False)
    program_id = Column(String(8), nullable=False)
    user_id = Column(String(8), nullable=False)
    audit_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        Index("ix_poshist_account_portfolio", "account_no", "portfolio_id"),
        Index("ix_poshist_trans_date", "trans_date"),
        UniqueConstraint(
            "account_no", "portfolio_id", "trans_date", "trans_time",
            name="uq_poshist_key"
        ),
    )

    def __repr__(self) -> str:
        return (
            f"<PositionHistory(account_no={self.account_no}, "
            f"portfolio_id={self.portfolio_id}, trans_date={self.trans_date})>"
        )

    @classmethod
    def from_history_record(
        cls,
        account_no: str,
        portfolio_id: str,
        trans_date: str,
        trans_time: str,
        trans_type: str,
        security_id: str,
        quantity: Decimal,
        price: Decimal,
        amount: Decimal,
        fees: Decimal = Decimal("0"),
        cost_basis: Decimal = Decimal("0"),
        gain_loss: Decimal = Decimal("0"),
        program_id: str = "HISTLD00",
        user_id: str = "SYSTEM",
    ) -> "PositionHistory":
        """Create a PositionHistory record from transaction data."""
        now = datetime.utcnow()
        total_amount = amount + fees
        return cls(
            account_no=account_no,
            portfolio_id=portfolio_id,
            trans_date=trans_date,
            trans_time=trans_time,
            trans_type=trans_type,
            security_id=security_id,
            quantity=quantity,
            price=price,
            amount=amount,
            fees=fees,
            total_amount=total_amount,
            cost_basis=cost_basis,
            gain_loss=gain_loss,
            process_date=now.strftime("%Y-%m-%d"),
            process_time=now.strftime("%H:%M:%S"),
            program_id=program_id,
            user_id=user_id,
            audit_timestamp=now,
        )


class ErrorLog(Base):
    """
    Error Log table.
    
    Corresponds to ERRLOG in DBTBLS.cpy and DB2 table definition.
    Stores error information for audit and troubleshooting.
    """
    __tablename__ = "errlog"

    id = Column(Integer, primary_key=True, autoincrement=True)
    error_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)
    program_id = Column(String(8), nullable=False, index=True)
    error_type = Column(String(1), nullable=False)
    error_severity = Column(Integer, nullable=False)
    error_code = Column(String(8), nullable=False)
    error_message = Column(String(200), nullable=False)
    process_date = Column(String(10), nullable=False)
    process_time = Column(String(8), nullable=False)
    user_id = Column(String(8), nullable=False)
    additional_info = Column(Text, nullable=True)

    __table_args__ = (
        Index("ix_errlog_program_timestamp", "program_id", "error_timestamp"),
    )

    def __repr__(self) -> str:
        return (
            f"<ErrorLog(program_id={self.program_id}, "
            f"error_code={self.error_code}, timestamp={self.error_timestamp})>"
        )

    @classmethod
    def create_error(
        cls,
        program_id: str,
        error_type: str,
        error_severity: int,
        error_code: str,
        error_message: str,
        user_id: str = "SYSTEM",
        additional_info: Optional[str] = None,
    ) -> "ErrorLog":
        """Create an ErrorLog record."""
        now = datetime.utcnow()
        return cls(
            error_timestamp=now,
            program_id=program_id,
            error_type=error_type,
            error_severity=error_severity,
            error_code=error_code,
            error_message=error_message[:200],
            process_date=now.strftime("%Y-%m-%d"),
            process_time=now.strftime("%H:%M:%S"),
            user_id=user_id,
            additional_info=additional_info,
        )


class BatchControl(Base):
    """
    Batch Control table.
    
    Replaces VSAM BCHCTL file with relational database storage.
    Stores job-level control and process sequencing information.
    """
    __tablename__ = "batch_control"

    id = Column(Integer, primary_key=True, autoincrement=True)
    job_name = Column(String(8), nullable=False, index=True)
    process_date = Column(String(8), nullable=False, index=True)
    sequence_no = Column(Integer, nullable=False, default=0)
    status = Column(String(1), nullable=False, default="R")
    step_name = Column(String(8), nullable=True)
    program_name = Column(String(8), nullable=True)
    start_time = Column(DateTime, nullable=True)
    end_time = Column(DateTime, nullable=True)
    return_code = Column(Integer, nullable=False, default=0)
    error_desc = Column(String(80), nullable=True)
    restart_count = Column(Integer, nullable=False, default=0)
    records_read = Column(Integer, nullable=False, default=0)
    records_written = Column(Integer, nullable=False, default=0)
    records_error = Column(Integer, nullable=False, default=0)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("job_name", "process_date", "sequence_no", name="uq_batch_control_key"),
        Index("ix_batch_control_status", "status"),
    )

    def __repr__(self) -> str:
        return (
            f"<BatchControl(job_name={self.job_name}, "
            f"process_date={self.process_date}, status={self.status})>"
        )


class Checkpoint(Base):
    """
    Checkpoint table.
    
    Replaces VSAM checkpoint file with relational database storage.
    Stores checkpoint/restart information for batch programs.
    """
    __tablename__ = "checkpoint"

    id = Column(Integer, primary_key=True, autoincrement=True)
    program_id = Column(String(8), nullable=False, index=True)
    run_date = Column(String(8), nullable=False)
    run_time = Column(String(6), nullable=False)
    status = Column(String(1), nullable=False, default="I")
    phase = Column(String(2), nullable=False, default="00")
    records_read = Column(Integer, nullable=False, default=0)
    records_processed = Column(Integer, nullable=False, default=0)
    records_error = Column(Integer, nullable=False, default=0)
    restart_count = Column(Integer, nullable=False, default=0)
    last_key = Column(String(50), nullable=True)
    last_checkpoint_time = Column(DateTime, nullable=True)
    commit_freq = Column(Integer, nullable=False, default=1000)
    max_errors = Column(Integer, nullable=False, default=100)
    max_restarts = Column(Integer, nullable=False, default=3)
    restart_mode = Column(String(1), nullable=False, default="N")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("program_id", "run_date", "run_time", name="uq_checkpoint_key"),
        Index("ix_checkpoint_program_date", "program_id", "run_date"),
    )

    def __repr__(self) -> str:
        return (
            f"<Checkpoint(program_id={self.program_id}, "
            f"run_date={self.run_date}, status={self.status})>"
        )
