"""
Model validation tests translated from COBOL TSTVAL00.cbl.

Tests:
- Pydantic model field validation
- Enum values match COBOL level-88 conditions
- Decimal precision matches COBOL COMP-3 PIC clauses
- String field length constraints
"""

from datetime import date
from decimal import Decimal

import pytest

from src.common.constants import (
    AccountType,
    AuditAction,
    AuditStatus,
    AuditType,
    BatchFunction,
    BatchStatus,
    ClientType,
    PortfolioStatus,
    PositionStatus,
    ReturnCode,
    RiskLevel,
    TransactionStatus,
    TransactionType,
)
from src.models.portfolio import PortfolioRecord
from src.models.position import PositionRecord
from src.models.transaction import TransactionRecord


class TestReturnCodes:
    """Verify return code mapping matches COBOL RTNCODE.cpy."""

    def test_success(self):
        assert ReturnCode.SUCCESS == 0

    def test_warning(self):
        assert ReturnCode.WARNING == 4

    def test_error(self):
        assert ReturnCode.ERROR == 8

    def test_severe(self):
        assert ReturnCode.SEVERE == 12

    def test_fatal(self):
        assert ReturnCode.FATAL == 16


class TestPortfolioEnums:
    """Verify enum values match COBOL level-88 conditions."""

    def test_portfolio_status_values(self):
        assert PortfolioStatus.ACTIVE == "A"
        assert PortfolioStatus.CLOSED == "C"
        assert PortfolioStatus.SUSPENDED == "S"
        assert PortfolioStatus.PENDING == "P"

    def test_account_type_values(self):
        assert AccountType.INDIVIDUAL == "IN"
        assert AccountType.JOINT == "JT"
        assert AccountType.CORPORATE == "CO"
        assert AccountType.TRUST == "TR"
        assert AccountType.RETIREMENT == "RT"

    def test_risk_level_values(self):
        assert RiskLevel.LOW == "L"
        assert RiskLevel.MEDIUM == "M"
        assert RiskLevel.HIGH == "H"
        assert RiskLevel.AGGRESSIVE == "A"

    def test_client_type_values(self):
        assert ClientType.INDIVIDUAL == "I"
        assert ClientType.CORPORATE == "C"
        assert ClientType.TRUST == "T"


class TestTransactionEnums:
    """Verify transaction enum values match COBOL TRNREC.cpy level-88."""

    def test_trn_type_values(self):
        assert TransactionType.BUY == "BU"
        assert TransactionType.SELL == "SL"
        assert TransactionType.TRANSFER == "TR"
        assert TransactionType.FEE == "FE"

    def test_trn_status_values(self):
        assert TransactionStatus.PENDING == "P"
        assert TransactionStatus.DONE == "D"
        assert TransactionStatus.FAILED == "F"
        assert TransactionStatus.REVERSED == "R"


class TestPositionEnums:
    def test_position_status_values(self):
        assert PositionStatus.ACTIVE == "A"
        assert PositionStatus.CLOSED == "C"
        assert PositionStatus.PENDING == "P"


class TestAuditEnums:
    def test_audit_type_values(self):
        assert AuditType.TRANSACTION == "TRAN"
        assert AuditType.USER == "USER"
        assert AuditType.SYSTEM == "SYST"

    def test_audit_action_values(self):
        assert AuditAction.CREATE == "CREATE"
        assert AuditAction.UPDATE == "UPDATE"
        assert AuditAction.DELETE == "DELETE"

    def test_audit_status_values(self):
        assert AuditStatus.SUCCESS == "SUCC"
        assert AuditStatus.FAILURE == "FAIL"
        assert AuditStatus.WARNING == "WARN"


class TestBatchEnums:
    def test_batch_status_values(self):
        assert BatchStatus.READY == "R"
        assert BatchStatus.ACTIVE == "A"
        assert BatchStatus.DONE == "D"
        assert BatchStatus.ERROR == "E"

    def test_batch_function_values(self):
        assert BatchFunction.INIT == "INIT"
        assert BatchFunction.CHECK == "CHEK"
        assert BatchFunction.UPDATE == "UPDT"
        assert BatchFunction.TERMINATE == "TERM"


class TestPortfolioModel:
    """Test PortfolioRecord Pydantic model. Translates TSTVAL00.cbl validation."""

    def test_valid_portfolio(self):
        p = PortfolioRecord(
            portfolio_id="PORT0001",
            account_number="ACCT000001",
            client_name="Test Client",
            client_type="I",
            create_date=date(2024, 1, 15),
            last_maint_date=date(2024, 1, 15),
            account_type="IN",
            branch_id="01",
            client_id="CLIENT001",
            portfolio_name="Test Portfolio",
            currency_code="USD",
            risk_level="M",
            status="A",
        )
        assert p.portfolio_id == "PORT0001"
        assert p.currency_code == "USD"

    def test_portfolio_id_max_length(self):
        """PIC X(08) — max 8 chars."""
        with pytest.raises(Exception):
            PortfolioRecord(
                portfolio_id="TOOLONGID",  # 9 chars
                account_number="ACCT000001",
                client_name="Test Client",
                client_type="I",
                create_date=date(2024, 1, 15),
                last_maint_date=date(2024, 1, 15),
                account_type="IN",
                branch_id="01",
                client_id="CLIENT001",
                portfolio_name="Test",
                currency_code="USD",
                risk_level="M",
                status="A",
            )


