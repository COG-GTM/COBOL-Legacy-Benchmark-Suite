"""Tests for Pydantic data models translated from COBOL copybooks.

Covers:
- Portfolio ID validation (PORT + 4-5 digits)
- Account number validation (10 numeric digits)
- Investment type validation (STK/BND/MMF/ETF)
- Amount range validation
- Enum values match COBOL constants
- Decimal precision for financial fields
- SQLAlchemy schema creation
"""

from datetime import date, datetime
from decimal import Decimal

import pytest
from sqlalchemy import create_engine, inspect

from db.schema import (
    Base,
)
from models.audit import AuditRecord
from models.batch import (
    BATCH_MAX_PREREQ,
    BATCH_MAX_RESTARTS,
    BatchControlRecord,
)
from models.enums import (
    AuditAction,
    AuditStatus,
    AuditType,
    BatchStatus,
    CheckpointStatus,
    ClientType,
    Currency,
    DeleteReason,
    ErrorCategory,
    InvestmentType,
    PortfolioStatus,
    PositionStatus,
    ReturnCode,
    TransactionStatus,
    TransactionType,
)
from models.errors import (
    AppError,
    DuplicateKeyError,
    ErrorMessage,
    ErrorSeverity,
    NotFoundError,
    ProcessingError,
    SystemError,
    ValidationError,
    VsamError,
)
from models.portfolio import Portfolio
from models.position import PositionRecord
from models.transaction import TransactionRecord

# =========================================================================
# Enum tests — values must match COBOL constants exactly
# =========================================================================


class TestReturnCodeEnum:
    def test_values(self):
        assert ReturnCode.SUCCESS == 0
        assert ReturnCode.WARNING == 4
        assert ReturnCode.ERROR == 8
        assert ReturnCode.SEVERE == 12
        assert ReturnCode.CRITICAL == 16


class TestPortfolioStatusEnum:
    def test_values(self):
        assert PortfolioStatus.ACTIVE == "A"
        assert PortfolioStatus.CLOSED == "C"
        assert PortfolioStatus.PENDING == "P"
        assert PortfolioStatus.SUSPENDED == "S"


class TestTransactionTypeEnum:
    def test_values(self):
        assert TransactionType.BUY == "BU"
        assert TransactionType.SELL == "SL"
        assert TransactionType.TRANSFER == "TR"
        assert TransactionType.FEE == "FE"


class TestTransactionStatusEnum:
    def test_values(self):
        assert TransactionStatus.PENDING == "P"
        assert TransactionStatus.DONE == "D"
        assert TransactionStatus.FAILED == "F"
        assert TransactionStatus.REVERSED == "R"


class TestPositionStatusEnum:
    def test_values(self):
        assert PositionStatus.ACTIVE == "A"
        assert PositionStatus.CLOSED == "C"
        assert PositionStatus.PENDING == "P"


class TestClientTypeEnum:
    def test_values(self):
        assert ClientType.INDIVIDUAL == "I"
        assert ClientType.CORPORATE == "C"
        assert ClientType.TRUST == "T"


class TestCurrencyEnum:
    def test_values(self):
        assert Currency.USD == "USD"
        assert Currency.EUR == "EUR"
        assert Currency.GBP == "GBP"
        assert Currency.JPY == "JPY"
        assert Currency.CAD == "CAD"


class TestAuditEnums:
    def test_audit_type(self):
        assert AuditType.TRANSACTION == "TRAN"
        assert AuditType.USER_ACTION == "USER"
        assert AuditType.SYSTEM_EVENT == "SYST"

    def test_audit_action(self):
        assert AuditAction.CREATE == "CREATE"
        assert AuditAction.UPDATE == "UPDATE"
        assert AuditAction.DELETE == "DELETE"
        assert AuditAction.INQUIRE == "INQUIRE"
        assert AuditAction.LOGIN == "LOGIN"
        assert AuditAction.LOGOUT == "LOGOUT"
        assert AuditAction.STARTUP == "STARTUP"
        assert AuditAction.SHUTDOWN == "SHUTDOWN"

    def test_audit_status(self):
        assert AuditStatus.SUCCESS == "SUCC"
        assert AuditStatus.FAILURE == "FAIL"
        assert AuditStatus.WARNING == "WARN"


