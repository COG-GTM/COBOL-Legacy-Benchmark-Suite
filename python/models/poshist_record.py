"""POSHIST-RECORD dataclass from DBTBLS.cpy."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Dict


@dataclass
class PosHistRecord:
    """Mirror of POSHIST-RECORD from DBTBLS.cpy.

    All COMP-3 financial fields use :class:`decimal.Decimal` to preserve
    exact precision. Field lengths from DBTBLS.cpy:

        PH-ACCOUNT-NO       PIC X(8)
        PH-PORTFOLIO-ID     PIC X(10)
        PH-TRANS-DATE       PIC X(10)   (YYYY-MM-DD)
        PH-TRANS-TIME       PIC X(8)    (HH:MM:SS)
        PH-TRANS-TYPE       PIC X(2)
        PH-SECURITY-ID      PIC X(12)
        PH-QUANTITY         PIC S9(12)V9(3) COMP-3
        PH-PRICE            PIC S9(12)V9(3) COMP-3
        PH-AMOUNT           PIC S9(13)V9(2) COMP-3
        PH-FEES             PIC S9(13)V9(2) COMP-3
        PH-TOTAL-AMOUNT     PIC S9(13)V9(2) COMP-3
        PH-COST-BASIS       PIC S9(13)V9(2) COMP-3
        PH-GAIN-LOSS        PIC S9(13)V9(2) COMP-3
        PH-PROCESS-DATE     PIC X(10)
        PH-PROCESS-TIME     PIC X(8)
        PH-PROGRAM-ID       PIC X(8)
        PH-USER-ID          PIC X(8)
        PH-AUDIT-TIMESTAMP  PIC X(26)
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
    process_date: str = ""
    process_time: str = ""
    program_id: str = ""
    user_id: str = ""
    audit_timestamp: str = ""

    # Decimal precision matching the COBOL PIC definitions
    _QUANTITY_PRECISION = Decimal("0.001")
    _PRICE_PRECISION = Decimal("0.001")
    _AMOUNT_PRECISION = Decimal("0.01")

    def __post_init__(self) -> None:
        self.account_no = self.account_no[:8]
        self.portfolio_id = self.portfolio_id[:10]
        self.trans_date = self.trans_date[:10]
        self.trans_time = self.trans_time[:8]
        self.trans_type = self.trans_type[:2]
        self.security_id = self.security_id[:12]
        self.process_date = self.process_date[:10]
        self.process_time = self.process_time[:8]
        self.program_id = self.program_id[:8]
        self.user_id = self.user_id[:8]
        self.audit_timestamp = self.audit_timestamp[:26]

        # Coerce numeric fields and quantize to COBOL precision.
        self.quantity = Decimal(self.quantity).quantize(self._QUANTITY_PRECISION)
        self.price = Decimal(self.price).quantize(self._PRICE_PRECISION)
        self.amount = Decimal(self.amount).quantize(self._AMOUNT_PRECISION)
        self.fees = Decimal(self.fees).quantize(self._AMOUNT_PRECISION)
        self.total_amount = Decimal(self.total_amount).quantize(self._AMOUNT_PRECISION)
        self.cost_basis = Decimal(self.cost_basis).quantize(self._AMOUNT_PRECISION)
        self.gain_loss = Decimal(self.gain_loss).quantize(self._AMOUNT_PRECISION)

    @classmethod
    def from_transaction_history(
        cls,
        history: "TransactionHistoryRecord",  # type: ignore[name-defined]
        program_id: str = "HISTLD00",
        user_id: str = "BATCH",
        process_at: Any = None,
    ) -> "PosHistRecord":
        """Build a POSHIST record from a TH-* transaction-history record.

        Implements the field mapping in HISTLD00.cbl ``2200-LOAD-TO-DB2``
        (lines 143-158): TH-* -> PH-*. Audit columns
        (PH-PROCESS-DATE/TIME, PH-PROGRAM-ID, PH-USER-ID, PH-AUDIT-TIMESTAMP)
        are populated from the supplied ``process_at`` (defaults to now).
        """
        if process_at is None:
            process_at = datetime.now(timezone.utc)
        return cls(
            account_no=history.account_no,
            portfolio_id=history.portfolio_id,
            trans_date=history.trans_date,
            trans_time=history.trans_time,
            trans_type=history.trans_type,
            security_id=history.security_id,
            quantity=history.quantity,
            price=history.price,
            amount=history.amount,
            fees=history.fees,
            total_amount=history.total_amount,
            cost_basis=history.cost_basis,
            gain_loss=history.gain_loss,
            process_date=process_at.strftime("%Y-%m-%d"),
            process_time=process_at.strftime("%H:%M:%S"),
            program_id=program_id[:8],
            user_id=user_id[:8],
            audit_timestamp=process_at.strftime("%Y-%m-%d-%H.%M.%S.%f")[:26],
        )

    def to_dict(self) -> Dict[str, Any]:
        """Return a mapping suitable for parameterized SQL INSERT statements."""
        return {
            "account_no": self.account_no,
            "portfolio_id": self.portfolio_id,
            "trans_date": self.trans_date,
            "trans_time": self.trans_time,
            "trans_type": self.trans_type,
            "security_id": self.security_id,
            "quantity": self.quantity,
            "price": self.price,
            "amount": self.amount,
            "fees": self.fees,
            "total_amount": self.total_amount,
            "cost_basis": self.cost_basis,
            "gain_loss": self.gain_loss,
            "process_date": self.process_date,
            "process_time": self.process_time,
            "program_id": self.program_id,
            "user_id": self.user_id,
            "audit_timestamp": self.audit_timestamp,
        }
