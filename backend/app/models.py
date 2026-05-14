"""SQLAlchemy ORM models.

Direct mapping from COBOL copybooks (PORTFLIO.cpy, POSREC.cpy, TRNREC.cpy,
HISTREC.cpy) and DB2 table definitions (db2-definitions.sql).

Financial fields use Numeric types to preserve decimal precision,
following the COMP-3 packed decimal migration pattern.
"""

from datetime import date, datetime

from sqlalchemy import (
    Date,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .database import Base


class Portfolio(Base):
    """Portfolio Master – replaces PORTMSTR VSAM KSDS file.

    Maps from PORTFLIO.cpy and PORTFOLIO_MASTER DB2 table.
    """

    __tablename__ = "portfolios"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    portfolio_id: Mapped[str] = mapped_column(String(8), unique=True, nullable=False)
    account_no: Mapped[str] = mapped_column(String(10), nullable=False)
    client_name: Mapped[str] = mapped_column(String(50), nullable=False)
    client_type: Mapped[str] = mapped_column(String(1), nullable=False, default="I")
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    risk_level: Mapped[str] = mapped_column(String(1), nullable=False, default="M")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="A")
    total_value: Mapped[float] = mapped_column(Numeric(18, 2), nullable=False, default=0)
    cash_balance: Mapped[float] = mapped_column(Numeric(18, 2), nullable=False, default=0)
    open_date: Mapped[date] = mapped_column(Date, nullable=False)
    close_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    last_maint_date: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False, default="SYSTEM")

    positions: Mapped[list["Position"]] = relationship(back_populates="portfolio", cascade="all, delete-orphan")
    transactions: Mapped[list["Transaction"]] = relationship(back_populates="portfolio", cascade="all, delete-orphan")


class Position(Base):
    """Investment Positions – replaces POSHIST VSAM KSDS file.

    Maps from POSREC.cpy and INVESTMENT_POSITIONS DB2 table.
    """

    __tablename__ = "positions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    portfolio_id: Mapped[str] = mapped_column(String(8), ForeignKey("portfolios.portfolio_id"), nullable=False)
    investment_id: Mapped[str] = mapped_column(String(10), nullable=False)
    investment_name: Mapped[str] = mapped_column(String(50), nullable=False)
    position_date: Mapped[date] = mapped_column(Date, nullable=False)
    quantity: Mapped[float] = mapped_column(Numeric(18, 4), nullable=False)
    cost_basis: Mapped[float] = mapped_column(Numeric(18, 2), nullable=False)
    market_value: Mapped[float] = mapped_column(Numeric(18, 2), nullable=False)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="A")
    last_maint_date: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False, default="SYSTEM")

    portfolio: Mapped["Portfolio"] = relationship(back_populates="positions")


class Transaction(Base):
    """Transaction History – replaces TRANHIST VSAM KSDS file.

    Maps from TRNREC.cpy and TRANSACTION_HISTORY DB2 table.
    Transaction types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
    Status codes: P=Pending, D=Done, F=Failed, R=Reversed
    """

    __tablename__ = "transactions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    transaction_id: Mapped[str] = mapped_column(String(32), unique=True, nullable=False)
    portfolio_id: Mapped[str] = mapped_column(String(8), ForeignKey("portfolios.portfolio_id"), nullable=False)
    investment_id: Mapped[str] = mapped_column(String(10), nullable=False)
    transaction_date: Mapped[date] = mapped_column(Date, nullable=False)
    transaction_type: Mapped[str] = mapped_column(String(2), nullable=False)
    quantity: Mapped[float] = mapped_column(Numeric(18, 4), nullable=False)
    price: Mapped[float] = mapped_column(Numeric(18, 4), nullable=False)
    amount: Mapped[float] = mapped_column(Numeric(18, 2), nullable=False)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="D")
    process_date: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    process_user: Mapped[str] = mapped_column(String(8), nullable=False, default="SYSTEM")

    portfolio: Mapped["Portfolio"] = relationship(back_populates="transactions")
