"""Portfolio model — translated from PORTFLIO.cpy and PORTFOLIO_MASTER DB2 table."""

from sqlalchemy import Column, String, Numeric, Date, DateTime, CheckConstraint, Index
from sqlalchemy.orm import relationship
from datetime import datetime, date
from .database import Base


class Portfolio(Base):
    __tablename__ = "portfolios"

    portfolio_id = Column(String(8), primary_key=True)
    account_number = Column(String(10), nullable=False, index=True)
    client_name = Column(String(50), nullable=False)
    client_type = Column(
        String(1),
        CheckConstraint("client_type IN ('I', 'C', 'T')"),
        nullable=False,
        default="I",
    )
    portfolio_name = Column(String(50), nullable=False, default="")
    currency_code = Column(String(3), nullable=False, default="USD")
    risk_level = Column(
        String(1),
        CheckConstraint("risk_level IN ('L', 'M', 'H')"),
        nullable=False,
        default="M",
    )
    status = Column(
        String(1),
        CheckConstraint("status IN ('A', 'C', 'S')"),
        nullable=False,
        default="A",
    )
    total_value = Column(Numeric(15, 2), nullable=False, default=0)
    cash_balance = Column(Numeric(15, 2), nullable=False, default=0)
    open_date = Column(Date, nullable=False, default=date.today)
    close_date = Column(Date, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )
    last_maint_user = Column(String(8), nullable=False, default="SYSTEM")

    positions = relationship("Position", back_populates="portfolio", cascade="all, delete-orphan")
    transactions = relationship("Transaction", back_populates="portfolio", cascade="all, delete-orphan")

    __table_args__ = (
        Index("idx_portfolio_account", "account_number"),
        Index("idx_portfolio_status", "status"),
        Index("idx_portfolio_client", "client_name"),
    )
