"""SQLAlchemy model for POSHIST (Position History) table.

Migrated from: src/database/db2/POSHIST.sql

Transaction types: BU=Buy, SL=Sell, TR=Transfer
"""

from datetime import date, datetime, time
from decimal import Decimal

from sqlalchemy import Date, DateTime, Index, Numeric, String, Time, text
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class PosHist(Base):
    """Position History table - stores all portfolio transaction history."""

    __tablename__ = "poshist"

    account_no: Mapped[str] = mapped_column(String(8), primary_key=True)
    portfolio_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    trans_date: Mapped[date] = mapped_column(Date, primary_key=True)
    trans_time: Mapped[time] = mapped_column(Time, primary_key=True)
    trans_type: Mapped[str] = mapped_column(String(2), nullable=False)
    security_id: Mapped[str] = mapped_column(String(12), nullable=False)
    quantity: Mapped[Decimal] = mapped_column(
        Numeric(precision=15, scale=3), nullable=False
    )
    price: Mapped[Decimal] = mapped_column(
        Numeric(precision=15, scale=3), nullable=False
    )
    amount: Mapped[Decimal] = mapped_column(
        Numeric(precision=15, scale=2), nullable=False
    )
    fees: Mapped[Decimal] = mapped_column(
        Numeric(precision=15, scale=2), nullable=False, server_default="0"
    )
    total_amount: Mapped[Decimal] = mapped_column(
        Numeric(precision=15, scale=2), nullable=False
    )
    cost_basis: Mapped[Decimal] = mapped_column(
        Numeric(precision=15, scale=2), nullable=False
    )
    gain_loss: Mapped[Decimal] = mapped_column(
        Numeric(precision=15, scale=2), nullable=False
    )
    process_date: Mapped[date] = mapped_column(Date, nullable=False)
    process_time: Mapped[time] = mapped_column(Time, nullable=False)
    program_id: Mapped[str] = mapped_column(String(8), nullable=False)
    user_id: Mapped[str] = mapped_column(String(8), nullable=False)
    audit_timestamp: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=text("now()")
    )

    __table_args__ = (
        Index("poshist_ix1", "security_id", "trans_date"),
        Index("poshist_ix2", "process_date", "program_id"),
    )
