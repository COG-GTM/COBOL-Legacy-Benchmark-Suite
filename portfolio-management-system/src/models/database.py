"""
SQLAlchemy ORM Models for Database Tables

This module contains SQLAlchemy ORM models for all database tables
migrated from DB2 and VSAM to PostgreSQL.

Tables included:
- portfolio_master: Portfolio Master (from VSAM PORTMSTR and DB2 PORTFOLIO_MASTER)
- investment_positions: Investment Positions (from VSAM POSHIST and DB2 INVESTMENT_POSITIONS)
- transaction_history: Transaction History (from VSAM TRANHIST and DB2 TRANSACTION_HISTORY)
- position_history: Position History (from DB2 POSHIST)
- error_log: Error Log (from DB2 ERRLOG)
- auth_permissions: Authentication Permissions (from AUTHFILE)
- audit_log: Audit Log (from AUDITLOG)
"""

from datetime import datetime, date, time
from decimal import Decimal
from typing import Optional, List
from sqlalchemy import (
    Column, String, Date, Time, Numeric, DateTime, Integer, Text,
    ForeignKey, Index, CheckConstraint
)
from sqlalchemy.orm import declarative_base, relationship

Base = declarative_base()


class PortfolioMaster(Base):
    """
    Portfolio Master Table.
    
    Migrated from:
    - VSAM: PORTMSTR (Key: Portfolio ID + Account Type + Branch ID)
    - DB2: PORTFOLIO_MASTER
    """
    __tablename__ = "portfolio_master"

    portfolio_id = Column(String(8), primary_key=True, nullable=False)
    account_type = Column(String(2), nullable=False)
    branch_id = Column(String(2), nullable=False)
    client_id = Column(String(10), nullable=False)
    portfolio_name = Column(String(50), nullable=False)
    currency_code = Column(String(3), nullable=False, default="USD")
    risk_level = Column(String(1), nullable=False, default="M")
    status = Column(String(1), nullable=False, default="A")
    open_date = Column(Date, nullable=False)
    close_date = Column(Date, nullable=True)
    last_maint_date = Column(DateTime, nullable=False, default=datetime.utcnow)
    last_maint_user = Column(String(8), nullable=False)

    positions = relationship("InvestmentPosition", back_populates="portfolio")
    transactions = relationship("TransactionHistory", back_populates="portfolio")

    __table_args__ = (
        Index("idx_portfolio_master_client", "client_id", "status"),
        Index("idx_portfolio_master_branch", "branch_id", "account_type"),
        CheckConstraint("status IN ('A', 'C', 'S')", name="chk_portfolio_status"),
        CheckConstraint("risk_level IN ('L', 'M', 'H')", name="chk_risk_level"),
    )

    def __repr__(self) -> str:
        return f"<PortfolioMaster(id={self.portfolio_id}, name={self.portfolio_name}, status={self.status})>"

    @property
    def is_active(self) -> bool:
        return self.status == "A" and (self.close_date is None or self.close_date > date.today())


class InvestmentPosition(Base):
    """
    Investment Positions Table.
    
    Migrated from:
    - VSAM: POSHIST (Key: Portfolio ID + Position Date + Investment ID)
    - DB2: INVESTMENT_POSITIONS
    """
    __tablename__ = "investment_positions"

    portfolio_id = Column(String(8), ForeignKey("portfolio_master.portfolio_id"), primary_key=True, nullable=False)
    investment_id = Column(String(10), primary_key=True, nullable=False)
    position_date = Column(Date, primary_key=True, nullable=False)
    quantity = Column(Numeric(18, 4), nullable=False)
    cost_basis = Column(Numeric(18, 2), nullable=False)
    market_value = Column(Numeric(18, 2), nullable=False)
    currency_code = Column(String(3), nullable=False, default="USD")
    status = Column(String(1), nullable=False, default="A")
    last_maint_date = Column(DateTime, nullable=False, default=datetime.utcnow)
    last_maint_user = Column(String(8), nullable=False)

    portfolio = relationship("PortfolioMaster", back_populates="positions")

    __table_args__ = (
        Index("idx_positions_date", "position_date", "portfolio_id"),
        Index("idx_positions_investment", "investment_id", "position_date"),
        CheckConstraint("status IN ('A', 'C', 'P')", name="chk_position_status"),
    )

    def __repr__(self) -> str:
        return f"<InvestmentPosition(portfolio={self.portfolio_id}, investment={self.investment_id}, qty={self.quantity})>"

    @property
    def unrealized_gain_loss(self) -> Decimal:
        return Decimal(str(self.market_value)) - Decimal(str(self.cost_basis))

    @property
    def gain_loss_percentage(self) -> Decimal:
        if self.cost_basis == 0:
            return Decimal("0")
        return (self.unrealized_gain_loss / Decimal(str(self.cost_basis))) * 100


