"""Transaction model — translated from TRNREC.cpy and TRANSACTION_HISTORY DB2 table."""

from sqlalchemy import Column, String, Numeric, Date, Time, DateTime, ForeignKey, CheckConstraint, Index
from sqlalchemy.orm import relationship
from datetime import datetime
from decimal import Decimal
from .database import Base


class Transaction(Base):
    __tablename__ = "transactions"

    transaction_id = Column(String(20), primary_key=True)
    portfolio_id = Column(String(8), ForeignKey("portfolios.portfolio_id"), nullable=False)
    investment_id = Column(String(10), nullable=False)
    transaction_date = Column(Date, nullable=False)
    transaction_time = Column(Time, nullable=False)
    sequence_no = Column(String(6), nullable=False)
    transaction_type = Column(
        String(2),
        CheckConstraint("transaction_type IN ('BU', 'SL', 'TR', 'FE')"),
        nullable=False,
    )
    quantity = Column(Numeric(15, 4), nullable=False, default=0)
    price = Column(Numeric(15, 4), nullable=False, default=0)
    amount = Column(Numeric(15, 2), nullable=False, default=0)
    currency = Column(String(3), nullable=False, default="USD")
    status = Column(
        String(1),
        CheckConstraint("status IN ('P', 'D', 'F', 'R')"),
        nullable=False,
        default="P",
    )
    process_date = Column(DateTime, nullable=True)
    process_user = Column(String(8), nullable=False, default="SYSTEM")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    portfolio = relationship("Portfolio", back_populates="transactions")

    __table_args__ = (
        Index("idx_txn_portfolio", "portfolio_id"),
        Index("idx_txn_date", "transaction_date"),
        Index("idx_txn_type", "transaction_type"),
        Index("idx_txn_status", "status"),
        Index("idx_txn_investment", "investment_id"),
    )

    VALID_STATUS_TRANSITIONS = {
        "P": ["D", "F"],
        "D": ["R"],
        "F": ["P"],
        "R": [],
    }

    def can_transition_to(self, new_status: str) -> bool:
        return new_status in self.VALID_STATUS_TRANSITIONS.get(self.status, [])

    def calculate_amount(self) -> Decimal:
        if self.quantity and self.price:
            return Decimal(str(self.quantity)) * Decimal(str(self.price))
        return Decimal("0.00")
