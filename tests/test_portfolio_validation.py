"""
Tests for portfolio validation rules from PORTVALD.cbl and PORTMSTR.cbl.

These tests encode the business rules for validating portfolio data elements
in the Investment Portfolio Management System.
"""

import pytest

from tests.business_rules.validators import (
    validate_portfolio_id,
    validate_account_number,
    validate_investment_type,
    validate_portfolio_name,
    validate_portfolio_status,
    validate_client_type,
    validate_amount_range,
    AMOUNT_MIN,
    AMOUNT_MAX,
)


# =====================================================================
# Portfolio ID Validation
# Reference: PORTVALD.cbl 1000-VALIDATE-ID
# Rule: Must start with 'PORT' followed by exactly 4 numeric digits
# =====================================================================
class TestPortfolioIdValidation:
    """Portfolio ID must start with 'PORT' followed by 4 numeric digits."""

    @pytest.mark.parametrize("port_id", ["PORT0001", "PORT9999"])
    def test_valid_portfolio_ids(self, port_id):
        result = validate_portfolio_id(port_id)
        assert result.valid is True
        assert result.error_message == ""

    def test_valid_portfolio_id_boundary_low(self):
        result = validate_portfolio_id("PORT0000")
        assert result.valid is True

    def test_invalid_prefix(self):
        result = validate_portfolio_id("ABCD0001")
        assert result.valid is False
        assert "Invalid Portfolio ID" in result.error_message

    def test_too_short_missing_digits(self):
        result = validate_portfolio_id("PORT")
        assert result.valid is False

    def test_non_numeric_suffix(self):
        result = validate_portfolio_id("PORTABCD")
        assert result.valid is False

    def test_too_long(self):
        result = validate_portfolio_id("PORT12345")
        assert result.valid is False

    def test_empty_string(self):
        result = validate_portfolio_id("")
        assert result.valid is False

    def test_case_sensitive_lowercase(self):
        result = validate_portfolio_id("port0001")
        assert result.valid is False

    def test_case_sensitive_mixed(self):
        result = validate_portfolio_id("Port0001")
        assert result.valid is False

    def test_three_digit_suffix(self):
        result = validate_portfolio_id("PORT123")
        assert result.valid is False

    def test_five_digit_suffix(self):
        result = validate_portfolio_id("PORT12345")
        assert result.valid is False


# =====================================================================
# Account Number Validation
# Reference: PORTVALD.cbl 2000-VALIDATE-ACCOUNT
# Rule: Must be exactly 10 numeric digits, cannot be all zeros
# =====================================================================
class TestAccountNumberValidation:
    """Account number must be exactly 10 numeric digits, not all zeros."""

    def test_valid_account_number(self):
        result = validate_account_number("1234567890")
        assert result.valid is True

    def test_valid_account_number_leading_zeros(self):
        result = validate_account_number("0000000001")
        assert result.valid is True

    def test_all_zeros_rejected(self):
        result = validate_account_number("0000000000")
        assert result.valid is False
        assert "Invalid Account Number" in result.error_message

    def test_alphabetic_characters(self):
        result = validate_account_number("ABCDEFGHIJ")
        assert result.valid is False

    def test_too_short(self):
        result = validate_account_number("12345")
        assert result.valid is False

    def test_too_long(self):
        result = validate_account_number("12345678901")
        assert result.valid is False

    def test_empty_string(self):
        result = validate_account_number("")
        assert result.valid is False

    def test_mixed_alpha_numeric(self):
        result = validate_account_number("12345ABCDE")
        assert result.valid is False

    def test_special_characters(self):
        result = validate_account_number("123-456-78")
        assert result.valid is False


# =====================================================================
# Investment Type Validation
# Reference: PORTVALD.cbl 3000-VALIDATE-TYPE
# Rule: Must be one of 'STK', 'BND', 'MMF', 'ETF'
# =====================================================================
class TestInvestmentTypeValidation:
    """Investment type must be one of STK, BND, MMF, ETF."""

    @pytest.mark.parametrize("inv_type", ["STK", "BND", "MMF", "ETF"])
    def test_valid_investment_types(self, inv_type):
        result = validate_investment_type(inv_type)
        assert result.valid is True

    def test_invalid_type_abc(self):
        result = validate_investment_type("ABC")
        assert result.valid is False
        assert "Invalid Investment Type" in result.error_message

    def test_case_sensitive_lowercase(self):
        result = validate_investment_type("stk")
        assert result.valid is False

    def test_empty_string(self):
        result = validate_investment_type("")
        assert result.valid is False

    def test_full_word(self):
        result = validate_investment_type("STOCK")
        assert result.valid is False

    def test_partial_match(self):
        result = validate_investment_type("ST")
        assert result.valid is False


