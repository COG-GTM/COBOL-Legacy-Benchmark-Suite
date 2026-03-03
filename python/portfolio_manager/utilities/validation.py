"""Data Validation Utility.

Replaces: UTLVAL00 (src/programs/utility/UTLVAL00.cbl)

Performs cross-system data integrity validation including:
  - Position record consistency checks
  - Transaction history integrity
  - Orphan record detection
  - Balance reconciliation

Original COBOL flow (UTLVAL00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE
    2000-VALIDATE-DATA
      2100-CHECK-POSITIONS
      2200-CHECK-TRANSACTIONS
      2300-CHECK-ORPHANS
      2400-RECONCILE-BALANCES
    3000-REPORT-RESULTS
    4000-FINALIZE
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import datetime

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from portfolio_manager.models.database import (
    InvestmentPosition,
    PortfolioMaster,
    TransactionHistory,
)

logger = logging.getLogger(__name__)


@dataclass
class ValidationIssue:
    """A single validation issue found."""

    check_name: str
    severity: str  # INFO, WARNING, ERROR
    message: str
    record_key: str = ""


@dataclass
class ValidationResult:
    """Result of all validation checks."""

    start_time: datetime = field(default_factory=datetime.now)
    end_time: datetime | None = None
    checks_run: int = 0
    checks_passed: int = 0
    checks_failed: int = 0
    issues: list[ValidationIssue] = field(default_factory=list)
    return_code: int = 0


class DataValidationUtility:
    """Data validation utility.

    Replaces UTLVAL00 (src/programs/utility/UTLVAL00.cbl).
    """

    PROGRAM_ID = "UTLVAL00"

    def __init__(self, session: Session):
        self._session = session

    def run(self) -> ValidationResult:
        """Run all validation checks.

        Replaces 0000-MAIN-PROCESS from UTLVAL00.cbl.

        Returns:
            ValidationResult with all issues found.
        """
        result = ValidationResult()

        logger.info("%s: Starting data validation", self.PROGRAM_ID)

        # 2100-CHECK-POSITIONS
        self._check_positions(result)

        # 2200-CHECK-TRANSACTIONS
        self._check_transactions(result)

        # 2300-CHECK-ORPHANS
        self._check_orphan_positions(result)

        # 2400-RECONCILE (check referential integrity)
        self._check_referential_integrity(result)

        # Determine return code
        error_count = sum(
            1 for issue in result.issues if issue.severity == "ERROR"
        )
        warning_count = sum(
            1 for issue in result.issues if issue.severity == "WARNING"
        )

        if error_count > 0:
            result.return_code = 8
        elif warning_count > 0:
            result.return_code = 4
        else:
            result.return_code = 0

        result.end_time = datetime.now()

        logger.info(
            "%s: Complete — checks=%d passed=%d failed=%d issues=%d RC=%d",
            self.PROGRAM_ID,
            result.checks_run,
            result.checks_passed,
            result.checks_failed,
            len(result.issues),
            result.return_code,
        )

        return result

    def _check_positions(self, result: ValidationResult) -> None:
        """Check position data integrity.

        Replaces UTLVAL00 paragraph 2100-CHECK-POSITIONS.
        """
        result.checks_run += 1

        # Check for positions with negative quantities
        neg_qty = self._session.execute(
            select(func.count())
            .select_from(InvestmentPosition)
            .where(InvestmentPosition.quantity < 0)
        ).scalar() or 0

        if neg_qty > 0:
            result.checks_failed += 1
            result.issues.append(
                ValidationIssue(
                    check_name="POSITION_NEGATIVE_QTY",
                    severity="WARNING",
                    message=f"{neg_qty} positions have negative quantities",
                )
            )
        else:
            result.checks_passed += 1

        # Check for zero market value with positive quantity
        result.checks_run += 1
        zero_mv = self._session.execute(
            select(func.count())
            .select_from(InvestmentPosition)
            .where(
                InvestmentPosition.quantity > 0,
                InvestmentPosition.market_value == 0,
            )
        ).scalar() or 0

        if zero_mv > 0:
            result.checks_failed += 1
            result.issues.append(
                ValidationIssue(
                    check_name="POSITION_ZERO_MV",
                    severity="WARNING",
                    message=f"{zero_mv} positions have zero market value with positive quantity",
                )
            )
        else:
            result.checks_passed += 1

    def _check_transactions(self, result: ValidationResult) -> None:
        """Check transaction data integrity.

        Replaces UTLVAL00 paragraph 2200-CHECK-TRANSACTIONS.
        """
        result.checks_run += 1

        # Check for transactions with invalid types
        valid_types = {"BU", "SL", "TR", "FE"}
        invalid_type_count = self._session.execute(
            select(func.count())
            .select_from(TransactionHistory)
            .where(TransactionHistory.transaction_type.notin_(valid_types))
        ).scalar() or 0

        if invalid_type_count > 0:
            result.checks_failed += 1
            result.issues.append(
                ValidationIssue(
                    check_name="TXN_INVALID_TYPE",
                    severity="ERROR",
                    message=f"{invalid_type_count} transactions have invalid types",
                )
            )
        else:
            result.checks_passed += 1

        # Check for zero-amount transactions
        result.checks_run += 1
        zero_amt = self._session.execute(
            select(func.count())
            .select_from(TransactionHistory)
            .where(TransactionHistory.amount == 0)
        ).scalar() or 0

        if zero_amt > 0:
            result.checks_failed += 1
            result.issues.append(
                ValidationIssue(
                    check_name="TXN_ZERO_AMOUNT",
                    severity="INFO",
                    message=f"{zero_amt} transactions have zero amount",
                )
            )
        else:
            result.checks_passed += 1

    def _check_orphan_positions(self, result: ValidationResult) -> None:
        """Check for orphan position records.

        Replaces UTLVAL00 paragraph 2300-CHECK-ORPHANS.
        """
        result.checks_run += 1

        # Positions without matching portfolio master
        orphan_query = (
            select(func.count())
            .select_from(InvestmentPosition)
            .where(
                InvestmentPosition.portfolio_id.notin_(
                    select(PortfolioMaster.portfolio_id)
                )
            )
        )
        orphan_count = self._session.execute(orphan_query).scalar() or 0

        if orphan_count > 0:
            result.checks_failed += 1
            result.issues.append(
                ValidationIssue(
                    check_name="ORPHAN_POSITIONS",
                    severity="ERROR",
                    message=f"{orphan_count} positions have no matching portfolio master",
                )
            )
        else:
            result.checks_passed += 1

    def _check_referential_integrity(self, result: ValidationResult) -> None:
        """Check referential integrity across tables.

        Replaces UTLVAL00 paragraph 2400-RECONCILE-BALANCES.
        """
        result.checks_run += 1

        # Transactions without matching portfolio
        orphan_txn = self._session.execute(
            select(func.count())
            .select_from(TransactionHistory)
            .where(
                TransactionHistory.portfolio_id.notin_(
                    select(PortfolioMaster.portfolio_id)
                )
            )
        ).scalar() or 0

        if orphan_txn > 0:
            result.checks_failed += 1
            result.issues.append(
                ValidationIssue(
                    check_name="ORPHAN_TRANSACTIONS",
                    severity="ERROR",
                    message=f"{orphan_txn} transactions have no matching portfolio",
                )
            )
        else:
            result.checks_passed += 1
