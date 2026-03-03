"""Test Validation Suite.

Replaces: TSTVAL00 (src/programs/test/TSTVAL00.cbl)

Pytest-based validation suite that verifies the correctness of
the migrated Python code against expected behavior from the
original COBOL programs.

Original COBOL flow (TSTVAL00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE
    2000-RUN-TESTS
      2100-TEST-VALIDATION (TRNVAL00)
      2200-TEST-POSITION-UPDATE (POSUPD00)
      2300-TEST-HISTORY-LOAD (HISTLD00)
      2400-TEST-INQUIRY (INQPORT/INQHIST)
      2500-TEST-ERROR-HANDLING
    3000-REPORT-RESULTS
    4000-FINALIZE
"""

from __future__ import annotations

from decimal import Decimal

from portfolio_manager.batch.transaction_validator import (
    TransactionValidator,
    validate_transaction,
)
from portfolio_manager.models.copybook_models import (
    BatchControlConstants,
    BatchControlRecord,
    BatchStatus,
    CheckpointControl,
    CheckpointPhase,
    CheckpointStatus,
    CurrencyCode,
    ErrorReturnCodes,
    HistoryActionCode,
    HistoryRecord,
    HistoryRecordType,
    PositionRecord,
    PositionStatus,
    TransactionRecord,
    TransactionStatus,
    TransactionType,
)
from portfolio_manager.services.error_handler import (
    DatabaseError,
    PortfolioError,
    ProcessingError,
    SecurityError,
    ValidationError,
)
from portfolio_manager.tests.test_data_generator import TestDataGenerator

# ---------------------------------------------------------------------------
# 2100-TEST-VALIDATION (TRNVAL00 validation rules)
# ---------------------------------------------------------------------------


