"""SQLAlchemy ORM model for the POSHIST table.

The schema mirrors POSHIST-RECORD from DBTBLS.cpy. The COBOL program does
not declare an explicit primary key on POSHIST, so we synthesize one from
the natural transaction key (account, portfolio, date, time, security,
trans-type) to support deduplication via SQLCODE -803 / IntegrityError.
"""

from __future__ import annotations

from sqlalchemy import Column, DateTime, Numeric, String
from sqlalchemy.orm import declarative_base
from sqlalchemy.schema import PrimaryKeyConstraint

Base = declarative_base()


class PosHist(Base):
    """``POSHIST`` table storing position history records loaded by HISTLD00."""

    __tablename__ = "poshist"

    account_no = Column(String(8), nullable=False)
    portfolio_id = Column(String(10), nullable=False)
    trans_date = Column(String(10), nullable=False)
    trans_time = Column(String(8), nullable=False)
    trans_type = Column(String(2), nullable=False)
    security_id = Column(String(12), nullable=False)
    quantity = Column(Numeric(15, 3), nullable=False, default=0)
    price = Column(Numeric(15, 3), nullable=False, default=0)
    amount = Column(Numeric(15, 2), nullable=False, default=0)
    fees = Column(Numeric(15, 2), nullable=False, default=0)
    total_amount = Column(Numeric(15, 2), nullable=False, default=0)
    cost_basis = Column(Numeric(15, 2), nullable=False, default=0)
    gain_loss = Column(Numeric(15, 2), nullable=False, default=0)
    process_date = Column(String(10), nullable=False, default="")
    process_time = Column(String(8), nullable=False, default="")
    program_id = Column(String(8), nullable=False, default="")
    user_id = Column(String(8), nullable=False, default="")
    audit_timestamp = Column(String(26), nullable=False, default="")

    __table_args__ = (
        PrimaryKeyConstraint(
            "account_no",
            "portfolio_id",
            "trans_date",
            "trans_time",
            "security_id",
            "trans_type",
            name="pk_poshist",
        ),
    )

    def __repr__(self) -> str:  # pragma: no cover - debugging aid
        return (
            f"<PosHist({self.account_no}/{self.portfolio_id} "
            f"{self.trans_date} {self.trans_time} {self.trans_type})>"
        )
