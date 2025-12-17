"""
SQLAlchemy ORM models for the COBOL to Python migration.

This module defines SQLAlchemy ORM models that replace the VSAM files
and DB2 tables from the original COBOL system.

VSAM File Replacements:
- PORTMSTR (Portfolio Master) -> PortfolioMaster
- TRANHIST (Transaction History) -> TransactionHistory
- POSHIST (Position History VSAM) -> PositionHistory
- BCHCTL (Batch Control) -> BatchControl

DB2 Table Replacements:
- POSHIST (Position History DB2) -> PositionHistory
- ERRLOG (Error Log) -> ErrorLog
"""

from datetime import datetime
from decimal import Decimal
from typing import Optional

from sqlalchemy import (
    Column, String, Integer, Numeric, DateTime, Text, Index,
    ForeignKey, CheckConstraint, UniqueConstraint
)
from sqlalchemy.orm import declarative_base, relationship

Base = declarative_base()


class PortfolioMaster(Base):
    """
    Portfolio Master table - replaces PORTMSTR VSAM KSDS file.
    
    Original VSAM Definition:
        DEFINE CLUSTER (NAME(PROD.PORTMSTR) -
               KEYS(26 0) -
               RECORDSIZE(500 500) -
               INDEXED)
    
    Primary Key: portfolio_id + position_date + investment_id
    """
    __tablename__ = 'portfolio_master'
    
    # Primary key fields (composite key matching VSAM KEYS)
    id = Column(Integer, primary_key=True, autoincrement=True)
    portfolio_id = Column(String(8), nullable=False, index=True)
    position_date = Column(String(8), nullable=False)  # YYYYMMDD
    investment_id = Column(String(10), nullable=False)
    
    # Position data fields
    quantity = Column(Numeric(15, 4), nullable=False, default=0)
    cost_basis = Column(Numeric(15, 2), nullable=False, default=0)
    market_value = Column(Numeric(15, 2), nullable=False, default=0)
    currency = Column(String(3), nullable=False, default='USD')
    status = Column(String(1), nullable=False, default='A')  # A=Active, C=Closed, P=Pending
    
    # Audit fields
    last_maint_date = Column(DateTime, nullable=True)
    last_maint_user = Column(String(8), nullable=True)
    created_date = Column(DateTime, nullable=False, default=datetime.now)
    
    # Indexes for common access patterns
    __table_args__ = (
        UniqueConstraint('portfolio_id', 'position_date', 'investment_id', 
                         name='uix_portfolio_position'),
        Index('ix_portfolio_date', 'portfolio_id', 'position_date'),
        Index('ix_investment', 'investment_id'),
        CheckConstraint("status IN ('A', 'C', 'P')", name='ck_position_status'),
    )
    
    def __repr__(self):
        return (f"<PortfolioMaster(portfolio_id='{self.portfolio_id}', "
                f"investment_id='{self.investment_id}', quantity={self.quantity})>")
    
    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss."""
        return Decimal(str(self.market_value)) - Decimal(str(self.cost_basis))
    
    @property
    def average_cost(self) -> Decimal:
        """Calculate average cost per unit."""
        if self.quantity == 0:
            return Decimal('0')
        return Decimal(str(self.cost_basis)) / Decimal(str(self.quantity))


class TransactionHistory(Base):
    """
    Transaction History table - replaces TRANHIST VSAM KSDS file.
    
    Original VSAM Definition:
        DEFINE CLUSTER (NAME(PROD.TRANHIST) -
               KEYS(28 0) -
               RECORDSIZE(200 200) -
               INDEXED)
    
    Primary Key: trans_date + trans_time + portfolio_id + sequence_no
    """
    __tablename__ = 'transaction_history'
    
    # Primary key fields (composite key matching VSAM KEYS)
    id = Column(Integer, primary_key=True, autoincrement=True)
    trans_date = Column(String(8), nullable=False)  # YYYYMMDD
    trans_time = Column(String(6), nullable=False)  # HHMMSS
    portfolio_id = Column(String(8), nullable=False, index=True)
    sequence_no = Column(String(6), nullable=False)
    
    # Transaction data fields
    investment_id = Column(String(10), nullable=False)
    trans_type = Column(String(2), nullable=False)  # BU=Buy, SL=Sell, TR=Transfer, FE=Fee
    quantity = Column(Numeric(15, 4), nullable=False)
    price = Column(Numeric(15, 4), nullable=False)
    amount = Column(Numeric(15, 2), nullable=False)
    currency = Column(String(3), nullable=False, default='USD')
    status = Column(String(1), nullable=False, default='P')  # P=Pending, D=Done, F=Failed, R=Reversed
    
    # Audit fields
    process_date = Column(DateTime, nullable=True)
    process_user = Column(String(8), nullable=True)
    created_date = Column(DateTime, nullable=False, default=datetime.now)
    
    # Validation fields
    validation_status = Column(String(1), nullable=True)  # V=Valid, I=Invalid
    validation_errors = Column(Text, nullable=True)
    
    # Indexes for common access patterns
    __table_args__ = (
        UniqueConstraint('trans_date', 'trans_time', 'portfolio_id', 'sequence_no',
                         name='uix_transaction_key'),
        Index('ix_trans_portfolio', 'portfolio_id', 'trans_date'),
        Index('ix_trans_investment', 'investment_id'),
        Index('ix_trans_status', 'status'),
        CheckConstraint("trans_type IN ('BU', 'SL', 'TR', 'FE')", name='ck_trans_type'),
        CheckConstraint("status IN ('P', 'D', 'F', 'R')", name='ck_trans_status'),
    )
    
    def __repr__(self):
        return (f"<TransactionHistory(portfolio_id='{self.portfolio_id}', "
                f"trans_type='{self.trans_type}', amount={self.amount})>")


class PositionHistory(Base):
    """
    Position History table - replaces POSHIST DB2 table.
    
    Original DB2 Definition (from POSHIST.sql):
        CREATE TABLE PORTFOLIO.POSHIST (
            PORTFOLIO_ID    CHAR(8) NOT NULL,
            POSITION_DATE   DATE NOT NULL,
            INVESTMENT_ID   CHAR(10) NOT NULL,
            ...
        ) PARTITION BY RANGE (POSITION_DATE)
    
    This table stores historical position snapshots for reporting and analysis.
    """
    __tablename__ = 'position_history'
    
    # Primary key fields
    id = Column(Integer, primary_key=True, autoincrement=True)
    portfolio_id = Column(String(8), nullable=False, index=True)
    position_date = Column(String(8), nullable=False)  # YYYYMMDD
    investment_id = Column(String(10), nullable=False)
    
    # Position data fields
    quantity = Column(Numeric(15, 4), nullable=False)
    cost_basis = Column(Numeric(15, 2), nullable=False)
    market_value = Column(Numeric(15, 2), nullable=False)
    currency = Column(String(3), nullable=False, default='USD')
    
    # Calculated fields (stored for reporting efficiency)
    unrealized_gain_loss = Column(Numeric(15, 2), nullable=True)
    unrealized_gain_loss_pct = Column(Numeric(7, 4), nullable=True)
    
    # History tracking fields
    record_type = Column(String(2), nullable=False, default='PS')  # PS=Position
    action_code = Column(String(1), nullable=False, default='A')  # A=Add, C=Change, D=Delete
    reason_code = Column(String(4), nullable=True)
    
    # Before/after images for audit trail (JSON stored as text)
    before_image = Column(Text, nullable=True)
    after_image = Column(Text, nullable=True)
    
    # Audit fields
    process_date = Column(DateTime, nullable=False, default=datetime.now)
    process_user = Column(String(8), nullable=True)
    batch_id = Column(String(20), nullable=True)
    
    # Indexes for common access patterns
    __table_args__ = (
        Index('ix_poshist_portfolio_date', 'portfolio_id', 'position_date'),
        Index('ix_poshist_investment', 'investment_id'),
        Index('ix_poshist_process_date', 'process_date'),
        CheckConstraint("record_type IN ('PT', 'PS', 'TR')", name='ck_hist_record_type'),
        CheckConstraint("action_code IN ('A', 'C', 'D')", name='ck_hist_action_code'),
    )
    
    def __repr__(self):
        return (f"<PositionHistory(portfolio_id='{self.portfolio_id}', "
                f"position_date='{self.position_date}', action='{self.action_code}')>")


class BatchControl(Base):
    """
    Batch Control table - replaces BCHCTL VSAM file.
    
    Original VSAM Definition:
        DEFINE CLUSTER (NAME(PROD.BCHCTL) -
               KEYS(20 0) -
               RECORDSIZE(300 300) -
               INDEXED)
    
    This table manages batch job execution, dependencies, and checkpoint/restart.
    """
    __tablename__ = 'batch_control'
    
    # Primary key fields
    id = Column(Integer, primary_key=True, autoincrement=True)
    job_name = Column(String(8), nullable=False)
    process_date = Column(String(8), nullable=False)  # YYYYMMDD
    sequence_no = Column(Integer, nullable=False, default=1)
    
    # Status and control fields
    status = Column(String(1), nullable=False, default='R')  # R=Ready, A=Active, W=Waiting, D=Done, E=Error
    step_name = Column(String(8), nullable=True)
    program_name = Column(String(8), nullable=True)
    
    # Timing fields
    start_time = Column(DateTime, nullable=True)
    end_time = Column(DateTime, nullable=True)
    
    # Return information
    return_code = Column(Integer, nullable=False, default=0)
    error_desc = Column(String(80), nullable=True)
    
    # Statistics
    restart_count = Column(Integer, nullable=False, default=0)
    records_read = Column(Integer, nullable=True)
    records_written = Column(Integer, nullable=True)
    records_rejected = Column(Integer, nullable=True)
    
    # Prerequisites (stored as JSON text)
    prerequisites = Column(Text, nullable=True)
    
    # Checkpoint data (stored as JSON text for restart capability)
    checkpoint_data = Column(Text, nullable=True)
    
    # Audit fields
    created_date = Column(DateTime, nullable=False, default=datetime.now)
    updated_date = Column(DateTime, nullable=True)
    
    # Indexes for common access patterns
    __table_args__ = (
        UniqueConstraint('job_name', 'process_date', 'sequence_no',
                         name='uix_batch_control_key'),
        Index('ix_batch_status', 'status'),
        Index('ix_batch_date', 'process_date'),
        CheckConstraint("status IN ('R', 'A', 'W', 'D', 'E')", name='ck_batch_status'),
    )
    
    def __repr__(self):
        return (f"<BatchControl(job_name='{self.job_name}', "
                f"process_date='{self.process_date}', status='{self.status}')>")


class ErrorLog(Base):
    """
    Error Log table - replaces ERRLOG DB2 table.
    
    Original DB2 Definition (from DBTBLS.cpy):
        01  ERRLOG-RECORD.
            05  ERR-TIMESTAMP     PIC X(26).
            05  ERR-PROGRAM       PIC X(08).
            05  ERR-SEVERITY      PIC X(01).
            05  ERR-CODE          PIC X(08).
            05  ERR-MESSAGE       PIC X(200).
            05  ERR-CONTEXT       PIC X(500).
    
    This table stores error information for debugging and audit purposes.
    """
    __tablename__ = 'error_log'
    
    # Primary key
    id = Column(Integer, primary_key=True, autoincrement=True)
    
    # Error identification
    error_timestamp = Column(DateTime, nullable=False, default=datetime.now)
    program_name = Column(String(8), nullable=False)
    severity = Column(String(1), nullable=False, default='E')  # I=Info, W=Warning, E=Error, S=Severe
    error_code = Column(String(8), nullable=False)
    
    # Error details
    error_message = Column(String(200), nullable=False)
    error_context = Column(Text, nullable=True)  # Additional context (JSON)
    
    # Execution context
    job_name = Column(String(8), nullable=True)
    step_name = Column(String(8), nullable=True)
    process_date = Column(String(8), nullable=True)
    
    # User context
    user_id = Column(String(8), nullable=True)
    terminal_id = Column(String(4), nullable=True)
    
    # Resolution tracking
    resolved = Column(String(1), nullable=False, default='N')  # Y=Yes, N=No
    resolved_date = Column(DateTime, nullable=True)
    resolved_by = Column(String(8), nullable=True)
    resolution_notes = Column(Text, nullable=True)
    
    # Indexes for common access patterns
    __table_args__ = (
        Index('ix_error_timestamp', 'error_timestamp'),
        Index('ix_error_program', 'program_name'),
        Index('ix_error_severity', 'severity'),
        Index('ix_error_code', 'error_code'),
        Index('ix_error_job', 'job_name', 'process_date'),
        CheckConstraint("severity IN ('I', 'W', 'E', 'S')", name='ck_error_severity'),
        CheckConstraint("resolved IN ('Y', 'N')", name='ck_error_resolved'),
    )
    
    def __repr__(self):
        return (f"<ErrorLog(program='{self.program_name}', "
                f"code='{self.error_code}', severity='{self.severity}')>")
