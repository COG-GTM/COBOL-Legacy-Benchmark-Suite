"""
Tests for transaction processing rules from PORTTRAN.cbl.

These tests encode the business rules for validating and processing
portfolio transactions in the Investment Portfolio Management System.
"""

import pytest

from tests.business_rules.validators import (
    Portfolio,
    Transaction,
    validate_transaction_type,
    validate_transaction_portfolio_id,
    validate_transaction_quantity,
    validate_transaction_price,
    validate_transaction_amount,
    validate_transaction_status,
    process_buy,
    process_sell,
    process_fee,
    process_transfer,
    should_halt_processing,
    ERROR_THRESHOLD,
)


# =====================================================================
# Transaction Type Validation
# Reference: PORTTRAN.cbl 2120-CHECK-TRANSACTION-TYPE
# Rule: Must be 'BU', 'SL', 'TR', or 'FE'
# =====================================================================
class TestTransactionTypeValidation:
    """Transaction type must be BU, SL, TR, or FE."""

    @pytest.mark.parametrize("txn_type", ["BU", "SL", "TR", "FE"])
    def test_valid_transaction_types(self, txn_type):
        result = validate_transaction_type(txn_type)
        assert result.valid is True

    def test_invalid_type_xx(self):
        result = validate_transaction_type("XX")
        assert result.valid is False
        assert "Invalid Transaction Type" in result.error_message

    def test_full_word_buy(self):
        result = validate_transaction_type("BUY")
        assert result.valid is False

    def test_empty_string(self):
        result = validate_transaction_type("")
        assert result.valid is False

    def test_case_sensitive_lowercase(self):
        result = validate_transaction_type("bu")
        assert result.valid is False

    def test_single_character(self):
        result = validate_transaction_type("B")
        assert result.valid is False


# =====================================================================
# Portfolio ID Required for Transactions
# Reference: PORTTRAN.cbl 2110-CHECK-PORTFOLIO
# Rule: Blank/empty portfolio ID is rejected; portfolio must exist
# =====================================================================
class TestTransactionPortfolioId:
    """Portfolio ID must be present and exist in the master file."""

    def test_blank_portfolio_id_rejected(self):
        result = validate_transaction_portfolio_id("", {})
        assert result.valid is False
        assert "required" in result.error_message.lower()

    def test_spaces_only_rejected(self):
        result = validate_transaction_portfolio_id("        ", {})
        assert result.valid is False
        assert "required" in result.error_message.lower()

    def test_portfolio_must_exist_in_master(self):
        master = {"PORT0001": Portfolio(portfolio_id="PORT0001")}
        result = validate_transaction_portfolio_id("PORT0001", master)
        assert result.valid is True

    def test_portfolio_not_in_master(self):
        master = {"PORT0001": Portfolio(portfolio_id="PORT0001")}
        result = validate_transaction_portfolio_id("PORT9999", master)
        assert result.valid is False
        assert "Invalid Portfolio ID" in result.error_message


# =====================================================================
# Quantity Validation
# Reference: PORTTRAN.cbl 2130-CHECK-AMOUNTS
# Rule: Must be greater than zero for all transaction types
# =====================================================================
class TestQuantityValidation:
    """Quantity must be greater than zero for all transaction types."""

    def test_positive_quantity(self):
        result = validate_transaction_quantity(50.0)
        assert result.valid is True

    def test_zero_quantity_invalid(self):
        result = validate_transaction_quantity(0)
        assert result.valid is False
        assert "greater than zero" in result.error_message.lower()

    def test_negative_quantity_invalid(self):
        result = validate_transaction_quantity(-1)
        assert result.valid is False


# =====================================================================
# Price Validation
# Reference: PORTTRAN.cbl 2130-CHECK-AMOUNTS
# Rule: Must be > 0 for all types except 'TR' (transfers)
# =====================================================================
class TestPriceValidation:
    """Price must be > 0 for all types except TR."""

    @pytest.mark.parametrize("txn_type", ["BU", "SL", "FE"])
    def test_zero_price_invalid_for_non_transfer(self, txn_type):
        result = validate_transaction_price(0, txn_type)
        assert result.valid is False
        assert "greater than zero" in result.error_message.lower()

    def test_zero_price_valid_for_transfer(self):
        result = validate_transaction_price(0, "TR")
        assert result.valid is True

    def test_positive_price_valid(self):
        result = validate_transaction_price(10.0, "BU")
        assert result.valid is True

    @pytest.mark.parametrize("txn_type", ["BU", "SL", "FE"])
    def test_negative_price_invalid_for_non_transfer(self, txn_type):
        result = validate_transaction_price(-5.0, txn_type)
        assert result.valid is False


# =====================================================================
# Amount Validation
# Reference: PORTTRAN.cbl 2130-CHECK-AMOUNTS
# Rule: Must be > 0 for all types except 'TR'
# =====================================================================
class TestAmountValidation:
    """Amount must be > 0 for all types except TR."""

    @pytest.mark.parametrize("txn_type", ["BU", "SL", "FE"])
    def test_zero_amount_invalid_for_non_transfer(self, txn_type):
        result = validate_transaction_amount(0, txn_type)
        assert result.valid is False

    def test_zero_amount_valid_for_transfer(self):
        result = validate_transaction_amount(0, "TR")
        assert result.valid is True

    def test_positive_amount_valid(self):
        result = validate_transaction_amount(500.0, "BU")
        assert result.valid is True

    @pytest.mark.parametrize("txn_type", ["BU", "SL", "FE"])
    def test_negative_amount_invalid_for_non_transfer(self, txn_type):
        result = validate_transaction_amount(-100.0, txn_type)
        assert result.valid is False


