from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import Date, DateTime, ForeignKey, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class InvestmentPosition(Base):
    __tablename__ = "investment_positions"

    portfolio_id: Mapped[str] = mapped_column(
        String(8), ForeignKey("portfolio_master.portfolio_id"), primary_key=True
    )
    investment_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    position_date: Mapped[date] = mapped_column(Date, primary_key=True)
    quantity: Mapped[Decimal] = mapped_column(Numeric(18, 4), nullable=False)
    cost_basis: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    market_value: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False)
    last_maint_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False)

    portfolio = relationship("PortfolioMaster", back_populates="positions")
