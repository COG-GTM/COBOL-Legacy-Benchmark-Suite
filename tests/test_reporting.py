"""
Tests for reporting rules from RPTPOS00.cbl.

These tests encode the business rules for position reporting calculations
in the Investment Portfolio Management System.
"""

import pytest

from tests.business_rules.validators import calculate_position_change_pct


# =====================================================================
# Position Change Percentage
# Reference: RPTPOS00.cbl 2110-FORMAT-POSITION
# Rule: (current_value - previous_value) / previous_value * 100
# =====================================================================
class TestPositionChangePercentage:
    """Position change pct = (current - previous) / previous * 100."""

    def test_positive_change(self):
        result = calculate_position_change_pct(110, 100)
        assert result == pytest.approx(10.0)

    def test_negative_change(self):
        result = calculate_position_change_pct(50, 100)
        assert result == pytest.approx(-50.0)

    def test_no_change(self):
        result = calculate_position_change_pct(100, 100)
        assert result == pytest.approx(0.0)

    def test_double_value(self):
        result = calculate_position_change_pct(200, 100)
        assert result == pytest.approx(100.0)

    def test_small_change(self):
        result = calculate_position_change_pct(100.5, 100)
        assert result == pytest.approx(0.5)

    def test_previous_value_zero_returns_none(self):
        """Division by zero guard: returns None when previous_value is 0."""
        result = calculate_position_change_pct(100, 0)
        assert result is None

    def test_both_zero_returns_none(self):
        """Both current and previous are zero -> division by zero."""
        result = calculate_position_change_pct(0, 0)
        assert result is None

    def test_current_zero_previous_nonzero(self):
        result = calculate_position_change_pct(0, 100)
        assert result == pytest.approx(-100.0)

    def test_large_values(self):
        result = calculate_position_change_pct(1_000_000, 500_000)
        assert result == pytest.approx(100.0)

    def test_fractional_values(self):
        result = calculate_position_change_pct(10.5, 10.0)
        assert result == pytest.approx(5.0)
