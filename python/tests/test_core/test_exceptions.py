"""Tests for the custom exception hierarchy."""

import pytest

from python.src.core.error_handling.exceptions import (
    CLBSError,
    ConnectionError,
    CursorError,
    DataError,
    DuplicateKeyError,
    ErrorCategory,
    ErrorSeverity,
    ProcessingError,
    RecordNotFoundError,
    RecoveryAction,
    SecurityError,
    SystemError,
    ValidationError,
)


class TestErrorSeverity:
    """Test ErrorSeverity enum values match COBOL ERRHAND.cpy return codes."""

    def test_severity_values(self):
        assert ErrorSeverity.WARNING.value == 4
        assert ErrorSeverity.ERROR.value == 8
        assert ErrorSeverity.SEVERE.value == 12
        assert ErrorSeverity.TERMINAL.value == 16


class TestErrorCategory:
    """Test ErrorCategory enum values match COBOL categories."""

    def test_category_values(self):
        assert ErrorCategory.VSAM.value == "VS"
        assert ErrorCategory.VALIDATION.value == "VL"
        assert ErrorCategory.PROCESSING.value == "PR"
        assert ErrorCategory.SYSTEM.value == "SY"
        assert ErrorCategory.CONNECTION.value == "CN"
        assert ErrorCategory.SECURITY.value == "SC"
        assert ErrorCategory.CURSOR.value == "CR"


class TestRecoveryAction:
    """Test RecoveryAction enum values match COBOL ERR-ACTION."""

    def test_action_values(self):
        assert RecoveryAction.RETURN.value == "R"
        assert RecoveryAction.CONTINUE.value == "C"
        assert RecoveryAction.ABEND.value == "A"


class TestCLBSError:
    """Test base CLBSError exception."""

    def test_default_construction(self):
        err = CLBSError("test error")
        assert str(err) == "test error"
        assert err.message == "test error"
        assert err.category == ErrorCategory.SYSTEM
        assert err.error_code == "0000"
        assert err.severity == ErrorSeverity.ERROR
        assert err.program == ""
        assert err.details == ""
        assert err.trace_id is None
        assert err.recovery_action == RecoveryAction.RETURN

    def test_custom_construction(self):
        err = CLBSError(
            message="custom error",
            category=ErrorCategory.VSAM,
            error_code="VS22",
            severity=ErrorSeverity.WARNING,
            program="TESTPGM",
            details="extra details",
            trace_id="abc123",
            recovery_action=RecoveryAction.CONTINUE,
        )
        assert err.message == "custom error"
        assert err.category == ErrorCategory.VSAM
        assert err.error_code == "VS22"
        assert err.severity == ErrorSeverity.WARNING
        assert err.program == "TESTPGM"
        assert err.details == "extra details"
        assert err.trace_id == "abc123"
        assert err.recovery_action == RecoveryAction.CONTINUE

    def test_to_dict(self):
        err = CLBSError(
            message="test",
            category=ErrorCategory.PROCESSING,
            error_code="PR01",
            severity=ErrorSeverity.SEVERE,
            program="PGM1",
            trace_id="trace1",
        )
        d = err.to_dict()
        assert d["message"] == "test"
        assert d["category"] == "PR"
        assert d["error_code"] == "PR01"
        assert d["severity"] == "severe"
        assert d["severity_value"] == 12
        assert d["program"] == "PGM1"
        assert d["trace_id"] == "trace1"
        assert d["recovery_action"] == "R"

    def test_is_exception(self):
        err = CLBSError("test")
        assert isinstance(err, Exception)

    def test_can_be_raised_and_caught(self):
        with pytest.raises(CLBSError) as exc_info:
            raise CLBSError("raised error")
        assert str(exc_info.value) == "raised error"


