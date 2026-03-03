"""Test Validation module - replaces TSTVAL00.cbl.

Compares actual vs expected results for translation verification.
Used to validate that the Python migration produces equivalent
output to the original COBOL system.

COBOL program flow (EVALUATE LS-TST-FUNCTION):
- COMP: Compare actual vs expected (P100-COMPARE)
- SUMM: Generate summary report (P200-SUMMARY)
- DETL: Generate detail report (P300-DETAIL)
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Any

logger = logging.getLogger("portfolio.utils.test_validation")


class ComparisonResult:
    """Result of a single field comparison."""

    def __init__(
        self,
        field_name: str,
        expected: Any,
        actual: Any,
        matched: bool,
        tolerance: float = 0.0,
    ) -> None:
        self.field_name = field_name
        self.expected = expected
        self.actual = actual
        self.matched = matched
        self.tolerance = tolerance


class TestValidator:
    """Test validator replacing TSTVAL00.cbl.

    Compares actual output from the Python system against expected
    results to verify migration accuracy.
    """

    def __init__(self, test_name: str = "") -> None:
        self.test_name = test_name
        self.comparisons: list[ComparisonResult] = []
        self.passed = 0
        self.failed = 0
        self.total = 0

    def compare_records(
        self,
        expected: dict[str, Any],
        actual: dict[str, Any],
        numeric_tolerance: float = 0.01,
    ) -> list[ComparisonResult]:
        """Compare actual vs expected records - replaces P100-COMPARE.

        Handles different data types:
        - Strings: exact match (case-insensitive strip)
        - Decimals: within tolerance
        - Integers: exact match
        - Lists: element-wise comparison
        """
        results: list[ComparisonResult] = []

        all_fields = set(expected.keys()) | set(actual.keys())

        for field in sorted(all_fields):
            exp_val = expected.get(field)
            act_val = actual.get(field)
            self.total += 1

            if exp_val is None and act_val is not None:
                result = ComparisonResult(field, exp_val, act_val, False)
                self.failed += 1
            elif exp_val is not None and act_val is None:
                result = ComparisonResult(field, exp_val, act_val, False)
                self.failed += 1
            elif isinstance(exp_val, (Decimal, float)) and isinstance(act_val, (Decimal, float)):
                diff = abs(float(exp_val) - float(act_val))
                matched = diff <= numeric_tolerance
                result = ComparisonResult(field, exp_val, act_val, matched, numeric_tolerance)
                if matched:
                    self.passed += 1
                else:
                    self.failed += 1
            elif isinstance(exp_val, str) and isinstance(act_val, str):
                matched = exp_val.strip().upper() == act_val.strip().upper()
                result = ComparisonResult(field, exp_val, act_val, matched)
                if matched:
                    self.passed += 1
                else:
                    self.failed += 1
            else:
                matched = exp_val == act_val
                result = ComparisonResult(field, exp_val, act_val, matched)
                if matched:
                    self.passed += 1
                else:
                    self.failed += 1

            results.append(result)

        self.comparisons.extend(results)
        return results

    def get_summary(self) -> dict[str, Any]:
        """Generate summary report - replaces P200-SUMMARY."""
        return {
            "test_name": self.test_name,
            "timestamp": datetime.now().isoformat(),
            "total_comparisons": self.total,
            "passed": self.passed,
            "failed": self.failed,
            "pass_rate": round(self.passed / self.total * 100, 2) if self.total > 0 else 0,
            "status": "PASS" if self.failed == 0 else "FAIL",
        }

    def get_detail_report(self) -> str:
        """Generate detail report - replaces P300-DETAIL."""
        lines = [
            "=" * 80,
            f" TEST VALIDATION REPORT: {self.test_name}",
            "=" * 80,
            f" Total: {self.total}  Passed: {self.passed}  Failed: {self.failed}",
            "-" * 80,
        ]

        for comp in self.comparisons:
            status = "PASS" if comp.matched else "FAIL"
            lines.append(
                f" [{status}] {comp.field_name}: "
                f"expected={comp.expected}, actual={comp.actual}"
            )

        lines.extend([
            "-" * 80,
            f" Result: {'PASS' if self.failed == 0 else 'FAIL'}",
            f" Generated: {datetime.now().isoformat()}",
            "=" * 80,
        ])
        return "\n".join(lines)

    def reset(self) -> None:
        """Reset validator for new test run."""
        self.comparisons.clear()
        self.passed = 0
        self.failed = 0
        self.total = 0
