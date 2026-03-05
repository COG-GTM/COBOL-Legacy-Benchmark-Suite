"""
Data integrity validation utility translated from COBOL program UTLVAL00.cbl.

Validates data integrity across tables:
- Referential integrity checks
- Balance verification
- Orphan record detection
"""

import logging
from datetime import datetime
from decimal import Decimal

from sqlalchemy import and_, func, select
from sqlalchemy.orm import Session

from src.common.constants import ReturnCode
from src.db.tables import (
    InvestmentPosition,
    PortfolioMaster,
    TransactionHistory,
)

logger = logging.getLogger(__name__)


class DataValidator:
    """Data integrity validation utility. Translates UTLVAL00.cbl."""

    def __init__(self, session: Session):
        self.session = session
        self.errors: list[dict] = []
        self.warnings: list[dict] = []

    def validate_all(self) -> ReturnCode:
        """
        Run all validation checks.
        Translates UTLVAL00.cbl 2000-PROCESS main loop.
        """
        self.errors = []
        self.warnings = []

        self._validate_referential_integrity()
        self._validate_position_balances()
        self._validate_transaction_integrity()
        self._validate_field_formats()

        if self.errors:
            return ReturnCode.ERROR
        if self.warnings:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def _validate_referential_integrity(self) -> None:
        """
        Translates 2100-CHECK-REFERENTIAL.
        Check FK relationships.
        """
        # Positions referencing non-existent portfolios
        orphan_positions = self.session.execute(
            select(InvestmentPosition.portfolio_id)
            .distinct()
            .where(
                ~InvestmentPosition.portfolio_id.in_(
                    select(PortfolioMaster.portfolio_id)
                )
            )
        ).scalars().all()

        for pid in orphan_positions:
            self.errors.append({
                "check": "referential_integrity",
                "table": "INVESTMENT_POSITIONS",
                "detail": f"Orphan position for portfolio_id={pid}",
            })

        # Transactions referencing non-existent portfolios
        orphan_trns = self.session.execute(
            select(TransactionHistory.portfolio_id)
            .distinct()
            .where(
                ~TransactionHistory.portfolio_id.in_(
                    select(PortfolioMaster.portfolio_id)
                )
            )
        ).scalars().all()

        for pid in orphan_trns:
            self.errors.append({
                "check": "referential_integrity",
                "table": "TRANSACTION_HISTORY",
                "detail": f"Orphan transaction for portfolio_id={pid}",
            })

        logger.info(
            "Referential integrity: %d orphan positions, %d orphan transactions",
            len(orphan_positions),
            len(orphan_trns),
        )

    def _validate_position_balances(self) -> None:
        """
        Translates 2200-CHECK-BALANCES.
        Verify position quantities and values are non-negative for active positions.
        """
        negative_qty = self.session.execute(
            select(
                InvestmentPosition.portfolio_id,
                InvestmentPosition.investment_id,
                InvestmentPosition.quantity,
            ).where(
                and_(
                    InvestmentPosition.status == "A",
                    InvestmentPosition.quantity < 0,
                )
            )
        ).all()

        for row in negative_qty:
            self.errors.append({
                "check": "position_balance",
                "table": "INVESTMENT_POSITIONS",
                "detail": (
                    f"Negative quantity for portfolio={row[0]}, "
                    f"investment={row[1]}: {row[2]}"
                ),
            })

        # Check for negative cost basis
        negative_cost = self.session.execute(
            select(
                InvestmentPosition.portfolio_id,
                InvestmentPosition.investment_id,
                InvestmentPosition.cost_basis,
            ).where(
                and_(
                    InvestmentPosition.status == "A",
                    InvestmentPosition.cost_basis < 0,
                )
            )
        ).all()

        for row in negative_cost:
            self.warnings.append({
                "check": "position_balance",
                "table": "INVESTMENT_POSITIONS",
                "detail": (
                    f"Negative cost basis for portfolio={row[0]}, "
                    f"investment={row[1]}: {row[2]}"
                ),
            })

    def _validate_transaction_integrity(self) -> None:
        """
        Translates 2300-CHECK-TRANSACTIONS.
        Verify transaction amounts match quantity * price.
        """
        transactions = self.session.execute(
            select(
                TransactionHistory.transaction_id,
                TransactionHistory.quantity,
                TransactionHistory.price,
                TransactionHistory.amount,
            ).where(TransactionHistory.status != "F")  # Skip failed
        ).all()

        for row in transactions:
            tid, qty, price, amount = row
            expected = (Decimal(str(qty)) * Decimal(str(price))).quantize(
                Decimal("0.01")
            )
            actual = Decimal(str(amount))
            if abs(expected - actual) > Decimal("0.01"):
                self.warnings.append({
                    "check": "transaction_integrity",
                    "table": "TRANSACTION_HISTORY",
                    "detail": (
                        f"Amount mismatch for trn_id={tid}: "
                        f"expected={expected}, actual={actual}"
                    ),
                })

    def _validate_field_formats(self) -> None:
        """
        Translates 2400-CHECK-FORMATS.
        Verify field formats (e.g., portfolio_id length).
        """
        # Check portfolio_id length
        bad_ids = self.session.execute(
            select(PortfolioMaster.portfolio_id).where(
                func.length(PortfolioMaster.portfolio_id) > 8
            )
        ).scalars().all()

        for pid in bad_ids:
            self.errors.append({
                "check": "field_format",
                "table": "PORTFOLIO_MASTER",
                "detail": f"Portfolio ID exceeds 8 chars: '{pid}'",
            })

    def get_report(self) -> dict:
        """Get validation results."""
        return {
            "timestamp": datetime.now().isoformat(),
            "errors": self.errors,
            "warnings": self.warnings,
            "summary": {
                "error_count": len(self.errors),
                "warning_count": len(self.warnings),
                "status": "PASS" if not self.errors else "FAIL",
            },
        }