class TestDataError:
    """Test DataError and subclasses (VSAM category)."""

    def test_data_error_defaults(self):
        err = DataError("data problem")
        assert err.category == ErrorCategory.VSAM
        assert err.error_code == "VS00"
        assert err.severity == ErrorSeverity.ERROR
        assert isinstance(err, CLBSError)

    def test_duplicate_key_error(self):
        err = DuplicateKeyError()
        assert err.message == "Duplicate record key"
        assert err.error_code == "VS22"
        assert err.severity == ErrorSeverity.ERROR
        assert isinstance(err, DataError)
        assert isinstance(err, CLBSError)

    def test_duplicate_key_custom_message(self):
        err = DuplicateKeyError("Portfolio ID already exists")
        assert err.message == "Portfolio ID already exists"
        assert err.error_code == "VS22"

    def test_record_not_found_error(self):
        err = RecordNotFoundError()
        assert err.message == "Record not found"
        assert err.error_code == "VS23"
        assert err.severity == ErrorSeverity.WARNING
        assert err.recovery_action == RecoveryAction.CONTINUE
        assert isinstance(err, DataError)


class TestValidationError:
    """Test ValidationError (VL category)."""

    def test_validation_error(self):
        err = ValidationError("Invalid amount")
        assert err.category == ErrorCategory.VALIDATION
        assert err.error_code == "VL00"
        assert err.severity == ErrorSeverity.ERROR
        assert isinstance(err, CLBSError)


class TestConnectionError:
    """Test ConnectionError (CN category)."""

    def test_connection_error(self):
        err = ConnectionError("DB2 connection lost", sqlcode=-803)
        assert err.category == ErrorCategory.CONNECTION
        assert err.error_code == "CN00"
        assert err.severity == ErrorSeverity.SEVERE
        assert err.sqlcode == -803
        assert isinstance(err, CLBSError)


class TestCursorError:
    """Test CursorError (CR category)."""

    def test_cursor_error(self):
        err = CursorError(
            "Cursor position invalid",
            cursor_name="PORT_CURSOR",
            sqlcode=-501,
        )
        assert err.category == ErrorCategory.CURSOR
        assert err.severity == ErrorSeverity.WARNING
        assert err.recovery_action == RecoveryAction.CONTINUE
        assert err.cursor_name == "PORT_CURSOR"
        assert err.sqlcode == -501


class TestSecurityError:
    """Test SecurityError (SC category)."""

    def test_security_error(self):
        err = SecurityError("Access denied")
        assert err.category == ErrorCategory.SECURITY
        assert err.error_code == "SC00"
        assert err.severity == ErrorSeverity.ERROR
        assert isinstance(err, CLBSError)


class TestProcessingError:
    """Test ProcessingError (PR category)."""

    def test_processing_error(self):
        err = ProcessingError("Calculation failed")
        assert err.category == ErrorCategory.PROCESSING
        assert err.error_code == "PR00"
        assert isinstance(err, CLBSError)


class TestSystemError:
    """Test SystemError (SY category)."""

    def test_system_error(self):
        err = SystemError("System failure")
        assert err.category == ErrorCategory.SYSTEM
        assert err.error_code == "SY00"
        assert err.severity == ErrorSeverity.TERMINAL
        assert err.recovery_action == RecoveryAction.ABEND
        assert isinstance(err, CLBSError)


class TestExceptionHierarchy:
    """Test that exception hierarchy is correct for catching."""

    def test_catch_data_errors_as_clbs(self):
        with pytest.raises(CLBSError):
            raise DuplicateKeyError()

    def test_catch_duplicate_as_data(self):
        with pytest.raises(DataError):
            raise DuplicateKeyError()

    def test_catch_not_found_as_data(self):
        with pytest.raises(DataError):
            raise RecordNotFoundError()

    def test_catch_system_as_clbs(self):
        with pytest.raises(CLBSError):
            raise SystemError("fatal")

    def test_catch_security_as_clbs(self):
        with pytest.raises(CLBSError):
            raise SecurityError("denied")
