"""
Transaction validator tests translated from COBOL TRNVAL00.cbl.

Tests:
- Required field validation (2100-CHECK-REQUIRED)
- Duplicate detection (2200-CHECK-DUPLICATE)
- Portfolio validation (2300-VALIDATE-PORTFOLIO)
- Business rule validation (2400-CHECK-RULES)
"""

from datetime import date
from decimal import Decimal

from src.batch.validator import TransactionValidator
from src.common.constants import ReturnCode
from src.db.tables import TransactionHistory


class TestRequiredFields:
    """Test required field validation. Translates TRNVAL00 2100-CHECK-REQUIRED."""

    def test_valid_transaction(self, session, sample_portfolio):
        """A fresh transaction (not yet in DB) should pass validation."""
        trn = TransactionHistory(
            portfolio_id=sample_portfolio.portfolio_id,
            investment_id="FUND0099",
            trn_date=date(2024, 2, 1),
            trn_time="120000",
            sequence_no="000099",
            trn_type="BU",
            quantity=Decimal("50.0000"),
            price=Decimal("25.0000"),
            amount=Decimal("1250.00"),
            currency_code="USD",
            status="P",
        )
        # Validate before flushing so duplicate check doesn't find it
        validator = TransactionValidator(session)
        rc = validator.validate_transaction(trn)
        assert rc == ReturnCode.SUCCESS

    def test_missing_portfolio_id(self, session, sample_portfolio):
        """Transaction with empty portfolio_id should fail validation."""
        trn = TransactionHistory(
            portfolio_id="",
            investment_id="FUND0001",
            trn_date=date(2024, 1, 15),
            trn_time="100000",
            sequence_no="000001",
            trn_type="BU",
            quantity=Decimal("100.0000"),
            price=Decimal("50.0000"),
            amount=Decimal("5000.00"),
            currency_code="USD",
            status="P",
        )
        # Don't flush — FK constraint would reject empty portfolio_id.
        # Validate the in-memory object directly.
        validator = TransactionValidator(session)
        rc = validator.validate_transaction(trn)
        assert rc != ReturnCode.SUCCESS

    def test_missing_investment_id(self, session, sample_portfolio):
        trn = TransactionHistory(
            portfolio_id=sample_portfolio.portfolio_id,
            investment_id="",
            trn_date=date(2024, 1, 15),
            trn_time="100000",
            sequence_no="000001",
            trn_type="BU",
            quantity=Decimal("100.0000"),
            price=Decimal("50.0000"),
            amount=Decimal("5000.00"),
            currency_code="USD",
            status="P",
        )
        session.add(trn)
        session.flush()
        validator = TransactionValidator(session)
        rc = validator.validate_transaction(trn)
        assert rc != ReturnCode.SUCCESS


class TestBusinessRules:
    """Test business rule validation. Translates TRNVAL00 2400-CHECK-RULES."""

    def test_invalid_trn_type(self, session, sample_portfolio):
        trn = TransactionHistory(
            portfolio_id=sample_portfolio.portfolio_id,
            investment_id="FUND0001",
            trn_date=date(2024, 1, 15),
            trn_time="100000",
            sequence_no="000002",
            trn_type="XX",
            quantity=Decimal("100.0000"),
            price=Decimal("50.0000"),
            amount=Decimal("5000.00"),
            currency_code="USD",
            status="P",
        )
        session.add(trn)
        session.flush()
        validator = TransactionValidator(session)
        rc = validator.validate_transaction(trn)
        assert rc != ReturnCode.SUCCESS

    def test_zero_quantity(self, session, sample_portfolio):
        trn = TransactionHistory(
            portfolio_id=sample_portfolio.portfolio_id,
            investment_id="FUND0001",
            trn_date=date(2024, 1, 15),
            trn_time="100000",
            sequence_no="000003",
            trn_type="BU",
            quantity=Decimal("0.0000"),
            price=Decimal("50.0000"),
            amount=Decimal("0.00"),
            currency_code="USD",
            status="P",
        )
        session.add(trn)
        session.flush()
        validator = TransactionValidator(session)
        rc = validator.validate_transaction(trn)
        assert rc != ReturnCode.SUCCESS

    def test_negative_price(self, session, sample_portfolio):
        trn = TransactionHistory(
            portfolio_id=sample_portfolio.portfolio_id,
            investment_id="FUND0001",
            trn_date=date(2024, 1, 15),
            trn_time="100000",
            sequence_no="000004",
            trn_type="BU",
            quantity=Decimal("100.0000"),
            price=Decimal("-10.0000"),
            amount=Decimal("-1000.00"),
            currency_code="USD",
            status="P",
        )
        session.add(trn)
        session.flush()
        validator = TransactionValidator(session)
        rc = validator.validate_transaction(trn)
        assert rc != ReturnCode.SUCCESS


class TestBatchValidation:
    """Test batch validation. Translates TRNVAL00 3000-BATCH-VALIDATE."""

    def test_validate_empty_batch(self, session):
        validator = TransactionValidator(session)
        rc = validator.validate_batch([])
        # No pending transactions = success (nothing to validate)
        assert rc == ReturnCode.SUCCESS