class TransactionHistory(Base):
    """
    Transaction History Table.
    
    Migrated from:
    - VSAM: TRANHIST (Key: Date + Time + Portfolio ID + Sequence)
    - DB2: TRANSACTION_HISTORY
    """
    __tablename__ = "transaction_history"

    transaction_id = Column(String(20), primary_key=True, nullable=False)
    portfolio_id = Column(String(8), ForeignKey("portfolio_master.portfolio_id"), nullable=False)
    transaction_date = Column(Date, nullable=False)
    transaction_time = Column(Time, nullable=False)
    investment_id = Column(String(10), nullable=False)
    transaction_type = Column(String(2), nullable=False)
    quantity = Column(Numeric(18, 4), nullable=False)
    price = Column(Numeric(18, 4), nullable=False)
    amount = Column(Numeric(18, 2), nullable=False)
    currency_code = Column(String(3), nullable=False, default="USD")
    status = Column(String(1), nullable=False, default="P")
    process_date = Column(DateTime, nullable=False, default=datetime.utcnow)
    process_user = Column(String(8), nullable=False)

    portfolio = relationship("PortfolioMaster", back_populates="transactions")

    __table_args__ = (
        Index("idx_trans_hist_portfolio", "portfolio_id", "transaction_date"),
        Index("idx_trans_hist_date", "transaction_date", "portfolio_id"),
        Index("idx_trans_hist_investment", "investment_id", "transaction_date"),
        CheckConstraint("transaction_type IN ('BU', 'SL', 'TR', 'FE')", name="chk_trans_type"),
        CheckConstraint("status IN ('P', 'D', 'F', 'R')", name="chk_trans_status"),
    )

    def __repr__(self) -> str:
        return f"<TransactionHistory(id={self.transaction_id}, type={self.transaction_type}, amount={self.amount})>"

    @property
    def is_buy(self) -> bool:
        return self.transaction_type == "BU"

    @property
    def is_sell(self) -> bool:
        return self.transaction_type == "SL"


class PositionHistory(Base):
    """
    Position History Table (Partitioned).
    
    Migrated from DB2 POSHIST table.
    Partitioned by trans_date (quarterly).
    """
    __tablename__ = "position_history"

    account_no = Column(String(8), primary_key=True, nullable=False)
    portfolio_id = Column(String(10), primary_key=True, nullable=False)
    trans_date = Column(Date, primary_key=True, nullable=False)
    trans_time = Column(Time, primary_key=True, nullable=False)
    trans_type = Column(String(2), nullable=False)
    security_id = Column(String(12), nullable=False)
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
    audit_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        Index("idx_poshist_security", "security_id", "trans_date"),
        Index("idx_poshist_process", "process_date", "program_id"),
        CheckConstraint("trans_type IN ('BU', 'SL', 'TR')", name="chk_poshist_trans_type"),
    )

    def __repr__(self) -> str:
        return f"<PositionHistory(account={self.account_no}, portfolio={self.portfolio_id}, date={self.trans_date})>"