# =====================================================================
# Buy Processing
# Reference: PORTTRAN.cbl 2210-PROCESS-BUY
# Rule: Adds quantity to total units and amount to total cost
# =====================================================================
class TestBuyProcessing:
    """Buy adds quantity to total units and amount to total cost."""

    def test_buy_adds_units(self, valid_portfolio, valid_transaction):
        result = process_buy(valid_portfolio, valid_transaction)
        assert result.success is True
        assert result.portfolio is not None
        assert result.portfolio.total_units == 150.0  # 100 + 50

    def test_buy_adds_cost(self, valid_portfolio, valid_transaction):
        result = process_buy(valid_portfolio, valid_transaction)
        assert result.success is True
        assert result.portfolio is not None
        assert result.portfolio.total_cost == 1500.0  # 1000 + 500

    def test_buy_specific_values(self, make_portfolio, make_transaction):
        portfolio = make_portfolio(total_units=100, total_cost=1000)
        txn = make_transaction(transaction_type="BU", quantity=50, amount=500)
        result = process_buy(portfolio, txn)
        assert result.portfolio.total_units == 150
        assert result.portfolio.total_cost == 1500


# =====================================================================
# Sell Processing
# Reference: PORTTRAN.cbl 2220-PROCESS-SELL
# Rule: Subtracts from portfolio; rejects if insufficient units
# =====================================================================
class TestSellProcessing:
    """Sell subtracts from portfolio; rejects if insufficient units."""

    def test_sell_success(self, make_portfolio, make_transaction):
        portfolio = make_portfolio(total_units=100, total_cost=1000)
        txn = make_transaction(transaction_type="SL", quantity=50, amount=500)
        result = process_sell(portfolio, txn)
        assert result.success is True
        assert result.portfolio.total_units == 50.0
        assert result.portfolio.total_cost == 500.0

    def test_sell_insufficient_units(self, make_portfolio, make_transaction):
        portfolio = make_portfolio(total_units=100, total_cost=1000)
        txn = make_transaction(transaction_type="SL", quantity=150, amount=1500)
        result = process_sell(portfolio, txn)
        assert result.success is False
        assert result.error_message == "Insufficient units for sale"

    def test_sell_exact_units(self, make_portfolio, make_transaction):
        portfolio = make_portfolio(total_units=100, total_cost=1000)
        txn = make_transaction(transaction_type="SL", quantity=100, amount=1000)
        result = process_sell(portfolio, txn)
        assert result.success is True
        assert result.portfolio.total_units == 0.0


# =====================================================================
# Fee Processing
# Reference: PORTTRAN.cbl 2240-PROCESS-FEE
# Rule: Subtracts fee amount from total cost; units unchanged
# =====================================================================
class TestFeeProcessing:
    """Fee subtracts from total cost; units remain unchanged."""

    def test_fee_deducts_cost(self, make_portfolio, make_transaction):
        portfolio = make_portfolio(total_units=100, total_cost=1000)
        txn = make_transaction(transaction_type="FE", amount=50)
        result = process_fee(portfolio, txn)
        assert result.success is True
        assert result.portfolio.total_cost == 950.0

    def test_fee_units_unchanged(self, make_portfolio, make_transaction):
        portfolio = make_portfolio(total_units=100, total_cost=1000)
        txn = make_transaction(transaction_type="FE", amount=50)
        result = process_fee(portfolio, txn)
        assert result.portfolio.total_units == 100.0


# =====================================================================
# Transfer Processing
# Reference: PORTTRAN.cbl 2230-PROCESS-TRANSFER
# Rule: Always returns error "Transfer processing not implemented"
# =====================================================================
class TestTransferProcessing:
    """Transfer always returns 'Transfer processing not implemented'."""

    def test_transfer_returns_error(self, valid_portfolio, valid_transaction):
        result = process_transfer(valid_portfolio, valid_transaction)
        assert result.success is False
        assert result.error_message == "Transfer processing not implemented"


# =====================================================================
# Error Threshold
# Reference: PORTTRAN.cbl 0000-MAIN (WS-ERROR-COUNT > 100)
# Rule: Processing halts when error count exceeds 100
# =====================================================================
class TestErrorThreshold:
    """Processing halts when error count exceeds 100."""

    def test_at_threshold_no_halt(self):
        assert should_halt_processing(ERROR_THRESHOLD) is False

    def test_above_threshold_halts(self):
        assert should_halt_processing(ERROR_THRESHOLD + 1) is True

    def test_below_threshold_no_halt(self):
        assert should_halt_processing(50) is False

    def test_zero_errors_no_halt(self):
        assert should_halt_processing(0) is False


# =====================================================================
# Transaction Status Lifecycle
# Rule: Valid statuses are P (Pending), D (Done), F (Failed), R (Reversed)
# =====================================================================
class TestTransactionStatusLifecycle:
    """Transaction status must be P, D, F, or R."""

    @pytest.mark.parametrize("status", ["P", "D", "F", "R"])
    def test_valid_statuses(self, status):
        result = validate_transaction_status(status)
        assert result.valid is True

    @pytest.mark.parametrize("status", ["X", "A", "", "p"])
    def test_invalid_statuses(self, status):
        result = validate_transaction_status(status)
        assert result.valid is False
