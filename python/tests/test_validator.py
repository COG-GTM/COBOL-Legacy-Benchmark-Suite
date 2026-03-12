"""
Transaction validator tests translated from TSTVAL00.cbl.

Tests validation rules from TRNVAL00.cbl:
  - Field validation
  - Duplicate detection
  - Business rule validation
"""

from decimal import Decimal

from sqlalchemy.orm import Session

from src.batch.validator import TransactionValidator
from src.common.constants import TransactionType
from src.db.tables import PortfolioMaster
from src.models.transaction import TransactionRecord


class TestTransactionValidator:
    """Test TransactionValidator (TRNVAL00.cbl)."""

    def test_valid_buy_transaction(self, session: Session, sample_portfolio: PortfolioMaster):
        validator = TransactionValidator(session)
        record = TransactionRecord(
            portfolio_id="PORT0001",
            investment_id="AAPL000001",
            trn_type=TransactionType.BUY,
            quantity=Decimal("100.0000"),
            price=Decimal("150.0000"),
            amount=Decimal("15000.00"),
        )
        result = validator.validate(record)
        assert result.is_valid
        assert validator.total_passed == 1

    def test_missing_investment_id_for_buy(self, session: Session, sample_portfolio):
        validator = TransactionValidator(session)
        record = TransactionRecord(
            portfolio_id="PORT0001",
            investment_id="",
            trn_type=TransactionType.BUY,
            quantity=Decimal("100.0000"),
            price=Decimal("150.0000"),
            amount=Decimal("15000.00"),
        )
        result = validator.validate(record)
        assert not result.is_valid
        assert any("Investment ID" in e for e in result.errors)

    def test_negative_quantity(self, session: Session, sample_portfolio):
        """Pydantic model rejects negative quantity at construction time."""
        import pytest
        from pydantic import ValidationError as PydanticValidationError

        with pytest.raises(PydanticValidationError):
            TransactionRecord(
                portfolio_id="PORT0001",
                investment_id="AAPL000001",
                trn_type=TransactionType.BUY,
                quantity=Decimal("-10.0000"),
                price=Decimal("150.0000"),
                amount=Decimal("15000.00"),
            )

    def test_portfolio_not_found(self, session: Session):
        validator = TransactionValidator(session)
        record = TransactionRecord(
            portfolio_id="PORT9999",
            investment_id="AAPL000001",
            trn_type=TransactionType.BUY,
            quantity=Decimal("100.0000"),
            price=Decimal("150.0000"),
            amount=Decimal("15000.00"),
        )
        result = validator.validate(record)
        assert not result.is_valid
        assert any("not found" in e for e in result.errors)

    def test_sell_insufficient_quantity(
        self, session: Session, sample_portfolio, sample_position
    ):
        """Selling more units than held should fail."""
        validator = TransactionValidator(session)
        record = TransactionRecord(
            portfolio_id="PORT0001",
            investment_id="AAPL000001",
            trn_type=TransactionType.SELL,
            quantity=Decimal("999.0000"),
            price=Decimal("150.0000"),
            amount=Decimal("149850.00"),
        )
        result = validator.validate(record)
        assert not result.is_valid
        assert any("Insufficient" in e for e in result.errors)

    def test_batch_counters(self, session: Session, sample_portfolio):
        validator = TransactionValidator(session)
        valid_record = TransactionRecord(
            portfolio_id="PORT0001",
            investment_id="AAPL000001",
            trn_type=TransactionType.BUY,
            quantity=Decimal("10.0000"),
            price=Decimal("150.0000"),
            amount=Decimal("1500.00"),
        )
        invalid_record = TransactionRecord(
            portfolio_id="PORT9999",
            investment_id="AAPL000001",
            trn_type=TransactionType.BUY,
            quantity=Decimal("10.0000"),
            price=Decimal("150.0000"),
            amount=Decimal("1500.00"),
        )
        validator.validate(valid_record)
        validator.validate(invalid_record)
        assert validator.total_validated == 2
        assert validator.total_passed == 1
        assert validator.total_failed == 1
