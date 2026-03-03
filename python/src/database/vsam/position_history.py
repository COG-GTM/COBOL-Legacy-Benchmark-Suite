"""
VSAM Position History File (POSHIST) → PostgreSQL Table.

Converted from:
  - VSAM definition: src/database/vsam/vsam-definitions.txt (POSHIST)
  - Record layout:   src/copybook/common/POSREC.cpy (POSITION-RECORD)

VSAM KSDS Properties:
  - Organization:  KSDS (Key-Sequenced Data Set)
  - Record Format: FIXED
  - Record Length:  350 bytes
  - Key Structure:  Portfolio ID (8) + Position Date (8) + Investment ID (10)

The composite primary key replicates VSAM KSDS key-sequenced behavior,
enabling efficient lookups by portfolio, date, and investment.
"""

from sqlalchemy import CheckConstraint, Index, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class PositionHistory(Base):
    """
    VSAM Position History file (POSHIST).

    Stores point-in-time investment position snapshots used by batch
    programs (POSUPD00) and online inquiry (INQPORT). The composite
    primary key mirrors the VSAM KSDS key for key-sequenced access.

    Note: This is distinct from the DB2 POSHIST table (which tracks
    transaction-level history with a different key structure) and the
    DB2 INVESTMENT_POSITIONS table (which uses DATE-typed columns).
    """

    __tablename__ = "vsam_position_history"

    # --- Composite Primary Key (VSAM KSDS Key from POS-KEY) ---
    portfolio_id: Mapped[str] = mapped_column(
        String(8),
        primary_key=True,
        comment="Portfolio identifier (POS-PORTFOLIO-ID)",
    )
    position_date: Mapped[str] = mapped_column(
        String(8),
        primary_key=True,
        comment="Position date YYYYMMDD (POS-DATE)",
    )
    investment_id: Mapped[str] = mapped_column(
        String(10),
        primary_key=True,
        comment="Investment identifier (POS-INVESTMENT-ID)",
    )

    # --- Data Fields (from POSREC.cpy POS-DATA) ---
    # Financial fields (COMP-3 packed decimal → Numeric)
    quantity: Mapped[Numeric] = mapped_column(
        Numeric(15, 4),
        nullable=False,
        comment="Holding quantity (POS-QUANTITY) PIC S9(11)V9(4) COMP-3",
    )
    cost_basis: Mapped[Numeric] = mapped_column(
        Numeric(15, 2),
        nullable=False,
        comment="Total cost basis (POS-COST-BASIS) PIC S9(13)V9(2) COMP-3",
    )
    market_value: Mapped[Numeric] = mapped_column(
        Numeric(15, 2),
        nullable=False,
        comment="Current market value (POS-MARKET-VALUE) PIC S9(13)V9(2) COMP-3",
    )
    currency: Mapped[str] = mapped_column(
        String(3), nullable=False, comment="Currency code (POS-CURRENCY)"
    )
    status: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="Status: A=Active, C=Closed, P=Pending (POS-STATUS)",
    )

    # --- Audit Fields (from POSREC.cpy POS-AUDIT) ---
    last_maint_date: Mapped[str] = mapped_column(
        String(26),
        nullable=False,
        comment="Last maintenance timestamp (POS-LAST-MAINT-DATE)",
    )
    last_maint_user: Mapped[str] = mapped_column(
        String(8),
        nullable=False,
        comment="Last maintenance user ID (POS-LAST-MAINT-USER)",
    )

    # --- Indexes (VSAM Alternate Index equivalents) ---
    __table_args__ = (
        Index("ix_vsam_poshist_date", "position_date", "portfolio_id"),
        Index("ix_vsam_poshist_investment", "investment_id", "position_date"),
        Index("ix_vsam_poshist_status", "status", "portfolio_id"),
        CheckConstraint("status IN ('A', 'C', 'P')", name="ck_poshist_status"),
        {"comment": "VSAM POSHIST - Position History KSDS file"},
    )

    def __repr__(self) -> str:
        return (
            f"PositionHistory(portfolio_id={self.portfolio_id!r}, "
            f"date={self.position_date!r}, "
            f"investment_id={self.investment_id!r})"
        )
