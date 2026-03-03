"""
VSAM Transaction History File (TRANHIST) → PostgreSQL Table.

Converted from:
  - VSAM definition: src/database/vsam/vsam-definitions.txt (TRANHIST)
  - Record layout:   src/copybook/common/TRNREC.cpy (TRANSACTION-RECORD)

VSAM KSDS Properties:
  - Organization:  KSDS (Key-Sequenced Data Set)
  - Record Format: FIXED
  - Record Length:  300 bytes
  - Key Structure:  Transaction Date (8) + Transaction Time (6)
                    + Portfolio ID (8) + Sequence Number (6)

The composite primary key replicates VSAM KSDS key-sequenced behavior,
ensuring records are stored and accessed in chronological order.
"""

from sqlalchemy import CheckConstraint, Index, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class TransactionHistory(Base):
    """
    VSAM Transaction History file (TRANHIST).

    Stores financial transaction records processed by batch programs
    (TRNVAL00, POSUPD00) and queried by online programs (INQHIST).
    The composite primary key ensures chronological key-sequenced access.

    Note: This is distinct from the DB2 TRANSACTION_HISTORY table which
    uses a single TRANSACTION_ID primary key for reporting purposes.
    """

    __tablename__ = "vsam_transaction_history"

    # --- Composite Primary Key (VSAM KSDS Key from TRN-KEY) ---
    transaction_date: Mapped[str] = mapped_column(
        String(8),
        primary_key=True,
        comment="Transaction date YYYYMMDD (TRN-DATE)",
    )
    transaction_time: Mapped[str] = mapped_column(
        String(6),
        primary_key=True,
        comment="Transaction time HHMMSS (TRN-TIME)",
    )
    portfolio_id: Mapped[str] = mapped_column(
        String(8),
        primary_key=True,
        comment="Portfolio identifier (TRN-PORTFOLIO-ID)",
    )
    sequence_no: Mapped[str] = mapped_column(
        String(6),
        primary_key=True,
        comment="Sequence number for multiple transactions (TRN-SEQUENCE-NO)",
    )

    # --- Data Fields (from TRNREC.cpy TRN-DATA) ---
    investment_id: Mapped[str] = mapped_column(
        String(10), nullable=False, comment="Investment identifier (TRN-INVESTMENT-ID)"
    )
    transaction_type: Mapped[str] = mapped_column(
        String(2),
        nullable=False,
        comment="Type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee (TRN-TYPE)",
    )

    # --- Financial Fields (COMP-3 packed decimal → Numeric) ---
    quantity: Mapped[Numeric] = mapped_column(
        Numeric(15, 4),
        nullable=False,
        comment="Transaction quantity (TRN-QUANTITY) PIC S9(11)V9(4) COMP-3",
    )
    price: Mapped[Numeric] = mapped_column(
        Numeric(15, 4),
        nullable=False,
        comment="Transaction price (TRN-PRICE) PIC S9(11)V9(4) COMP-3",
    )
    amount: Mapped[Numeric] = mapped_column(
        Numeric(15, 2),
        nullable=False,
        comment="Transaction amount (TRN-AMOUNT) PIC S9(13)V9(2) COMP-3",
    )
    currency: Mapped[str] = mapped_column(
        String(3), nullable=False, comment="Currency code (TRN-CURRENCY)"
    )
    status: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="Status: P=Pending, D=Done, F=Failed, R=Reversed (TRN-STATUS)",
    )

    # --- Audit Fields (from TRNREC.cpy TRN-AUDIT) ---
    process_date: Mapped[str] = mapped_column(
        String(26), nullable=False, comment="Process timestamp (TRN-PROCESS-DATE)"
    )
    process_user: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Processing user ID (TRN-PROCESS-USER)"
    )

    # --- Indexes (VSAM Alternate Index equivalents) ---
    __table_args__ = (
        Index("ix_vsam_tranhist_portfolio", "portfolio_id", "transaction_date"),
        Index("ix_vsam_tranhist_investment", "investment_id", "transaction_date"),
        Index("ix_vsam_tranhist_status", "status", "transaction_date"),
        CheckConstraint(
            "transaction_type IN ('BU', 'SL', 'TR', 'FE')",
            name="ck_tranhist_type",
        ),
        CheckConstraint(
            "status IN ('P', 'D', 'F', 'R')",
            name="ck_tranhist_status",
        ),
        {"comment": "VSAM TRANHIST - Transaction History KSDS file"},
    )

    def __repr__(self) -> str:
        return (
            f"TransactionHistory(date={self.transaction_date!r}, "
            f"time={self.transaction_time!r}, "
            f"portfolio_id={self.portfolio_id!r}, "
            f"seq={self.sequence_no!r})"
        )