class TestTransactionValidation:
    """Test transaction validation rules.

    Replaces TSTVAL00 paragraph 2100-TEST-VALIDATION.
    """

    def test_valid_buy_transaction(self) -> None:
        """Test that a valid BUY transaction passes validation."""
        record = TransactionRecord(
            trn_date="20250315",
            trn_time="093000",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="AAPL000001",
            transaction_type=TransactionType.BUY,
            quantity=Decimal("100.0000"),
            price=Decimal("150.5000"),
            amount=Decimal("15050.00"),
            currency=CurrencyCode.USD,
            status=TransactionStatus.PENDING,
        )
        result = validate_transaction(record)
        assert result.valid is True
        assert len(result.errors) == 0

    def test_valid_sell_transaction(self) -> None:
        """Test that a valid SELL transaction passes validation."""
        record = TransactionRecord(
            trn_date="20250315",
            trn_time="103000",
            portfolio_id="PORT0002",
            sequence_no="000002",
            investment_id="GOOG000002",
            transaction_type=TransactionType.SELL,
            quantity=Decimal("50.0000"),
            price=Decimal("175.2500"),
            amount=Decimal("8762.50"),
            currency=CurrencyCode.EUR,
            status=TransactionStatus.PENDING,
        )
        result = validate_transaction(record)
        assert result.valid is True

    def test_missing_portfolio_id(self) -> None:
        """Test validation fails for missing portfolio ID."""
        record = TransactionRecord(
            trn_date="20250315",
            trn_time="093000",
            portfolio_id="",
            sequence_no="000001",
            investment_id="AAPL000001",
            transaction_type=TransactionType.BUY,
            quantity=Decimal("100"),
            price=Decimal("150"),
            amount=Decimal("15000.00"),
        )
        result = validate_transaction(record)
        assert result.valid is False
        assert any("Portfolio ID" in e for e in result.errors)

    def test_invalid_date(self) -> None:
        """Test validation fails for invalid date."""
        record = TransactionRecord(
            trn_date="20251301",  # month 13
            trn_time="093000",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="AAPL000001",
            transaction_type=TransactionType.BUY,
            quantity=Decimal("100"),
            price=Decimal("150"),
            amount=Decimal("15000.00"),
        )
        result = validate_transaction(record)
        assert result.valid is False

    def test_missing_investment_id(self) -> None:
        """Test validation fails for missing investment ID."""
        record = TransactionRecord(
            trn_date="20250315",
            trn_time="093000",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="",
            transaction_type=TransactionType.BUY,
            quantity=Decimal("100"),
            price=Decimal("150"),
            amount=Decimal("15000.00"),
        )
        result = validate_transaction(record)
        assert result.valid is False

    def test_negative_quantity(self) -> None:
        """Test validation fails for negative quantity."""
        record = TransactionRecord(
            trn_date="20250315",
            trn_time="093000",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="AAPL000001",
            transaction_type=TransactionType.BUY,
            quantity=Decimal("-100"),
            price=Decimal("150"),
            amount=Decimal("-15000.00"),
        )
        result = validate_transaction(record)
        assert result.valid is False

    def test_amount_mismatch_warning(self) -> None:
        """Test warning for amount != qty * price."""
        record = TransactionRecord(
            trn_date="20250315",
            trn_time="093000",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="AAPL000001",
            transaction_type=TransactionType.BUY,
            quantity=Decimal("100"),
            price=Decimal("150"),
            amount=Decimal("20000.00"),  # Should be 15000
        )
        result = validate_transaction(record)
        assert result.valid is True  # warning, not error
        assert len(result.warnings) > 0

    def test_batch_validator_all_valid(self) -> None:
        """Test batch validator with all valid records."""
        records = [
            TransactionRecord(
                trn_date="20250315",
                trn_time="093000",
                portfolio_id="PORT0001",
                sequence_no=f"{i:06d}",
                investment_id="AAPL000001",
                transaction_type=TransactionType.BUY,
                quantity=Decimal("100"),
                price=Decimal("150"),
                amount=Decimal("15000.00"),
            )
            for i in range(5)
        ]

        validator = TransactionValidator()
        batch_result, valid, invalid = validator.run(records)

        assert batch_result.return_code == 0
        assert batch_result.records_read == 5
        assert batch_result.records_valid == 5
        assert batch_result.records_invalid == 0
        assert len(valid) == 5
        assert len(invalid) == 0


# ---------------------------------------------------------------------------
# Test copybook/Pydantic models
# ---------------------------------------------------------------------------


class TestCopybookModels:
    """Test that Pydantic models correctly represent COBOL copybook structures."""

    def test_position_record_defaults(self) -> None:
        """Test PositionRecord default values."""
        pos = PositionRecord(
            portfolio_id="PORT0001",
            position_date="20250315",
            investment_id="AAPL000001",
        )
        assert pos.quantity == Decimal("0")
        assert pos.cost_basis == Decimal("0")
        assert pos.market_value == Decimal("0")
        assert pos.currency == CurrencyCode.USD
        assert pos.status == PositionStatus.ACTIVE

    def test_history_record(self) -> None:
        """Test HistoryRecord creation."""
        hist = HistoryRecord(
            portfolio_id="PORT0001",
            hist_date="20250315",
            hist_time="093000",
            seq_no="0001",
            record_type=HistoryRecordType.TRANSACTION,
            action_code=HistoryActionCode.ADD,
        )
        assert hist.record_type == HistoryRecordType.TRANSACTION
        assert hist.action_code == HistoryActionCode.ADD
        assert hist.before_image == ""

    def test_batch_control_record(self) -> None:
        """Test BatchControlRecord with dependencies."""
        bcr = BatchControlRecord(
            job_name="TRNVAL00",
            process_date="20250315",
            sequence_no=1,
            status=BatchStatus.ACTIVE,
        )
        assert bcr.status == BatchStatus.ACTIVE
        assert bcr.restart_count == 0

    def test_checkpoint_control(self) -> None:
        """Test CheckpointControl structure."""
        ckpt = CheckpointControl(
            program_id="TRNVAL00",
            status=CheckpointStatus.ACTIVE,
            phase=CheckpointPhase.PROCESS,
            commit_freq=1000,
            max_errors=100,
            max_restarts=3,
        )
        assert ckpt.commit_freq == 1000
        assert ckpt.max_errors == 100
        assert ckpt.phase == CheckpointPhase.PROCESS

    def test_error_return_codes(self) -> None:
        """Test error return code constants."""
        codes = ErrorReturnCodes()
        assert codes.SUCCESS == 0
        assert codes.WARNING == 4
        assert codes.ERROR == 8
        assert codes.SEVERE == 12
        assert codes.TERMINAL == 16

    def test_batch_control_constants(self) -> None:
        """Test batch control constants."""
        constants = BatchControlConstants()
        assert constants.RC_SUCCESS == 0
        assert constants.MAX_PREREQ == 10
        assert constants.MAX_RESTARTS == 3


