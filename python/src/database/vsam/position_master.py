"""
VSAM Portfolio Master File (PORTMSTR) → PostgreSQL Table.

Converted from:
  - VSAM definition: src/database/vsam/vsam-definitions.txt (PORTMSTR)
  - Record layout:   src/copybook/common/PORTFLIO.cpy (PORT-RECORD)

VSAM KSDS Properties:
  - Organization:  KSDS (Key-Sequenced Data Set)
  - Record Format: FIXED
  - Record Length:  400 bytes
  - Key Length:     12 bytes at position 1
  - Key Structure:  Portfolio ID (8) + Account Type (2) + Branch ID (2)

The composite primary key replicates VSAM KSDS key-sequenced behavior.
Additional data fields are sourced from the PORTFLIO.cpy copybook.
"""

from sqlalchemy import CheckConstraint, Index, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class PortfolioMaster(Base):
    """
    VSAM Portfolio Master file (PORTMSTR).

    Stores operational portfolio data accessed by CICS online programs
    and updated by batch processing. The composite primary key mirrors
    the VSAM KSDS key structure for key-sequenced access.

    Note: This is distinct from the DB2 PORTFOLIO_MASTER table which
    serves reporting purposes with a single-column primary key.
    """

    __tablename__ = "vsam_portfolio_master"

    # --- Composite Primary Key (VSAM KSDS Key: 12 bytes) ---
    # Matches VSAM key structure: Portfolio ID (8) + Account Type (2) + Branch ID (2)
    portfolio_id: Mapped[str] = mapped_column(
        String(8), primary_key=True, comment="Portfolio identifier (PORT-ID)"
    )
    account_type: Mapped[str] = mapped_column(
        String(2), primary_key=True, comment="Account type code"
    )
    branch_id: Mapped[str] = mapped_column(
        String(2), primary_key=True, comment="Branch identifier"
    )

    # --- Data Fields (from PORTFLIO.cpy PORT-RECORD) ---
    account_no: Mapped[str] = mapped_column(
        String(10), nullable=False, comment="Account number (PORT-ACCOUNT-NO)"
    )
    client_name: Mapped[str] = mapped_column(
        String(30), nullable=False, comment="Client name (PORT-CLIENT-NAME)"
    )
    client_type: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="Client type: I=Individual, C=Corporate, T=Trust (PORT-CLIENT-TYPE)",
    )
    create_date: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Creation date YYYYMMDD (PORT-CREATE-DATE)"
    )
    last_maint_date: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Last maintenance date YYYYMMDD (PORT-LAST-MAINT)"
    )
    status: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="Status: A=Active, C=Closed, S=Suspended (PORT-STATUS)",
    )

    # --- Financial Fields (COMP-3 packed decimal → Numeric) ---
    total_value: Mapped[Numeric] = mapped_column(
        Numeric(15, 2),
        nullable=False,
        default=0,
        comment="Total portfolio value (PORT-TOTAL-VALUE) PIC S9(13)V99 COMP-3",
    )
    cash_balance: Mapped[Numeric] = mapped_column(
        Numeric(15, 2),
        nullable=False,
        default=0,
        comment="Cash balance (PORT-CASH-BALANCE) PIC S9(13)V99 COMP-3",
    )

    # --- Audit Fields ---
    last_user: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Last maintenance user (PORT-LAST-USER)"
    )
    last_trans_date: Mapped[str] = mapped_column(
        String(8), nullable=False, comment="Last transaction date YYYYMMDD (PORT-LAST-TRANS)"
    )

    # --- Indexes (VSAM Alternate Index equivalents) ---
    __table_args__ = (
        Index("ix_vsam_portmstr_account", "account_no"),
        Index("ix_vsam_portmstr_client", "client_name", "client_type"),
        Index("ix_vsam_portmstr_status", "status", "portfolio_id"),
        CheckConstraint("client_type IN ('I', 'C', 'T')", name="ck_portmstr_client_type"),
        CheckConstraint("status IN ('A', 'C', 'S')", name="ck_portmstr_status"),
        {"comment": "VSAM PORTMSTR - Portfolio Master KSDS file"},
    )

    def __repr__(self) -> str:
        return (
            f"PortfolioMaster(portfolio_id={self.portfolio_id!r}, "
            f"account_type={self.account_type!r}, branch_id={self.branch_id!r})"
        )
