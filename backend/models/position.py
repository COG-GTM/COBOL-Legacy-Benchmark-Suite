"""Position model — translated from POSREC.cpy and INVESTMENT_POSITIONS DB2 table."""

from sqlalchemy import Column, String, Numeric, Date, DateTime, ForeignKey, CheckConstraint, Index
from sqlalchemy.orm import relationship
from datetime import datetime, date
from .database import Base


class Position(Base):
    __tablename__ = "positions"

    id = Column(String(36), primary_key=True)
    portfolio_id = Column(String(8), ForeignKey("portfolios.portfolio_id"), nullable=False)
    investment_id = Column(String(10), nullable=False)
    symbol = Column(String(10), nullable=False, default="")
    name = Column(String(50), nullable=False, default="")
    position_date = Column(Date, nullable=False, default=date.today)
    quantity = Column(Numeric(15, 4), nullable=False, default=0)
    cost_basis = Column(Numeric(15, 2), nullable=False, default=0)
    market_value = Column(Numeric(15, 2), nullable=False, default=0)
    current_price = Column(Numeric(15, 4), nullable=False, default=0)
    currency = Column(String(3), nullable=False, default="USD")
    status = Column(
        String(1),
        CheckConstraint("status IN ('A', 'C', 'P')"),
        nullable=False,
        default="A",
    )
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )
    last_maint_user = Column(String(8), nullable=False, default="SYSTEM")

    portfolio = relationship("Portfolio", back_populates="positions")

    __table_args__ = (
        Index("idx_position_portfolio", "portfolio_id"),
        Index("idx_position_investment", "investment_id"),
        Index("idx_position_date", "position_date"),
        Index("idx_position_status", "status"),
    )

    @property
    def gain_loss(self):
        return float(self.market_value or 0) - float(self.cost_basis or 0)

    @property
    def gain_loss_percent(self):
        cb = float(self.cost_basis or 0)
        if cb == 0:
            return 0.0
        return ((float(self.market_value or 0) - cb) / cb) * 100