# ---------------------------------------------------------------------------
# 2500-TEST-ERROR-HANDLING
# ---------------------------------------------------------------------------


class TestErrorHandling:
    """Test error handling and exception hierarchy.

    Replaces TSTVAL00 paragraph 2500-TEST-ERROR-HANDLING.
    """

    def test_portfolio_error_hierarchy(self) -> None:
        """Test that all errors inherit from PortfolioError."""
        assert issubclass(ValidationError, PortfolioError)
        assert issubclass(DatabaseError, PortfolioError)
        assert issubclass(SecurityError, PortfolioError)
        assert issubclass(ProcessingError, PortfolioError)

    def test_validation_error_severity(self) -> None:
        """Test that ValidationError has correct default severity."""
        err = ValidationError("Test error")
        assert err.severity == 2  # WARNING level
        assert err.error_code == "E008"

    def test_database_error_attributes(self) -> None:
        """Test DatabaseError attributes."""
        err = DatabaseError("Connection failed", program_id="DB2ONLN")
        assert err.error_code == "E005"
        assert err.program_id == "DB2ONLN"
        assert err.severity == 3


# ---------------------------------------------------------------------------
# Test data generator
# ---------------------------------------------------------------------------


class TestDataGeneration:
    """Test the test data generator.

    Verifies that generated data is consistent and reproducible.
    """

    def test_reproducibility(self) -> None:
        """Test that same seed produces same data."""
        gen1 = TestDataGenerator(seed=42)
        gen2 = TestDataGenerator(seed=42)

        portfolios1 = gen1.generate_portfolios(5)
        portfolios2 = gen2.generate_portfolios(5)

        for p1, p2 in zip(portfolios1, portfolios2):
            assert p1.portfolio_id == p2.portfolio_id
            assert p1.portfolio_name == p2.portfolio_name

    def test_portfolio_generation(self) -> None:
        """Test portfolio generation produces valid records."""
        gen = TestDataGenerator(seed=42)
        portfolios = gen.generate_portfolios(10)

        assert len(portfolios) == 10
        for p in portfolios:
            assert p.portfolio_id.startswith("PORT")
            assert len(p.portfolio_id) == 8
            assert p.account_type in ["IN", "CO", "TR", "RT", "MF"]
            assert p.status in ["A", "C"]

    def test_position_generation(self) -> None:
        """Test position generation from portfolios."""
        gen = TestDataGenerator(seed=42)
        portfolios = gen.generate_portfolios(5)
        positions = gen.generate_positions(portfolios, positions_per_portfolio=3)

        assert len(positions) > 0
        for pos in positions:
            assert pos.quantity > 0
            assert pos.cost_basis > 0

    def test_transaction_record_generation(self) -> None:
        """Test batch pipeline transaction record generation."""
        gen = TestDataGenerator(seed=42)
        records = gen.generate_transaction_records(20)

        assert len(records) == 20
        for rec in records:
            assert len(rec.trn_date) == 8
            assert len(rec.portfolio_id) == 8
            assert rec.quantity > 0
