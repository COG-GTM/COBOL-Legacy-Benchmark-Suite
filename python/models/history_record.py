"""HISTORY-RECORD and TRANSACTION-HISTORY models from HISTREC.cpy.

The COBOL ``HISTLD00`` program declares its TRANSACTION-HISTORY file with
``COPY HISTREC`` (yielding ``HIST-*`` field names), but the load routine
(``2200-LOAD-TO-DB2``) references ``TH-*`` prefixed fields that are not
defined in HISTREC.cpy. This is a known inconsistency in the legacy benchmark
suite. To preserve fidelity:

* :class:`HistoryRecord` and :class:`HistoryKey` mirror HISTREC.cpy literally.
* :class:`TransactionHistoryRecord` defines the ``TH-*`` fields actually used
  by the load step, sized to align with the destination ``PH-*`` fields in
  DBTBLS.cpy / POSHIST-RECORD.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal
from enum import Enum


class HistoryRecordType(str, Enum):
    """HIST-RECORD-TYPE 88-level values."""

    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    """HIST-ACTION-CODE 88-level values."""

    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


@dataclass
class HistoryKey:
    """HIST-KEY composite key.

    Mirrors HISTREC.cpy:
        HIST-PORTFOLIO-ID  PIC X(08)
        HIST-DATE          PIC X(08)  (YYYYMMDD)
        HIST-TIME          PIC X(06)  (HHMMSS)
        HIST-SEQ-NO        PIC X(04)
    """

    portfolio_id: str = ""
    date: str = ""
    time: str = ""
    seq_no: str = ""

    def __post_init__(self) -> None:
        self.portfolio_id = self.portfolio_id.ljust(8)[:8]
        self.date = self.date.ljust(8)[:8]
        self.time = self.time.ljust(6)[:6]
        self.seq_no = self.seq_no.ljust(4)[:4]

    def as_string(self) -> str:
        """Return the 26-character composite VSAM key."""
        return f"{self.portfolio_id}{self.date}{self.time}{self.seq_no}"


@dataclass
class HistoryRecord:
    """HISTORY-RECORD from HISTREC.cpy.

    Field lengths (HIST-DATA + HIST-AUDIT + HIST-FILLER):
        HIST-RECORD-TYPE   PIC X(02)
        HIST-ACTION-CODE   PIC X(01)
        HIST-BEFORE-IMAGE  PIC X(400)
        HIST-AFTER-IMAGE   PIC X(400)
        HIST-REASON-CODE   PIC X(04)
        HIST-PROCESS-DATE  PIC X(26)
        HIST-PROCESS-USER  PIC X(08)
        HIST-FILLER        PIC X(50)
    """

    key: HistoryKey = field(default_factory=HistoryKey)
    record_type: str = ""
    action_code: str = ""
    before_image: str = ""
    after_image: str = ""
    reason_code: str = ""
    process_date: str = ""
    process_user: str = ""
    filler: str = ""

    def __post_init__(self) -> None:
        self.record_type = self.record_type[:2]
        self.action_code = self.action_code[:1]
        self.before_image = self.before_image[:400]
        self.after_image = self.after_image[:400]
        self.reason_code = self.reason_code[:4]
        self.process_date = self.process_date[:26]
        self.process_user = self.process_user[:8]
        self.filler = self.filler[:50]


@dataclass
class TransactionHistoryRecord:
    """TH-* fields referenced by HISTLD00 ``2200-LOAD-TO-DB2``.

    Mapped 1:1 to ``POSHIST-RECORD`` (DBTBLS.cpy) fields. All financial
    fields use :class:`decimal.Decimal` to mirror COBOL COMP-3 PIC clauses
    without losing precision.

    Field lengths (matching destination PH-* fields in DBTBLS.cpy):
        TH-ACCOUNT-NO    PIC X(8)
        TH-PORTFOLIO-ID  PIC X(10)
        TH-TRANS-DATE    PIC X(10)   (YYYY-MM-DD)
        TH-TRANS-TIME    PIC X(8)    (HH:MM:SS)
        TH-TRANS-TYPE    PIC X(2)
        TH-SECURITY-ID   PIC X(12)
        TH-QUANTITY      PIC S9(12)V9(3) COMP-3 -> Decimal (3 decimal places)
        TH-PRICE         PIC S9(12)V9(3) COMP-3 -> Decimal (3 decimal places)
        TH-AMOUNT        PIC S9(13)V9(2) COMP-3 -> Decimal (2 decimal places)
        TH-FEES          PIC S9(13)V9(2) COMP-3 -> Decimal (2 decimal places)
        TH-TOTAL-AMOUNT  PIC S9(13)V9(2) COMP-3 -> Decimal (2 decimal places)
        TH-COST-BASIS    PIC S9(13)V9(2) COMP-3 -> Decimal (2 decimal places)
        TH-GAIN-LOSS     PIC S9(13)V9(2) COMP-3 -> Decimal (2 decimal places)
    """

    account_no: str = ""
    portfolio_id: str = ""
    trans_date: str = ""
    trans_time: str = ""
    trans_type: str = ""
    security_id: str = ""
    quantity: Decimal = Decimal("0.000")
    price: Decimal = Decimal("0.000")
    amount: Decimal = Decimal("0.00")
    fees: Decimal = Decimal("0.00")
    total_amount: Decimal = Decimal("0.00")
    cost_basis: Decimal = Decimal("0.00")
    gain_loss: Decimal = Decimal("0.00")

    def __post_init__(self) -> None:
        self.account_no = self.account_no[:8]
        self.portfolio_id = self.portfolio_id[:10]
        self.trans_date = self.trans_date[:10]
        self.trans_time = self.trans_time[:8]
        self.trans_type = self.trans_type[:2]
        self.security_id = self.security_id[:12]
        # Coerce all numeric fields to Decimal for safety
        self.quantity = Decimal(self.quantity)
        self.price = Decimal(self.price)
        self.amount = Decimal(self.amount)
        self.fees = Decimal(self.fees)
        self.total_amount = Decimal(self.total_amount)
        self.cost_basis = Decimal(self.cost_basis)
        self.gain_loss = Decimal(self.gain_loss)
