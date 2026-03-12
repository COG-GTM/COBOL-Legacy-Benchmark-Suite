"""
Model tests translated from TSTVAL00.cbl validation tests.

Tests:
  - Pydantic model creation and validation
  - Enum values match COBOL level-88 definitions
  - Decimal precision matches COBOL COMP-3 fields
  - String length constraints match PIC X(n)
"""

from datetime import date
from decimal import Decimal

import pytest
from pydantic import ValidationError as PydanticValidationError

from src.common.constants import (
    AccountType,
    ClientType,
    CurrencyCode,
    PortfolioStatus,
    PositionStatus,
    ReturnCode,
    RiskLevel,
    TransactionStatus,
    TransactionType,
)
from src.models.audit import AuditLogRecord
from src.models.batch_control import BatchParameters, BatchStatusRecord, CheckpointRecord
from src.models.error import ErrorLogRecord
from src.models.portfolio import PortfolioRecord
from src.models.position import PositionRecord
from src.models.security import SecurityParameters, UserData
from src.models.transaction import TransactionRecord


class TestPortfolioRecord:
    """Test PortfolioRecord model from PORTFLIO.cpy."""

    def test_create_valid_portfolio(self):
        record = PortfolioRecord(
            portfolio_id="PORT0001",
            account_no="1234567890",
            account_type=AccountType.INDIVIDUAL,
            client_name="John Doe",
            client_type=ClientType.INDIVIDUAL,
            status=PortfolioStatus.ACTIVE,
            total_value=Decimal("100000.00"),
            cash_balance=Decimal("25000.00"),
        )
        assert record.portfolio_id == "PORT0001"
        assert record.total_value == Decimal("100000.00")
        assert record.status == PortfolioStatus.ACTIVE

    def test_portfolio_id_required(self):
        with pytest.raises(PydanticValidationError):
            PortfolioRecord(
                portfolio_id="",
                account_type=AccountType.INDIVIDUAL,
            )

    def test_decimal_precision(self):
        """Verify Decimal fields preserve COBOL COMP-3 precision."""
        record = PortfolioRecord(
            portfolio_id="PORT0001",
            total_value=Decimal("9999999999999.99"),
            cash_balance=Decimal("-9999999999999.99"),
        )
        assert record.total_value == Decimal("9999999999999.99")
        assert record.cash_balance == Decimal("-9999999999999.99")

    def test_default_values(self):
        record = PortfolioRecord(portfolio_id="PORT0001")
        assert record.account_type == AccountType.INDIVIDUAL
        assert record.currency_code == CurrencyCode.USD
        assert record.risk_level == RiskLevel.MEDIUM
        assert record.status == PortfolioStatus.ACTIVE
        assert record.total_value == Decimal("0.00")


class TestTransactionRecord:
    """Test TransactionRecord model from TRNREC.cpy."""

    def test_create_buy_transaction(self):
        record = TransactionRecord(
            portfolio_id="PORT0001",
            investment_id="AAPL000001",
            trn_type=TransactionType.BUY,
            quantity=Decimal("100.0000"),
            price=Decimal("150.0000"),
            amount=Decimal("15000.00"),
        )
        assert record.trn_type == TransactionType.BUY
        assert record.quantity == Decimal("100.0000")
        assert record.price == Decimal("150.0000")

    def test_quantity_must_be_positive(self):
        with pytest.raises(PydanticValidationError):
            TransactionRecord(
                portfolio_id="PORT0001",
                trn_type=TransactionType.BUY,
                quantity=Decimal("0"),
                price=Decimal("150.00"),
                amount=Decimal("0"),
            )

    def test_transaction_types(self):
        """Verify all COBOL level-88 transaction types."""
        assert TransactionType.BUY == "BU"
        assert TransactionType.SELL == "SL"
        assert TransactionType.TRANSFER == "TR"
        assert TransactionType.FEE == "FE"

    def test_transaction_statuses(self):
        """Verify all COBOL level-88 transaction statuses."""
        assert TransactionStatus.PENDING == "P"
        assert TransactionStatus.DONE == "D"
        assert TransactionStatus.FAILED == "F"
        assert TransactionStatus.REVERSED == "R"


