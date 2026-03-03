"""
VSAM Audit History File → PostgreSQL Table.

Converted from:
  - Record layout: src/copybook/common/HISTREC.cpy (HISTORY-RECORD)

This model represents the audit/change history trail maintained as a
VSAM KSDS file. HISTREC.cpy captures before/after images of record
changes across portfolio, position, and transaction data.

VSAM KSDS Properties (inferred from HISTREC.cpy):
  - Organization:  KSDS (Key-Sequenced Data Set)
  - Record Format: FIXED
  - Key Structure:  Portfolio ID (8) + History Date (8)
                    + History Time (6) + Sequence No (4)
"""

from sqlalchemy import CheckConstraint, Index, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class AuditHistory(Base):
    """
    VSAM Audit History file based on HISTREC.cpy.

    Stores before/after images of all record modifications for audit
    and recovery purposes. Used by batch programs (HISTLD00) and
    audit reporting (RPTAUD00).

    Note: This has no direct equivalent in the DB2 DDL definitions.
    The DB2 ERRLOG table tracks errors, not record change history.
    """

    __tablename__ = "vsam_audit_history"

    # --- Composite Primary Key (VSAM KSDS Key from HIST-KEY) ---
    portfolio_id: Mapped[str] = mapped_column(
        String(8),
        primary_key=True,
        comment="Portfolio identifier (HIST-PORTFOLIO-ID)",
    )
    history_date: Mapped[str] = mapped_column(
        String(8),
        primary_key=True,
        comment="History date YYYYMMDD (HIST-DATE)",
    )
    history_time: Mapped[str] = mapped_column(
        String(6),
        primary_key=True,
        comment="History time HHMMSS (HIST-TIME)",
    )
    sequence_no: Mapped[str] = mapped_column(
        String(4),
        primary_key=True,
        comment="Sequence number (HIST-SEQ-NO)",
    )

    # --- Data Fields (from HISTREC.cpy HIST-DATA) ---
    record_type: Mapped[str] = mapped_column(
        String(2),
        nullable=False,
        comment="Record type: PT=Portfolio, PS=Position, TR=Transaction (HIST-RECORD-TYPE)",
    )
    action_code: Mapped[str] = mapped_column(
        String(1),
        nullable=False,
        comment="Action: A=Add, C=Change, D=Delete (HIST-ACTION-CODE)",
    )
    before_image: Mapped[str] = mapped_column(
        Text,
        nullable=True,
        comment="Record image before change (HIST-BEFORE-IMAGE) PIC X(400)",
    )
    after_image: Mapped[str] = mapped_column(
        Text,
        nullable=True,
        comment="Record image after change (HIST-AFTER-IMAGE) PIC X(400)",
    )
    reason_code: Mapped[str] = mapped_column(
        String(4),
        nullable=False,
        comment="Reason for change (HIST-REASON-CODE)",
    )

    # --- Audit Fields (from HISTREC.cpy HIST-AUDIT) ---
    process_date: Mapped[str] = mapped_column(
        String(26),
        nullable=False,
        comment="Process timestamp (HIST-PROCESS-DATE)",
    )
    process_user: Mapped[str] = mapped_column(
        String(8),
        nullable=False,
        comment="Processing user ID (HIST-PROCESS-USER)",
    )

    # --- Indexes (VSAM Alternate Index equivalents) ---
    __table_args__ = (
        Index("ix_vsam_audhist_date", "history_date", "portfolio_id"),
        Index("ix_vsam_audhist_rectype", "record_type", "history_date"),
        Index("ix_vsam_audhist_action", "action_code", "history_date"),
        CheckConstraint(
            "record_type IN ('PT', 'PS', 'TR')",
            name="ck_audhist_record_type",
        ),
        CheckConstraint(
            "action_code IN ('A', 'C', 'D')",
            name="ck_audhist_action_code",
        ),
        {"comment": "VSAM Audit History KSDS file (from HISTREC.cpy)"},
    )

    def __repr__(self) -> str:
        return (
            f"AuditHistory(portfolio_id={self.portfolio_id!r}, "
            f"date={self.history_date!r}, time={self.history_time!r}, "
            f"seq={self.sequence_no!r})"
        )