class TestInvestmentTypeEnum:
    def test_values(self):
        assert InvestmentType.STOCK == "STK"
        assert InvestmentType.BOND == "BND"
        assert InvestmentType.MONEY_MARKET == "MMF"
        assert InvestmentType.ETF == "ETF"

    def test_invalid_type(self):
        with pytest.raises(ValueError):
            InvestmentType("INVALID")


class TestDeleteReasonEnum:
    def test_values(self):
        assert DeleteReason.CLOSED == "01"
        assert DeleteReason.TRANSFERRED == "02"
        assert DeleteReason.REQUESTED == "03"


class TestBatchAndCheckpointEnums:
    def test_batch_status(self):
        assert BatchStatus.READY == "R"
        assert BatchStatus.ACTIVE == "A"
        assert BatchStatus.DONE == "D"
        assert BatchStatus.ERROR == "E"

    def test_checkpoint_status(self):
        assert CheckpointStatus.INITIAL == "I"
        assert CheckpointStatus.ACTIVE == "A"
        assert CheckpointStatus.COMPLETE == "C"
        assert CheckpointStatus.FAILED == "F"
        assert CheckpointStatus.RESTARTED == "R"


# =========================================================================
# Portfolio model tests
# =========================================================================


def _valid_portfolio(**overrides) -> dict:
    """Return a valid Portfolio kwargs dict; override any key."""
    data = {
        "id": "PORT1234",
        "account_no": "0123456789",
        "client_name": "Test Client",
        "client_type": ClientType.INDIVIDUAL,
        "create_date": date(2024, 1, 1),
        "last_maint": date(2024, 6, 15),
        "status": PortfolioStatus.ACTIVE,
        "total_value": Decimal("100000.00"),
        "cash_balance": Decimal("25000.50"),
        "last_user": "ADMIN",
        "last_trans": date(2024, 6, 15),
    }
    data.update(overrides)
    return data


class TestPortfolioValidation:
    def test_valid_portfolio(self):
        p = Portfolio(**_valid_portfolio())
        assert p.id == "PORT1234"
        assert isinstance(p.total_value, Decimal)

    def test_valid_5_digit_id(self):
        p = Portfolio(**_valid_portfolio(id="PORT12345"))
        assert p.id == "PORT12345"

    def test_invalid_id_no_prefix(self):
        with pytest.raises(Exception):
            Portfolio(**_valid_portfolio(id="ABCD1234"))

    def test_invalid_id_too_few_digits(self):
        with pytest.raises(Exception):
            Portfolio(**_valid_portfolio(id="PORT123"))

    def test_invalid_id_too_many_digits(self):
        with pytest.raises(Exception):
            Portfolio(**_valid_portfolio(id="PORT123456"))

    def test_invalid_id_non_numeric_suffix(self):
        with pytest.raises(Exception):
            Portfolio(**_valid_portfolio(id="PORTABCD"))

    def test_valid_account_no(self):
        p = Portfolio(**_valid_portfolio(account_no="9999999999"))
        assert p.account_no == "9999999999"

    def test_invalid_account_no_too_short(self):
        with pytest.raises(Exception):
            Portfolio(**_valid_portfolio(account_no="12345"))

    def test_invalid_account_no_non_numeric(self):
        with pytest.raises(Exception):
            Portfolio(**_valid_portfolio(account_no="ABCDEFGHIJ"))

    def test_client_name_max_length(self):
        name_30 = "A" * 30
        p = Portfolio(**_valid_portfolio(client_name=name_30))
        assert len(p.client_name) == 30

    def test_client_name_too_long(self):
        with pytest.raises(Exception):
            Portfolio(**_valid_portfolio(client_name="A" * 31))

    def test_decimal_precision_total_value(self):
        p = Portfolio(**_valid_portfolio(total_value=Decimal("9999999999999.99")))
        assert p.total_value == Decimal("9999999999999.99")

    def test_decimal_precision_cash_balance(self):
        p = Portfolio(**_valid_portfolio(cash_balance=Decimal("0.01")))
        assert p.cash_balance == Decimal("0.01")