# =====================================================================
# Portfolio Name Validation
# Reference: PORTMSTR.cbl 2100-VALIDATE-PORTFOLIO
# Rule: Required, cannot be blank or all spaces
# =====================================================================
class TestPortfolioNameValidation:
    """Portfolio name is required and cannot be blank/all spaces."""

    def test_valid_name(self):
        result = validate_portfolio_name("My Portfolio")
        assert result.valid is True

    def test_valid_name_with_numbers(self):
        result = validate_portfolio_name("Portfolio 123")
        assert result.valid is True

    def test_all_spaces(self):
        result = validate_portfolio_name("          ")
        assert result.valid is False
        assert "required" in result.error_message.lower()

    def test_empty_string(self):
        result = validate_portfolio_name("")
        assert result.valid is False

    def test_single_character(self):
        result = validate_portfolio_name("A")
        assert result.valid is True


# =====================================================================
# Portfolio Status Validation
# Reference: PORTMSTR.cbl WS-VALID-STATUS 88-level
# Rule: Must be 'A' (Active), 'I' (Inactive), or 'C' (Closed)
# =====================================================================
class TestPortfolioStatusValidation:
    """Portfolio status must be A, I, or C."""

    @pytest.mark.parametrize("status", ["A", "I", "C"])
    def test_valid_statuses(self, status):
        result = validate_portfolio_status(status)
        assert result.valid is True

    def test_invalid_status_x(self):
        result = validate_portfolio_status("X")
        assert result.valid is False
        assert "Invalid Portfolio Status" in result.error_message

    def test_case_sensitive_lowercase(self):
        result = validate_portfolio_status("a")
        assert result.valid is False

    def test_empty_string(self):
        result = validate_portfolio_status("")
        assert result.valid is False

    def test_invalid_status_d(self):
        result = validate_portfolio_status("D")
        assert result.valid is False


# =====================================================================
# Client Type Validation
# Rule: Must be 'I' (Individual), 'C' (Corporate), or 'T' (Trust)
# =====================================================================
class TestClientTypeValidation:
    """Client type must be I, C, or T."""

    @pytest.mark.parametrize("client_type", ["I", "C", "T"])
    def test_valid_client_types(self, client_type):
        result = validate_client_type(client_type)
        assert result.valid is True

    def test_invalid_client_type_x(self):
        result = validate_client_type("X")
        assert result.valid is False

    def test_empty_string(self):
        result = validate_client_type("")
        assert result.valid is False

    def test_case_sensitive_lowercase(self):
        result = validate_client_type("i")
        assert result.valid is False


# =====================================================================
# Amount Range Validation
# Reference: PORTVALD.cbl 4000-VALIDATE-AMOUNT
# Rule: Must be between -9999999999999.99 and +9999999999999.99
# =====================================================================
class TestAmountRangeValidation:
    """Amount must be within the allowed range."""

    def test_zero(self):
        result = validate_amount_range(0)
        assert result.valid is True

    def test_positive_amount(self):
        result = validate_amount_range(100.50)
        assert result.valid is True

    def test_negative_amount(self):
        result = validate_amount_range(-100.50)
        assert result.valid is True

    def test_max_boundary(self):
        result = validate_amount_range(AMOUNT_MAX)
        assert result.valid is True

    def test_min_boundary(self):
        result = validate_amount_range(AMOUNT_MIN)
        assert result.valid is True

    def test_exceeds_max(self):
        result = validate_amount_range(AMOUNT_MAX + 0.01)
        assert result.valid is False
        assert "out of valid range" in result.error_message.lower()

    def test_exceeds_min(self):
        result = validate_amount_range(AMOUNT_MIN - 0.01)
        assert result.valid is False
