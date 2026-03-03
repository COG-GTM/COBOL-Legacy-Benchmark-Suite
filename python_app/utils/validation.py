"""Data Validation module - replaces UTLVAL00.cbl.

Provides data validation utilities for integrity, cross-reference,
format, and balance checks.

COBOL program flow (EVALUATE LS-VAL-FUNCTION):
- INTEGRITY: Data integrity checks (P100-CHECK-INTEGRITY)
- XREF: Cross-reference validation (P200-CHECK-XREF)
- FORMAT: Format validation (P300-CHECK-FORMAT)
- BALANCE: Balance reconciliation (P400-CHECK-BALANCE)
"""

import logging
import re
from datetime import datetime
from decimal import Decimal
from typing import Any

from python_app.models.position import PositionRecord
from python_app.models.portfolio import PortfolioRecord
from python_app.models.transaction import TransactionRecord

logger = logging.getLogger("portfolio.utils.validation")


class ValidationResult:
    """Result of a validation check."""

    def __init__(self, check_name: str) -> None:
        self.check_name = check_name
        self.passed = True
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.records_checked = 0
        self.timestamp = datetime.now().isoformat()

    def add_error(self, message: str) -> None:
        self.errors.append(message)
        self.passed = False

    def add_warning(self, message: str) -> None:
        self.warnings.append(message)

    def to_dict(self) -> dict[str, Any]:
        return {
            "check_name": self.check_name,
            "passed": self.passed,
            "errors": self.errors,
            "warnings": self.warnings,
            "records_checked": self.records_checked,
            "timestamp": self.timestamp,
        }


class DataValidator:
    """Data validator replacing UTLVAL00.cbl.

    Provides comprehensive data validation across the system.
    """

    def __init__(self) -> None:
        self.results: list[ValidationResult] = []

    def check_integrity(
        self,
        portfolios: list[PortfolioRecord],
        positions: list[PositionRecord],
    ) -> ValidationResult:
        """Check data integrity - replaces P100-CHECK-INTEGRITY.

        Validates:
        - All positions reference valid portfolios
        - No orphaned records
        - Key uniqueness
        """
        result = ValidationResult("INTEGRITY")

        portfolio_ids = {p.id for p in portfolios}
        position_keys: set[str] = set()

        for pos in positions:
            result.records_checked += 1

            # Check portfolio reference
            if pos.portfolio_id not in portfolio_ids:
                result.add_error(
                    f"Position references non-existent portfolio: {pos.portfolio_id}"
                )

            # Check key uniqueness
            key = pos.composite_key
            if key in position_keys:
                result.add_error(f"Duplicate position key: {key}")
            position_keys.add(key)

        self.results.append(result)
        logger.info(
            "UTLVAL00 INTEGRITY: checked=%d, passed=%s, errors=%d",
            result.records_checked, result.passed, len(result.errors),
        )
        return result

    def check_xref(
        self,
        transactions: list[TransactionRecord],
        positions: list[PositionRecord],
    ) -> ValidationResult:
        """Cross-reference validation - replaces P200-CHECK-XREF.

        Validates:
        - All transactions reference valid positions or create new ones
        - Transaction amounts are consistent
        """
        result = ValidationResult("XREF")

        position_keys = {
            f"{p.portfolio_id}:{p.investment_id}" for p in positions
        }

        for txn in transactions:
            result.records_checked += 1
            key = f"{txn.portfolio_id}:{txn.investment_id}"

            # SELL/TRANSFER should reference existing position
            if txn.type in ("SL", "TR") and key not in position_keys:
                result.add_warning(
                    f"Transaction {txn.composite_key} references non-existent position: {key}"
                )

        self.results.append(result)
        logger.info(
            "UTLVAL00 XREF: checked=%d, passed=%s, errors=%d, warnings=%d",
            result.records_checked, result.passed,
            len(result.errors), len(result.warnings),
        )
        return result

    def check_format(
        self,
        transactions: list[TransactionRecord],
    ) -> ValidationResult:
        """Format validation - replaces P300-CHECK-FORMAT.

        Validates:
        - Date format (YYYYMMDD)
        - Time format (HHMMSS)
        - Currency code (3 alpha)
        - Numeric field ranges
        """
        result = ValidationResult("FORMAT")

        date_pattern = re.compile(r"^\d{8}$")
        time_pattern = re.compile(r"^\d{6}$")
        currency_pattern = re.compile(r"^[A-Z]{3}$")

        for txn in transactions:
            result.records_checked += 1

            if not date_pattern.match(txn.date):
                result.add_error(f"Invalid date format: {txn.date} in {txn.composite_key}")

            if not time_pattern.match(txn.time):
                result.add_error(f"Invalid time format: {txn.time} in {txn.composite_key}")

            if not currency_pattern.match(txn.currency):
                result.add_error(f"Invalid currency: {txn.currency} in {txn.composite_key}")

            if txn.quantity < 0:
                result.add_warning(f"Negative quantity: {txn.quantity} in {txn.composite_key}")

        self.results.append(result)
        logger.info(
            "UTLVAL00 FORMAT: checked=%d, passed=%s, errors=%d",
            result.records_checked, result.passed, len(result.errors),
        )
        return result

    def check_balance(
        self,
        positions: list[PositionRecord],
        portfolios: list[PortfolioRecord],
    ) -> ValidationResult:
        """Balance reconciliation - replaces P400-CHECK-BALANCE.

        Validates:
        - Sum of position market values matches portfolio total
        - No negative quantities for active positions
        """
        result = ValidationResult("BALANCE")

        # Group positions by portfolio
        port_totals: dict[str, Decimal] = {}
        for pos in positions:
            result.records_checked += 1
            port_totals.setdefault(pos.portfolio_id, Decimal("0"))
            port_totals[pos.portfolio_id] += pos.market_value

            if pos.status == "A" and pos.quantity < 0:
                result.add_error(
                    f"Active position with negative quantity: "
                    f"{pos.portfolio_id}/{pos.investment_id} = {pos.quantity}"
                )

        # Check against portfolio totals
        for portfolio in portfolios:
            if portfolio.total_value > 0:
                calculated = port_totals.get(portfolio.id, Decimal("0"))
                if abs(calculated - portfolio.total_value) > Decimal("0.01"):
                    result.add_warning(
                        f"Portfolio {portfolio.id} balance mismatch: "
                        f"calculated={calculated}, recorded={portfolio.total_value}"
                    )

        self.results.append(result)
        logger.info(
            "UTLVAL00 BALANCE: checked=%d, passed=%s, errors=%d",
            result.records_checked, result.passed, len(result.errors),
        )
        return result

    def get_summary(self) -> dict[str, Any]:
        """Get validation summary across all checks."""
        return {
            "total_checks": len(self.results),
            "passed": sum(1 for r in self.results if r.passed),
            "failed": sum(1 for r in self.results if not r.passed),
            "total_errors": sum(len(r.errors) for r in self.results),
            "total_warnings": sum(len(r.warnings) for r in self.results),
            "details": [r.to_dict() for r in self.results],
        }