# =========================================================================
# Transaction model tests
# =========================================================================


def _valid_transaction(**overrides) -> dict:
    data = {
        "date": "20240615",
        "time": "143022",
        "portfolio_id": "PORT1234",
        "sequence_no": "000001",
        "investment_id": "AAPL000001",
        "trn_type": TransactionType.BUY,
        "quantity": Decimal("100.0000"),
        "price": Decimal("150.2500"),
        "amount": Decimal("15025.00"),
        "currency": "USD",
        "status": TransactionStatus.DONE,
        "process_date": datetime(2024, 6, 15, 14, 30, 22),
        "process_user": "BATCHUSR",
    }
    data.update(overrides)
    return data


class TestTransactionValidation:
    def test_valid_transaction(self):
        t = TransactionRecord(**_valid_transaction())
        assert t.quantity == Decimal("100.0000")
        assert t.price == Decimal("150.2500")
        assert t.amount == Decimal("15025.00")

    def test_invalid_date_format(self):
        with pytest.raises(Exception):
            TransactionRecord(**_valid_transaction(date="2024-06-15"))

    def test_invalid_time_format(self):
        with pytest.raises(Exception):
            TransactionRecord(**_valid_transaction(time="14:30"))

    def test_decimal_4_places_quantity(self):
        t = TransactionRecord(**_valid_transaction(quantity=Decimal("1.2345")))
        assert t.quantity == Decimal("1.2345")

    def test_decimal_4_places_price(self):
        t = TransactionRecord(**_valid_transaction(price=Decimal("999.9999")))
        assert t.price == Decimal("999.9999")


# =========================================================================
# Position model tests
# =========================================================================


def _valid_position(**overrides) -> dict:
    data = {
        "portfolio_id": "PORT1234",
        "date": "20240615",
        "investment_id": "AAPL000001",
        "quantity": Decimal("100.0000"),
        "cost_basis": Decimal("15025.00"),
        "market_value": Decimal("15500.00"),
        "currency": "USD",
        "status": PositionStatus.ACTIVE,
        "last_maint_date": datetime(2024, 6, 15, 14, 30, 22),
        "last_maint_user": "BATCHUSR",
    }
    data.update(overrides)
    return data


class TestPositionValidation:
    def test_valid_position(self):
        p = PositionRecord(**_valid_position())
        assert isinstance(p.cost_basis, Decimal)
        assert isinstance(p.market_value, Decimal)

    def test_invalid_date(self):
        with pytest.raises(Exception):
            PositionRecord(**_valid_position(date="2024-06"))


# =========================================================================
# Audit model tests
# =========================================================================


def _valid_audit(**overrides) -> dict:
    data = {
        "timestamp": datetime(2024, 6, 15, 14, 30, 22),
        "system_id": "SYS001",
        "user_id": "ADMIN",
        "program": "PORTMSTR",
        "terminal": "TERM001",
        "audit_type": AuditType.TRANSACTION,
        "action": AuditAction.CREATE,
        "status": AuditStatus.SUCCESS,
        "portfolio_id": "PORT1234",
        "account_no": "0123456789",
        "before_image": "",
        "after_image": "new record",
        "message": "Portfolio created",
    }
    data.update(overrides)
    return data


class TestAuditValidation:
    def test_valid_audit(self):
        a = AuditRecord(**_valid_audit())
        assert a.audit_type == AuditType.TRANSACTION

    def test_message_too_long(self):
        with pytest.raises(Exception):
            AuditRecord(**_valid_audit(message="X" * 101))


# =========================================================================
# Error model tests
# =========================================================================


class TestErrorSeverity:
    def test_values(self):
        assert ErrorSeverity.SUCCESS == 0
        assert ErrorSeverity.WARNING == 4
        assert ErrorSeverity.ERROR == 8
        assert ErrorSeverity.SEVERE == 12
        assert ErrorSeverity.TERMINAL == 16


