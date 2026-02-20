"""Test Validation Framework - migrated from TSTVAL00.cbl.

Validates system functionality through structured test cases covering
data integrity, processing accuracy, report generation, and error
handling.
"""

import logging
from dataclasses import dataclass
from datetime import datetime

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.position import PositionRecord
from portfolio_management.models.transaction import TransactionRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "TSTVAL00"


@dataclass
class TestResult:
    test_id: str = ""
    test_name: str = ""
    category: str = ""
    status: str = ""
    expected: str = ""
    actual: str = ""
    message: str = ""
    duration_ms: float = 0.0


class TestValidator:
    def __init__(self):
        self._results: list[TestResult] = []
        self._tests_passed = 0
        self._tests_failed = 0
        self._tests_skipped = 0

    def run_all_tests(
        self,
        portfolios: list[PortfolioRecord],
        positions: list[PositionRecord],
        transactions: list[TransactionRecord],
    ) -> int:
        self._results = []
        self._tests_passed = 0
        self._tests_failed = 0
        self._tests_skipped = 0

        self._test_data_integrity(portfolios, positions)
        self._test_processing_accuracy(portfolios, transactions)
        self._test_validation_rules(portfolios)
        self._test_edge_cases()

        self._display_results()

        if self._tests_failed > 0:
            return ReturnCode.ERROR
        return ReturnCode.SUCCESS

    def _test_data_integrity(
        self,
        portfolios: list[PortfolioRecord],
        positions: list[PositionRecord],
    ) -> None:
        self._run_test(
            "DI-001",
            "Portfolio records exist",
            "DATA_INTEGRITY",
            len(portfolios) > 0,
            "True",
            str(len(portfolios) > 0),
        )

        self._run_test(
            "DI-002",
            "Position records exist",
            "DATA_INTEGRITY",
            len(positions) > 0,
            "True",
            str(len(positions) > 0),
        )

        portfolio_ids = {p.port_id for p in portfolios}
        orphan_positions = [
            p for p in positions if p.portfolio_id not in portfolio_ids
        ]
        self._run_test(
            "DI-003",
            "No orphaned positions",
            "DATA_INTEGRITY",
            len(orphan_positions) == 0,
            "0",
            str(len(orphan_positions)),
        )

        for portfolio in portfolios:
            has_id = bool(portfolio.port_id)
            has_account = bool(portfolio.account_no)
            self._run_test(
                f"DI-004-{portfolio.port_id}",
                f"Portfolio {portfolio.port_id} has required fields",
                "DATA_INTEGRITY",
                has_id and has_account,
                "True",
                str(has_id and has_account),
            )

    def _test_processing_accuracy(
        self,
        portfolios: list[PortfolioRecord],
        transactions: list[TransactionRecord],
    ) -> None:
        for portfolio in portfolios:
            self._run_test(
                f"PA-001-{portfolio.port_id}",
                f"Portfolio {portfolio.port_id} non-negative value",
                "PROCESSING",
                portfolio.total_value >= 0,
                ">=0",
                str(portfolio.total_value),
            )

        for transaction in transactions:
            self._run_test(
                f"PA-002-{transaction.transaction_key[:20]}",
                "Transaction amount non-negative",
                "PROCESSING",
                transaction.amount >= 0,
                ">=0",
                str(transaction.amount),
            )

    def _test_validation_rules(self, portfolios: list[PortfolioRecord]) -> None:
        valid_statuses = {"A", "C", "S", "P"}
        for portfolio in portfolios:
            self._run_test(
                f"VR-001-{portfolio.port_id}",
                f"Portfolio {portfolio.port_id} valid status",
                "VALIDATION",
                portfolio.status in valid_statuses,
                f"one of {valid_statuses}",
                portfolio.status,
            )

    def _test_edge_cases(self) -> None:
        empty_record = PortfolioRecord()
        self._run_test(
            "EC-001",
            "Empty portfolio has zero value",
            "EDGE_CASE",
            empty_record.total_value == 0,
            "0",
            str(empty_record.total_value),
        )

        self._run_test(
            "EC-002",
            "Empty portfolio key is empty string concat",
            "EDGE_CASE",
            empty_record.port_key == "",
            "empty",
            repr(empty_record.port_key),
        )

    def _run_test(
        self,
        test_id: str,
        test_name: str,
        category: str,
        passed: bool,
        expected: str,
        actual: str,
    ) -> None:
        result = TestResult(
            test_id=test_id,
            test_name=test_name,
            category=category,
            status="PASS" if passed else "FAIL",
            expected=expected,
            actual=actual,
        )

        if passed:
            self._tests_passed += 1
        else:
            self._tests_failed += 1
            result.message = f"Expected {expected}, got {actual}"

        self._results.append(result)

    def _display_results(self) -> None:
        logger.info(
            "Test Validation Results:\n"
            "  Tests Passed:  %d\n"
            "  Tests Failed:  %d\n"
            "  Tests Skipped: %d\n"
            "  Total Tests:   %d",
            self._tests_passed,
            self._tests_failed,
            self._tests_skipped,
            len(self._results),
        )

        for result in self._results:
            if result.status == "FAIL":
                logger.error(
                    "  FAIL [%s] %s: %s",
                    result.test_id,
                    result.test_name,
                    result.message,
                )

    def get_results(self) -> list[TestResult]:
        return list(self._results)

    def get_summary(self) -> dict:
        return {
            "tests_passed": self._tests_passed,
            "tests_failed": self._tests_failed,
            "tests_skipped": self._tests_skipped,
            "total_tests": len(self._results),
            "pass_rate": (
                self._tests_passed / len(self._results) * 100
                if self._results
                else 0
            ),
        }

    def generate_report(self) -> str:
        lines = [
            "=" * 80,
            f"{'TEST VALIDATION REPORT':^80}",
            f"{'Generated: ' + datetime.now().strftime('%Y-%m-%d %H:%M:%S'):^80}",
            "=" * 80,
            "",
            f"Total Tests: {len(self._results)}",
            f"Passed:      {self._tests_passed}",
            f"Failed:      {self._tests_failed}",
            f"Skipped:     {self._tests_skipped}",
            "",
            "-" * 80,
            f"{'ID':<15} {'Name':<35} {'Status':<8} {'Category':<15}",
            "-" * 80,
        ]

        for result in self._results:
            lines.append(
                f"{result.test_id:<15} {result.test_name[:35]:<35} "
                f"{result.status:<8} {result.category:<15}"
            )
            if result.status == "FAIL":
                lines.append(f"{'':>15} -> {result.message}")

        lines.extend(["", "=" * 80])
        return "\n".join(lines)