class TestTransactionModel:
    """Test TransactionRecord Pydantic model."""

    def test_valid_transaction(self):
        t = TransactionRecord(
            trn_date=date(2024, 1, 15),
            trn_time="100000",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="FUND0001",
            trn_type="BU",
            quantity=Decimal("100.0000"),
            price=Decimal("50.0000"),
            amount=Decimal("5000.00"),
            currency="USD",
            status="P",
        )
        assert t.trn_type == "BU"
        assert t.quantity == Decimal("100.0000")

    def test_decimal_precision(self):
        """Verify COMP-3 precision: PIC S9(11)V9(4) for quantity."""
        t = TransactionRecord(
            trn_date=date(2024, 1, 15),
            trn_time="100000",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="FUND0001",
            trn_type="BU",
            quantity=Decimal("12345678901.1234"),
            price=Decimal("123.4567"),
            amount=Decimal("5000.00"),
            currency="USD",
            status="P",
        )
        # Quantity should preserve 4 decimal places
        assert t.quantity == Decimal("12345678901.1234")
        # Price should preserve 4 decimal places
        assert t.price == Decimal("123.4567")


class TestPositionModel:
    """Test PositionRecord Pydantic model."""

    def test_valid_position(self):
        p = PositionRecord(
            portfolio_id="PORT0001",
            position_date=date(2024, 1, 15),
            investment_id="FUND0001",
            quantity=Decimal("100.0000"),
            cost_basis=Decimal("5000.00"),
            market_value=Decimal("5500.00"),
            currency="USD",
            status="A",
        )
        assert p.quantity == Decimal("100.0000")
        assert p.cost_basis == Decimal("5000.00")


class TestDecimalArithmetic:
    """
    Critical: Verify Decimal arithmetic matches COBOL COMP-3 precision.
    Translates TSTVAL00.cbl decimal validation tests.
    """

    def test_buy_amount_calculation(self):
        """COMPUTE TRN-AMOUNT = TRN-QUANTITY * TRN-PRICE"""
        quantity = Decimal("100.0000")
        price = Decimal("50.2500")
        amount = (quantity * price).quantize(Decimal("0.01"))
        assert amount == Decimal("5025.00")

    def test_sell_gain_loss(self):
        """COMPUTE GAIN-LOSS = SELL-PROCEEDS - COST-OF-SOLD"""
        sell_quantity = Decimal("50.0000")
        sell_price = Decimal("55.0000")
        proceeds = (sell_quantity * sell_price).quantize(Decimal("0.01"))

        old_quantity = Decimal("100.0000")
        old_cost = Decimal("5000.00")
        avg_cost = (old_cost / old_quantity).quantize(Decimal("0.0001"))
        cost_of_sold = (avg_cost * sell_quantity).quantize(Decimal("0.01"))

        gain_loss = (proceeds - cost_of_sold).quantize(Decimal("0.01"))
        assert proceeds == Decimal("2750.00")
        assert avg_cost == Decimal("50.0000")
        assert cost_of_sold == Decimal("2500.00")
        assert gain_loss == Decimal("250.00")

    def test_position_update_after_buy(self):
        """Verify position update arithmetic for BUY."""
        old_qty = Decimal("100.0000")
        old_cost = Decimal("5000.00")
        buy_qty = Decimal("50.0000")
        buy_amount = Decimal("2750.00")

        new_qty = (old_qty + buy_qty).quantize(Decimal("0.0001"))
        new_cost = (old_cost + buy_amount).quantize(Decimal("0.01"))

        assert new_qty == Decimal("150.0000")
        assert new_cost == Decimal("7750.00")

    def test_position_update_after_sell(self):
        """Verify position update arithmetic for SELL."""
        old_qty = Decimal("100.0000")
        old_cost = Decimal("5000.00")
        sell_qty = Decimal("30.0000")

        avg_cost = (old_cost / old_qty).quantize(Decimal("0.0001"))
        cost_of_sold = (avg_cost * sell_qty).quantize(Decimal("0.01"))

        new_qty = (old_qty - sell_qty).quantize(Decimal("0.0001"))
        new_cost = (old_cost - cost_of_sold).quantize(Decimal("0.01"))

        assert avg_cost == Decimal("50.0000")
        assert cost_of_sold == Decimal("1500.00")
        assert new_qty == Decimal("70.0000")
        assert new_cost == Decimal("3500.00")

    def test_change_percentage(self):
        """COMPUTE POS-CHANGE-PCT from RPTPOS00.cbl."""
        cost = Decimal("5000.00")
        market = Decimal("5500.00")
        change_pct = (((market - cost) / cost) * Decimal("100")).quantize(Decimal("0.01"))
        assert change_pct == Decimal("10.00")

    def test_never_use_float(self):
        """Ensure float multiplication differs — proving Decimal is required."""
        # Float: 0.1 + 0.2 != 0.3
        float_result = 0.1 + 0.2
        assert float_result != 0.3  # Known float imprecision

        # Decimal: exact
        decimal_result = Decimal("0.1") + Decimal("0.2")
        assert decimal_result == Decimal("0.3")