class TestExceptionHierarchy:
    def test_app_error_base(self):
        e = AppError("test", severity=ErrorSeverity.ERROR, code="E001")
        assert str(e) == "test"
        assert e.severity == ErrorSeverity.ERROR
        assert e.code == "E001"

    def test_validation_error(self):
        e = ValidationError("bad data")
        assert isinstance(e, AppError)
        assert e.code == "E008"

    def test_vsam_duplicate_key(self):
        e = DuplicateKeyError()
        assert isinstance(e, VsamError)
        assert isinstance(e, AppError)
        assert e.vsam_status == "22"
        assert e.code == "E003"

    def test_vsam_not_found(self):
        e = NotFoundError()
        assert isinstance(e, VsamError)
        assert e.vsam_status == "23"
        assert e.severity == ErrorSeverity.WARNING

    def test_processing_error(self):
        e = ProcessingError("failed")
        assert isinstance(e, AppError)
        assert e.code == "E007"

    def test_system_error(self):
        e = SystemError("crash")
        assert isinstance(e, AppError)
        assert e.severity == ErrorSeverity.SEVERE


class TestErrorMessage:
    def test_defaults(self):
        msg = ErrorMessage()
        assert msg.severity == ErrorSeverity.ERROR
        assert msg.category == ErrorCategory.SYSTEM

    def test_custom(self):
        msg = ErrorMessage(
            program="TRNVAL00",
            category=ErrorCategory.VALIDATION,
            code="E008",
            severity=ErrorSeverity.WARNING,
            text="Invalid amount",
            details="Amount negative",
        )
        assert msg.program == "TRNVAL00"
        assert msg.severity == ErrorSeverity.WARNING


# =========================================================================
# Batch model tests
# =========================================================================


class TestBatchConstants:
    def test_max_prereq(self):
        assert BATCH_MAX_PREREQ == 10

    def test_max_restarts(self):
        assert BATCH_MAX_RESTARTS == 3


class TestBatchControlRecord:
    def test_valid_record(self):
        rec = BatchControlRecord(
            job_name="TRNVAL00",
            process_date="20240615",
            sequence_no=1,
            status=BatchStatus.READY,
            step_name="STEP001",
            program_name="TRNVAL00",
            start_time="14:30:00",
            end_time="",
            prereq_count=0,
            prerequisites=[],
            return_code=0,
            error_desc="",
            restart_count=0,
        )
        assert rec.status == BatchStatus.READY


# =========================================================================
# SQLAlchemy schema tests
# =========================================================================


class TestSqlAlchemySchema:
    @pytest.fixture()
    def engine(self):
        e = create_engine("sqlite:///:memory:")
        Base.metadata.create_all(e)
        return e

    def test_all_tables_created(self, engine):
        inspector = inspect(engine)
        tables = inspector.get_table_names()
        assert "portfolio_master" in tables
        assert "investment_positions" in tables
        assert "transaction_history" in tables
        assert "errlog" in tables
        assert "auditlog" in tables
        assert "authfile" in tables

    def test_portfolio_master_columns(self, engine):
        inspector = inspect(engine)
        cols = {c["name"] for c in inspector.get_columns("portfolio_master")}
        assert "portfolio_id" in cols
        assert "client_id" in cols
        assert "status" in cols
        assert "open_date" in cols
        assert "close_date" in cols

    def test_investment_positions_pk(self, engine):
        inspector = inspect(engine)
        pk = inspector.get_pk_constraint("investment_positions")
        assert set(pk["constrained_columns"]) == {
            "portfolio_id",
            "investment_id",
            "position_date",
        }

    def test_transaction_history_fk(self, engine):
        inspector = inspect(engine)
        fks = inspector.get_foreign_keys("transaction_history")
        assert len(fks) >= 1
        fk_cols = fks[0]["constrained_columns"]
        assert "portfolio_id" in fk_cols

    def test_indexes_exist(self, engine):
        inspector = inspect(engine)
        pm_indexes = {idx["name"] for idx in inspector.get_indexes("portfolio_master")}
        assert "idx_port_master_client" in pm_indexes

        ip_indexes = {idx["name"] for idx in inspector.get_indexes("investment_positions")}
        assert "idx_positions_date" in ip_indexes

        th_indexes = {idx["name"] for idx in inspector.get_indexes("transaction_history")}
        assert "idx_trans_hist_port" in th_indexes
        assert "idx_trans_hist_date" in th_indexes
