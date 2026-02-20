"""Data Validation Utility - migrated from UTLVAL00.cbl.

Performs comprehensive data validation including data integrity checks,
cross-reference validation, format verification, and balance reconciliation.
"""

import logging
from decimal import Decimal

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.position import PositionRecord
from portfolio_management.models.transaction import TransactionRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "UTLVAL00"


class ValidationIssue:
    def __init__(self, category: str, severity: str, key: str, message: str):
        self.category = category
        self.severity = severity
        self.key = key
        self.message = message


class DataValidation:
    def __init__(self):
        self._issues: list[ValidationIssue] = []
        self._records_checked = 0
        self._errors_found = 0
        self._warnings_found = 0

    def validate_all(
        self,
        portfolios: list[PortfolioRecord],
        positions: list[PositionRecord],
        transactions: list[TransactionRecord],
    ) -> int:
        self._issues = []
        self._records_checked = 0
        self._errors_found = 0
        self._warnings_found = 0

        self._check_data_integrity(portfolios, positions)
        self._check_cross_references(portfolios, positions, transactions)
        self._check_format_verification(portfolios)
        self._check_balance_reconciliation(portfolios, positions)

        self._display_results()

        if self._errors_found > 0:
            return ReturnCode.ERROR
        if self._warnings_found > 0:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def _check_data_integrity(
        self, portfolios: list[PortfolioRecord], positions: list[PositionRecord]
    ) -> None:
        logger.info("Checking data integrity")

        for portfolio in portfolios:
            self._records_checked += 1

            if not portfolio.port_id:
                self._add_issue("INTEGRITY", "ERROR", portfolio.port_key, "Missing portfolio ID")
            if not portfolio.account_no:
                self._add_issue("INTEGRITY", "ERROR", portfolio.port_key, "Missing account number")
            if portfolio.total_value < 0 and portfolio.status == "A":
                self._add_issue(
                    "INTEGRITY", "WARNING", portfolio.port_key,
                    f"Active portfolio with negative value: {portfolio.total_value}",
                )

        for position in positions:
            self._records_checked += 1

            if not position.portfolio_id:
                self._add_issue("INTEGRITY", "ERROR", position.position_key, "Missing portfolio ID")
            if position.quantity == 0 and position.market_value != 0:
                self._add_issue(
                    "INTEGRITY", "WARNING", position.position_key,
                    "Zero quantity with non-zero market value",
                )

    def _check_cross_references(
        self,
        portfolios: list[PortfolioRecord],
        positions: list[PositionRecord],
        transactions: list[TransactionRecord],
    ) -> None:
        logger.info("Checking cross-references")

        portfolio_ids = {p.port_id for p in portfolios}

        for position in positions:
            if position.portfolio_id not in portfolio_ids:
                self._add_issue(
                    "XREF", "ERROR", position.position_key,
                    f"Position references non-existent portfolio: {position.portfolio_id}",
                )

        for transaction in transactions:
            if transaction.portfolio_id not in portfolio_ids:
                self._add_issue(
                    "XREF", "ERROR", transaction.transaction_key,
                    f"Transaction references non-existent portfolio: {transaction.portfolio_id}",
                )

    def _check_format_verification(self, portfolios: list[PortfolioRecord]) -> None:
        logger.info("Checking format verification")

        for portfolio in portfolios:
            if portfolio.port_id and not portfolio.port_id.startswith("PORT"):
                self._add_issue(
                    "FORMAT", "WARNING", portfolio.port_key,
                    f"Portfolio ID does not follow naming convention: {portfolio.port_id}",
                )

            if portfolio.status and portfolio.status not in ("A", "C", "S", "P"):
                self._add_issue(
                    "FORMAT", "ERROR", portfolio.port_key,
                    f"Invalid status code: {portfolio.status}",
                )

    def _check_balance_reconciliation(
        self, portfolios: list[PortfolioRecord], positions: list[PositionRecord]
    ) -> None:
        logger.info("Checking balance reconciliation")

        portfolio_position_values: dict[str, Decimal] = {}
        for position in positions:
            pid = position.portfolio_id
            if pid not in portfolio_position_values:
                portfolio_position_values[pid] = Decimal("0")
            portfolio_position_values[pid] += position.market_value

        for portfolio in portfolios:
            position_total = portfolio_position_values.get(portfolio.port_id, Decimal("0"))
            expected_total = position_total + portfolio.cash_balance

            if portfolio.total_value != 0 and expected_total != 0:
                diff = abs(portfolio.total_value - expected_total)
                if diff > Decimal("0.01"):
                    self._add_issue(
                        "BALANCE", "WARNING", portfolio.port_key,
                        f"Balance mismatch: Portfolio={portfolio.total_value}, "
                        f"Calculated={expected_total}, Diff={diff}",
                    )

    def _add_issue(self, category: str, severity: str, key: str, message: str) -> None:
        self._issues.append(ValidationIssue(category, severity, key, message))
        if severity == "ERROR":
            self._errors_found += 1
        else:
            self._warnings_found += 1

    def _display_results(self) -> None:
        logger.info(
            "Data Validation Results:\n"
            "  Records Checked: %d\n"
            "  Errors Found:    %d\n"
            "  Warnings Found:  %d",
            self._records_checked,
            self._errors_found,
            self._warnings_found,
        )

        for issue in self._issues:
            logger.log(
                logging.ERROR if issue.severity == "ERROR" else logging.WARNING,
                "  [%s] %s: %s - %s",
                issue.severity,
                issue.category,
                issue.key,
                issue.message,
            )

    def get_issues(self) -> list[ValidationIssue]:
        return list(self._issues)

    def get_statistics(self) -> dict:
        return {
            "records_checked": self._records_checked,
            "errors_found": self._errors_found,
            "warnings_found": self._warnings_found,
        }