class ErrorLog(Base):
    """
    Error Log Table.
    
    Migrated from DB2 ERRLOG table.
    """
    __tablename__ = "error_log"

    id = Column(Integer, primary_key=True, autoincrement=True)
    error_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    program_id = Column(String(8), nullable=False)
    error_type = Column(String(1), nullable=False)
    error_severity = Column(Integer, nullable=False)
    error_code = Column(String(8), nullable=False)
    error_message = Column(String(200), nullable=False)
    process_date = Column(Date, nullable=False, default=date.today)
    process_time = Column(Time, nullable=False)
    user_id = Column(String(8), nullable=False)
    additional_info = Column(String(500), nullable=True)

    __table_args__ = (
        Index("idx_errlog_timestamp", "error_timestamp", "program_id"),
        Index("idx_errlog_severity", "process_date", "error_severity"),
        CheckConstraint("error_type IN ('S', 'A', 'D')", name="chk_error_type"),
        CheckConstraint("error_severity BETWEEN 1 AND 4", name="chk_error_severity"),
    )

    def __repr__(self) -> str:
        return f"<ErrorLog(id={self.id}, program={self.program_id}, severity={self.error_severity})>"

    @property
    def is_severe(self) -> bool:
        return self.error_severity >= 3


class AuthPermission(Base):
    """
    Authentication Permissions Table.
    
    Replaces AUTHFILE for user authorization.
    """
    __tablename__ = "auth_permissions"

    user_id = Column(String(8), primary_key=True, nullable=False)
    resource_type = Column(String(2), primary_key=True, nullable=False)
    resource_id = Column(String(20), primary_key=True, nullable=False)
    permission_level = Column(String(1), nullable=False)
    granted_date = Column(Date, nullable=False, default=date.today)
    granted_by = Column(String(8), nullable=False)
    expiry_date = Column(Date, nullable=True)
    status = Column(String(1), nullable=False, default="A")
    last_maint_date = Column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        Index("idx_auth_resource", "resource_type", "resource_id"),
        Index("idx_auth_status", "status", "expiry_date"),
        CheckConstraint("resource_type IN ('PT', 'RP', 'SY')", name="chk_resource_type"),
        CheckConstraint("permission_level IN ('R', 'W', 'A')", name="chk_permission_level"),
        CheckConstraint("status IN ('A', 'I', 'R')", name="chk_auth_status"),
    )

    def __repr__(self) -> str:
        return f"<AuthPermission(user={self.user_id}, resource={self.resource_type}:{self.resource_id})>"

    @property
    def is_valid(self) -> bool:
        if self.status != "A":
            return False
        if self.expiry_date and self.expiry_date < date.today():
            return False
        return True


class AuditLog(Base):
    """
    Audit Log Table.
    
    Replaces AUDITLOG for security audit trail.
    """
    __tablename__ = "audit_log"

    audit_id = Column(Integer, primary_key=True, autoincrement=True)
    audit_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    user_id = Column(String(8), nullable=False)
    action_type = Column(String(2), nullable=False)
    resource_type = Column(String(2), nullable=False)
    resource_id = Column(String(20), nullable=False)
    action_detail = Column(String(500), nullable=True)
    ip_address = Column(String(45), nullable=True)
    session_id = Column(String(50), nullable=True)
    status = Column(String(1), nullable=False, default="S")
    error_message = Column(String(200), nullable=True)

    __table_args__ = (
        Index("idx_audit_timestamp", "audit_timestamp"),
        Index("idx_audit_user", "user_id", "audit_timestamp"),
        Index("idx_audit_resource", "resource_type", "resource_id", "audit_timestamp"),
        CheckConstraint("action_type IN ('LI', 'LO', 'RD', 'WR', 'DL')", name="chk_action_type"),
        CheckConstraint("status IN ('S', 'F')", name="chk_audit_status"),
    )

    def __repr__(self) -> str:
        return f"<AuditLog(id={self.audit_id}, user={self.user_id}, action={self.action_type})>"

    @property
    def is_success(self) -> bool:
        return self.status == "S"