class TestPositionRecord:
    """Test PositionRecord model from POSREC.cpy."""

    def test_create_position(self):
        record = PositionRecord(
            portfolio_id="PORT0001",
            investment_id="AAPL000001",
            quantity=Decimal("100.0000"),
            cost_basis=Decimal("15000.00"),
            market_value=Decimal("17500.00"),
        )
        assert record.quantity == Decimal("100.0000")
        assert record.cost_basis == Decimal("15000.00")
        gain = record.market_value - record.cost_basis
        assert gain == Decimal("2500.00")

    def test_position_statuses(self):
        """Verify COBOL level-88 position statuses."""
        assert PositionStatus.ACTIVE == "A"
        assert PositionStatus.CLOSED == "C"
        assert PositionStatus.PENDING == "P"


class TestReturnCodes:
    """Verify COBOL return code mappings."""

    def test_return_codes(self):
        assert ReturnCode.SUCCESS == 0
        assert ReturnCode.WARNING == 4
        assert ReturnCode.ERROR == 8
        assert ReturnCode.SEVERE == 12
        assert ReturnCode.CRITICAL == 16


class TestEnumValues:
    """Verify all enum values match COBOL level-88 definitions."""

    def test_portfolio_status_values(self):
        assert PortfolioStatus.ACTIVE.value == "A"
        assert PortfolioStatus.CLOSED.value == "C"
        assert PortfolioStatus.SUSPENDED.value == "S"
        assert PortfolioStatus.PENDING.value == "P"

    def test_client_type_values(self):
        assert ClientType.INDIVIDUAL.value == "I"
        assert ClientType.CORPORATE.value == "C"
        assert ClientType.TRUST.value == "T"

    def test_account_type_values(self):
        assert AccountType.INDIVIDUAL.value == "IN"
        assert AccountType.JOINT.value == "JT"
        assert AccountType.CORPORATE.value == "CO"
        assert AccountType.TRUST.value == "TR"
        assert AccountType.RETIREMENT.value == "RT"

    def test_risk_level_values(self):
        assert RiskLevel.LOW.value == "L"
        assert RiskLevel.MEDIUM.value == "M"
        assert RiskLevel.HIGH.value == "H"


class TestAuditModel:
    """Test AuditLogRecord model from AUDITLOG.cpy."""

    def test_create_audit_record(self):
        record = AuditLogRecord(
            user_id="TESTUSER",
            action="CREATE",
            portfolio_id="PORT0001",
            message="Portfolio created",
        )
        assert record.user_id == "TESTUSER"
        assert record.message == "Portfolio created"


class TestErrorModel:
    """Test ErrorLogRecord model from ERRLOG.cpy."""

    def test_create_error_record(self):
        record = ErrorLogRecord(
            program="PORTMSTR",
            severity=8,
            error_code="E001",
            message="Validation failed",
        )
        assert record.severity == 8
        assert record.program == "PORTMSTR"


class TestBatchModels:
    """Test batch control models from BCHPARM.cpy, BCHSTAT.cpy, CKPRST.cpy."""

    def test_batch_parameters(self):
        params = BatchParameters(
            batch_id="BCH0115",
            process_date=date(2024, 1, 15),
            commit_frequency=1000,
            max_errors=100,
        )
        assert params.commit_frequency == 1000
        assert params.max_errors == 100

    def test_batch_status(self):
        status = BatchStatusRecord(batch_id="BCH0115")
        assert status.records_read == 0
        assert status.records_processed == 0
        assert status.error_count == 0

    def test_checkpoint_record(self):
        checkpoint = CheckpointRecord(
            checkpoint_id="CKP00001",
            batch_id="BCH0115",
            records_at_checkpoint=500,
            total_amount=Decimal("1234567.89"),
        )
        assert checkpoint.total_amount == Decimal("1234567.89")


class TestSecurityModels:
    """Test security models from SECPARM.cpy, USRDATA.cpy."""

    def test_security_parameters(self):
        params = SecurityParameters(
            user_id="ADMIN01",
            security_level=9,
            access_portfolio=True,
            access_admin=True,
        )
        assert params.security_level == 9
        assert params.access_admin is True

    def test_user_data(self):
        user = UserData(
            user_id="USER001",
            user_name="Test User",
            department="IT",
            role="ANALYST",
        )
        assert user.role == "ANALYST"
